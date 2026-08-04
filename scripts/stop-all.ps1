<#
.SYNOPSIS
  Stops BloodBridge Java services, frontend (Vite/Node), and optionally Kafka/Zookeeper.

.PARAMETER IncludeKafka
  Also stop Kafka broker and Zookeeper processes.

.EXAMPLE
  .\scripts\stop-all.ps1
  .\scripts\stop-all.ps1 -IncludeKafka
#>
[CmdletBinding()]
param(
    [switch]$IncludeKafka
)

$ErrorActionPreference = "Continue"

function Stop-MatchingProcesses {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][scriptblock]$Predicate
    )
    $targets = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object $Predicate
    if (-not $targets) {
        Write-Host "  (none) $Label" -ForegroundColor DarkGray
        return
    }
    foreach ($p in $targets) {
        try {
            Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop
            Write-Host "  stopped PID $($p.ProcessId): $Label" -ForegroundColor Yellow
        } catch {
            Write-Host "  failed PID $($p.ProcessId): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "BloodBridge — stopping stack" -ForegroundColor Cyan
Write-Host ""

# Spring Boot apps launched via `mvn spring-boot:run` (Maven wrapper process)
Stop-MatchingProcesses -Label "Maven spring-boot:run" -Predicate {
    $_.Name -match '^(java|javaw|mvn|mvn\.cmd)$' -and
    $_.CommandLine -and
    ($_.CommandLine -match 'spring-boot:run' -or $_.CommandLine -match 'bloodbridge')
}

# Also catch fat-jar / Boot apps by main class package if they were started differently
Stop-MatchingProcesses -Label "BloodBridge JVM" -Predicate {
    $_.Name -match '^(java|javaw)$' -and
    $_.CommandLine -and
    $_.CommandLine -match 'com\.globallogic\.bloodbridge'
}

# Vite / frontend
Stop-MatchingProcesses -Label "Vite frontend" -Predicate {
    $_.Name -match '^(node|node\.exe)$' -and
    $_.CommandLine -and
    ($_.CommandLine -match 'vite' -or $_.CommandLine -match 'bloodbridge[\\/]frontend')
}

if ($IncludeKafka) {
    Stop-MatchingProcesses -Label "Kafka" -Predicate {
        $_.CommandLine -and
        ($_.CommandLine -match 'kafka\.Kafka' -or $_.CommandLine -match 'kafka-server-start')
    }
    Stop-MatchingProcesses -Label "Zookeeper" -Predicate {
        $_.CommandLine -and
        ($_.CommandLine -match 'zookeeper\.QuorumPeerMain' -or $_.CommandLine -match 'zookeeper-server-start')
    }
} else {
    Write-Host "  Kafka/Zookeeper left running (pass -IncludeKafka to stop them)." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
