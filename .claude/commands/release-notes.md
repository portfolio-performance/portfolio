# Release Notes Command

Create release notes for any major changes since the last release.

## Task

Analyze the git commit history since the last release and create release notes.

## Analyze commits since last tag

git log --oneline $(git describe --tags --abbrev=0)..HEAD

## Expected Output Format

Create release notes following this structure:
```xml
    <release version="X.Y.Z" date="YYYY-MM-DD">
      <description>
        <ul>
          <li>Category: Brief description</li>
        </ul>
      </description>
    </release>
```

- Categories: "New:", "Improvement:", "Fix:" (as prefixes)
- Each entry should be 1-2 lines maximum
- Use consistent formatting: "Prefix: Brief description"

## What to Exclude

- Merge commits
- Version bump commits
- CI/CD related changes
- Documentation-only changes (unless significant)

## Special Handling Rules
- **PDF Importers**: Combine all PDF importer changes into one line like:
  "Improvement: Enhanced PDF importers for [list specific banks/sources affected]"

## Validation
Before finalizing:
1. Verify all user-facing changes are included
2. Ensure technical jargon is user-friendly
3. Confirm categorization follows existing pattern in metainfo.xml

## Analysis Scope
- Focus on commits that affect end-user functionality
- Include changes to UI, calculations, import/export features
- Prioritize changes that users would notice or benefit from
- Consider impact level (high/medium/low) when ordering items
