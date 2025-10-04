# Release Notes Generation Protocol

## Purpose
Generate release notes for Portfolio Performance from git commit history in the proper XML format for metainfo.xml.

## When to Use
- Before creating a new release
- After a significant development cycle
- When asked to create release notes

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

## Example Output

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

## Usage with Slash Command

Quick access via `/release-notes` command:

```
/release-notes
```

This will:
1. Automatically run git log analysis
2. Prompt for categorization guidance
3. Generate formatted XML output
4. Request validation before finalizing

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

Release notes are added to:
- `portfolio-app/org.eclipse.oss.portfolio.product/metainfo.xml`

The XML format must comply with:
- AppStream metadata specification
- Portfolio Performance metainfo schema

## Automation Notes

The `/release-notes` command automates this protocol by:
1. Running git log automatically
2. Filtering based on commit patterns
3. Suggesting categorization
4. Formatting in correct XML structure
5. Requesting user validation

Manual intervention still needed for:
- Final categorization decisions
- User-friendly wording
- Combining similar changes
- Version number confirmation
