param(
  [switch]$ResetDatabasePassword,
  [string]$DatabaseName
)

$ErrorActionPreference = 'Stop'
$safeFlowRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$agentRoot = 'D:\1-project\safeguard-agent'
$runtimeFile = Join-Path $safeFlowRoot '.env.agent-pilot.local'
$java = (Get-Command java.exe -ErrorAction Stop).Source
$node = (Get-Command node.exe -ErrorAction Stop).Source

function Stop-Port([int]$Port) {
  Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

function Container-Env([string]$Container, [string]$Name) {
  $line = docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' $Container |
    Where-Object { $_ -like "$Name=*" } | Select-Object -First 1
  if (-not $line) { throw "容器 $Container 未配置 $Name" }
  return $line.Substring($Name.Length + 1)
}

function Container-Port([string]$Container) {
  $line = docker port $Container '5432/tcp' | Select-Object -First 1
  if ($line -notmatch ':(\d+)$') { throw "无法读取 $Container 的 PostgreSQL 映射端口" }
  return $Matches[1]
}

function Read-EnvFile([string]$Path) {
  $values = @{}
  if (Test-Path -LiteralPath $Path) {
    Get-Content -LiteralPath $Path | ForEach-Object {
      if ($_ -match '^([^#=]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
  }
  return $values
}

$dbContainer = 'safeteam-portfolio-postgres'
$dbUser = Container-Env $dbContainer 'POSTGRES_USER'
$dbSuperPassword = Container-Env $dbContainer 'POSTGRES_PASSWORD'
$dbPort = Container-Port $dbContainer
$agentDbPassword = Container-Env 'safeguard-postgres' 'POSTGRES_PASSWORD'
$rustfsAccessKey = Container-Env 'safeguard-rustfs' 'RUSTFS_ACCESS_KEY'
$rustfsSecretKey = Container-Env 'safeguard-rustfs' 'RUSTFS_SECRET_KEY'
$redisPassword = docker inspect --format '{{index .Config.Cmd 4}}' safeguard-redis-dev
$runtime = Read-EnvFile $runtimeFile
$dbName = if ($DatabaseName) { $DatabaseName } elseif ($runtime['PINGAN_DB_NAME']) { $runtime['PINGAN_DB_NAME'] } else { Container-Env $dbContainer 'POSTGRES_DB' }

if ($ResetDatabasePassword -or -not $runtime['PINGAN_DB_PASSWORD']) {
  $runtime['PINGAN_DB_PASSWORD'] = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')).Substring(0, 48)
  docker exec $dbContainer psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1 -c "ALTER ROLE `"$dbUser`" PASSWORD '$($runtime['PINGAN_DB_PASSWORD'])';" | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "无法重置 $dbContainer 的应用账号密码；本地数据库角色与容器配置不一致。" }
}

if (-not $runtime['SAFEGUARD_SERVICE_TOKEN']) { $runtime['SAFEGUARD_SERVICE_TOKEN'] = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N') }
if (-not $runtime['PINGAN_JWT_SECRET']) { $runtime['PINGAN_JWT_SECRET'] = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N') }
$runtime['PINGAN_DB_USERNAME'] = $dbUser
$runtime['PINGAN_DB_NAME'] = $dbName
$runtime['PINGAN_DB_URL'] = "jdbc:postgresql://127.0.0.1:$dbPort/$dbName"
$runtime['SAFEGUARD_POSTGRES_PASSWORD'] = $agentDbPassword
$runtime['SAFEGUARD_REDIS_PASSWORD'] = $redisPassword
$runtime['SAFEGUARD_RUSTFS_ACCESS_KEY_ID'] = $rustfsAccessKey
$runtime['SAFEGUARD_RUSTFS_SECRET_ACCESS_KEY'] = $rustfsSecretKey

$content = ($runtime.Keys | Sort-Object | ForEach-Object { "$_=$($runtime[$_])" }) -join [Environment]::NewLine
[System.IO.File]::WriteAllText($runtimeFile, $content + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

Stop-Port 9090; Stop-Port 28080; Stop-Port 5173; Stop-Port 15666

docker start safeguard-ollama 2>$null | Out-Null
$ollamaDeadline = [DateTime]::UtcNow.AddSeconds(45)
do {
  try {
    $ollamaReady = (Invoke-RestMethod -Uri 'http://127.0.0.1:11434/api/tags' -TimeoutSec 3).models.Count -gt 0
  } catch { $ollamaReady = $false }
  if (-not $ollamaReady) { Start-Sleep -Seconds 2 }
} while (-not $ollamaReady -and [DateTime]::UtcNow -lt $ollamaDeadline)
if (-not $ollamaReady) { throw 'Ollama 未就绪，无法启动视觉研判服务。' }

Push-Location $agentRoot
try { & .\mvnw.cmd -q -pl bootstrap -am -DskipTests package } finally { Pop-Location }
& (Join-Path $safeFlowRoot 'mvnw.cmd') -q -f (Join-Path $safeFlowRoot 'backend\pom.xml') -DskipTests package

$agentEnv = @{ SAFEGUARD_SERVICE_TOKEN=$runtime['SAFEGUARD_SERVICE_TOKEN']; SAFEGUARD_DEMO_MODE='false'; SAFEGUARD_POSTGRES_PASSWORD=$runtime['SAFEGUARD_POSTGRES_PASSWORD']; SAFEGUARD_REDIS_PASSWORD=$runtime['SAFEGUARD_REDIS_PASSWORD']; SAFEGUARD_RUSTFS_ACCESS_KEY_ID=$runtime['SAFEGUARD_RUSTFS_ACCESS_KEY_ID']; SAFEGUARD_RUSTFS_SECRET_ACCESS_KEY=$runtime['SAFEGUARD_RUSTFS_SECRET_ACCESS_KEY'] }
$safeFlowEnv = @{ PINGAN_DB_USERNAME=$runtime['PINGAN_DB_USERNAME']; PINGAN_DB_PASSWORD=$runtime['PINGAN_DB_PASSWORD']; PINGAN_DB_URL=$runtime['PINGAN_DB_URL']; PINGAN_JWT_SECRET=$runtime['PINGAN_JWT_SECRET']; SAFEGUARD_SERVICE_TOKEN=$runtime['SAFEGUARD_SERVICE_TOKEN']; PINGAN_SAFEGUARD_AGENT_ENABLED='true'; PINGAN_SAFEGUARD_AGENT_ASSESSMENT_ENABLED='true'; PINGAN_SAFEGUARD_AGENT_REVIEW_DRAFT_ENABLED='true'; PINGAN_SAFEGUARD_AGENT_WRITEBACK_ENABLED='false' }

foreach ($key in $agentEnv.Keys) { [Environment]::SetEnvironmentVariable($key, $agentEnv[$key], 'Process') }
Start-Process -FilePath $java -ArgumentList '-jar',(Join-Path $agentRoot 'bootstrap\target\bootstrap-0.0.1-SNAPSHOT.jar'),'--spring.profiles.active=local','--server.port=9090' -WorkingDirectory $agentRoot -WindowStyle Hidden | Out-Null
foreach ($key in $safeFlowEnv.Keys) { [Environment]::SetEnvironmentVariable($key, $safeFlowEnv[$key], 'Process') }
Start-Process -FilePath $java -ArgumentList '-jar',(Join-Path $safeFlowRoot 'backend\target\banzu-backend-0.1.0-SNAPSHOT.jar'),'--spring.profiles.active=local','--server.port=28080' -WorkingDirectory (Join-Path $safeFlowRoot 'backend') -WindowStyle Hidden | Out-Null
Start-Process -FilePath $node -ArgumentList (Join-Path $agentRoot 'frontend\node_modules\vite\bin\vite.js'),'--host','127.0.0.1','--port','5173','--strictPort' -WorkingDirectory (Join-Path $agentRoot 'frontend') -WindowStyle Hidden | Out-Null
Start-Process -FilePath $node -ArgumentList (Join-Path $safeFlowRoot 'vben\node_modules\vite\bin\vite.js'),'--host','127.0.0.1','--port','15666','--strictPort' -WorkingDirectory (Join-Path $safeFlowRoot 'vben\apps\web-antd') -WindowStyle Hidden | Out-Null

Write-Host '已启动：safeguard-agent 9090/5173，safe-flow 28080/15666；AI 写回保持关闭。'
