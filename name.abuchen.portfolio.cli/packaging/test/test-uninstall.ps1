<#
.SYNOPSIS
Tests windows\uninstall.ps1, the agent skill step of install.ps1 and the skill check of smoke-check.ps1 end to end in temp directories.

.DESCRIPTION
Builds fake installations (launcher, lib, runtime, AppCDS archive, skill) in temp directories and runs the
real script in a separate Windows PowerShell process. The skills directories are temp directories too (or the
scripts run with -SkipSkill), so ~\.agents and ~\.claude are never touched. The PATH round trip uses the real user PATH
in the registry: the value is backed up first and restored at the end, whatever happens.
A shell without symlink privilege cannot create the Claude link, so there a junction stands in for it and the
unprivileged install is checked instead of the linked one.
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
    New-Item -ItemType Directory (Join-Path $dir 'skills\pp-cli') | Out-Null
    Set-Content (Join-Path $dir 'skills\pp-cli\SKILL.md') "---`nname: pp-cli`ndescription: test`n---`n# pp-cli"
    Set-Content (Join-Path $dir 'skills\pp-cli\write-protocol.md') 'protocol'
    # install.ps1 only needs the launcher to write the AppCDS archive when asked
    Set-Content (Join-Path $dir 'pp-cli.cmd') "@echo off`r`nif defined PP_CLI_BUILD_ARCHIVE type nul > `"%~dp0pp-cli.jsa`""
    Copy-Item (Join-Path (Split-Path $uninstaller) 'install.ps1'), $uninstaller $dir
    $dir
}

# a directory symlink, or a junction (which needs no privilege) when the caller cannot create symlinks
function New-DirLink([string]$Path, [string]$Target, [switch]$Junction) {
    New-Item -ItemType $(if ($Junction) { 'Junction' } else { 'SymbolicLink' }) -Path $Path -Target $Target | Out-Null
}

function Test-CanSymlink {
    $probe = Join-Path $root 'symlink-probe'
    try { New-DirLink $probe $root; $true } catch { $false } finally { if (Test-Path $probe) { [IO.Directory]::Delete($probe) } }
}

function Test-IsLink([string]$Path) {
    $item = Get-Item -LiteralPath $Path -Force -ErrorAction SilentlyContinue
    [bool]($item -and $item.LinkType)
}

function Get-LinkTarget([string]$Path) { [IO.Path]::GetFullPath(@((Get-Item -LiteralPath $Path -Force).Target)[0]).TrimEnd('\') }

# Scripts that handle the agent skill get -SkipSkill unless -Skill is given, and then the caller must pass
# both skills directories, so no test can reach the real ~\.agents or ~\.claude.
function Invoke-Script([string]$Script, [string[]]$Arguments = @(), [string]$WorkingDirectory = $root, [switch]$Skill) {
    if (-not $Skill) { $Arguments += '-SkipSkill' }
    elseif ($Arguments -notcontains '-AgentsSkillsDir' -or $Arguments -notcontains '-ClaudeSkillsDir') { throw 'A skill test must pass -AgentsSkillsDir and -ClaudeSkillsDir.' }
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

    $canLink = Test-CanSymlink
    Write-Host "Agent skill ($(if ($canLink) { 'symlink privilege available' } else { 'no symlink privilege: junctions stand in for the link and the unprivileged install is checked' }))"
    $bundle = New-FakeBundle 'skill bundle'
    $dir = Join-Path $root 'skill install'
    $agents = Join-Path $root 'home\.agents\skills'
    $claude = Join-Path $root 'home\.claude\skills'
    $canonical = Join-Path $agents 'pp-cli'
    $link = Join-Path $claude 'pp-cli'
    $skillArgs = @('-InstallDir', $dir, '-SkipPath', '-AgentsSkillsDir', $agents, '-ClaudeSkillsDir', $claude)

    function Assert-Installed([string]$Label) {
        Assert-That ((Test-Path (Join-Path $dir 'pp-cli.jsa')) -and (Test-Path (Join-Path $dir 'uninstall.ps1'))) "$Label`: pp-cli itself is installed"
        Assert-That ((Get-Content (Join-Path $canonical 'SKILL.md') -Raw) -ceq (Get-Content (Join-Path $bundle 'skills\pp-cli\SKILL.md') -Raw)) "$Label`: the bundle's skill is at the canonical path"
        Assert-That (Test-Path (Join-Path $canonical 'write-protocol.md')) "$Label`: the write protocol came along"
        if ($canLink) {
            Assert-That ((Test-IsLink $link) -and ((Get-LinkTarget $link) -ieq $canonical)) "$Label`: the Claude path is a symlink to the canonical copy"
        }
        else {
            Assert-That (-not (Test-Path $link)) "$Label`: no Claude entry is left behind"
        }
    }

    function Assert-InstallExit($Result, [string]$Label) {
        if ($canLink) { Assert-That ($Result.ExitCode -eq 0) "$Label`: exit code 0 ($($Result.ExitCode): $($Result.Output))" }
        else {
            Assert-That ($Result.ExitCode -ne 0) "$Label`: exit code is not 0 without symlink privilege"
            Assert-That ($Result.Output.Contains("New-Item -ItemType SymbolicLink -Path '$link' -Target '$canonical'")) "$Label`: the output shows the elevated command ($($Result.Output))"
        }
    }

    $result = Invoke-Script (Join-Path $bundle 'install.ps1') $skillArgs -Skill
    Assert-InstallExit $result 'first install'
    Assert-Installed 'first install'

    Set-Content (Join-Path $canonical 'stale.txt') 'old'
    if (Test-IsLink $link) { [IO.Directory]::Delete($link) }
    New-Item -ItemType Directory $link | Out-Null
    Set-Content (Join-Path $link 'mine.txt') 'x'
    $result = Invoke-Script (Join-Path $bundle 'install.ps1') $skillArgs -Skill
    Assert-InstallExit $result 'second install'
    Assert-Installed 'second install'
    Assert-That (-not (Test-Path (Join-Path $canonical 'stale.txt'))) 'second install: the canonical folder was replaced, not merged'

    Write-Host 'Uninstalling the skill'
    if (-not (Test-IsLink $link)) { if (Test-Path $link) { Remove-Item -Recurse -Force $link }; New-DirLink $link $canonical -Junction }
    $result = Invoke-Script (Join-Path $dir 'uninstall.ps1') @('-AgentsSkillsDir', $agents, '-ClaudeSkillsDir', $claude) $dir -Skill
    Assert-That ($result.ExitCode -eq 0) "exit code 0 ($($result.ExitCode): $($result.Output))"
    Assert-That (-not (Test-IsLink $link) -and -not (Test-Path $link)) 'the Claude link is gone, not dangling'
    Assert-That (-not (Test-Path $canonical)) 'the canonical folder is gone'
    Assert-That (($result.Output -split "`n" | Where-Object { $_.Contains($link) }).Count -eq 1 -and ($result.Output -split "`n" | Where-Object { $_.Contains($canonical) }).Count -eq 1) "one output line per item ($($result.Output))"
    $result = Invoke-Script $uninstaller (@('-InstallDir', $dir, '-AgentsSkillsDir', $agents, '-ClaudeSkillsDir', $claude)) -Skill
    Assert-That ($result.ExitCode -eq 0) "a second uninstall exits 0 ($($result.ExitCode): $($result.Output))"
    Assert-That ($result.Output -match 'Nothing to remove\.') 'a second uninstall says there is nothing to remove'

    Write-Host 'Removing the Claude link without following it'
    $precious = Join-Path $root 'precious'
    New-Item -ItemType Directory $precious, $canonical | Out-Null
    Set-Content (Join-Path $precious 'keep.txt') 'keep me'
    New-DirLink $link $precious -Junction:(-not $canLink)
    $result = Invoke-Script $uninstaller @('-InstallDir', $dir, '-AgentsSkillsDir', $agents, '-ClaudeSkillsDir', $claude) -Skill
    Assert-That ($result.ExitCode -eq 0 -and -not (Test-IsLink $link) -and -not (Test-Path $link)) "the link is removed ($($result.Output))"
    Assert-That (Test-Path (Join-Path $precious 'keep.txt')) 'what the link pointed to is untouched'

    Write-Host 'Removing a dangling Claude link'
    $gone = Join-Path $root 'gone'
    New-Item -ItemType Directory $gone | Out-Null
    New-DirLink $link $gone -Junction:(-not $canLink)
    Remove-Item -Recurse -Force $gone
    Assert-That (Test-IsLink $link) 'the link is dangling before the uninstall'
    $result = Invoke-Script $uninstaller @('-InstallDir', $dir, '-AgentsSkillsDir', $agents, '-ClaudeSkillsDir', $claude) -Skill
    Assert-That ($result.ExitCode -eq 0 -and -not (Test-IsLink $link)) "the dangling link is removed ($($result.Output))"
    New-Item -ItemType Directory $gone | Out-Null
    New-DirLink $link $gone -Junction:(-not $canLink)
    Remove-Item -Recurse -Force $gone
    $result = Invoke-Script (Join-Path $bundle 'install.ps1') $skillArgs -Skill
    Assert-That (-not (Test-IsLink $link) -or (Test-Path (Join-Path $link 'SKILL.md'))) 'an install replaces a dangling link'

    Write-Host '-SkipSkill touches neither skills directory'
    $skipAgents = Join-Path $root 'skip home\.agents\skills'
    $skipClaude = Join-Path $root 'skip home\.claude\skills'
    $result = Invoke-Script (Join-Path $bundle 'install.ps1') @('-InstallDir', (Join-Path $root 'skip skill install'), '-SkipPath', '-SkipSkill', '-AgentsSkillsDir', $skipAgents, '-ClaudeSkillsDir', $skipClaude) -Skill
    Assert-That ($result.ExitCode -eq 0) "install exits 0 ($($result.ExitCode): $($result.Output))"
    Assert-That (-not (Test-Path (Join-Path $root 'skip home'))) 'install created no skills directory'
    New-Item -ItemType Directory (Join-Path $skipAgents 'pp-cli'), $skipClaude | Out-Null
    Set-Content (Join-Path $skipAgents 'pp-cli\SKILL.md') 'mine'
    Set-Content (Join-Path $skipClaude 'pp-cli') 'mine'
    $result = Invoke-Script $uninstaller @('-InstallDir', (Join-Path $root 'skip skill install'), '-SkipSkill', '-AgentsSkillsDir', $skipAgents, '-ClaudeSkillsDir', $skipClaude) -Skill
    Assert-That ($result.ExitCode -eq 0) "uninstall exits 0 ($($result.ExitCode): $($result.Output))"
    Assert-That ((Test-Path (Join-Path $skipAgents 'pp-cli\SKILL.md')) -and (Test-Path (Join-Path $skipClaude 'pp-cli'))) 'uninstall left both skill entries in place'

    Write-Host 'Smoke check of the skill'
    $smoke = Join-Path (Split-Path $uninstaller) 'smoke-check.ps1'
    function New-SkillHome([string]$Name, [string]$SkillName = 'pp-cli') {
        $script:sAgents = Join-Path $root "$Name\.agents\skills"
        $script:sClaude = Join-Path $root "$Name\.claude\skills"
        $script:sCanonical = Join-Path $sAgents 'pp-cli'
        $script:sLink = Join-Path $sClaude 'pp-cli'
        New-Item -ItemType Directory $sCanonical, $sClaude | Out-Null
        Set-Content (Join-Path $sCanonical 'SKILL.md') "---`nname: $SkillName`ndescription: test`n---"
    }
    function Invoke-SkillSmoke { Invoke-Script $smoke @('-SkillOnly', '-AgentsSkillsDir', $sAgents, '-ClaudeSkillsDir', $sClaude) -Skill }
    New-SkillHome 'smoke good'
    New-DirLink $sLink $sCanonical -Junction:(-not $canLink)
    $result = Invoke-SkillSmoke
    Assert-That ($result.ExitCode -eq 0 -and $result.Output -match 'Smoke check passed') "passes with both locations and a link to the canonical copy ($($result.ExitCode): $($result.Output))"
    New-SkillHome 'smoke no link'
    $result = Invoke-SkillSmoke
    Assert-That ($result.ExitCode -eq 1 -and $result.Output -match 'FAIL') 'fails when the Claude link is missing'
    New-SkillHome 'smoke elsewhere'
    $elsewhere = Join-Path $root 'smoke elsewhere\other'
    New-Item -ItemType Directory $elsewhere | Out-Null
    Set-Content (Join-Path $elsewhere 'SKILL.md') "---`nname: pp-cli`n---"
    New-DirLink $sLink $elsewhere -Junction:(-not $canLink)
    $result = Invoke-SkillSmoke
    Assert-That ($result.ExitCode -eq 1 -and $result.Output -match 'FAIL') 'fails when the Claude link points elsewhere'
    New-SkillHome 'smoke wrong name' 'other-skill'
    New-DirLink $sLink $sCanonical -Junction:(-not $canLink)
    $result = Invoke-SkillSmoke
    Assert-That ($result.ExitCode -eq 1 -and $result.Output -match 'FAIL') 'fails when SKILL.md does not name pp-cli'
    New-SkillHome 'smoke no canonical'
    New-DirLink $sLink $sCanonical -Junction:(-not $canLink)
    [IO.Directory]::Delete($sLink)
    Remove-Item -Recurse -Force $sCanonical
    $result = Invoke-SkillSmoke
    Assert-That ($result.ExitCode -eq 1 -and $result.Output -match 'FAIL') 'fails when the canonical folder is missing'
}
finally {
    Set-RawUserPath $originalPath
    Remove-Item -Recurse -Force $root -ErrorAction SilentlyContinue
}

Assert-That ((Get-RawUserPath) -ceq $originalPath) 'the real user PATH is restored'
if ($failures -gt 0) { Write-Host "`n$failures check(s) failed."; exit 1 }
Write-Host "`nAll checks passed."
