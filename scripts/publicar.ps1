# Compila la release firmada y la publica en GitHub Releases (rboluda/Klippo).
# La app instalada la detecta al abrirse (una vez al día) o con "Buscar actualizaciones".
#
# Versiones semánticas MAYOR.MENOR.PARCHE (val versionKlippo en app/build.gradle.kts):
#   -Subir parche   1.0.0 -> 1.0.1   correcciones
#   -Subir menor    1.0.0 -> 1.1.0   funciones nuevas
#   -Subir mayor    1.0.0 -> 2.0.0   cambios grandes
# Sin -Subir publica la versión actual (si ya existe en GitHub, se detiene).
#
# Uso:  .\scripts\publicar.ps1 -Subir menor -Notas "Qué cambia en esta versión"
param(
    [string]$Notas = "",
    [ValidateSet("", "parche", "menor", "mayor")]
    [string]$Subir = ""
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = Join-Path ${env:ProgramFiles} "Android\Android Studio\jbr"
$gradle = Join-Path $raiz "app\build.gradle.kts"
$patron = 'val versionKlippo = "(\d+)\.(\d+)\.(\d+)"'

Push-Location $raiz
try {
    $texto = [IO.File]::ReadAllText($gradle)
    $m = [regex]::Match($texto, $patron)
    if (-not $m.Success) { throw "No se encuentra versionKlippo en app/build.gradle.kts" }
    $mayor = [int]$m.Groups[1].Value; $menor = [int]$m.Groups[2].Value; $parche = [int]$m.Groups[3].Value

    switch ($Subir) {
        "mayor" { $mayor++; $menor = 0; $parche = 0 }
        "menor" { $menor++; $parche = 0 }
        "parche" { $parche++ }
    }
    $version = "$mayor.$menor.$parche"

    # Nunca se publica dos veces el mismo número
    gh release view "v$version" --repo rboluda/Klippo *> $null
    if ($LASTEXITCODE -eq 0) { throw "La versión $version ya está publicada: usa -Subir parche, menor o mayor" }

    if ($Subir) {
        $texto = [regex]::Replace($texto, $patron, "val versionKlippo = `"$version`"")
        [IO.File]::WriteAllText($gradle, $texto, (New-Object Text.UTF8Encoding $false))
        git add $gradle
        git commit -q -m "Versión $version"
        git push -q
        if ($LASTEXITCODE -ne 0) { throw "No se pudo subir el cambio de versión" }
    }

    & .\gradlew.bat assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "La compilación ha fallado" }

    $apk = Join-Path $raiz "apk\klippo-$version.apk"
    if (-not (Test-Path $apk)) { throw "No se encuentra $apk" }
    if (-not $Notas) { $Notas = "Klippo $version" }

    gh release create "v$version" $apk --repo rboluda/Klippo `
        --title "Klippo $version" --notes $Notas --latest
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear la release en GitHub" }
    Write-Output "Publicada la versión $version"
}
finally {
    Pop-Location
}
