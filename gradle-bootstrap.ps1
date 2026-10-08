param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
$gradleVersion = "9.7.1"
$javaMajor = 25
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Use-Java25 {
    $systemJavaOk = $false
    try {
        $javaVersionText = (& java -version 2>&1 | Out-String)
        $systemJavaOk = $javaVersionText -match 'version\s+"25(?:\.|\")'
    } catch {}

    if ($systemJavaOk) { return }

    $jdkRoot = Join-Path $root ".jdk-dist"
    $jdkHome = Get-ChildItem -Path $jdkRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "jdk-*" } |
        Select-Object -First 1

    if (-not $jdkHome) {
        New-Item -ItemType Directory -Force -Path $jdkRoot | Out-Null
        $jdkZip = Join-Path $jdkRoot "temurin-25.zip"
        Write-Host "Java 25 not found. Downloading Eclipse Temurin 25..."
        Invoke-WebRequest -UseBasicParsing `
            -Uri "https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse" `
            -OutFile $jdkZip
        Expand-Archive -Path $jdkZip -DestinationPath $jdkRoot -Force
        Remove-Item $jdkZip -Force
        $jdkHome = Get-ChildItem -Path $jdkRoot -Directory |
            Where-Object { $_.Name -like "jdk-*" } |
            Select-Object -First 1
    }

    if (-not $jdkHome) { throw "Could not provision Java 25." }
    $env:JAVA_HOME = $jdkHome.FullName
    $env:PATH = (Join-Path $jdkHome.FullName "bin") + ";" + $env:PATH
}

Use-Java25

$distRoot = Join-Path $root ".gradle-dist"
$gradleHome = Join-Path $distRoot "gradle-$gradleVersion"
$gradleBat = Join-Path $gradleHome "bin\gradle.bat"
$zip = Join-Path $distRoot "gradle-$gradleVersion-bin.zip"

if (-not (Test-Path $gradleBat)) {
    New-Item -ItemType Directory -Force -Path $distRoot | Out-Null
    Write-Host "Downloading Gradle $gradleVersion..."
    Invoke-WebRequest -UseBasicParsing -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zip
    Write-Host "Extracting Gradle..."
    Expand-Archive -Path $zip -DestinationPath $distRoot -Force
    Remove-Item $zip -Force
}

Push-Location $root
try {
    & $gradleBat @GradleArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
