$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $PSScriptRoot "dpdms-processes.json"

Write-Host ""
Write-Host "========================================"
Write-Host "   DPDMS - FULL SYSTEM STARTUP"
Write-Host "========================================"
Write-Host ""

# --------------------------------------------------
# 1. Check Docker
# --------------------------------------------------

Write-Host "Checking Docker Desktop..."

docker info *> $null

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Docker Desktop is not running."
    Write-Host "Start Docker Desktop and run this script again."
    exit 1
}

Write-Host "Docker Desktop: OK"

# --------------------------------------------------
# 2. Start MySQL
# --------------------------------------------------

Write-Host ""
Write-Host "Starting DPDMS MySQL..."

docker compose -f "$root\docker-compose.yml" up -d mysql

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: DPDMS MySQL could not be started."
    exit 1
}

Write-Host "Waiting for MySQL to become healthy..."

$mysqlReady = $false

for ($i = 1; $i -le 30; $i++) {

    $health = docker inspect -f '{{.State.Health.Status}}' dpdms-mysql 2>$null

    if ($health -eq "healthy") {
        $mysqlReady = $true
        break
    }

    Start-Sleep -Seconds 2
}

if (-not $mysqlReady) {
    Write-Host ""
    Write-Host "ERROR: MySQL did not become healthy."
    Write-Host "Check: docker logs dpdms-mysql"
    exit 1
}

Write-Host "MySQL: HEALTHY"

# --------------------------------------------------
# Process tracking
# --------------------------------------------------

$startedProcesses = @()

# --------------------------------------------------
# 3. Start Eureka first
# --------------------------------------------------

Write-Host ""
Write-Host "Starting Eureka Discovery Service..."

$process = Start-Process powershell `
    -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$root'; mvn -pl discovery-service spring-boot:run"
    ) `
    -PassThru

$startedProcesses += [PSCustomObject]@{
    Name = "discovery-service"
    PID  = $process.Id
}

Start-Sleep -Seconds 10

# --------------------------------------------------
# 4. Start backend services
# --------------------------------------------------

$services = @(
    'auth-service',
    'flood-service',
    'drought-service',
    'fire-service',
    'zoonotic-service',
    'mining-service',
    'report-service',
    'alert-service',
    'dashboard-service',
    'api-gateway'
)

foreach ($service in $services) {

    Write-Host "Starting $service..."

    $process = Start-Process powershell `
        -ArgumentList @(
            '-NoExit',
            '-Command',
            "Set-Location '$root'; mvn -pl $service spring-boot:run"
        ) `
        -PassThru

    $startedProcesses += [PSCustomObject]@{
        Name = $service
        PID  = $process.Id
    }

    Start-Sleep -Seconds 2
}

# --------------------------------------------------
# 5. Start frontend
# --------------------------------------------------

Write-Host ""
Write-Host "Starting DPDMS frontend..."

$process = Start-Process powershell `
    -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$root\frontend'; npm run dev"
    ) `
    -PassThru

$startedProcesses += [PSCustomObject]@{
    Name = "frontend"
    PID  = $process.Id
}

# --------------------------------------------------
# 6. Save process IDs
# --------------------------------------------------

$startedProcesses |
    ConvertTo-Json |
    Set-Content -Path $pidFile -Encoding UTF8

# --------------------------------------------------
# 7. Finished
# --------------------------------------------------

Write-Host ""
Write-Host "========================================"
Write-Host "   DPDMS STARTUP COMMANDS SENT"
Write-Host "========================================"
Write-Host ""

Write-Host "Frontend: http://localhost:5173"
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Eureka:   http://localhost:8761"
Write-Host ""

Write-Host "Process tracking file:"
Write-Host $pidFile
Write-Host ""

Write-Host "Wait for all services to finish starting."
Write-Host ""