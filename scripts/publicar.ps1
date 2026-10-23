# Compila la release firmada y la publica en GitHub Releases (rboluda/Klipo).
# La app instalada la detecta al abrirse (una vez al día) o con "Buscar actualizaciones".
#
# Uso:  .\scripts\publicar.ps1 -Notas "Qué cambia en esta versión"
param([string]$Notas = "")

$ErrorActionPreference = "Stop"
$raiz = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = Join-Path ${env:ProgramFiles} "Android\Android Studio\jbr"

Push-Location $raiz
try {
    & .\gradlew.bat assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "La compilación ha fallado" }

    $apk = Get-ChildItem "apk\klippo-*-release.apk" | Sort-Object LastWriteTime | Select-Object -Last 1
    if (-not $apk) { throw "No se encuentra el APK de release en apk\" }

    # klippo-0.1.0-20261023.1426-release.apk -> 0.1.0-20261023.1426
    $version = $apk.Name -replace '^klippo-', '' -replace '-release\.apk$', ''
    if (-not $Notas) { $Notas = "Klippo $version" }

    gh release create "v$version" $apk.FullName --repo rboluda/Klipo `
        --title "Klippo $version" --notes $Notas --latest
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear la release en GitHub" }
    Write-Output "Publicada la versión $version"
}
finally {
    Pop-Location
}
