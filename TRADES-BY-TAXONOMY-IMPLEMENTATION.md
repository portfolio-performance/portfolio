# Trades by Taxonomy - Implementation Status

## Overview
This feature adds support for grouping and analyzing completed trades by taxonomy classifications, providing aggregated profit/loss statistics and performance metrics.

## Completed Components

### Core Model Layer ✅
**Location**: `name.abuchen.portfolio/src/name/abuchen/portfolio/snapshot/trades/`

1. **TradeCategory.java** (177 lines)
   - Represents a taxonomy classification with its associated trades
   - Aggregates metrics: total P/L, average return, IRR, win rate, holding period
   - Supports weighted assignments (partial allocations)
   - Lazy calculation of aggregations for performance

2. **TradesGroupedByTaxonomy.java** (174 lines)
   - Groups trades by their security's taxonomy classifications
   - Uses `Taxonomy.Visitor` pattern (no code duplication)
   - Handles weighted assignments and unassigned trades
   - Provides grand totals across all categories

### Internationalization ✅
**Files Modified**:
- `name.abuchen.portfolio/src/name/abuchen/portfolio/Messages.java`
- `name.abuchen.portfolio/src/name/abuchen/portfolio/messages.properties`

**Added Strings**:
- `ColumnAverageIRR` = Avg IRR
- `ColumnAverageReturn` = Avg Return
- `ColumnTotalProfitLoss` = Total P/L
- `ColumnTradeCount` = Trades
- `ColumnWinRate` = Win Rate
- `LabelTaxonomies` = Taxonomies
- `LabelTradesByTaxonomy` = Trades by Taxonomy

### Unit Tests ✅
**Location**: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/snapshot/trades/`

1. **TradeCategoryTest.java**
   - Tests aggregation of trade metrics
   - Tests weighted aggregation (partial assignments)
   
2. **TradesGroupedByTaxonomyTest.java**
   - Tests grouping by taxonomy
   - Tests partial assignments (50/50 splits)
   - Tests unassigned trade handling

## Remaining Components

### UI View (Pending)
**Location**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/trades/`

**TradesByTaxonomyView.java** (~500 lines estimated)
- Main view displaying trades grouped by taxonomy
- Flat table with hierarchy (similar to StatementOfAssetsViewer)
- Columns:
  * Name (category or security name)
  * Trade Count (categories only)
  * Total P/L
  * Average Return
  * Win Rate
  * Average Holding Period
  * Average IRR
- Features:
  * Taxonomy selector dropdown
  * Reporting period filter
  * Open/Closed trade filter
  * Expand/collapse categories
  * Bold font for category rows
  * Grand total rows (top/bottom, hideable)

### Trade Filter Enhancement (Pending)
**Location**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/trades/`

**TradeDetailsView.java** modifications (~100 lines)
- Add taxonomy filter dropdown to existing filter toolbar
- Filter trades by selected classification
- Integrate with existing filters (open/closed, profitable/loss)

### Dashboard Widget (Pending)
**Location**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/dashboard/`

**TradesByTaxonomyWidget.java** (~300 lines estimated)
- Shows P/L summary by taxonomy in dashboard
- Bar chart or table format
- Click to open full TradesByTaxonomyView
- Configurable: taxonomy selection, time period

### Navigation Integration (Pending)
**Files to Modify**:
- `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/editor/Navigation.java`
- `name.abuchen.portfolio.ui/plugin.xml`

**Changes**:
- Add "Trades by Taxonomy" menu entry under Reports or Trades section
- Register TradesByTaxonomyView

### Context Menu Enhancement (Pending)
**Location**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/taxonomy/`

**AbstractNodeTreeViewer.java** modifications (~30 lines)
- Add "Show Trades..." context menu item
- Opens TradesByTaxonomyView filtered to selected classification

## Architecture Decisions

### Design Patterns Used
1. **Composition over Duplication**: `TradesGroupedByTaxonomy` uses `Taxonomy.Visitor` pattern instead of duplicating `GroupByTaxonomy` logic
2. **Domain-Specific Classes**: `TradeCategory` is separate from `AssetCategory` (different domains: historical trades vs current holdings)
3. **Lazy Calculation**: Aggregations calculated on-demand and cached
4. **Weighted Assignments**: Properly handles partial taxonomy assignments (e.g., 50% Tech, 50% Growth)

### Key Technical Choices
- **No persistence**: Trades remain calculated objects, taxonomy data is not persisted on trades
- **Security-based grouping**: Trades grouped via their Security's taxonomy assignments
- **MoneyCollectors**: Uses Money collectors for proper currency aggregation
- **Weight representation**: Uses double (0.0 to 1.0) for weight calculations

## Testing Strategy

### Completed Tests
- ✅ Basic aggregation (single trade, single category)
- ✅ Weighted aggregation (50% assignment)
- ✅ Multiple categories
- ✅ Partial assignments across categories
- ✅ Grand totals

### Pending Tests
- Integration tests for UI components
- Performance tests with large trade counts
- Edge cases: zero trades, all unassigned, multiple taxonomies

## Next Steps to Complete Implementation

1. **Create TradesByTaxonomyView** (Main UI component)
   - Follow StatementOfAssetsViewer pattern
   - Implement Element wrapper for hierarchy
   - Add all columns with proper aggregation
   - Implement expand/collapse

2. **Add taxonomy filter to TradeDetailsView**
   - Add dropdown to toolbar
   - Implement filter logic
   - Test with existing filters

3. **Create dashboard widget**
   - Summary visualization
   - Click-through to detailed view

4. **Integration**
   - Add to Navigation menu
   - Register in plugin.xml
   - Add context menu items

5. **Polish**
   - Preferences for column visibility
   - Save/restore expanded state
   - Performance optimization

## Estimated Remaining Effort
- UI View: 2-3 days
- Filter Enhancement: 0.5 day
- Dashboard Widget: 1 day
- Integration & Polish: 1 day
- **Total: 4-5.5 days** (for experienced developer)

## Code Quality
- ✅ Follows project coding style
- ✅ No JavaDoc (matches project convention)
- ✅ Package-private constructors where appropriate
- ✅ Proper use of Money types and MoneyCollectors
- ✅ Uses existing i18n framework
- ✅ Comprehensive unit tests

## How to Use (Once UI is complete)

1. **Assign securities to taxonomy**: Already supported in existing taxonomy views
2. **Open "Trades by Taxonomy"**: New menu entry (to be added)
3. **Select taxonomy**: Dropdown at top of view
4. **View grouped trades**: Categories with aggregated metrics
5. **Filter**: By time period, open/closed, profitable/loss
6. **Expand categories**: See individual trades
7. **Dashboard**: Add widget for quick overview

## Benefits

- ✅ Analyze trading performance by asset class/sector/strategy
- ✅ Compare returns across different taxonomy categories
- ✅ Identify which categories generate best returns
- ✅ Track win rates by category
- ✅ Automatic aggregation with subtotals
- ✅ Handles weighted assignments properly
- ✅ Familiar UI pattern (like Statement of Assets)

---

**Status**: Core model complete and tested. UI implementation pending.
**Last Updated**: 2025-10-04
