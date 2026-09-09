param(
  [switch]$ConfirmDemo,
  [string]$Username = "portfolio-admin"
)

$ErrorActionPreference = "Stop"
if (-not $ConfirmDemo) { throw "Pass -ConfirmDemo to initialize only the local Portfolio Demo database." }
$dbUrl = if ($env:PINGAN_DB_URL) { $env:PINGAN_DB_URL } else { "jdbc:postgresql://localhost:15433/demo_safeteam" }
$dbUser = if ($env:PINGAN_DB_USERNAME) { $env:PINGAN_DB_USERNAME } else { "pingan" }
$dbPassword = if ($env:PINGAN_DB_PASSWORD) { $env:PINGAN_DB_PASSWORD } else { "" }
if ($dbUrl -notmatch "localhost|127\.0\.0\.1" -or $dbUrl -notmatch "demo_safeteam") { throw "Refusing non-local or non-Demo database: $dbUrl" }
if ([string]::IsNullOrWhiteSpace($dbPassword)) { throw "Set PINGAN_DB_PASSWORD for the local Demo database." }

$chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*"
$bytes = New-Object byte[] 24
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$password = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })

$securityJar = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\security\spring-security-crypto\*\spring-security-crypto-*.jar" | Sort-Object LastWriteTime | Select-Object -Last 1
$postgresJar = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql\*\postgresql-*.jar" | Sort-Object LastWriteTime | Select-Object -Last 1
$loggingJar = Get-ChildItem "$env:USERPROFILE\.m2\repository\commons-logging\commons-logging\*\commons-logging-*.jar" | Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $securityJar -or -not $postgresJar -or -not $loggingJar) { throw "Run the backend Maven build first so BCrypt, PostgreSQL JDBC and commons-logging jars are available." }
$temp = Join-Path $env:TEMP ("safeteam-demo-cli-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $temp | Out-Null
$source = Join-Path $PSScriptRoot "DemoAccountCli.java"
javac -cp "$($securityJar.FullName);$($postgresJar.FullName);$($loggingJar.FullName)" -d $temp $source
$env:SAFE_DEMO_DB_PASSWORD = $dbPassword
$env:SAFE_DEMO_PASSWORD = $password
$result = & java -cp "$temp;$($securityJar.FullName);$($postgresJar.FullName);$($loggingJar.FullName)" DemoAccountCli init $Username $dbUrl $dbUser
if ($LASTEXITCODE -ne 0) { throw "Demo account initialization failed; no password was displayed as a valid credential." }
if ($result -eq "EXISTS") { Write-Output "Account already exists; password was not changed or displayed."; exit 0 }
Write-Output "Created local Demo account: $Username"
Write-Output "Generated password (displayed once): $password"
