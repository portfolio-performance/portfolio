# How do backup and dry-run behave on writes?

Type: grilling
Status: open
Blocked by: 05

## Question

What exactly happens around a write: where and how backups are named and retained, what a dry-run reports (structured diff of domain changes vs textual file diff), and whether writes are atomic (temp file + rename)?
