$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $PSScriptRoot "dpdms-processes.json"

Write-Host ""
Write-Host "========================================"
Write-Host "   DPDMS - FULL SYSTEM SHUTDOWN"
Write-Host "========================================"
Write-Host ""

# --------------------------------------------------
# 1. Stop processes tracked by run-all.ps1
# --------------------------------------------------

if (Test-Path $pidFile) {

    Write-Host "Stopping tracked DPDMS processes..."
    Write-Host ""

    try {

        $processes = Get-Content $pidFile -Raw | ConvertFrom-Json

        foreach ($process in $processes) {

            $pid = [int]$process.PID
            $name = $process.Name

            $existingProcess = Get-Process -Id $pid -ErrorAction SilentlyContinue

            if ($existingProcess) {

                Write-Host "Stopping $name (PID $pid)..."

                taskkill /PID $pid /T /F *> $null

                if ($LASTEXITCODE -eq 0) {
                    Write-Host "$name stopped."
                }
                else {
                    Write-Host "$name could not be stopped automatically."
                }

            }
            else {

                Write-Host "$name (PID $pid) is already stopped."

            }
        }

    }
    catch {

        Write-Host "WARNING: Could not read the DPDMS process tracking file."
        Write-Host $_

    }

    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

}
else {

    Write-Host "No DPDMS process tracking file found."
    Write-Host "Checking for remaining DPDMS processes..."

}

# --------------------------------------------------
# 2. Safety cleanup for remaining DPDMS Java processes
# --------------------------------------------------

Write-Host ""
Write-Host "Checking for remaining DPDMS backend processes..."

$javaProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {

        $_.Name -eq "java.exe" -and
        $_.CommandLine -and
        (
            $_.CommandLine -like "*$root*" -or
            $_.CommandLine -like "*dpdms-complete*"
        )

    }

foreach ($process in $javaProcesses) {

    Write-Host "Stopping remaining Java process PID $($process.ProcessId)..."

    taskkill /PID $process.ProcessId /T /F *> $null

}

# --------------------------------------------------
# 3. Safety cleanup for remaining DPDMS Node processes
# --------------------------------------------------

Write-Host ""
Write-Host "Checking for remaining DPDMS frontend processes..."

$nodeProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {

        $_.Name -eq "node.exe" -and
        $_.CommandLine -and
        (
            $_.CommandLine -like "*$root*" -or
            $_.CommandLine -like "*dpdms-complete*"
        )

    }

foreach ($process in $nodeProcesses) {

    Write-Host "Stopping remaining Node process PID $($process.ProcessId)..."

    taskkill /PID $process.ProcessId /T /F *> $null

}

# --------------------------------------------------
# 4. Stop DPDMS MySQL Docker container
# --------------------------------------------------

Write-Host ""
Write-Host "Stopping DPDMS MySQL container..."

docker compose -f "$root\docker-compose.yml" stop mysql

# --------------------------------------------------
# 5. Final status
# --------------------------------------------------

Write-Host ""
Write-Host "========================================"
Write-Host "   DPDMS SHUTDOWN COMPLETE"
Write-Host "========================================"
Write-Host ""

Write-Host "DPDMS backend services : STOPPED"
Write-Host "DPDMS frontend         : STOPPED"
Write-Host "DPDMS MySQL container  : STOPPED"
Write-Host ""

Write-Host "Docker Desktop itself was NOT closed."
Write-Host "Windows MySQL80 service was NOT changed."
Write-Host ""

Write-Host "All DPDMS processes started by the startup script"
Write-Host "have been targeted for shutdown."
Write-Host ""

Write-Host "You can now close this PowerShell window."
Write-Host ""