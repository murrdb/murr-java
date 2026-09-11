# Starts a native murr server in the background for tests on Windows, where Linux containers don't run.
# The server version follows the client version (0.2.2-1 -> 0.2.2), same as MurrContainer.
# Set MURR_VERSION to use another release.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Set-Location "$PSScriptRoot/.."

$version = $env:MURR_VERSION
if (-not $version) {
    # quoted: PowerShell splits unquoted -Dfoo=a.b at the dot
    $client = mvn -B -q help:evaluate "-Dexpression=project.version" "-DforceStdout"
    if ($LASTEXITCODE -ne 0) { throw "failed to read project version" }
    $version = $client.Trim() -replace '-SNAPSHOT$', '' -replace '-\d+$', ''
}

$tmp = if ($env:RUNNER_TEMP) { $env:RUNNER_TEMP } else { [System.IO.Path]::GetTempPath() }
$dir = Join-Path $tmp 'murr'
$bin = Join-Path $dir 'murr.exe'
$endpoint = 'http://127.0.0.1:8080'
New-Item -ItemType Directory -Force -Path $dir | Out-Null

Write-Host "downloading murr $version"
Invoke-WebRequest -UseBasicParsing -OutFile $bin `
    -Uri "https://github.com/murrdb/murr/releases/download/v$version/murr-windows-x64.exe"

# murr keeps data in .\murr under its working directory. MURR_STORAGE_PATH would be the obvious
# knob, but murr 0.2.2 fails to parse the storage config when only the path is set.
$proc = Start-Process -FilePath $bin -WorkingDirectory $dir -NoNewWindow -PassThru `
    -RedirectStandardOutput (Join-Path $dir 'murr.log') `
    -RedirectStandardError (Join-Path $dir 'murr.err.log')
Write-Host "started murr (pid $($proc.Id)), logs in $dir"

for ($i = 0; $i -lt 60; $i++) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri "$endpoint/health" | Out-Null
        Write-Host "murr is up at $endpoint"
        # The GitHub runner kills everything a step started when that step ends, so CI runs this
        # script and mvn in the same step: set the endpoint for the calling session, not GITHUB_ENV.
        $env:MURR_ENDPOINT = $endpoint
        if ($env:GITHUB_ENV) {
            Add-Content -Path $env:GITHUB_ENV -Value "MURR_LOG_DIR=$dir"
        } else {
            Write-Host "MURR_ENDPOINT is set in this session, run tests with: mvn verify"
        }
        return
    } catch {
        Start-Sleep -Seconds 1
    }
}

Get-Content (Join-Path $dir 'murr.log'), (Join-Path $dir 'murr.err.log') -ErrorAction SilentlyContinue
# throw, not exit 1: the caller runs mvn next and must stop here
throw "murr did not become healthy in 60s"
