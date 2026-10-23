<#
.SYNOPSIS
Installs or updates pp-cli for the current user.

.DESCRIPTION
Copies this bundle to %LOCALAPPDATA%\pp-cli, adds that directory to the user PATH once and
regenerates the AppCDS archive the launcher uses. Running it again gives the same result,
so updating means rebuilding the bundle and running this script again.

.PARAMETER InstallDir
Where to install. Defaults to %LOCALAPPDATA%\pp-cli.

.PARAMETER SkipPath
Do not touch the user PATH.

.PARAMETER SkipSkill
Do not install the agent skill.

.PARAMETER AgentsSkillsDir
Where the canonical copy of the skill goes. Defaults to ~\.agents\skills (read by Copilot, Codex, Cursor and others).

.PARAMETER ClaudeSkillsDir
Where the link for Claude Code goes. Defaults to ~\.claude\skills. Claude Code does not read .agents.

.NOTES
The skill is copied to <AgentsSkillsDir>\pp-cli, replacing whatever is there, and <ClaudeSkillsDir>\pp-cli
becomes a symlink to it. Creating a symlink needs an elevated shell or Developer Mode. Without it the script
finishes everything else, prints the elevated command that completes the link and exits with 1.
#>
[CmdletBinding()]
param(
    [string]$InstallDir = (Join-Path $env:LOCALAPPDATA 'pp-cli'),
    [switch]$SkipPath,
    [switch]$SkipSkill,
    [string]$AgentsSkillsDir = (Join-Path $HOME '.agents\skills'),
    [string]$ClaudeSkillsDir = (Join-Path $HOME '.claude\skills')
)

$ErrorActionPreference = 'Stop'

$bundleDir = $PSScriptRoot
$installDir = [IO.Path]::GetFullPath($InstallDir).TrimEnd('\')
$managed = 'lib', 'runtime', 'sample', 'pp-cli', 'pp-cli.cmd', 'pp-cli.jsa', 'install.ps1', 'uninstall.ps1', 'smoke-check.ps1'

foreach ($name in 'lib', 'runtime', 'pp-cli.cmd') {
    if (-not (Test-Path (Join-Path $bundleDir $name))) { throw "$bundleDir is not a pp-cli bundle ($name is missing). Run build-bundle.ps1 first." }
}
if (-not $SkipSkill -and -not (Test-Path (Join-Path $bundleDir 'skills\pp-cli\SKILL.md'))) { throw "$bundleDir has no agent skill (skills\pp-cli\SKILL.md is missing). Run build-bundle.ps1 first." }
if ($bundleDir.TrimEnd('\') -ieq $installDir) { throw 'Run install.ps1 from the built bundle, not from the install directory.' }

function Add-UserPathEntry([string]$Entry) {
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true)
    try {
        $current = [string]$key.GetValue('Path', '', [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
        $present = $current.Split(';', [StringSplitOptions]::RemoveEmptyEntries) |
            Where-Object { [Environment]::ExpandEnvironmentVariables($_).Trim().TrimEnd('\') -ieq $Entry }
        if ($present) { return $false }

        $updated = if ($current.Trim()) { $current.TrimEnd(';') + ';' + $Entry } else { $Entry }
        $key.SetValue('Path', $updated, [Microsoft.Win32.RegistryValueKind]::ExpandString)
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

New-Item -ItemType Directory -Force $installDir | Out-Null
try {
    foreach ($name in $managed) {
        $target = Join-Path $installDir $name
        if (Test-Path $target) { Remove-Item -Recurse -Force $target }
    }
}
catch {
    throw "Could not replace the existing installation (is pp-cli running?). Close it and run install.ps1 again. $_"
}
foreach ($name in $managed) {
    $source = Join-Path $bundleDir $name
    if (Test-Path $source) { Copy-Item -Recurse $source (Join-Path $installDir $name) }
}
Write-Host "Installed pp-cli to $installDir"

$launcher = Join-Path $installDir 'pp-cli.cmd'
$archive = Join-Path $installDir 'pp-cli.jsa'
$training = Join-Path $installDir 'sample\sample-client.xml'
$env:PP_CLI_BUILD_ARCHIVE = '1'
$previousPreference = $ErrorActionPreference
try {
    # one representative run: loads a file and computes holdings, so those classes land in the archive;
    # core's log lines go to stderr, which Windows PowerShell would otherwise report as an error
    $ErrorActionPreference = 'Continue'
    & $launcher holdings --file $training 2>$null | Out-Null
    $trainingExit = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $previousPreference
    Remove-Item Env:\PP_CLI_BUILD_ARCHIVE
}
if ($trainingExit -ne 0 -or -not (Test-Path $archive)) { throw "Generating the AppCDS archive failed (exit code $trainingExit)." }
Write-Host 'Generated the AppCDS archive.'

if ($SkipPath) { Write-Host 'Skipped the user PATH (-SkipPath).' }
elseif (Add-UserPathEntry $installDir) {
    Write-Host "Added $installDir to the user PATH. Open a new terminal (restart VS Code) to pick it up."
}
else {
    Write-Host 'The user PATH already contains the install directory.'
}

# Removes whatever is at $Path; a link is removed itself and never followed
function Remove-Entry([string]$Path) {
    $item = Get-Item -LiteralPath $Path -Force -ErrorAction SilentlyContinue
    if (-not $item) { return }
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
        if ($item.Attributes -band [IO.FileAttributes]::Directory) { [IO.Directory]::Delete($Path) } else { [IO.File]::Delete($Path) }
    }
    else { Remove-Item -LiteralPath $Path -Recurse -Force }
}

if ($SkipSkill) {
    Write-Host 'Skipped the agent skill (-SkipSkill).'
    return
}

$canonical = [IO.Path]::GetFullPath((Join-Path $AgentsSkillsDir 'pp-cli'))
$link = [IO.Path]::GetFullPath((Join-Path $ClaudeSkillsDir 'pp-cli'))
New-Item -ItemType Directory -Force (Split-Path $canonical), (Split-Path $link) | Out-Null
Remove-Entry $link
Remove-Entry $canonical
Copy-Item -Recurse (Join-Path $bundleDir 'skills\pp-cli') $canonical
Write-Host "Installed the agent skill to $canonical"

function New-SkillLink {
    try { New-Item -ItemType SymbolicLink -Path $link -Target $canonical -ErrorAction Stop | Out-Null; return $null }
    catch { $reason = $_.Exception.Message }
    # Windows PowerShell 5.1 asks for elevation even in Developer Mode; mklink does not
    & $env:ComSpec /d /s /c "mklink /D `"$link`" `"$canonical`" >nul 2>&1"
    if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $link)) { return $null }
    $reason
}

$failure = New-SkillLink
if (-not $failure) { Write-Host "Linked $link to $canonical" }
else {
    Write-Host "Could not create the symlink $link ($failure)."
    Write-Host 'Claude Code reads only ~\.claude\skills. Run this in an elevated PowerShell, or turn on Developer Mode and run install.ps1 again:'
    Write-Host "  New-Item -ItemType SymbolicLink -Path '$($link.Replace("'", "''"))' -Target '$($canonical.Replace("'", "''"))'"
    exit 1
}
