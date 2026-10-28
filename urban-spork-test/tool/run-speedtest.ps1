param(
    [string]$ProxyHost = 'localhost',
    [int]$ProxyPort = 1080,
    [string]$DstHost = '',
    [ValidateRange(0, 65535)]
    [int]$DstPort = 0,
    [int]$Connections = 1,
    [string]$Payload = '1024m',
    [string]$Rate = '256m'
)

$testRoot = Split-Path -Parent $PSScriptRoot
$root = Split-Path -Parent $testRoot
if (-not (Test-Path -LiteralPath (Join-Path $testRoot 'pom.xml')) -or -not (Test-Path -LiteralPath (Join-Path $root 'pom.xml'))) {
    throw "Cannot locate the urban-spork-test module from '$PSScriptRoot'"
}
$testTarget = Join-Path $testRoot 'target'
$depsFile = Join-Path $testTarget 'speedtest-deps.txt'

$java = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) { "$env:JAVA_HOME\bin\java.exe" } else { (Get-Command java.exe -CommandType Application).Source }

$mvn = (Get-Command mvn.cmd -CommandType Application -ErrorAction SilentlyContinue).Source
if (-not $mvn) { $mvn = (Get-Command mvn -CommandType Application).Source }
$mvnHome = if ($env:MAVEN_HOME) { $env:MAVEN_HOME } else { Split-Path (Split-Path $mvn -Parent) -Parent }
$settings = if ($env:MAVEN_SETTINGS) { $env:MAVEN_SETTINGS } else { @("$mvnHome\settings.xml", "$mvnHome\conf\settings.xml") | Where-Object { Test-Path $_ } | Select-Object -First 1 }

# A stale classpath from a previous run must never mask a failed build.
Remove-Item -LiteralPath $depsFile -ErrorAction SilentlyContinue
$mvnArgs = @()
if ($settings) { $mvnArgs += '-s', $settings }
$mvnArgs += '-q', '-pl', 'urban-spork-test', '-am', '-DskipTests', '-Dskip=true', 'compile',
    'dependency:build-classpath', '-Dmdep.outputFile=target/speedtest-deps.txt', '-Dmdep.includeScope=runtime'
Push-Location -LiteralPath $root
try {
    & $mvn @mvnArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $depsFile) -or (Get-Item -LiteralPath $depsFile).Length -eq 0) {
    throw "Maven did not write the dependency classpath to '$depsFile'"
}
$deps = (Get-Content -LiteralPath $depsFile -Raw).Trim()
$classPath = "$(Join-Path $testTarget 'classes');$deps"

$vmArgs = @("-DproxyHost=$ProxyHost", "-DproxyPort=$ProxyPort", "-Dconnections=$Connections", "-Dpayload=$Payload")
if ($Rate) { $vmArgs += "-Drate=$Rate" }
if ($DstPort -gt 0) { $vmArgs += "-DdstPort=$DstPort" }
if ($DstHost) { $vmArgs += "-DdstHost=$DstHost" }
# Keep every frame inside the visible viewport, rather than the scrollback buffer.
$window = $Host.UI.RawUI.WindowSize
if ($window.Width -gt 1 -and $window.Height -gt 1) {
    $vmArgs += "-DterminalColumns=$($window.Width)", "-DterminalRows=$($window.Height)"
}
$vmArgs += '--enable-native-access=ALL-UNNAMED', '-Dfile.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8', '-Dsun.stderr.encoding=UTF-8'
$vmArgs += '-classpath', $classPath, 'com.urbanspork.test.tool.SpeedTestLocalServer'

& $java @vmArgs
