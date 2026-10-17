$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$repository = Join-Path $projectRoot '.m2\repository'

Push-Location $projectRoot
try {
    & mvn.cmd -B -ntp "-Dmaven.repo.local=$repository" spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "La aplicación terminó con código $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
