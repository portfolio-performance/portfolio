<#
.SYNOPSIS
Release-time smoke check of the installed pp-cli.

.DESCRIPTION
Checks the installed bundle (own runtime, AppCDS archive, no pdfbox, the win32 x86_64 SWT
fragment) and then runs `pp-cli info` and `pp-cli describe` against a sample file from fresh
PowerShell, cmd and Git Bash sessions. A fresh session gets the PATH stored in the registry,
not the PATH of this process, and no JAVA_HOME or other Java on PATH, so the check proves
the PATH wiring and the bundled runtime. Exits with 1 if anything fails.

.PARAMETER InstallDir
The installation to check. Defaults to %LOCALAPPDATA%\pp-cli.

.PARAMETER SampleFile
The client file to read. Defaults to the sample shipped in the bundle.

.PARAMETER SkipGitBash
Do not check Git Bash, for machines without Git for Windows.
#>
[CmdletBinding()]
param(
    [string]$InstallDir = (Join-Path $env:LOCALAPPDATA 'pp-cli'),
    [string]$SampleFile,
    [switch]$SkipGitBash
)

$ErrorActionPreference = 'Stop'

$installDir = [IO.Path]::GetFullPath($InstallDir).TrimEnd('\')
if (-not $SampleFile) { $SampleFile = Join-Path $installDir 'sample\sample-client.xml' }
$failures = [Collections.Generic.List[string]]::new()

function Assert-Check([bool]$Condition, [string]$Description) {
    if ($Condition) { Write-Host "  ok    $Description" }
    else { Write-Host "  FAIL  $Description"; $failures.Add($Description) }
}

function Get-RegistryPath([Microsoft.Win32.RegistryKey]$Hive, [string]$SubKey) {
    $key = $Hive.OpenSubKey($SubKey)
    if (-not $key) { return }
    try { ([string]$key.GetValue('Path', '')).Split(';', [StringSplitOptions]::RemoveEmptyEntries) | ForEach-Object { [Environment]::ExpandEnvironmentVariables($_) } }
    finally { $key.Close() }
}

# The PATH a newly started shell gets, without any directory that provides a java.exe
$freshPath = @(
    Get-RegistryPath ([Microsoft.Win32.Registry]::LocalMachine) 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment'
    Get-RegistryPath ([Microsoft.Win32.Registry]::CurrentUser) 'Environment'
) | Where-Object { $_.Trim() -and -not (Test-Path (Join-Path $_ 'java.exe')) }
$freshPath = $freshPath -join ';'

function Invoke-FreshShell([string]$Exe, [string]$Arguments) {
    $info = [Diagnostics.ProcessStartInfo]::new($Exe, $Arguments)
    $info.UseShellExecute = $false
    # cmd looks in the working directory first, which must not hide the installed launcher
    $info.WorkingDirectory = [IO.Path]::GetTempPath()
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.Encoding]::UTF8
    $info.Environment['Path'] = $freshPath
    foreach ($name in 'JAVA_HOME', 'JDK_HOME', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS') { [void]$info.Environment.Remove($name) }
    $process = [Diagnostics.Process]::Start($info)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit(120000)) {
        $process.Kill()
        return [pscustomobject]@{ ExitCode = -1; Stdout = ''; Stderr = 'timed out after 120 s' }
    }
    [pscustomobject]@{ ExitCode = $process.ExitCode; Stdout = $stdout.Result; Stderr = $stderr.Result }
}

function Test-Response([string]$Shell, [string]$Command, [scriptblock]$Check, [pscustomobject]$Result) {
    $document = $null
    try { $document = $Result.Stdout | ConvertFrom-Json } catch { Write-Verbose "Not JSON: $_" }
    $valid = $Result.ExitCode -eq 0 -and $document -and $null -ne $document.data -and ($null -ne $document.warnings) -and (& $Check $document)
    Assert-Check ([bool]$valid) "${Shell}: pp-cli $Command"
    if (-not $valid) { Write-Host "        exit code $($Result.ExitCode); stdout: $($Result.Stdout.Trim()); stderr: $($Result.Stderr.Trim())" }
}

Write-Host "Installed bundle: $installDir"
$lib = Join-Path $installDir 'lib'
Assert-Check (Test-Path (Join-Path $installDir 'runtime\bin\java.exe')) 'bundled runtime is present'
Assert-Check (Test-Path (Join-Path $installDir 'pp-cli.jsa')) 'AppCDS archive is present'
if (Test-Path (Join-Path $installDir 'pp-cli.jsa')) {
    # -Xshare:on fails instead of silently ignoring an archive the JVM rejects (the launcher falls back quietly)
    & (Join-Path $installDir 'runtime\bin\java.exe') "-XX:SharedArchiveFile=$(Join-Path $installDir 'pp-cli.jsa')" -Xshare:on -Xlog:disable -cp "$installDir\lib\*" name.abuchen.portfolio.cli.PpCli describe 2>$null | Out-Null
    Assert-Check ($LASTEXITCODE -eq 0) 'the JVM accepts and uses the AppCDS archive'
}
Assert-Check (-not (Get-ChildItem $lib -Filter *.jar | Where-Object { $_.Name -match '^(name\.abuchen\.portfolio\.pdfbox[13]|pdfbox|pdfbox-io|fontbox|jempbox)-\d' })) 'no pdfbox jars on the classpath'
$swt = @(Get-ChildItem $lib -Filter 'org.eclipse.swt.*.jar' | Where-Object { $_.Name -match '^org\.eclipse\.swt\.[a-z0-9]+\.[a-z0-9]+\.[a-z0-9_]+-\d' })
Assert-Check ($swt.Count -eq 1 -and $swt[0].Name -like 'org.eclipse.swt.win32.win32.x86_64-*') 'exactly one SWT fragment, win32 x86_64'
Assert-Check (($freshPath -split ';' | ForEach-Object { $_.TrimEnd('\') }) -contains $installDir) 'the user PATH contains the install directory'
Assert-Check (Test-Path $SampleFile) "sample file exists ($SampleFile)"
if ($failures.Count -gt 0) { Write-Host "`n$($failures.Count) check(s) failed."; exit 1 }

$infoCheck = { param($d) $d.data.counts -and $d.data.pinnedUpstreamTag -and $d.data.format }
$describeCheck = { param($d) @($d.data.commands).Count -gt 0 -and $d.data.pinnedUpstreamTag -and $d.data.limits }

$shells = [ordered]@{
    PowerShell = @{ Exe = (Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'); Arguments = { param($c) "-NoProfile -NonInteractive -Command `"$c`"" }; Sample = { param($f) "'$f'" } }
    cmd        = @{ Exe = (Join-Path $env:SystemRoot 'System32\cmd.exe'); Arguments = { param($c) "/d /s /c `"$c`"" }; Sample = { param($f) "`"$f`"" } }
}
if (-not $SkipGitBash) {
    $git = Get-Command git.exe -ErrorAction SilentlyContinue
    # git.exe is in Git\cmd or Git\mingw64\bin; walk up to the directory that has bin\bash.exe
    $bash = $null
    if ($git) {
        for ($dir = Split-Path $git.Source; $dir -and -not $bash; $dir = Split-Path $dir) {
            if (Test-Path (Join-Path $dir 'bin\bash.exe')) { $bash = Join-Path $dir 'bin\bash.exe' }
        }
    }
    if ($bash) { $shells['Git Bash'] = @{ Exe = $bash; Arguments = { param($c) "-lc `"$c`"" }; Sample = { param($f) "'" + ($f -replace '\\', '/') + "'" } } }
    else { Assert-Check $false 'Git Bash was found (use -SkipGitBash if Git for Windows is not installed)' }
}

foreach ($name in $shells.Keys) {
    Write-Host "Fresh $name"
    $shell = $shells[$name]
    $sample = & $shell.Sample $SampleFile
    Test-Response $name 'info' $infoCheck (Invoke-FreshShell $shell.Exe (& $shell.Arguments "pp-cli info --file $sample"))
    Test-Response $name 'describe' $describeCheck (Invoke-FreshShell $shell.Exe (& $shell.Arguments 'pp-cli describe'))
}

if ($failures.Count -gt 0) { Write-Host "`n$($failures.Count) check(s) failed."; exit 1 }
Write-Host "`nSmoke check passed."
