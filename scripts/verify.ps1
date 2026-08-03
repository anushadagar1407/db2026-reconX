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
    $ReportRootFullPath = [System.IO.Path]::GetFullPath($ReportRoot, $RepoRoot)
} catch {
    Write-Error "Invalid report directory '$ReportRoot': $($_.Exception.Message)"
    exit 2
}
if ($ReportRootFullPath -eq [System.IO.Path]::GetPathRoot($ReportRootFullPath)) {
    Write-Error "Refusing filesystem-root report directory: $ReportRoot"
    exit 2
}
if ($ReportRoot -match '(^|[\\/])\.\.?(?:[\\/]|$)') {
    Write-Error "Refusing aliased report directory: $ReportRoot"
    exit 2
}
if (Test-Path -LiteralPath $ReportRoot) {
    $ReportRootItem = Get-Item -LiteralPath $ReportRoot -Force
    if ($ReportRootItem.Attributes.HasFlag([System.IO.FileAttributes]::ReparsePoint)) {
        Write-Error "Refusing symbolic-link report directory: $ReportRoot"
        exit 2
    }
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

$VerifyMarkerName = ".reconx-verify-owned"
$LoadMarkerName = ".reconx-load-owned"

function Prepare-OwnedDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedPath,
        [Parameter(Mandatory = $true)]
        [string]$MarkerName
    )

    if ($Path -ne $ExpectedPath) {
        Write-Error "Refusing cleanup path outside its ownership invariant: $Path"
        return $false
    }

    if ($Path -eq [System.IO.Path]::GetPathRoot($Path) -or $Path -eq $RepoRoot -or $Path -eq $ReportRoot) {
        Write-Error "Refusing non-leaf cleanup path: $Path"
        return $false
    }
    $OwnedPrefix = $Path.TrimEnd($TrimChars) + $Separator
    if ($RepoPrefix.StartsWith($OwnedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-Error "Refusing cleanup path that contains the repository: $Path"
        return $false
    }

    $MarkerPath = Join-Path $Path $MarkerName
    $MarkerValue = "reconx-owned-v1`npath=$Path`nkind=$MarkerName"
    if (Test-Path -LiteralPath $Path) {
        $Item = Get-Item -LiteralPath $Path -Force
        if (-not $Item.PSIsContainer -or $Item.Attributes.HasFlag([System.IO.FileAttributes]::ReparsePoint)) {
            Write-Error "Refusing symbolic-link or non-directory cleanup path: $Path"
            return $false
        }
        if (-not (Test-Path -LiteralPath $MarkerPath -PathType Leaf)) {
            Write-Error "Refusing cleanup without wrapper ownership marker: $Path"
            return $false
        }
        $MarkerItem = Get-Item -LiteralPath $MarkerPath -Force
        if ($MarkerItem.Attributes.HasFlag([System.IO.FileAttributes]::ReparsePoint)) {
            Write-Error "Refusing symbolic-link ownership marker: $MarkerPath"
            return $false
        }
        $ActualMarker = Get-Content -LiteralPath $MarkerPath -Raw -ErrorAction Stop
        if ($ActualMarker.TrimEnd("`r", "`n") -cne $MarkerValue) {
            Write-Error "Refusing cleanup with stale or forged ownership marker: $Path"
            return $false
        }
        try {
            Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
        } catch {
            Write-Error "Unable to clear wrapper-owned directory '$Path': $($_.Exception.Message)"
            return $false
        }
    }

    try {
        New-Item -ItemType Directory -Path $Path -Force -ErrorAction Stop | Out-Null
        Set-Content -LiteralPath $MarkerPath -Value $MarkerValue -NoNewline -ErrorAction Stop
    } catch {
        Write-Error "Unable to create wrapper-owned directory '$Path': $($_.Exception.Message)"
        return $false
    }
    return $true
}

switch ($Mode) {
    "all" { $PrepareBackend = $true; $PrepareFrontend = $true }
    "backend" { $PrepareBackend = $true; $PrepareFrontend = $false }
    "frontend" { $PrepareBackend = $false; $PrepareFrontend = $true }
    "load" { $PrepareBackend = $false; $PrepareFrontend = $false }
}
if ($PrepareBackend -and -not (Prepare-OwnedDirectory `
        -Path (Join-Path $ReportRoot "backend") `
        -ExpectedPath (Join-Path $ReportRoot "backend") `
        -MarkerName $VerifyMarkerName)) {
    exit 2
}
if ($PrepareFrontend -and -not (Prepare-OwnedDirectory `
        -Path (Join-Path $ReportRoot "frontend") `
        -ExpectedPath (Join-Path $ReportRoot "frontend") `
        -MarkerName $VerifyMarkerName)) {
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

    if ($TestStatus -ne 0) { return $TestStatus }
    if ($CopyStatus -ne 0) { return $CopyStatus }
    return $CleanupStatus
}

function Invoke-LoggedLoadCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$LogPath,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $AllArguments = $script:ComposeArguments + $Arguments
    & docker compose @AllArguments *> $LogPath
    $Status = [int]$LASTEXITCODE
    Get-Content -LiteralPath $LogPath | ForEach-Object { Write-Host $_ }
    return $Status
}

function Require-LoadArtifacts {
    $RequiredArtifacts = @(
        "k6-summary.json",
        "k6-raw-summary.json",
        "prometheus-query-evidence.json",
        "load-run-metadata.json",
        "k6.log",
        "prometheus-query.log",
        "compose.log"
    )
    foreach ($Artifact in $RequiredArtifacts) {
        $Path = Join-Path $script:LoadArtifactDir $Artifact
        if (-not (Test-Path -LiteralPath $Path -PathType Leaf) -or (Get-Item -LiteralPath $Path).Length -eq 0) {
            Write-Error "Required load artifact is missing or empty: $Artifact"
            return 1
        }
    }
    return 0
}

function Test-LoadArtifactCredentials {
    & node (Join-Path $RepoRoot "scripts/load/validate-artifacts.mjs") $script:LoadArtifactDir
    return [int]$LASTEXITCODE
}

function Invoke-Load {
    $ProjectName = "reconx-adv097-{0}-{1}" -f [DateTimeOffset]::UtcNow.ToString("yyyyMMddHHmmss"), $PID
    $ComposeFile = Join-Path $RepoRoot "docker-compose.load.yml"
    $LoadArtifactInput = $env:RECONX_LOAD_ARTIFACT_DIR
    if ([string]::IsNullOrWhiteSpace($LoadArtifactInput)) {
        $LoadArtifactInput = Join-Path $RepoRoot ".verification-reports/load"
    }
    if ($LoadArtifactInput -match '(^|[\\/])\.\.?(?:[\\/]|$)') {
        Write-Error "Refusing aliased load artifact directory: $LoadArtifactInput"
        return 2
    }
    if (-not [System.IO.Path]::IsPathRooted($LoadArtifactInput)) {
        $LoadArtifactInput = Join-Path $RepoRoot $LoadArtifactInput
    }
    $LoadArtifactInput = [System.IO.Path]::GetFullPath($LoadArtifactInput)
    $LoadArtifactName = Split-Path -Leaf $LoadArtifactInput
    $LoadArtifactParent = Split-Path -Parent $LoadArtifactInput
    if ([string]::IsNullOrWhiteSpace($LoadArtifactName) -or $LoadArtifactName -in @(".", "..")) {
        Write-Error "Refusing load artifact path without a unique directory name: $LoadArtifactInput"
        return 2
    }
    try {
        New-Item -ItemType Directory -Path $LoadArtifactParent -Force -ErrorAction Stop | Out-Null
        $LoadArtifactParent = (Resolve-Path -LiteralPath $LoadArtifactParent -ErrorAction Stop).Path
        $LoadArtifactDir = Join-Path $LoadArtifactParent $LoadArtifactName
    } catch {
        Write-Error "Unable to create load artifact parent directory '$LoadArtifactParent': $($_.Exception.Message)"
        return 1
    }
    if (Test-Path -LiteralPath $LoadArtifactDir) {
        $LoadItem = Get-Item -LiteralPath $LoadArtifactDir -Force
        if ($LoadItem.Attributes.HasFlag([System.IO.FileAttributes]::ReparsePoint)) {
            Write-Error "Refusing symbolic-link load artifact directory: $LoadArtifactDir"
            return 2
        }
    }

    $RepoPrefix = $RepoRoot.TrimEnd($TrimChars) + $Separator
    $LoadPrefix = $LoadArtifactDir.TrimEnd($TrimChars) + $Separator
    if ($RepoPrefix.StartsWith($LoadPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-Error "Refusing load artifact directory that contains the repository: $LoadArtifactDir"
        return 2
    }
    if ($LoadArtifactDir -eq $ReportRoot) {
        Write-Error "Refusing load artifact directory shared with verification reports: $LoadArtifactDir"
        return 2
    }
    if (-not (Prepare-OwnedDirectory `
            -Path $LoadArtifactDir `
            -ExpectedPath $LoadArtifactDir `
            -MarkerName $LoadMarkerName)) {
        return 2
    }

    $script:LoadArtifactDir = $LoadArtifactDir
    $env:RECONX_LOAD_ARTIFACT_DIR = $LoadArtifactDir
    if ($IsLinux -or $IsMacOS) {
        $LoadUid = (& id -u).Trim()
        $LoadGid = (& id -g).Trim()
        $env:RECONX_LOAD_EXPORT_USER = "$LoadUid`:$LoadGid"
    } else {
        $env:RECONX_LOAD_EXPORT_USER = "0:0"
    }
    $script:ComposeArguments = @("-f", $ComposeFile, "-p", $ProjectName)
    $LoadStatus = 0
    $CleanupStatus = 0
    try {
        Write-Host ""
        Write-Host "==> Starting isolated ADV097 Compose project $ProjectName"
        $LoadStatus = Invoke-Compose -Arguments ($script:ComposeArguments + @(
            "up", "--build", "--force-recreate", "--detach",
            "load-postgres", "load-zookeeper", "load-kafka", "load-backend", "load-prometheus", "load-grafana"
        ))

        $GrafanaUrl = ""
        $PrometheusUrl = ""
        if ($LoadStatus -eq 0) {
            $GrafanaPort = (& docker compose @script:ComposeArguments port load-grafana 3000 2>$null | Select-Object -First 1)
            $PrometheusPort = (& docker compose @script:ComposeArguments port load-prometheus 9090 2>$null | Select-Object -First 1)
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
                cleanupCommand = "docker compose -f docker-compose.load.yml -p $ProjectName --profile load down --volumes --remove-orphans"
            } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $LoadArtifactDir "load-run-metadata.json")
            Write-Host "Grafana observation URL: $GrafanaUrl"

            Write-Host ""
            Write-Host "==> Preparing project-scoped non-root load artifact volume"
            $LoadStatus = Invoke-LoggedLoadCommand -LogPath (Join-Path $LoadArtifactDir "results-init.log") -Arguments @("run", "--rm", "--no-deps", "load-results-init")
        }

        if ($LoadStatus -eq 0) {
            Write-Host ""
            Write-Host "==> Capturing Prometheus pre-run counter baseline"
            $LoadStatus = Invoke-LoggedLoadCommand -LogPath (Join-Path $LoadArtifactDir "prometheus-baseline.log") -Arguments @("run", "--rm", "--no-deps", "-e", "PROMETHEUS_PHASE=baseline", "load-query")
        }

        if ($LoadStatus -eq 0) {
            Write-Host ""
            Write-Host "==> Running pinned Grafana k6 load generator"
            $LoadStatus = Invoke-LoggedLoadCommand -LogPath (Join-Path $LoadArtifactDir "k6.log") -Arguments @("run", "--rm", "--no-deps", "load-k6")
        }

        if (Test-Path -LiteralPath (Join-Path $LoadArtifactDir "k6.log") -PathType Leaf) {
            Write-Host ""
            Write-Host "==> Capturing aligned Prometheus panel-query evidence"
            $QueryStatus = Invoke-LoggedLoadCommand -LogPath (Join-Path $LoadArtifactDir "prometheus-query.log") -Arguments @("run", "--rm", "--no-deps", "load-query")
            if ($LoadStatus -eq 0 -and $QueryStatus -ne 0) { $LoadStatus = $QueryStatus }
        }

        if ($LoadStatus -eq 0 -or (Test-Path -LiteralPath (Join-Path $LoadArtifactDir "k6.log") -PathType Leaf)) {
            Write-Host ""
            Write-Host "==> Validating and exporting load artifacts"
            $ExportStatus = Invoke-LoggedLoadCommand -LogPath (Join-Path $LoadArtifactDir "export.log") -Arguments @("run", "--rm", "--no-deps", "load-export")
            if ($LoadStatus -eq 0 -and $ExportStatus -ne 0) { $LoadStatus = $ExportStatus }
        }

        $ComposeLog = & docker compose @script:ComposeArguments logs --no-color load-backend load-prometheus load-grafana 2>&1 |
            ForEach-Object {
                $Line = "$_"
                $Line = $Line -replace '(?i)(Using generated security password:\s*)\S+', '${1}[REDACTED]'
                $Line = $Line -replace '(?i)(Authorization:\s*Bearer\s+)\S+', '${1}[REDACTED]'
                $Line = $Line -replace '(?i)(GF_SECURITY_ADMIN_PASSWORD[=:]\s*)[^\s,"]+', '${1}[REDACTED]'
                $Line = $Line -replace '(?i)(JWT_SECRET[=:]\s*)[^\s,"]+', '${1}[REDACTED]'
                $Line = $Line -replace '\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b', '[REDACTED]'
                $Line
            }
        $ComposeLog | Set-Content -LiteralPath (Join-Path $LoadArtifactDir "compose.log")
    } finally {
        if ($env:RECONX_LOAD_KEEP_STACK -eq "1") {
            Write-Host ""
            Write-Host "Keeping isolated Compose project $ProjectName for Grafana observation."
        } else {
            $CleanupStatus = Invoke-Compose -Arguments ($script:ComposeArguments + @("--profile", "load", "down", "--volumes", "--remove-orphans"))
        }
    }

    $ArtifactStatus = Require-LoadArtifacts
    $CredentialStatus = Test-LoadArtifactCredentials
    if ($LoadStatus -eq 0 -and $ArtifactStatus -ne 0) { $LoadStatus = $ArtifactStatus }
    if ($LoadStatus -eq 0 -and $CredentialStatus -ne 0) { $LoadStatus = $CredentialStatus }
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
