<#
.SYNOPSIS
Removes what install.ps1 created for the current user.

.DESCRIPTION
Deletes the install directory (including the bundled runtime and the AppCDS archive) and removes
the install directory from the user PATH. Backups and lock files next to client files are never
touched, because `pp-cli restore` depends on the backups. Running it again with nothing installed
exits 0.

Run the copy in the install directory: it uninstalls the directory it sits in. Pass -InstallDir
to uninstall another directory.

.PARAMETER InstallDir
What to uninstall. Defaults to the directory of this script when it is an installed copy (the
installer generates pp-cli.jsa next to it), otherwise %LOCALAPPDATA%\pp-cli.

.PARAMETER SkipPath
Do not touch the user PATH.
#>
[CmdletBinding()]
param(
    [string]$InstallDir,
    [switch]$SkipPath
)

$ErrorActionPreference = 'Stop'

if (-not $InstallDir) {
    $InstallDir = if ($PSScriptRoot -and (Test-Path (Join-Path $PSScriptRoot 'pp-cli.jsa'))) { $PSScriptRoot }
    else { Join-Path $env:LOCALAPPDATA 'pp-cli' }
}
$installDir = [IO.Path]::GetFullPath($InstallDir).TrimEnd('\')

function Remove-UserPathEntry([string]$Entry) {
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true)
    try {
        $current = [string]$key.GetValue('Path', '', [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
        $parts = $current.Split(';')
        $kept = @($parts | Where-Object { [Environment]::ExpandEnvironmentVariables($_).Trim().TrimEnd('\') -ine $Entry })
        if ($kept.Count -eq $parts.Count) { return $false }

        $key.SetValue('Path', ($kept -join ';'), [Microsoft.Win32.RegistryValueKind]::ExpandString)
    }
    finally { $key.Close() }

    # tell running programs (Explorer, and so new terminals) that the environment changed
    if (-not ('PpCli.Native' -as [type])) {
        Add-Type -Namespace PpCli -Name Native -MemberDefinition @'
[System.Runtime.InteropServices.DllImport("user32.dll", SetLastError = true, CharSet = System.Runtime.InteropServices.CharSet.Auto)]
public static extern System.IntPtr SendMessageTimeout(System.IntPtr hWnd, uint msg, System.UIntPtr wParam, string lParam, uint flags, uint timeout, out System.UIntPtr result);
'@
    }
    $result = [UIntPtr]::Zero
    [void][PpCli.Native]::SendMessageTimeout([IntPtr]0xffff, 0x1A, [UIntPtr]::Zero, 'Environment', 2, 5000, [ref]$result)
    return $true
}

$exists = Test-Path -LiteralPath $installDir
if ($exists) {
    foreach ($name in 'pp-cli.cmd', 'lib', 'runtime') {
        if (-not (Test-Path -LiteralPath (Join-Path $installDir $name))) {
            throw "$installDir does not look like a pp-cli installation ($name is missing). Nothing was removed."
        }
    }
}

$removed = $false
if ($exists) {
    # a directory cannot be deleted while it is the working directory of this process
    foreach ($cwd in (Get-Location).ProviderPath, [Environment]::CurrentDirectory) {
        if ($cwd.TrimEnd('\') -ieq $installDir -or $cwd.StartsWith("$installDir\", [StringComparison]::OrdinalIgnoreCase)) {
            Set-Location ([IO.Path]::GetTempPath())
            [Environment]::CurrentDirectory = [IO.Path]::GetTempPath()
            break
        }
    }
    try { Remove-Item -LiteralPath $installDir -Recurse -Force }
    catch { throw "Could not remove $installDir (is pp-cli running?). Close it and run uninstall.ps1 again. $_" }
    Write-Host "Removed $installDir"
    $removed = $true
}
else {
    Write-Host "$installDir does not exist; nothing to remove there."
}

if ($SkipPath) { Write-Host 'Skipped the user PATH (-SkipPath).' }
elseif (Remove-UserPathEntry $installDir) {
    Write-Host "Removed $installDir from the user PATH. Open a new terminal (restart VS Code) to pick it up."
    $removed = $true
}
else {
    Write-Host 'The user PATH does not contain the install directory.'
}

if ($removed) { Write-Host 'Kept the backups folders and .<name>.pp-cli.lock files next to your client files, because restore needs them.' }
else { Write-Host 'Nothing to remove.' }
