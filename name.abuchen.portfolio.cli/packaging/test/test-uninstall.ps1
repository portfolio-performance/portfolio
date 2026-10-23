<#
.SYNOPSIS
Tests windows\uninstall.ps1 end to end in temp directories.

.DESCRIPTION
Builds fake installations (launcher, lib, runtime, AppCDS archive) in temp directories and runs the
real script in a separate Windows PowerShell process. The PATH round trip uses the real user PATH
in the registry: the value is backed up first and restored at the end, whatever happens.
Exits with 1 if anything fails.
#>
$ErrorActionPreference = 'Stop'

$uninstaller = Join-Path $PSScriptRoot '..\windows\uninstall.ps1'
$root = Join-Path ([IO.Path]::GetTempPath()) ("pp-cli-uninstall-test-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $root | Out-Null
$failures = 0

function Assert-That([bool]$Condition, [string]$Description) {
    if ($Condition) { Write-Host "  ok    $Description" }
    else { Write-Host "  FAIL  $Description"; $script:failures++ }
}

function New-FakeInstall([string]$Name) {
    $dir = Join-Path $root $Name
    New-Item -ItemType Directory (Join-Path $dir 'lib'), (Join-Path $dir 'runtime\bin'), (Join-Path $dir 'sample') | Out-Null
    foreach ($file in 'pp-cli', 'pp-cli.cmd', 'pp-cli.jsa', 'install.ps1', 'lib\core.jar', 'runtime\bin\java.exe') { Set-Content (Join-Path $dir $file) 'x' }
    Copy-Item $uninstaller (Join-Path $dir 'uninstall.ps1')
    $dir
}

function New-FakeBundle([string]$Name) {
    $dir = Join-Path $root $Name
    New-Item -ItemType Directory (Join-Path $dir 'lib'), (Join-Path $dir 'runtime\bin'), (Join-Path $dir 'sample') | Out-Null
    foreach ($file in 'pp-cli', 'lib\core.jar', 'runtime\bin\java.exe', 'sample\sample-client.xml') { Set-Content (Join-Path $dir $file) 'x' }
    # install.ps1 only needs the launcher to write the AppCDS archive when asked
    Set-Content (Join-Path $dir 'pp-cli.cmd') "@echo off`r`nif defined PP_CLI_BUILD_ARCHIVE type nul > `"%~dp0pp-cli.jsa`""
    Copy-Item (Join-Path (Split-Path $uninstaller) 'install.ps1'), $uninstaller $dir
    $dir
}

function Invoke-Script([string]$Script, [string[]]$Arguments = @(), [string]$WorkingDirectory = $root) {
    Push-Location $WorkingDirectory
    $ErrorActionPreference = 'Continue' # stderr of the child must not stop this script
    try {
        $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $Script @Arguments 2>&1 | ForEach-Object { "$_" }
        [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = ($output -join "`n") }
    }
    finally { $ErrorActionPreference = 'Stop'; Pop-Location }
}

function Get-RawUserPath {
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment')
    try { [string]$key.GetValue('Path', $null, [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames) }
    finally { $key.Close() }
}

function Set-RawUserPath($Value) {
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true)
    try {
        if ($null -eq $Value) { $key.DeleteValue('Path', $false) }
        else { $key.SetValue('Path', $Value, [Microsoft.Win32.RegistryValueKind]::ExpandString) }
    }
    finally { $key.Close() }
}

$originalPath = Get-RawUserPath
try {
    Write-Host 'Uninstalling from the installed copy into a custom directory'
    $dir = New-FakeInstall 'custom install'
    $clientDir = Join-Path $root 'client'
    New-Item -ItemType Directory (Join-Path $clientDir 'backups') | Out-Null
    Set-Content (Join-Path $clientDir 'backups\client.xml') 'backup'
    Set-Content (Join-Path $clientDir '.client.xml.pp-cli.lock') ''
    $result = Invoke-Script (Join-Path $dir 'uninstall.ps1') @('-SkipPath') $dir
    Assert-That ($result.ExitCode -eq 0) "exit code 0 ($($result.ExitCode): $($result.Output))"
    Assert-That (-not (Test-Path $dir)) 'the install directory is gone, although the script ran from it'
    Assert-That ($result.Output -match 'Kept the backups folders and \.<name>\.pp-cli\.lock files') 'the output says backups and lock files are kept'
    Assert-That ((Test-Path (Join-Path $clientDir 'backups\client.xml')) -and (Test-Path (Join-Path $clientDir '.client.xml.pp-cli.lock'))) 'backups and lock files are untouched'

    Write-Host 'Running it again'
    $result = Invoke-Script $uninstaller @('-InstallDir', $dir, '-SkipPath')
    Assert-That ($result.ExitCode -eq 0) "exit code 0 ($($result.ExitCode): $($result.Output))"
    Assert-That ($result.Output -match 'Nothing to remove\.') 'the output says there is nothing to remove'

    Write-Host 'Refusing a directory that is not a pp-cli installation'
    $other = Join-Path $root 'other'
    New-Item -ItemType Directory (Join-Path $other 'lib') | Out-Null
    Set-Content (Join-Path $other 'precious.txt') 'keep me'
    Set-RawUserPath "C:\Before;$other"
    $result = Invoke-Script $uninstaller @('-InstallDir', $other)
    Assert-That ($result.ExitCode -ne 0) 'exit code is not 0'
    Assert-That ($result.Output -match 'does not look like a pp-cli installation') "the output explains the refusal ($($result.Output))"
    Assert-That (Test-Path (Join-Path $other 'precious.txt')) 'the directory is untouched'
    Assert-That ((Get-RawUserPath) -ceq "C:\Before;$other") 'the user PATH is untouched'
    Set-RawUserPath $originalPath

    Write-Host 'User PATH round trip with the real install.ps1'
    $bundle = New-FakeBundle 'bundle'
    $dir = Join-Path $root 'path install'
    foreach ($before in $originalPath, '', 'C:\One;%SystemRoot%\system32', $null) {
        Set-RawUserPath $before
        $result = Invoke-Script (Join-Path $bundle 'install.ps1') @('-InstallDir', $dir)
        Assert-That ($result.ExitCode -eq 0 -and (Test-Path (Join-Path $dir 'uninstall.ps1')) -and (Get-RawUserPath).EndsWith($dir)) "installed and added to the PATH ($($result.ExitCode): $($result.Output))"
        $result = Invoke-Script (Join-Path $dir 'uninstall.ps1') @() $dir
        Assert-That ($result.ExitCode -eq 0) "exit code 0 ($($result.ExitCode): $($result.Output))"
        $label = if ($null -eq $before) { '(no value)' } elseif ($before -eq $originalPath) { 'the real user PATH' } else { $before }
        Assert-That ((Get-RawUserPath) -ceq $(if ($null -eq $before) { '' } else { $before })) "the PATH is byte-identical after install and uninstall: $label"
    }

    Write-Host 'The PATH entry is matched like the installer does and other entries stay'
    $dir = New-FakeInstall 'match install'
    Set-RawUserPath "C:\Before;$($dir.ToUpper())\;C:\After;$dir\bin"
    $result = Invoke-Script $uninstaller @('-InstallDir', $dir)
    Assert-That ((Get-RawUserPath) -ceq "C:\Before;C:\After;$dir\bin") "only the entry equal to the install directory is removed ($(Get-RawUserPath))"

    Write-Host '-SkipPath leaves the PATH alone'
    $dir = New-FakeInstall 'skip install'
    Set-RawUserPath "C:\Before;$dir"
    $result = Invoke-Script $uninstaller @('-InstallDir', $dir, '-SkipPath')
    Assert-That (-not (Test-Path $dir)) 'the install directory is gone'
    Assert-That ((Get-RawUserPath) -ceq "C:\Before;$dir") 'the user PATH is untouched'
}
finally {
    Set-RawUserPath $originalPath
    Remove-Item -Recurse -Force $root -ErrorAction SilentlyContinue
}

Assert-That ((Get-RawUserPath) -ceq $originalPath) 'the real user PATH is restored'
if ($failures -gt 0) { Write-Host "`n$failures check(s) failed."; exit 1 }
Write-Host "`nAll checks passed."
