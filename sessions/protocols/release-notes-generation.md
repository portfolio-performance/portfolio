# Release Notes Generation Protocol

## 🚨 QUICK REFERENCE - START HERE

### Critical Steps (Do These FIRST!)

1. **Get commits since last release**
   ```bash
   git log --oneline $(git describe --tags --abbrev=0)..HEAD
   ```

2. **Filter commits** - Include only user-facing changes, exclude:
   - Merge commits
   - Version bumps
   - CI/CD changes
   - Documentation-only changes (unless significant)

3. **Categorize changes** into:
   - **New:** Completely new features
   - **Improvement:** Enhancements to existing features
   - **Fix:** Bug fixes

4. **Use the XML template** (copy-paste ready):
   ```xml
   <release version="X.Y.Z" date="YYYY-MM-DD">
     <description>
       <ul>
         <li>New: Brief description of new feature</li>
         <li>Improvement: Brief description of improvement</li>
         <li>Fix: Brief description of bug fix</li>
       </ul>
     </description>
   </release>
   ```

### Quick Access via Slash Command
```
/release-notes
```
This command automates this protocol by running git log analysis and generating formatted output.

---

## Purpose
Generate release notes for Portfolio Performance from git commit history in the proper XML format for metainfo.xml.

## When to Use
- Before creating a new release
- After a significant development cycle
- When asked to create release notes
- Via `/release-notes` slash command for automated generation

## Prerequisites
- Access to git repository
- Last release tag exists
- Commits since last release exist

## Process

### 1. Analyze Commits Since Last Release

```bash
# Get commits since last tag
git log --oneline $(git describe --tags --abbrev=0)..HEAD
```

### 2. Categorize Changes

Group commits into categories:
- **New:** Completely new features
- **Improvement:** Enhancements to existing features
- **Fix:** Bug fixes

### 3. Filter Commits

**Exclude:**
- Merge commits
- Version bump commits
- CI/CD related changes
- Documentation-only changes (unless significant)

**Include:**
- User-facing changes
- UI modifications
- Calculation changes
- Import/export features
- Features users would notice

### 4. Format Output

Create release notes in this XML format:

```xml
<release version="X.Y.Z" date="YYYY-MM-DD">
  <description>
    <ul>
      <li>New: Brief description of new feature</li>
      <li>Improvement: Brief description of improvement</li>
      <li>Fix: Brief description of bug fix</li>
    </ul>
  </description>
</release>
```

### 5. Formatting Rules

**Per Entry:**
- 1-2 lines maximum
- Use consistent formatting: "Prefix: Brief description"
- Make technical jargon user-friendly
- Focus on user benefit, not implementation details

**Special Handling:**
- **PDF Importers**: Combine multiple importer changes into one line:
  ```
  Improvement: Enhanced PDF importers for [DKB, Trade Republic, Comdirect]
  ```

### 6. Validation Checklist

Before finalizing:
- [ ] All user-facing changes included
- [ ] Technical jargon is user-friendly
- [ ] Categorization follows existing pattern
- [ ] Version number is correct
- [ ] Release date is accurate
- [ ] XML format is valid

### 7. Analysis Scope Priority

Order changes by impact:
1. **High impact**: Major features, critical fixes
2. **Medium impact**: Improvements, minor features
3. **Low impact**: Small enhancements, edge case fixes

## 🚨 Common Mistakes to Avoid

### Mistake 1: Including Too Many Technical Details
**❌ Wrong:**
```xml
<li>Fix: Corrected NullPointerException in PortfolioTransaction.calculateIRR() method when handling empty transaction list</li>
```

**✅ Correct:**
```xml
<li>Fix: Resolved calculation error for portfolios with no transactions</li>
```

### Mistake 2: Listing Every Minor Commit
**❌ Wrong:**
```xml
<li>Fix: Typo in German translation for "Portfolio"</li>
<li>Fix: Typo in German translation for "Account"</li>
<li>Fix: Typo in German translation for "Security"</li>
```

**✅ Correct:**
```xml
<li>Fix: Corrected German translation errors</li>
```

### Mistake 3: Not Grouping PDF Importers
**❌ Wrong:**
```xml
<li>Improvement: Enhanced DKB PDF importer for dividend transactions</li>
<li>Improvement: Enhanced Trade Republic PDF importer for buy transactions</li>
<li>Improvement: Enhanced Comdirect PDF importer for forex support</li>
```

**✅ Correct:**
```xml
<li>Improvement: Enhanced PDF importers for DKB, Trade Republic, and Comdirect</li>
```

### Mistake 4: Missing User Benefit
**❌ Wrong:**
```xml
<li>New: Implemented ChronoUnit.DAYS for date calculations</li>
```

**✅ Correct:**
```xml
<li>Improvement: Faster performance calculations for large portfolios</li>
```

### Mistake 5: Incorrect Categorization
**❌ Wrong:** Marking a bug fix as "Improvement"
**❌ Wrong:** Marking an enhancement as "New" (when it's not a wholly new feature)

**✅ Correct:**
- **New** = Completely new feature (e.g., cryptocurrency support)
- **Improvement** = Enhancement to existing feature (e.g., better CSV import)
- **Fix** = Bug fix (e.g., corrected currency conversion)

## Example Output

### Good Example (Realistic Release Notes)
```xml
<release version="0.68.5" date="2025-10-15">
  <description>
    <ul>
      <li>New: Support for cryptocurrency portfolio tracking</li>
      <li>Improvement: Enhanced PDF importers for DKB, Trade Republic, and Scalable Capital</li>
      <li>Improvement: Performance optimization for large portfolios (10,000+ transactions)</li>
      <li>Fix: Corrected currency conversion for dividend payments</li>
      <li>Fix: Resolved date parsing issue in CSV import</li>
    </ul>
  </description>
</release>
```

### Bad Example (Too Technical, Too Detailed)
```xml
<release version="0.68.5" date="2025-10-15">
  <description>
    <ul>
      <li>Refactor: Moved PortfolioTransaction logic to separate service class</li>
      <li>Fix: NPE in DkbPDFExtractor.java line 269 when parsing forex section</li>
      <li>Fix: Trade Republic PDF importer now handles ISIN FR0011550185</li>
      <li>Fix: Comdirect PDF importer fixed for document type "Bardividende Netto"</li>
      <li>Update: Bumped version to 0.68.5</li>
      <li>CI: Updated GitHub Actions workflow</li>
      <li>Docs: Updated CONTRIBUTING.md with new examples</li>
    </ul>
  </description>
</release>
```
**Problems:**
- "Refactor" is not user-facing
- NPE with line number is too technical
- Individual PDF importer fixes should be grouped
- Version bump is meta, not a feature
- CI changes are not user-facing
- Documentation updates not significant enough to mention

## 📋 Step-by-Step Workflow

### Manual Workflow
```
□ Step 1: GET COMMITS
  └─ Run: git log --oneline $(git describe --tags --abbrev=0)..HEAD

□ Step 2: ANALYZE COMMITS
  └─ Read through commit messages
  └─ Identify user-facing changes
  └─ Group similar changes together

□ Step 3: CATEGORIZE CHANGES
  └─ New: Completely new features
  └─ Improvement: Enhancements to existing features
  └─ Fix: Bug fixes

□ Step 4: FILTER OUT NON-USER-FACING
  └─ Exclude: Merges, version bumps, CI, docs (unless significant)

□ Step 5: GROUP PDF IMPORTERS
  └─ Combine all PDF importer changes into one line

□ Step 6: MAKE USER-FRIENDLY
  └─ Remove technical jargon
  └─ Focus on user benefit
  └─ Keep 1-2 lines per entry

□ Step 7: FORMAT AS XML
  └─ Use template from Quick Reference
  └─ Add version number and date
  └─ Validate XML structure
```

## Usage with Slash Command

Quick access via `/release-notes` command:

```
/release-notes
```

**What the command does:**
1. Automatically runs git log analysis
2. Filters commits based on patterns
3. Suggests categorization
4. Generates **two formats**:
   - Bilingual text (German + English) for GitHub releases
   - XML for metainfo.xml
5. Requests validation before finalizing

**Command location:** `.claude/commands/release-notes.md`

**Integration:** This protocol guides the command's behavior

**Output formats:**
- **Bilingual Text:** Ready to paste into GitHub release description (German section first, then English)
- **XML:** Ready to paste into `metainfo.xml` file

## Best Practices

### Do:
- ✅ Focus on user benefit
- ✅ Use clear, concise language
- ✅ Group related changes
- ✅ Verify all major changes are included

### Don't:
- ❌ Include internal refactoring
- ❌ List every minor commit
- ❌ Use technical implementation details
- ❌ Duplicate similar changes

## Integration with Portfolio Performance

Release notes are published in two locations:

**1. GitHub Releases** (bilingual text format)
- Location: https://github.com/portfolio-performance/portfolio/releases
- Format: Markdown with German section first, then English section
- Categories: Neu/New, Verbesserung/Improvement, Fehlerbehebung/Fix

**2. MetaInfo File** (XML format)
- File: `portfolio-app/org.eclipse.oss.portfolio.product/metainfo.xml`
- Format: XML complying with AppStream metadata specification
- Categories: New, Improvement, Fix (English only)

## Automation Notes

The `/release-notes` command automates this protocol by:
1. Running git log automatically
2. Filtering based on commit patterns
3. Suggesting categorization
4. Formatting in **two structures**:
   - Bilingual text (German + English) for GitHub
   - XML for metainfo.xml
5. Requesting user validation

**German Translation Support:**
- New → Neu
- Improvement → Verbesserung
- Fix → Fehlerbehebung
- Common phrases provided in command documentation

Manual intervention still needed for:
- Final categorization decisions
- User-friendly wording (especially German translations)
- Combining similar changes
- Version number confirmation
- Ensuring natural German phrasing

---

## 🎯 QUICK CHECKLIST - Use This Every Time

Before finalizing release notes:
- [ ] ✅ All PDF importer changes grouped into one line
- [ ] ✅ No technical jargon (class names, line numbers, stack traces)
- [ ] ✅ No merge commits, version bumps, or CI changes
- [ ] ✅ Each entry focuses on user benefit, not implementation
- [ ] ✅ Categorization correct (New vs Improvement vs Fix)
- [ ] ✅ 1-2 lines maximum per entry
- [ ] ✅ XML format is valid
- [ ] ✅ Version number and date are correct

**Most Common Mistakes to Avoid:**
1. ❌ Including too many technical details
2. ❌ Listing every minor commit separately
3. ❌ Not grouping PDF importer changes
4. ❌ Wrong categorization (Fix marked as Improvement)
5. ❌ Including internal refactoring or CI changes
6. ❌ Forgetting to translate to German or using unnatural German phrasing

**Emergency Reference:**
- **New** = Wholly new feature (e.g., cryptocurrency support)
- **Improvement** = Enhancement to existing (e.g., better CSV import, enhanced PDF importers)
- **Fix** = Bug fix (e.g., corrected currency conversion)
- **Combine PDF importers** =
  - English: "Enhanced PDF importers for [Bank1, Bank2, Bank3]"
  - German: "PDF-Importer für [Bank1, Bank2, Bank3] verbessert"

**Target locations:**
- GitHub Releases: https://github.com/portfolio-performance/portfolio/releases (bilingual text)
- MetaInfo file: `portfolio-app/org.eclipse.oss.portfolio.product/metainfo.xml` (XML)
