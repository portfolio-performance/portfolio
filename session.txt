```
███████╗███████╗███████╗███████╗██╗ ██████╗ ███╗   ██╗███████╗
██╔════╝██╔════╝██╔════╝██╔════╝██║██╔═══██╗████╗  ██║██╔════╝
███████╗█████╗  ███████╗███████╗██║██║   ██║██╔██╗ ██║███████╗
╚════██║██╔══╝  ╚════██║╚════██║██║██║   ██║██║╚██╗██║╚════██║
███████║███████╗███████║███████║██║╚██████╔╝██║ ╚████║███████║
╚══════╝╚══════╝╚══════╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝
```

<sub>_A public good brought to you by GWUDCAP and Three AIrrows Capital_</sub>

---

<br>
<div align="center">

##### ⚡ SHOCKING REPORT REVEALS ⚡

# **Vibe coding is shitty and confusing and produces garbage software that sucks to work on.**<br>

### _Claude Code makes it less shitty, but not by enough._

---
</div>
explainer: coming soon...
<br>
working demo: https://www.youtube.com/watch?v=B-sIBb-XvO4

## 🔥 The Problem

<details>
<summary><strong>How you got here</strong> <em>(click to read the painful journey)</em></summary>

<br>

I'm going to guess how you got here and you can tell me if I get it right:

- 💭 The LLM programmer hype gave you a nerd chub  
- 😬 The people hyping LLM programming made your nerd chub crawl back into your body <br> <sup>_(are you ready to 'scale your impact', dog?)_</sup> 
- 🤮 You held your nose and downloaded Cursor/added Cline or Roo Code/npm installed Claude Code

At first this was obviously novel and interesting. Some things were shitty but mostly you were enjoying not having to write a context manager or even recognize that you needed one for your dumb client wrapper.

**You were _scaling_ your _impact_** _(whew)_.

But then Claude started doing some concerning things. 

You asked it to add error handling to **one** function. It added error handling to **_every function in the file_**. And changed your error types. And your logging format. And somehow your indentation is different now?

The context window thing started getting annoying. You're explaining the same architecture for the fifth time today. Claude's like _'let me look for the database'_ **Brother. We've been using Postgres for six hours. You were just in there.**

Your CLAUDE.md is now longer than your actual code. 
- `'NEVER use class components.'` 
- `'ALWAYS use the existing auth middleware.'` 
- `'DO NOT refactor unrelated code.'` 
- `'REMEMBER we use PostgreSQL.'` 

Claude reads the first line and then macrodoses window pane LSD for the rest.

You tried the subagents, but quickly realized that **you can't even talk to these things.** 10 minutes into a "code review" and the agent hits some kind of API error and returns to your main thread with no explanation of what it did or what it discovered. 

Run it again, I guess? 

_This fucking sucks_.

Now you're here. Your codebase is 'done' but you couldn't, in a million years, explain what that means or how it satisfies the definition. 

There's three different global clients for the same database connection and two of them use hallucinated environment variables (the other just yeets your client secret into the service code). 

You've got utility functions that are duplicated in four files because Claude kept forgetting they exist.

20% of your code lines are comments explaining why something *isn't* there and *is* somewhere else.

You don't even know exactly what's wrong and fixing it means understanding code you didn't write in patterns you don't recognize using approaches you wouldn't choose.

### **Are you scaling your impact yet?**

</details>

## 💊 The Solution

<details>
<summary><strong>What We Fixed</strong> <em>(what you get when you use cc-sessions)</em></summary>

<br>

So, now you're here. Since this is exclusively about Claude Code I'm going to assume that you are a CC user and you are looking to make that better. **Lit.**

Let's talk about Claude Code.

Of the major AI programming IDEs/scaffolds, Claude Code is probably the best and Claude models are probably the best _(though Google is kinda coming for that ass)_.

But, Claude Code is not without its **major faults, flaws, and flaccidity-inducing frustrations**.

For instance, **it would be nice if**:

- Claude had to talk to you before writing code so you didn't end up with 500 lines of implementation for a one-line change.

- you didn't lose everything when the context window died and Claude actually remembered what you were working on tomorrow.

- you didn't have to explain your entire architecture every. single. session. and Claude actually inherited understanding from previous work.

- Claude couldn't randomly refactor working code while you're trying to add a button.

- you didn't have to manually check which branch you're on in five different repos and Claude actually stopped you before you edited the wrong one.

- Claude followed the patterns in your codebase instead of inventing new ones every time it touches a file.

- you didn't have to write increasingly desperate rules in CLAUDE.md and Claude was actually forced to follow consistent behavior.

### **This is what Sessions does.**

It makes all of these nice things happen. Not through more rules or documentation or prompting techniques, but through **brute authoritarian rule over the agentic loop**.

</details>

## ⚙️ How It Works

**Sessions enforces discussion before implementation through hooks that Claude can't bypass.**

You install Sessions. Five minutes, one script. It sets up hooks that Claude can't bypass, state management that persists, and a task system that actually remembers things.

If you want to get acquainted you can read about individual features below:

<details>
<summary><strong>Claude waits for alignment</strong></summary>
<br>

Before Sessions, this was your life:

```
You: Add error handling to the auth flow

Claude: I'll add comprehensive error handling!
[IMMEDIATELY STARTS EDITING 15 FILES]
[ADDS TRY-CATCH TO EVERY FUNCTION]
[CHANGES YOUR ERROR MESSAGE FORMAT]
[REFACTORS YOUR LOGGING LIBRARY]

You: wait no stop STOP

Claude: I've updated the error handling across your entire codebase!

You: [muffled screaming]
```

With Sessions:

```
You: Add error handling to the auth flow

Claude: Let me analyze your current error patterns first...

[Claude reads auth code, examines existing error handling]

Claude: I found your error handling pattern:
- You're using custom AppError classes
- Errors bubble to middleware/errorHandler.ts  
- Auth errors specifically use 401/403 status codes
- You log errors to Sentry in production

I'll add error handling to the login and refresh token endpoints
following your existing AppError pattern. Ready?

You: perfect, go ahead

Claude: [NOW edits exactly 2 files with correct patterns]
```

The hooks literally block Write/Edit tools until you approve. Claude HAS to discuss first. No exceptions. No workarounds. No surprise refactors.

</details>

<details>
<summary><strong>OP automatic compaction</strong></summary>
<br>

When Claude's context window hits 75%, a hook will tell Claude to start wrapping shid up. At 90%, the hook will be more aggressive about it. 

When you okay (i.e. "let's compact"), Claude will use a protocol and several sub-agents to record all important info from your session to the current task file, along with next steps to begin immediately after you clear context.

You just type `/clear` and another hook will automatically yeet the current task file into Claude's context window and you can continue as if you never left.

```
[Friday, 4pm - deep in implementing a payment system]

Claude: ⚠️ Context at 75% - Consider wrapping up soon

You: I gotta run, lets compact and come back to this later.

Claude: Cool, running context-compaction.md protocol - first, let me call the logging agent to write our task log for the session...

You: [Close laptop, weekend happens]

[Monday, 9am]

You: /clear

You: lets continue

Claude: Welcome back! Resuming work on m-payment-integration.
Last session we:
- Integrated Stripe checkout flow
- Added webhook handlers for payment events  
- Created payment status tracking

Current state: Testing webhook signatures
Next: Add retry logic for failed webhooks

Let's continue with the retry logic implementation.
```

Zero context rebuilding. Claude knows exactly where you left off.

</details>

<details>
<summary><strong>Better subagents</strong></summary>
<br>

There's no obvious way to get Claude to call subagents with all of the necessary context. There's also not great visiblity into how Claude prompts them, or even what the subagents do (or why they fail when they fail).

To deal with this, we've designated specific agents for specific tasks in the sessions system.

These are (currently):
  - Context Gathering Agent | Digs through whole files at a time in your codebase to get all the right context for a task, then writes it to a context manifest in the task file so Claude doesn't have to re-learn it.
  - Code Review Agent | Reviews code changes before they are committed to ensure they follow your patterns
  - Logging Agent | Writes a log of what was done in a session to the task file

To make this a lot easier, the logging agent gets the entire conversation history - instead of relying on Claude to re-output everything important that happened in this session, the logging agent just branches off from your conversation, writes the logs, and passes it back to the main thread.

In the background, any call to a subagent copies the current conversation into a set of conversation history logs in a dedicated directory for the agent. If you ever want an agent to load the full conversation context before following their prompt, you can just adjust their agent prompt to tell them to read all of the files in .claude/state/{agent-name}/.
  
```
You: Lets start the caching upgrades                                                                                                                                                                                                                                                                  e

Claude: No problem! Let me update current-task.json and then I'll use the context-gathering agent to analyze your caching system.

[Agent runs in separate context - doesn't pollute your main thread]

Agent results: I've created a comprehensive context manifest:

CACHING ARCHITECTURE:
- Redis for session cache (30min TTL)
- PostgreSQL materialized views for reports  
- React Query for client-side caching (5min stale time)
- CDN caching for static assets

KEY PATTERNS:
- Cache keys use format: {service}:{entity}:{id}:{version}
- Invalidation happens through CacheManager.invalidate()
- All cache operations logged to cache_operations table

IMPLEMENTATION LOCATIONS:
- Server cache: /lib/cache/redis-cache.ts
- Client cache: /hooks/useQueryWithCache.ts
- Cache invalidation: /api/middleware/cache-invalidator.ts

[Full context manifest saved to task file - available forever]
```

The agent understood your patterns because it could analyze everything without worrying about preserving the context window.

</details>

<details>
<summary><strong>Tasks and Branches Is All You Need</strong></summary>
<br>

Every piece of work is a task. Every task has its own branch. Sessions enforces both.

```
You: Let's fix the payment webhooks

Claude: I'll create a task for that. What priority?
- h- (high): Critical/blocking issues
- m- (medium): Normal features/fixes  
- l- (low): Nice-to-haves
- ?- (investigate): Not sure if needed

You: high priority

Claude: Creating h-fix-payment-webhooks
[Creates task file]
[Runs context-gathering agent]
[Sets up feature/fix-payment-webhooks branch]

Context manifest created:
- Stripe webhooks configured in /api/webhooks/stripe.ts
- Events: payment_intent.succeeded, charge.failed
- Current issue: signature verification failing
- Webhook secret in STRIPE_WEBHOOK_SECRET env var

Ready to start work on feature/fix-payment-webhooks?
```

Now your task has:
- **Permanent context** that survives session restarts
- **Its own branch** that Sessions enforces
- **Work logs** tracking everything done
- **Clear scope** preventing scope creep

When you come back tomorrow:

```
You: /clear

SessionStart: Loading task h-fix-payment-webhooks
Branch: feature/fix-payment-webhooks
Last work: Fixed signature verification
Next: Add retry logic for failed events

You: continue

Claude: Picking up where we left off - implementing retry logic for failed webhook events...
```

No context rebuilding. No confusion about what branch. No losing work.

</details>

<details>
<summary><strong>Ultrathink & API Mode</strong></summary>
<br>

Sessions automatically enables Claude's deep thinking mode on every message. But if you're paying per token, that gets expensive fast.

**Max Mode (Default):**
```
You: [any message]

[Claude automatically uses maximum thinking budget]
[2-3x more tokens but better reasoning]
[$20-200/month flat rate - use all you want]
```

**API Mode (Token Saver):**
```bash
# During installation:
"Enable API mode to save tokens? (y/n)"

# Or toggle later:
/api-mode
```

In API mode:
- Ultrathink disabled by default (saves ~50-67% tokens)
- You control when to use it: `[[ ultrathink ]] solve this hard problem`
- All other Sessions features still save tokens through context preservation

The truth? Sessions **saves** tokens for API users:
- Context persistence: -2000 tokens/task (no re-explaining)
- Auto-loading: -500 tokens/session (no "what are we doing?")
- DAIC enforcement: -1000 tokens/task (no failed attempts)
- Specialized agents: -3000 tokens (work in minimal contexts)

You lose ultrathink but gain token efficiency everywhere else.

</details>

<details>
<summary><strong>Branch Enforcement</strong></summary>

```
[Working on task m-fix-auth, supposed to be on feature/fix-auth branch]

You: update the login validation

Claude: I'll update the login validation. Let me look at the current implementation.

[Claude tries to edit src/auth/login.ts]

❌ BLOCKED: Branch mismatch!
- Task requires: feature/fix-auth  
- You're on: main

Run this command:
git checkout feature/fix-auth

Claude: git checkout feature/fix-auth

Claude: [NOW can edit the file safely on the correct branch]
```

</details>

<details>
<summary><strong>Code Not Claude</strong></summary>
<br>

Before Sessions, your CLAUDE.md looked like this:

```markdown
# CLAUDE.md (1,247 lines)

## CRITICAL RULES (PLEASE FOLLOW)

### NEVER DO THESE THINGS
- NEVER use require() we use import
- NEVER use callbacks we use async/await  
- NEVER use class components
- NEVER refactor working code
- NEVER change the database schema
- NEVER modify the auth middleware
- NEVER use var, always const/let
- NEVER commit directly to main
[... 200 more NEVER rules ...]

### ALWAYS DO THESE THINGS  
- ALWAYS use the existing error handler
- ALWAYS follow our naming conventions
- ALWAYS use PostgreSQL not MySQL
- ALWAYS check if a utility exists first
[... 200 more ALWAYS rules ...]

### REMEMBER
- We use Tailwind not CSS modules
- The database is PostgreSQL
- Yes, still PostgreSQL
- I SAID POSTGRESQL
[... 500 lines of context ...]
```

Claude reads line 1 and forgets the rest.

With Sessions:

```markdown
# CLAUDE.md (< 100 lines)

## Project Overview
AI waifu platform. Node + React + PostgreSQL.

## Behavioral Rules
@CLAUDE.sessions.md

That's it. The rest is enforced by hooks.
```

Rules aren't suggestions anymore. They're **enforced by code**:
- Can't edit without permission? Hook blocks the tools
- Wrong branch? Hook blocks the edit
- Need to follow patterns? Context manifest has them documented
- Must remember PostgreSQL? It's in the task context that auto-loads

Your documentation describes your project. The hooks enforce behavior. Claude can't ignore hooks.

</details>


<details>
<summary><strong>Statusline keeping you informed</strong></summary>

```
[Bottom of your Claude Code window - two lines]

██████░░░░ 45.2% (72k/160k) | Task: m-payment-integration
DAIC: Discussion | ✎ 3 files | [4 open]

[After you say "go ahead"]

██████░░░░ 47.1% (75k/160k) | Task: m-payment-integration  
DAIC: Implementation | ✎ 5 files | [4 open]

[When approaching context limit - bar turns red]

████████░░ 78.3% (125k/160k) | Task: m-payment-integration
DAIC: Discussion | ✎ 12 files | [4 open]

[When no task is active]

██░░░░░░░░ 12.1% (19k/160k) | Task: None
DAIC: Discussion | ✎ 0 files | [4 open]
```

Progress bar changes color: green < 50%, orange < 80%, red >= 80%.

</details>

This isn't complex. It's not heavy process. It's invisible rails that keep Claude from going off the cliff. You still describe what you want in natural language. Claude still writes code. But now it happens in a way that doesn't produce garbage.

You code at the same speed. You just don't spend the next three hours unfucking what Claude just did.

## 🚀 Installation

Alright, you're convinced. **Let's unfuck your workflow.**

### ✅ Requirements

You need:
- **Claude Code** _(you have this or you wouldn't be here)_
- **Python 3 + pip** _(for the hooks)_
- **Git** _(probably)_
- **5 minutes**

### 📦 Install

Pick your poison:

**NPM/NPX** (TypeScript Andys):
```bash
cd your-broken-project
npx cc-sessions                  # One-time install
# or
npm install -g cc-sessions       # Global install
cc-sessions                      # Run in your project
```

**Pip/Pipx/UV** (Pythonistas):
```bash
pipx install cc-sessions         # Isolated install (recommended)
cd your-broken-project
cc-sessions                      # Run the installer
# or
pip install cc-sessions          # Regular pip
# or  
uv pip install cc-sessions       # UV package manager
```

**Direct Bash** ('build from source' gigachads):
```bash
git clone https://github.com/GWUDCAP/cc-sessions
cd your-broken-project
/path/to/cc-sessions/install.sh
```

The installer asks you:
- Your name (so Claude knows who it's disappointing)
- If you want the statusline (you do)
- What triggers implementation mode ('go ahead', 'ship it', whatever)
- Some other config shit (just hit enter)

That's it. Restart Claude Code. You're done.

### ✨ What Just Happened

You now have:
- Hooks that enforce discussion before implementation
- State that persists between sessions
- Task management that actually works
- Agents that aren't completely useless
- Git branch enforcement
- Token warnings before death
- A chance at maintaining this code in 6 months

### 🎯 Your First Task

Tell Claude:
```
Create a task for: 
[EXPLAIN THE TASK]
```

Claude will:
- Create the task file with proper structure
- Use the context-gathering agent to gather everything they need to know to complete the task based on your existing codebase
- Build a context manifest so it never forgets what it learned
- Set up the task as your current focus

Then just say 'let's work on [task-name]' and watch Claude actually discuss before implementing.

### **Welcome to software development with guardrails.**

---

## 🔧 Customizing Sessions

Sessions is built to be modified. You can use Claude Code itself to improve Sessions or adapt it to your workflow.

<details>
<summary>How to customize Sessions</summary>

### Understanding the Structure

Sessions comes with knowledge files that explain its own architecture:
```
sessions/knowledge/claude-code/
├── hooks-reference.md     # How hooks work and can be modified
├── subagents.md          # Agent capabilities and customization
├── tool-permissions.md   # Tool blocking configuration
└── slash-commands.md     # Command system reference
```

### Modifying Behaviors

Tell Claude:
```
Using the hooks reference at @sessions/knowledge/claude-code/hooks-reference.md, 
modify the DAIC enforcement to allow Bash commands in discussion mode
```

Claude can:
- Adjust trigger phrases in `sessions/sessions-config.json`
- Modify hook behaviors in `.claude/hooks/`
- Update protocols in `sessions/protocols/`
- Create new agents in `.claude/agents/`
- Customize task templates

### Common Customizations

**Change what tools are blocked:**
```json
// sessions/sessions-config.json
"blocked_tools": ["Edit", "Write"]  // Remove MultiEdit to allow it
```

**Add your own trigger phrases:**
```json
"trigger_phrases": ["make it so", "ship it", "do the thing"]
```

**Modify agent prompts:**
Edit files in `.claude/agents/` to change how agents behave.

**Update workflows:**
Protocols in `sessions/protocols/` are just markdown - edit them to match your process.

### Pro Tips

1. Sessions has its own CLAUDE.md at `sessions/CLAUDE.md` for meta work
2. Use the knowledge files to understand the system deeply
3. Test changes in a separate branch first
4. The hooks are just Python - add logging if needed
5. Keep your customizations documented

Remember: You're not just using Sessions, you're evolving it. Make it yours.

</details>
