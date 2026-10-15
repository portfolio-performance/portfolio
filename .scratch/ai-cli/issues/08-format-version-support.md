# Which formats and file versions does v1 read and write, and how are encrypted files handled?

Type: grilling
Blocked by: 07

## Question

Given the facts from ticket 07, which formats does v1 read and which does it write (plain XML is required; compressed, binary and encrypted are nice-to-have)? How does the CLI refuse a newer-than-supported file, and how does that refusal surface in the JSON error? For encrypted files, confirm or amend ticket 04's password input (`PP_PASSWORD` or stdin prompt, never a flag) for writes too, and decide the wrong-password error. What is the policy for keeping the fork in step with upstream format changes?
