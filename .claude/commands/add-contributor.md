---
description: Add a pull request's author to the About-dialog contributor list and commit it
argument-hint: <pr-number>
---

Add the author of pull request **#$1** to the About-dialog contributor list,
then commit. Follow these steps exactly.

The contributor is the **PR author** (`.user.login`) — NOT the issue reporter
and NOT the git commit author name/email.

1. Resolve the PR author's GitHub username (pin the upstream repo — this clone
   has many remotes):

   ```bash
   gh api repos/portfolio-performance/portfolio/pulls/$1 --jq '.user.login'
   ```

   Use that login verbatim. Do not guess from the commit author name or email.

2. Append it to the contributor file, preserving the file's convention of **no
   trailing newline** at end of file:

   ```bash
   FILE=name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/dialogs/about.contributors.txt
   ```

   First check it is not already present (`grep -qxF "<user>" "$FILE"`); if it
   is, stop and report — nothing to do. Otherwise append with a leading newline
   and no trailing newline:

   ```bash
   printf '\n%s' "<user>" >> "$FILE"
   ```

3. Commit with this exact message shape:

   ```bash
   git add "$FILE"
   git commit -m "Added @<user> to about text" -m "Issue: #$1"
   ```

   - Subject: `Added @<user> to about text`
   - Body: `Issue: #$1` — the PR number, which GitHub renders as a link.

Common mistakes to avoid:
- Using the git author or issue reporter instead of the PR author.
- Adding a trailing newline (the file ends without one).
- Forgetting the `Issue: #$1` body line.
