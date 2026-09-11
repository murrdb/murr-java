# Starts a native murr server in the background for tests on Windows, where Linux containers don't run.
# The server version follows the client version (0.2.2-1 -> 0.2.2), same as MurrContainer.
# Set MURR_VERSION to use another release.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Set-Location "$PSScriptRoot/.."

$version = $env:MURR_VERSION
if (-not $version) {
    $client = mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout
    if ($LASTEXITCODE -ne 0) { throw "failed to read project version" }
    $version = $client.Trim() -replace '-SNAPSHOT$', '' -replace '-\d+$', ''
}

$tmp = if ($env:RUNNER_TEMP) { $env:RUNNER_TEMP } else { [System.IO.Path]::GetTempPath() }
$dir = Join-Path $tmp 'murr'
$bin = Join-Path $dir 'murr.exe'
$endpoint = 'http://127.0.0.1:8080'
New-Item -ItemType Directory -Force -Path (Join-Path $dir 'data') | Out-Null

Write-Host "downloading murr $version"
Invoke-WebRequest -UseBasicParsing -OutFile $bin `
    -Uri "https://github.com/murrdb/murr/releases/download/v$version/murr-windows-x64.exe"

$env:MURR_STORAGE_PATH = Join-Path $dir 'data'
$proc = Start-Process -FilePath $bin -NoNewWindow -PassThru `
    -RedirectStandardOutput (Join-Path $dir 'murr.log') `
    -RedirectStandardError (Join-Path $dir 'murr.err.log')
Write-Host "started murr (pid $($proc.Id)), logs in $dir"

for ($i = 0; $i -lt 60; $i++) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri "$endpoint/health" | Out-Null
        Write-Host "murr is up at $endpoint"
        if ($env:GITHUB_ENV) {
            Add-Content -Path $env:GITHUB_ENV -Value "MURR_ENDPOINT=$endpoint"
            Add-Content -Path $env:GITHUB_ENV -Value "MURR_LOG_DIR=$dir"
        } else {
            Write-Host "run tests with: `$env:MURR_ENDPOINT='$endpoint'; mvn verify"
        }
        exit 0
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Error "murr did not become healthy in 60s" -ErrorAction Continue
Get-Content (Join-Path $dir 'murr.log'), (Join-Path $dir 'murr.err.log') -ErrorAction SilentlyContinue
exit 1
