# Write protocol

Every change to the client file, `pp-cli apply` or `pp-cli restore`, goes through these steps, once per batch. A small edit, an obvious edit and an edit the user asked for all take the same path.

1. **Resolve.** Take each operation's input schema from `pp-cli apply --help <operation>`. Look up the UUID of every entity the batch refers to with the read commands (such as `pp-cli instruments --file "<path>"`). Done when every reference in the batch is a UUID that a read returned.
2. **Write the batch.** With your file tool, write `{"operations": [...]}` to a new file in the session's temp directory (see Shell in the skill), UTF-8 without a BOM. Done when the file exists and holds exactly the batch you will summarize.
3. **Dry run.** Run `pp-cli apply --file "<client file>" --input "<batch file>" --dry-run`. On a failure, act on its code as Troubleshooting in the skill says, correct the batch file, and run the dry run again. Done when the dry run exits 0.
4. **Summarize.** Tell the user in plain language what the batch does, entity by entity, from `results` in the dry run's `data`: every created, updated and deleted entity (an instrument, cash account, investment account, transaction or taxonomy assignment), with its name, the fields that change and their new values (dates, amounts, shares, currencies). Add every entry of `warnings`. Ask the user to save and close the file in Portfolio Performance if it is open there, because unsaved edits in the app can't be detected. Then ask for approval of this batch. Done when every entity in `results` appears in the summary.
5. **Approval.** Wait for the user's explicit yes to this batch. A question, a partial answer or silence is a no. Any change to the batch, however small, sends you back to step 2 with a new dry run and a new approval. Done when the user has approved this exact batch file.
6. **Apply.** Run the same command without `--dry-run`, with the same, unchanged batch file. Done when it exits 0.
7. **Report.** Tell the user what changed, the backup path from `backupPath` in `meta`, and that `pp-cli restore --file "<client file>" --backup "<backup path>"` undoes it. If `warnings` holds `file-migrated`, say that the file now has a newer version, which older desktop releases can't open. Delete the batch file. Done when the user has the backup path and the restore command and the batch file is gone.

## Restore

A restore follows the same steps without a batch file. Step 3 is `pp-cli restore --file "<client file>" --backup "<backup path>" --dry-run`. Step 4 names the backup that replaces the client file and says that the current file is backed up first.
