param([string[]]$Tasks = @('assembleDebug', 'testDebugUnitTest', 'lintDebug'))
$ErrorActionPreference = 'Stop'
$workspace = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$toolchain = Join-Path $workspace 'work/toolchain'
$jdk = Get-ChildItem (Join-Path $toolchain 'java') -Directory | Select-Object -First 1
if (-not $jdk) { throw 'No se encuentra el JDK local. Usa Android Studio o configura JAVA_HOME.' }
$env:JAVA_HOME = $jdk.FullName
$env:ANDROID_HOME = Join-Path $toolchain 'sdk'
$env:ANDROID_USER_HOME = Join-Path $workspace 'work/android-user'
$env:GRADLE_USER_HOME = Join-Path $workspace 'work/gradle-cache'
& (Join-Path $PSScriptRoot 'gradlew.bat') -p $PSScriptRoot @Tasks --console=plain
exit $LASTEXITCODE
