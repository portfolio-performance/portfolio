# 36: Bundle, install, smoke-check and uninstall the skill

Type: task
Status: ready-for-agent
Blocked by: 31, 32, 34, 35

Spec: [spec.md](../spec.md), user stories 110 and 117, "Agent skill" and "Seam 2" under "Testing Decisions". ADR 0003.

## What to build

The skill from ticket 35 reaches both agents on each of the owner's machines with the same step that installs `pp-cli`, and leaves with it.

- **Bundle**: `windows/build-bundle.ps1` and `macos/build-bundle.sh` copy `skills/pp-cli/` into the bundle.
- **Install** (`install.ps1` / `install.sh`, which stay idempotent): copy the skill to `~/.agents/skills/pp-cli/`, replacing any existing folder. Then make `~/.claude/skills/pp-cli` a symlink to that folder, because Claude Code does not read `.agents`. Updating `pp-cli` re-runs the same step, so the skill always matches the installed CLI.
- **Uninstall** (`uninstall.ps1` / `uninstall.sh`): remove the Claude symlink itself without following it, then `~/.agents/skills/pp-cli/`, one output line per item. With nothing installed it exits 0 and says so.
- **Smoke check** (`smoke-check.ps1` / `smoke-check.sh`): both locations exist, `SKILL.md` has the `pp-cli` name, and the Claude link resolves to the canonical copy.
- **Docs**: an `Agent skill` section in `packaging/README.md` (where it is installed and why two locations, how to skip it, the Windows privilege note) and the install, update, uninstall and smoke-check steps updated.

## Decisions

- **Canonical location** `~/.agents/skills/pp-cli`, read by Copilot, Codex, Cursor and others. Claude Code's personal skills are only under `~/.claude/skills/`, so it gets a symlink (it follows symlinked skill folders).
- **The folders are ours**: install replaces both `pp-cli` entries whatever they are, and uninstall removes them. There is no marker file and no ownership check.
- **Windows stays non-admin.** Only the symlink needs elevation or Developer Mode. If it can't be created, the installer finishes everything else (the `%LOCALAPPDATA%\pp-cli` install, the AppCDS archive, the `PATH` entry, the `~/.agents` copy), prints the exact elevated command that completes the link, and exits non-zero at the end. Re-running in an elevated shell, or with Developer Mode on, completes it.
- **macOS** creates the symlink with `ln -sfn` and needs no privilege.
- **Interface mirrors the existing scripts**: Windows `-SkipSkill`, `-AgentsSkillsDir` and `-ClaudeSkillsDir`; macOS `--skip-skill`, `--agents-skills-dir` and `--claude-skills-dir`. The overrides exist so tests never touch the real home folders; the defaults are `$HOME/.agents/skills` and `$HOME/.claude/skills`.
- **Uninstall without the skill present** is not an error. `-SkipSkill` / `--skip-skill` on uninstall leaves the skill in place.
- **Do not name any skill folder `synced`**: Claude Code reserves it. Ours is `pp-cli`.
- **ADR 0003 and the spec** already record this (Consequences line, "Agent skill" decisions), so nothing more to write there. No glossary change.

## Acceptance criteria

- [ ] After install into temp directories, the bundle's skill is at the canonical path, the Claude path is a symlink to it, and a second install gives the same result
- [ ] Uninstall removes the symlink (leaving no dangling link) and the canonical folder, and a second uninstall exits 0
- [ ] Windows without symlink privilege: everything else is installed, the output shows the elevated command, and the exit code is non-zero; with privilege (or Developer Mode) the link is created and the exit code is 0
- [ ] `-SkipSkill` / `--skip-skill` touches neither skills directory, on install and on uninstall
- [ ] The existing uninstall tests (`packaging/test/`) are extended for the skill and pass, with temp directories only
- [ ] The smoke check passes on both platforms and fails if the link is missing or points elsewhere
- [ ] The installed skill is picked up once by hand in Copilot (VS Code) and in Claude Code on each machine
- [ ] `packaging/README.md` documents the above
