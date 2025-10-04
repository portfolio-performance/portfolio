# Release Notes Command

Create release notes for any major changes since the last release in both bilingual text (for GitHub) and XML (for metainfo.xml) formats.

## Task

Analyze the git commit history since the last release and create release notes in two formats:
1. Bilingual text format (German first, then English) for GitHub releases
2. XML format for metainfo.xml

## Analyze commits since last tag

git log --oneline $(git describe --tags --abbrev=0)..HEAD

## Expected Output Formats

### Format 1: Bilingual Text (for GitHub Releases)

**German Section:**
- Neu:
  * [New feature description in German]
- Verbesserung:
  * [Improvement description in German]
- Fehlerbehebung:
  * [Fix description in German]

**English Section:**
- New:
  * [New feature description in English]
- Improvement:
  * [Improvement description in English]
- Fix:
  * [Fix description in English]

### Format 2: XML (for metainfo.xml)

```xml
    <release version="X.Y.Z" date="YYYY-MM-DD">
      <description>
        <ul>
          <li>New: Brief description</li>
          <li>Improvement: Brief description</li>
          <li>Fix: Brief description</li>
        </ul>
      </description>
    </release>
```

## Output Structure

Generate release notes in this order:
1. **Bilingual Text Section** (ready to paste into GitHub releases)
2. **XML Section** (ready to paste into metainfo.xml)

## Category Translations

- **English → German:**
  - New → Neu
  - Improvement → Verbesserung
  - Fix → Fehlerbehebung

## Formatting Rules

- Each entry should be 1-2 lines maximum
- Use consistent formatting: "Prefix: Brief description"
- German descriptions should be in German
- English descriptions should be in English
- Same commit filtering and grouping rules apply to both formats

## What to Exclude

- Merge commits
- Version bump commits
- CI/CD related changes
- Documentation-only changes (unless significant)

## Special Handling Rules

### PDF Importers
Combine all PDF importer changes into one line in **both languages**:

**German:**
- "Verbesserung: PDF-Importer für [Bank1, Bank2, Bank3] verbessert"

**English:**
- "Improvement: Enhanced PDF importers for [Bank1, Bank2, Bank3]"

### Common German Phrases
- "hinzugefügt" = added
- "verbessert" = improved/enhanced
- "behebt" = fixes/resolved
- "Fehler" = error/bug
- "Problem" = problem/issue

## Validation
Before finalizing:
1. Verify all user-facing changes are included
2. Ensure technical jargon is user-friendly in both languages
3. Confirm categorization follows existing pattern
4. Check German translations are natural and accurate
5. Verify XML format is valid

## Analysis Scope
- Focus on commits that affect end-user functionality
- Include changes to UI, calculations, import/export features
- Prioritize changes that users would notice or benefit from
- Consider impact level (high/medium/low) when ordering items

## Example Output

**BILINGUAL TEXT (for GitHub):**
```
**German:**
- Neu: MEXC Crypto Exchange als Online-Kurslieferant hinzugefügt
- Verbesserung: PDF-Importer für DKB und Trade Republic verbessert
- Fehlerbehebung: Behebt Fehler beim Wechsel der Wertpapieransicht

**English:**
- New: Added MEXC Crypto Exchange as an online price feed
- Improvement: Enhanced PDF importers for DKB and Trade Republic
- Fix: Resolved a crash when switching securities view
```

**XML (for metainfo.xml):**
```xml
    <release version="0.80.4" date="2025-10-04">
      <description>
        <ul>
          <li>New: Added MEXC Crypto Exchange as an online price feed</li>
          <li>Improvement: Enhanced PDF importers for DKB and Trade Republic</li>
          <li>Fix: Resolved a crash when switching securities view</li>
        </ul>
      </description>
    </release>
```
