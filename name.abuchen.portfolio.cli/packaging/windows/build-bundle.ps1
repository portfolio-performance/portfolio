<#
.SYNOPSIS
Builds the self-contained pp-cli bundle for Windows x64.

.DESCRIPTION
Runs the Tycho build of the core and CLI bundles, collects the resolved jars with
tycho-dependency-tools:list-dependencies, keeps only what a flat classpath on Windows x64
needs (no pdfbox, no source jars, only the win32 x86_64 SWT fragment), builds a jlink runtime
and assembles target\pp-cli-windows-x64\ next to the launcher and installer.

Requires JAVA_HOME to point to a Java 21 JDK (with jmods).

.PARAMETER SkipMaven
Reuse an existing name.abuchen.portfolio.cli\target\dependencies-list.txt.
#>
[CmdletBinding()]
param(
    [switch]$SkipMaven
)

$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$cliDir = Join-Path $repoRoot 'name.abuchen.portfolio.cli'
$bundleDir = Join-Path $cliDir 'target\pp-cli-windows-x64'
$swtFragment = 'org.eclipse.swt.win32.win32.x86_64'

if (-not $env:JAVA_HOME) { throw 'JAVA_HOME must point to a Java 21 JDK.' }
$jdkBin = Join-Path $env:JAVA_HOME 'bin'
foreach ($tool in 'java.exe', 'jdeps.exe', 'jlink.exe') {
    if (-not (Test-Path (Join-Path $jdkBin $tool))) { throw "$tool not found in $jdkBin. JAVA_HOME must be a JDK, not a JRE." }
}
if (-not (Test-Path (Join-Path $env:JAVA_HOME 'jmods'))) { throw "No jmods directory in $env:JAVA_HOME; jlink needs a full JDK." }
$javaVersion = Select-String -Path (Join-Path $env:JAVA_HOME 'release') -Pattern '^JAVA_VERSION="([^"]+)"'
if (-not $javaVersion -or $javaVersion.Matches[0].Groups[1].Value -notmatch '^21(\.|$)') { throw "Java 21 is required in JAVA_HOME ($env:JAVA_HOME)." }

function Invoke-Native([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed with exit code $LASTEXITCODE" }
}

if (-not $SkipMaven) {
    $reactor = ':portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.cli'
    if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xmx4g' }
    Invoke-Native 'mvn' @('-f', (Join-Path $repoRoot 'portfolio-app\pom.xml'), 'package', '-Plocal-dev,cli-bundle', '-DskipTests', '-pl', $reactor)
}

$listFile = Join-Path $cliDir 'target\dependencies-list.txt'
if (-not (Test-Path $listFile)) { throw "$listFile not found. Run without -SkipMaven." }

$pdfbox = '^(name\.abuchen\.portfolio\.pdfbox[13]|pdfbox|pdfbox-io|fontbox|jempbox)-\d'
$sources = '\.source[-_]'
$swtFragments = '^org\.eclipse\.swt\.[a-z0-9]+\.[a-z0-9]+\.[a-z0-9_]+-\d'

$jars = Get-Content $listFile |
    Where-Object { $_.Trim() } |
    ForEach-Object { Get-Item -LiteralPath $_.Trim() } |
    Where-Object { $_.Name -notmatch $sources -and $_.Name -notmatch $pdfbox } |
    Where-Object { $_.Name -notmatch $swtFragments -or $_.Name -like "$swtFragment-*" } |
    Sort-Object Name -Unique

$fragmentJars = @($jars | Where-Object { $_.Name -like "$swtFragment-*" })
if ($fragmentJars.Count -ne 1) { throw "Expected exactly one $swtFragment jar, found $($fragmentJars.Count)." }
if (@($jars | Where-Object { $_.Name -match $swtFragments -and $_.Name -notlike "$swtFragment-*" }).Count -gt 0) { throw 'A foreign SWT fragment is on the classpath.' }
if (@($jars | Where-Object { $_.Name -match '^name\.abuchen\.portfolio-\d' }).Count -ne 1) { throw 'The core jar is missing from the dependency list.' }
if (@($jars | Where-Object { $_.Name -match '^name\.abuchen\.portfolio\.cli-\d' }).Count -ne 1) { throw 'The CLI jar is missing from the dependency list.' }

if (Test-Path $bundleDir) { Remove-Item -Recurse -Force $bundleDir }
$libDir = Join-Path $bundleDir 'lib'
New-Item -ItemType Directory -Force $libDir | Out-Null
$jars | Copy-Item -Destination $libDir

# Modules the classpath needs, plus the ones only reached reflectively or by service lookup
$deps = & (Join-Path $jdkBin 'jdeps.exe') --multi-release 21 --ignore-missing-deps --print-module-deps -R --class-path "$libDir\*" (Get-ChildItem $libDir -Filter 'name.abuchen.portfolio*.jar').FullName
if ($LASTEXITCODE -ne 0) { throw 'jdeps failed.' }
$modules = @($deps.Trim().Split(',') + 'jdk.crypto.ec', 'jdk.localedata', 'jdk.unsupported') | Sort-Object -Unique
Invoke-Native (Join-Path $jdkBin 'jlink.exe') @('--add-modules', ($modules -join ','), '--strip-debug', '--no-header-files', '--no-man-pages',
    '--compress', 'zip-6', '--generate-cds-archive', '--output', (Join-Path $bundleDir 'runtime'))

function Copy-Text([string]$Name, [string]$Eol) {
    $text = (Get-Content (Join-Path $PSScriptRoot $Name) -Raw) -replace "`r?`n", $Eol
    [IO.File]::WriteAllText((Join-Path $bundleDir $Name), $text, [Text.UTF8Encoding]::new($false))
}

# the launcher must have CRLF for cmd and the shim LF for sh, whatever the checkout converted them to
Copy-Text 'pp-cli.cmd' "`r`n"
Copy-Text 'pp-cli' "`n"
Copy-Item (Join-Path $PSScriptRoot 'install.ps1') $bundleDir
Copy-Item (Join-Path $PSScriptRoot 'uninstall.ps1') $bundleDir
Copy-Item (Join-Path $PSScriptRoot 'smoke-check.ps1') $bundleDir
Copy-Item -Recurse (Join-Path $cliDir 'skills') (Join-Path $bundleDir 'skills')
$sampleDir = Join-Path $bundleDir 'sample'
New-Item -ItemType Directory $sampleDir | Out-Null
Copy-Item (Join-Path $repoRoot 'name.abuchen.portfolio.cli.tests\src\name\abuchen\portfolio\cli\historic\client69.xml') (Join-Path $sampleDir 'sample-client.xml')

Write-Host "Bundle ready: $bundleDir ($($jars.Count) jars, modules: $($modules -join ','))"
Write-Host "Install it with: powershell -ExecutionPolicy Bypass -File `"$bundleDir\install.ps1`""
