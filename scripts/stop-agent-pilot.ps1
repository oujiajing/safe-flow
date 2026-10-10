$ErrorActionPreference = 'Stop'
foreach ($port in 9090, 28080, 5173, 15666) {
  Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}
Write-Host '已停止 Agent Pilot 的本地前后端进程。Docker 中间件保持运行。'
