param(
    [Parameter(Position = 0)]
    [string]$Mode = "all"
)

function Show-Usage {
    [Console]::Error.WriteLine("Usage: scripts/verify.ps1 [all|backend|frontend]")
}

if ($args.Count -gt 0) {
    Show-Usage
    exit 2
}

$Mode = $Mode.ToLowerInvariant()
if ($Mode -notin @("all", "backend", "frontend")) {
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

$BackendStatus = 0
$FrontendStatus = 0

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
}

if ($BackendStatus -ne 0 -or $FrontendStatus -ne 0) {
    Write-Error "Container verification failed (backend=$BackendStatus, frontend=$FrontendStatus)."
    exit 1
}

Write-Host ""
Write-Host "Container verification passed. Reports: $ReportRoot"
