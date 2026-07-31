param(
    [int[]]$Versions = @(17, 21, 25)
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$assetsRoot = Join-Path $root 'app\src\main\assets\jdk'
$tempBase = Join-Path $env:TEMP 'mckaifu_jdk_dl'

New-Item -ItemType Directory -Force -Path $tempBase | Out-Null

foreach ($v in $Versions) {
    $targetDir = Join-Path $assetsRoot "$v"
    if ((Test-Path (Join-Path $targetDir 'bin\java')) -or (Test-Path (Join-Path $targetDir 'bin\java.exe'))) {
        Write-Host "[$v] already installed, skip."
        continue
    }

    $tmpTar = Join-Path $tempBase "jdk$v.tar.gz"
    $extractDir = Join-Path $tempBase "extract$v"
    if (Test-Path $extractDir) { Remove-Item -Recurse -Force $extractDir }

    $urlJre = "https://api.adoptium.net/v3/binary/latest/$v/ga/linux/aarch64/jre/hotspot/normal/eclipse"
    $urlJdk = "https://api.adoptium.net/v3/binary/latest/$v/ga/linux/aarch64/jdk/hotspot/normal/eclipse"

    $downloaded = $false
    foreach ($u in @($urlJre, $urlJdk)) {
        try {
            Write-Host "[$v] downloading from $u"
            Invoke-WebRequest -Uri $u -OutFile $tmpTar -UseBasicParsing -MaximumRedirection 20
            $downloaded = $true
            break
        } catch {
            Write-Host "  download failed: $($_.Exception.Message)"
        }
    }
    if (-not $downloaded) {
        Write-Host "[$v] SKIP (all download attempts failed)"
        continue
    }

    New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
    tar -xzf $tmpTar -C $extractDir
    $inner = Get-ChildItem -LiteralPath $extractDir -Directory | Select-Object -First 1
    if (-not $inner) {
        Write-Host "[$v] SKIP (no top-level dir found in archive)"
        continue
    }
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item -Path (Join-Path $inner.FullName '*') -Destination $targetDir -Recurse -Force
    $javaBin = Join-Path $targetDir 'bin\java'
    Write-Host "[$v] extracted -> $javaBin exists: $(Test-Path $javaBin)"
}

Remove-Item -Recurse -Force $tempBase -ErrorAction SilentlyContinue
Write-Host "Done."
