# 02: Skill password setup keeps the password out of shell history

**What to build:** The `pp-cli` agent skill tells the user how to set `PP_PASSWORD` without leaving the password in shell history. The current literal-assignment forms (`$env:PP_PASSWORD = '<password>'`, `export PP_PASSWORD='<password>'`) are stored by PSReadLine, `~/.bash_history` and `~/.zsh_history`, which defeats the reason for the env-var design (spec user story 17: no leaks into shell history or process listings). Replace them with prompt-based forms that do not echo the password and leave nothing in history. Suggested: PowerShell `$env:PP_PASSWORD = Read-Host -MaskInput 'PP password'`; bash/zsh `read -rs PP_PASSWORD && export PP_PASSWORD`. Verify each form works as written. Keep the existing rules: show the user both forms on `password-required`, wait on `incorrect-password`, and never ask for the password in chat, type it into a command, or pipe it to `pp-cli`.

**Implementer note:** This changes a skill file. Use `/writing-for-agents` before editing it.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Neither shell form writes the literal password into a command line that history records
- [ ] The prompt does not echo the typed password
- [ ] Both forms were run once in a real shell and leave `PP_PASSWORD` set for the child process
- [ ] The existing never-ask, never-type, never-pipe rule is unchanged
- [ ] Origin: CodeRabbit review thread on PR #6262 (SKILL.md, line 23)
