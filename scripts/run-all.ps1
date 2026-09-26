$root = Split-Path -Parent $PSScriptRoot
Write-Host "Starting MySQL..."
docker compose -f "$root\docker-compose.yml" up -d mysql
Write-Host "Starting Eureka first..."
Start-Process powershell -ArgumentList '-NoExit','-Command',"Set-Location '$root'; mvn -pl discovery-service spring-boot:run"
Start-Sleep -Seconds 8
$services = @('auth-service','flood-service','drought-service','fire-service','zoonotic-service','mining-service','report-service','alert-service','dashboard-service','api-gateway')
foreach($service in $services){
  Start-Process powershell -ArgumentList '-NoExit','-Command',"Set-Location '$root'; mvn -pl $service spring-boot:run"
}
Write-Host "Backend terminals started. In another terminal: cd frontend; npm install; npm run dev"
