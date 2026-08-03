param(
    [Parameter(Position = 0)]
    [string]$Mode = "all"
)

function Show-Usage {
    [Console]::Error.WriteLine("Usage: scripts/verify.ps1 [all|backend|frontend|load]")
}

if ($args.Count -gt 0) {
    Show-Usage
    exit 2
}

$Mode = $Mode.ToLowerInvariant()
if ($Mode -notin @("all", "backend", "frontend", "load")) {
    Show-Usage
    exit 2
}

if ($null -eq (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker is required to run container verification."
    exit 127
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot

$ReportRoot = $env:RECONX_VERIFY_REPORT_DIR
if ([string]::IsNullOrWhiteSpace($ReportRoot)) {
    $ReportRoot = Join-Path $RepoRoot ".verification-reports"
}
try {
    New-Item -ItemType Directory -Path $ReportRoot -Force -ErrorAction Stop | Out-Null
    $ReportRoot = (Resolve-Path -LiteralPath $ReportRoot -ErrorAction Stop).Path
} catch {
    Write-Error "Unable to create report directory '$ReportRoot': $($_.Exception.Message)"
    exit 1
}

$Separator = [System.IO.Path]::DirectorySeparatorChar
$TrimChars = [char[]]@(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar
)
$RepoPrefix = $RepoRoot.TrimEnd($TrimChars) + $Separator
$ReportPrefix = $ReportRoot.TrimEnd($TrimChars) + $Separator
if ($RepoPrefix.StartsWith($ReportPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    Write-Error "Refusing report directory that contains the repository: $ReportRoot"
    exit 2
}

$BackendReportsPath = $env:RECONX_BACKEND_REPORTS_PATH
if ([string]::IsNullOrWhiteSpace($BackendReportsPath)) {
    $BackendReportsPath = "/workspace/backend/target"
}

$FrontendReportsPath = $env:RECONX_FRONTEND_REPORTS_PATH
if ([string]::IsNullOrWhiteSpace($FrontendReportsPath)) {
    $FrontendReportsPath = "/app/test-results"
}

Remove-Item -LiteralPath (Join-Path $ReportRoot "backend") -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath (Join-Path $ReportRoot "frontend") -Recurse -Force -ErrorAction SilentlyContinue

function Invoke-Compose {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & docker compose @Arguments 2>&1 | ForEach-Object { Write-Host $_ }
    return [int]$LASTEXITCODE
}

function Copy-Reports {
    param(
        [string]$Service,
        [string]$SourcePath,
        [string]$Destination
    )

    try {
        New-Item -ItemType Directory -Path $Destination -Force -ErrorAction Stop | Out-Null
    } catch {
        Write-Warning "Unable to create report destination '$Destination': $($_.Exception.Message)"
        return 1
    }
    $ContainerPath = "{0}:{1}/." -f $Service, $SourcePath
    $CopyStatus = Invoke-Compose -Arguments @("cp", $ContainerPath, $Destination)
    if ($CopyStatus -eq 0) {
        Write-Host "$Service reports copied to $Destination"
        return 0
    } else {
        Write-Warning "No reports copied from $Service`:$SourcePath (the test may have failed before producing them)."
        return 1
    }
}

function Invoke-TestService {
    param(
        [string]$Service,
        [string]$SourcePath,
        [string]$Destination,
        [string[]]$CleanupServices
    )

    Write-Host ""
    Write-Host "==> Running $Service"
    $TestStatus = 1
    $CopyStatus = 1
    $CleanupStatus = 1
    try {
        $TestStatus = Invoke-Compose -Arguments @("up", "--build", "--force-recreate", "--abort-on-container-exit", "--exit-code-from", $Service, $Service)

        # `up` is deliberately not given `--rm`: the stopped one-shot container
        # is needed by `cp` below.
        Write-Host ""
        Write-Host "==> Preserving $Service reports"
        $CopyStatus = Copy-Reports -Service $Service -SourcePath $SourcePath -Destination $Destination
    } finally {
        $CleanupStatus = Invoke-Compose -Arguments (@("rm", "--force", "--stop") + $CleanupServices)
        if ($CleanupStatus -ne 0) {
            Write-Warning "Unable to remove one or more test service containers: $($CleanupServices -join ', ')"
        } else {
            Write-Host "Removed test service containers: $($CleanupServices -join ', ')"
        }
    }

    if ($TestStatus -ne 0) {
        return $TestStatus
    }
    if ($CopyStatus -ne 0) {
        return $CopyStatus
    }
    return $CleanupStatus
}

function Invoke-Load {
    $ProjectName = "reconx-adv097-{0}-{1}" -f [DateTimeOffset]::UtcNow.ToString("yyyyMMddHHmmss"), $PID
    $ComposeFile = Join-Path $RepoRoot "docker-compose.load.yml"
    $LoadArtifactDir = $env:RECONX_LOAD_ARTIFACT_DIR
    if ([string]::IsNullOrWhiteSpace($LoadArtifactDir)) {
        $LoadArtifactDir = Join-Path $RepoRoot ".verification-reports/load"
    }

    try {
        New-Item -ItemType Directory -Path $LoadArtifactDir -Force -ErrorAction Stop | Out-Null
        $LoadArtifactDir = (Resolve-Path -LiteralPath $LoadArtifactDir -ErrorAction Stop).Path
    } catch {
        Write-Error "Unable to create load artifact directory '$LoadArtifactDir': $($_.Exception.Message)"
        return 1
    }

    $RepoPrefix = $RepoRoot.TrimEnd($TrimChars) + $Separator
    $LoadPrefix = $LoadArtifactDir.TrimEnd($TrimChars) + $Separator
    if ($RepoPrefix.StartsWith($LoadPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-Error "Refusing load artifact directory that contains the repository: $LoadArtifactDir"
        return 2
    }

    $env:RECONX_LOAD_ARTIFACT_DIR = $LoadArtifactDir
    @(
        "k6-summary.json",
        "k6-raw-summary.json",
        "prometheus-query-evidence.json",
        "load-run-metadata.json",
        "compose.log"
    ) | ForEach-Object {
        Remove-Item -LiteralPath (Join-Path $LoadArtifactDir $_) -Force -ErrorAction SilentlyContinue
    }

    $ComposeArguments = @("-f", $ComposeFile, "-p", $ProjectName)
    $LoadStatus = 0
    $CleanupStatus = 0
    try {
        Write-Host ""
        Write-Host "==> Starting isolated ADV097 Compose project $ProjectName"
        $LoadStatus = Invoke-Compose -Arguments ($ComposeArguments + @(
            "up", "--build", "--force-recreate", "--detach",
            "load-postgres", "load-zookeeper", "load-kafka", "load-backend", "load-prometheus", "load-grafana"
        ))

        $GrafanaUrl = ""
        $PrometheusUrl = ""
        if ($LoadStatus -eq 0) {
            $GrafanaPort = (& docker compose @ComposeArguments port load-grafana 3000 2>$null | Select-Object -First 1)
            $PrometheusPort = (& docker compose @ComposeArguments port load-prometheus 9090 2>$null | Select-Object -First 1)
            if (-not [string]::IsNullOrWhiteSpace($GrafanaPort)) {
                $GrafanaUrl = "http://127.0.0.1:{0}" -f (($GrafanaPort -split ":")[-1]).Trim()
            }
            if (-not [string]::IsNullOrWhiteSpace($PrometheusPort)) {
                $PrometheusUrl = "http://127.0.0.1:{0}" -f (($PrometheusPort -split ":")[-1]).Trim()
            }
            [ordered]@{
                ticket = "TICKET-ADV097"
                composeProject = $ProjectName
                grafanaUrl = $GrafanaUrl
                prometheusUrl = $PrometheusUrl
                cleanupCommand = "docker compose -f docker-compose.load.yml -p $ProjectName down --volumes --remove-orphans"
            } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $LoadArtifactDir "load-run-metadata.json")
            Write-Host "Grafana observation URL: $GrafanaUrl"

            Write-Host ""
            Write-Host "==> Running pinned Grafana k6 load generator"
            $LoadStatus = Invoke-Compose -Arguments ($ComposeArguments + @("run", "--rm", "load-k6"))
        }

        if ($LoadStatus -eq 0) {
            Write-Host ""
            Write-Host "==> Capturing Prometheus panel-query evidence"
            $LoadStatus = Invoke-Compose -Arguments ($ComposeArguments + @("run", "--rm", "load-query"))
        }

        (& docker compose @ComposeArguments logs --no-color load-backend load-prometheus load-grafana 2>&1) |
            Set-Content -LiteralPath (Join-Path $LoadArtifactDir "compose.log")
    } finally {
        if ($env:RECONX_LOAD_KEEP_STACK -eq "1") {
            Write-Host ""
            Write-Host "Keeping isolated Compose project $ProjectName for Grafana observation."
        } else {
            $CleanupStatus = Invoke-Compose -Arguments ($ComposeArguments + @("down", "--volumes", "--remove-orphans"))
        }
    }

    if ($LoadStatus -ne 0) {
        Write-Error "ADV097 load verification failed (status=$LoadStatus). Artifacts: $LoadArtifactDir"
        return 1
    }
    if ($CleanupStatus -ne 0) {
        Write-Error "ADV097 cleanup failed (status=$CleanupStatus). Project: $ProjectName"
        return 1
    }
    Write-Host "ADV097 load verification passed. Artifacts: $LoadArtifactDir"
    return 0
}

$BackendStatus = 0
$FrontendStatus = 0
$LoadStatus = 0

switch ($Mode) {
    "all" {
        $BackendStatus = Invoke-TestService `
            -Service "test-backend" `
            -SourcePath $BackendReportsPath `
            -Destination (Join-Path $ReportRoot "backend/target") `
            -CleanupServices @("test-backend", "test-postgres")
        $FrontendStatus = Invoke-TestService `
            -Service "test-frontend" `
            -SourcePath $FrontendReportsPath `
            -Destination (Join-Path $ReportRoot "frontend/test-results") `
            -CleanupServices @("test-frontend")
    }
    "backend" {
        $BackendStatus = Invoke-TestService `
            -Service "test-backend" `
            -SourcePath $BackendReportsPath `
            -Destination (Join-Path $ReportRoot "backend/target") `
            -CleanupServices @("test-backend", "test-postgres")
    }
    "frontend" {
        $FrontendStatus = Invoke-TestService `
            -Service "test-frontend" `
            -SourcePath $FrontendReportsPath `
            -Destination (Join-Path $ReportRoot "frontend/test-results") `
            -CleanupServices @("test-frontend")
    }
    "load" {
        $LoadStatus = Invoke-Load
    }
}

if ($BackendStatus -ne 0 -or $FrontendStatus -ne 0 -or $LoadStatus -ne 0) {
    Write-Error "Container verification failed (backend=$BackendStatus, frontend=$FrontendStatus, load=$LoadStatus)."
    exit 1
}

Write-Host ""
Write-Host "Container verification passed. Reports: $ReportRoot"
