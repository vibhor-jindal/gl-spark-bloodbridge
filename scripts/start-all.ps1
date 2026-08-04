<#
.SYNOPSIS
  Starts Kafka (Zookeeper + broker), all BloodBridge microservices, API Gateway, and the frontend.

.PARAMETER SkipKafka
  Skip starting Zookeeper/Kafka (use if they are already running).

.PARAMETER SkipFrontend
  Skip starting the React Vite app.

.PARAMETER KafkaHome
  Path to the Kafka install directory (folder that contains bin\windows\).

.PARAMETER DbPassword
  PostgreSQL password for user postgres. Defaults to env DB_PASSWORD or "postgres".
#>
[CmdletBinding()]
param(
    [switch]$SkipKafka,
    [switch]$SkipFrontend,
    [string]$KafkaHome,
    [string]$DbPassword
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $Root "pom.xml"))) {
    throw "Could not find bloodbridge root (expected pom.xml under $Root)."
}

# Optional local overrides (gitignored): scripts\local.ps1
$localConfig = Join-Path $PSScriptRoot "local.ps1"
if (Test-Path $localConfig) {
    . $localConfig
    Write-Host "Loaded scripts\local.ps1" -ForegroundColor DarkGray
}

if (-not $KafkaHome) {
    $KafkaHome = $env:KAFKA_HOME
}
if (-not $DbPassword) {
    $DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }
}

function Assert-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "'$Name' is not on PATH. Install it / add it to PATH, then re-run."
    }
}

function Start-LoggedWindow {
    param(
        [Parameter(Mandatory)][string]$Title,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$Command
    )

    $tmp = Join-Path $env:TEMP ("bloodbridge-start-" + [guid]::NewGuid().ToString() + ".ps1")
    $script = @(
        "`$Host.UI.RawUI.WindowTitle = '$($Title.Replace("'","''"))'"
        "Set-Location -LiteralPath '$($WorkingDirectory.Replace("'","''"))'"
        "Write-Host '=== $Title ===' -ForegroundColor Cyan"
        "Write-Host ''"
        $Command
        "Write-Host ''"
        "Write-Host 'Process exited. Press Enter to close.' -ForegroundColor Yellow"
        "Read-Host | Out-Null"
    ) -join "`r`n"

    Set-Content -Path $tmp -Value $script -Encoding UTF8
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-File", $tmp
    ) | Out-Null
    Write-Host "  started: $Title" -ForegroundColor Green
}

function Resolve-KafkaHome {
    param([string]$Hint)
    $candidates = @()
    if ($Hint) { $candidates += $Hint }
    $candidates += @(
        $env:KAFKA_HOME,
        "C:\kafka",
        "C:\Apache\kafka",
        "C:\tools\kafka",
        (Join-Path $env:USERPROFILE "kafka"),
        (Join-Path $env:USERPROFILE "Downloads\kafka")
    ) | Where-Object { $_ }

    foreach ($c in $candidates) {
        $zk = Join-Path $c "bin\windows\zookeeper-server-start.bat"
        if (Test-Path $zk) { return (Resolve-Path $c).Path }
        if (Test-Path $c) {
            $nested = Get-ChildItem -Path $c -Directory -Filter "kafka_*" -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending |
                Select-Object -First 1
            if ($nested) {
                $zkNested = Join-Path $nested.FullName "bin\windows\zookeeper-server-start.bat"
                if (Test-Path $zkNested) { return $nested.FullName }
            }
        }
    }
    return $null
}

Write-Host ""
Write-Host "BloodBridge - starting stack" -ForegroundColor Cyan
Write-Host "Root: $Root"
Write-Host ""

Assert-Command "mvn"
Assert-Command "java"
if (-not $SkipFrontend) { Assert-Command "npm" }

if (-not $SkipKafka) {
    $resolvedKafka = Resolve-KafkaHome -Hint $KafkaHome
    if (-not $resolvedKafka) {
        Write-Host "Kafka not found." -ForegroundColor Yellow
        Write-Host "  Set KAFKA_HOME or pass -KafkaHome, or use -SkipKafka if already running." -ForegroundColor Yellow
        throw "KAFKA_HOME not set / Kafka install not found."
    }

    Write-Host "Kafka home: $resolvedKafka" -ForegroundColor DarkGray
    Start-LoggedWindow -Title "BloodBridge - Zookeeper" -WorkingDirectory $resolvedKafka -Command `
        ".\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties"
    Start-Sleep -Seconds 8
    Start-LoggedWindow -Title "BloodBridge - Kafka" -WorkingDirectory $resolvedKafka -Command `
        ".\bin\windows\kafka-server-start.bat .\config\server.properties"
    Start-Sleep -Seconds 10
} else {
    Write-Host "Skipping Kafka (-SkipKafka)." -ForegroundColor DarkGray
}

$services = @(
    @{ Name = "Eureka Server";     Dir = "eureka-server";        Wait = 25 },
    @{ Name = "Auth Service";      Dir = "auth-service";         Wait = 12 },
    @{ Name = "Donor Service";     Dir = "donor-service";        Wait = 8 },
    @{ Name = "Request Service";   Dir = "request-service";      Wait = 8 },
    @{ Name = "Matching Service";  Dir = "matching-service";     Wait = 8 },
    @{ Name = "Notification Svc";  Dir = "notification-service"; Wait = 8 },
    @{ Name = "Inventory Service"; Dir = "inventory-service";    Wait = 8 },
    @{ Name = "Analytics Service"; Dir = "analytics-service";    Wait = 8 },
    @{ Name = "Rewards Service";   Dir = "rewards-service";      Wait = 8 },
    @{ Name = "API Gateway";       Dir = "api-gateway";          Wait = 15 }
)

Write-Host ""
Write-Host "Starting Java microservices (DB_PASSWORD set)..." -ForegroundColor Cyan
$pwLiteral = $DbPassword.Replace("'", "''")
foreach ($svc in $services) {
    $dir = Join-Path $Root $svc.Dir
    if (-not (Test-Path $dir)) { throw "Missing module directory: $dir" }
    $bootCmd = @(
        "`$env:DB_USER = 'postgres'"
        "`$env:DB_PASSWORD = '$pwLiteral'"
        "`$env:DB_HOST = 'localhost'"
        "`$env:DB_PORT = '5432'"
        "mvn spring-boot:run `"-DskipTests`""
    ) -join "`r`n"
    Start-LoggedWindow -Title ("BloodBridge - " + $svc.Name) -WorkingDirectory $dir -Command $bootCmd
    Start-Sleep -Seconds $svc.Wait
}

if (-not $SkipFrontend) {
    Write-Host ""
    Write-Host "Starting frontend..." -ForegroundColor Cyan
    $frontend = Join-Path $Root "frontend"
    $envFile = Join-Path $frontend ".env"
    $envExample = Join-Path $frontend ".env.example"
    if (-not (Test-Path $envFile) -and (Test-Path $envExample)) {
        Copy-Item $envExample $envFile
        Write-Host "  created frontend\.env from .env.example" -ForegroundColor DarkGray
    }
    $nodeModules = Join-Path $frontend "node_modules"
    if (-not (Test-Path $nodeModules)) {
        $frontendCmd = "npm install; if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }; npm run dev"
    } else {
        $frontendCmd = "npm run dev"
    }
    Start-LoggedWindow -Title "BloodBridge - Frontend" -WorkingDirectory $frontend -Command $frontendCmd
}

Write-Host ""
Write-Host "All start commands launched." -ForegroundColor Green
Write-Host ""
Write-Host "URLs (once healthy):"
Write-Host "  Eureka     http://localhost:8761"
Write-Host "  Gateway    http://localhost:8080"
Write-Host "  Frontend   http://localhost:3000"
Write-Host "  Kafka      localhost:9092"
Write-Host ""
Write-Host "Stop everything:  .\scripts\stop-all.ps1"
