# Access control and permissions

We support fine-grained permissions so that you're able to specify exactly what the agent is allowed to do (e.g. run tests, run linter) and what it is not allowed to do (e.g. update cloud infrastructure). These permission settings can be checked into version control and distributed to all developers in your organization, as well as customized by individual developers.

## Permission system

Claude Code uses a tiered permission system to balance power and safety:

| Tool Type         | Example              | Approval Required | "Yes, don't ask again" Behavior               |
| :---------------- | :------------------- | :---------------- | :-------------------------------------------- |
| Read-only         | File reads, LS, Grep | No                | N/A                                           |
| Bash Commands     | Shell execution      | Yes               | Permanently per project directory and command |
| File Modification | Edit/write files     | Yes               | Until session end                             |

## Configuring permissions

You can view & manage Claude Code's tool permissions with `/permissions`. This UI lists all permission rules and the settings.json file they are sourced from.

* **Allow** rules will allow Claude Code to use the specified tool without further manual approval.
* **Deny** rules will prevent Claude Code from using the specified tool. Deny rules take precedence over allow rules.
* **Additional directories** extend Claude's file access to directories beyond the initial working directory.
* **Default mode** controls Claude's permission behavior when encountering new requests.

Permission rules use the format: `Tool` or `Tool(optional-specifier)`

A rule that is just the tool name matches any use of that tool. For example, adding `Bash` to the list of allow rules would allow Claude Code to use the Bash tool without requiring user approval.

### Permission modes

Claude Code supports several permission modes that can be set as the `defaultMode` in [settings files](/en/docs/claude-code/settings#settings-files):

| Mode                | Description                                                                  |
| :------------------ | :--------------------------------------------------------------------------- |
| `default`           | Standard behavior - prompts for permission on first use of each tool         |
| `acceptEdits`       | Automatically accepts file edit permissions for the session                  |
| `plan`              | Plan mode - Claude can analyze but not modify files or execute commands      |
| `bypassPermissions` | Skips all permission prompts (requires safe environment - see warning below) |

### Working directories

By default, Claude has access to files in the directory where it was launched. You can extend this access:

* **During startup**: Use `--add-dir <path>` CLI argument
* **During session**: Use `/add-dir` slash command
* **Persistent configuration**: Add to `additionalDirectories` in [settings files](/en/docs/claude-code/settings#settings-files)

Files in additional directories follow the same permission rules as the original working directory - they become readable without prompts, and file editing permissions follow the current permission mode.

### Tool-specific permission rules

Some tools support more fine-grained permission controls:

**Bash**

* `Bash(npm run build)` Matches the exact Bash command `npm run build`
* `Bash(npm run test:*)` Matches Bash commands starting with `npm run test`.

<Tip>
  Claude Code is aware of shell operators (like `&&`) so a prefix match rule like `Bash(safe-cmd:*)` won't give it permission to run the command `safe-cmd && other-cmd`
</Tip>

**Read & Edit**

`Edit` rules apply to all built-in tools that edit files. Claude will make a best-effort attempt to apply `Read` rules to all built-in tools that read files like Grep, Glob, and LS.

Read & Edit rules both follow the [gitignore](https://git-scm.com/docs/gitignore) specification. Patterns are resolved relative to the directory containing `.claude/settings.json`. To reference an absolute path, use `//`. For a path relative to your home directory, use `~/`.

* `Edit(docs/**)` Matches edits to files in the `docs` directory of your project
* `Read(~/.zshrc)` Matches reads to your `~/.zshrc` file
* `Edit(//tmp/scratch.txt)` Matches edits to `/tmp/scratch.txt`

**WebFetch**

* `WebFetch(domain:example.com)` Matches fetch requests to example.com

**MCP**

* `mcp__puppeteer` Matches any tool provided by the `puppeteer` server (name configured in Claude Code)
* `mcp__puppeteer__puppeteer_navigate` Matches the `puppeteer_navigate` tool provided by the `puppeteer` server

## Additional permission control with hooks

[Claude Code hooks](/en/docs/claude-code/hooks-guide) provide a way to register custom shell commands to perform permission evaluation at runtime. When Claude Code makes a tool call, PreToolUse hooks run before the permission system runs, and the hook output can determine whether to approve or deny the tool call in place of the permission system.

## Settings precedence

When multiple settings sources exist, they are applied in the following order (highest to lowest precedence):

1. Enterprise policies
2. Command line arguments
3. Local project settings (`.claude/settings.local.json`)
4. Shared project settings (`.claude/settings.json`)
5. User settings (`~/.claude/settings.json`)

This hierarchy ensures that organizational policies are always enforced while still allowing flexibility at the project and user levels where appropriate.

