# Release Notes Command

Create release notes for the major changes since the last release.

This is a **three-phase workflow**. Do NOT jump straight to XML. Complete each
phase and get explicit approval before moving to the next one.

- Phase 1: English bullet list (plain text, no XML)
- Phase 2: German translation (plain text, no XML)
- Phase 3: XML block for `metainfo.xml`

The target file is:
`portfolio-product/metainfo/info.portfolio_performance.PortfolioPerformance.metainfo.xml`

---

## Phase 1 — English bullet list

Analyze the commit history since the last tag and produce a plain-text bullet
list in **English only**. No XML yet.

### Analyze commits since last tag

```
git log --oneline $(git describe --tags --abbrev=0)..HEAD
```

### Output format

A simple markdown bullet list, one entry per line:

- New: Brief description
- Improvement: Brief description
- Fix: Brief description

Rules:
- Categories are prefixes: "New:", "Improvement:", "Fix:"
- Each entry is 1-2 lines maximum
- Consistent phrasing: "Prefix: Brief description"
- Order entries by category (New, then Improvement, then Fix), and within a
  category by impact (high → low)

### What to exclude

- Merge commits
- Version bump commits
- CI/CD related changes
- Documentation-only changes (unless significant)

### Special handling rules

- **PDF Importers**: Combine all PDF importer changes into one line like:
  "Improvement: Enhanced PDF importers for [list specific banks/sources affected]"

### Analysis scope

- Focus on commits that affect end-user functionality
- Include changes to UI, calculations, import/export features
- Prioritize changes that users would notice or benefit from

### Stop and iterate

Present the English list and **stop**. Iterate with the user until they
explicitly approve the English version. Do not translate or write XML yet.

---

## Phase 2 — German translation

Once the English list is approved, translate it to **German**, presented as a
plain-text bullet list (no XML yet). Show it so it can be reviewed easily.

Rules:
- Category prefixes map as follows:
  - "New:" → "Neu:"
  - "Improvement:" → "Verbesserung:"
  - "Fix:" → "Fehlerbehebung:"
- Use the informal address ("du", "dein", ...), matching existing entries
- Keep the same order as the approved English list
- Keep product/bank names untranslated

### Stop and iterate

Present the German list and **stop**. Iterate with the user until they
explicitly approve the German version. Do not write XML yet.

---

## Phase 3 — XML block

Only after both the English and German lists are approved, assemble the XML
`<release>` block.

### Structure

```xml
    <release version="X.Y.Z" date="YYYY-MM-DD">
      <description>
        <ul>
          <li>New: ...</li>
          <li>Improvement: ...</li>
          <li>Fix: ...</li>
          <li xml:lang="de">Neu: ...</li>
          <li xml:lang="de">Verbesserung: ...</li>
          <li xml:lang="de">Fehlerbehebung: ...</li>
        </ul>
      </description>
    </release>
```

Rules:
- All English `<li>` entries first, then all German `<li xml:lang="de">` entries
- Same order in both language blocks
- Insert the new `<release>` as the first entry inside `<releases>` (newest first)

### Validation

Before finalizing:
1. Verify all user-facing changes are included
2. Ensure technical jargon is user-friendly
3. Confirm categorization and formatting match existing entries in metainfo.xml
4. Confirm English and German lists have the same number of entries in the same order
