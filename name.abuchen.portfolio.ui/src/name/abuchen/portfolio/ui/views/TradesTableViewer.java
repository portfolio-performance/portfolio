package name.abuchen.portfolio.ui.views;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCategory;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.views.trades.TradeElement;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TabularDataSource;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.NumberColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ToolTipCustomProviderSupport;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;
import name.abuchen.portfolio.util.TextUtil;

public class TradesTableViewer
{
    public enum ViewMode
    {
        SINGLE_SECURITY, MULTIPLE_SECURITES
    }

    private AbstractFinanceView view;

    private TableViewer trades;
    private ShowHideColumnHelper support;

    public TradesTableViewer(AbstractFinanceView view)
    {
        this.view = view;
    }

    /**
     * Helper method to extract Trade from either a Trade or TradeElement
     * 
     * @return the Trade, or null if the element is a category
     */
    private static Trade asTrade(Object element)
    {
        if (element instanceof Trade)
            return (Trade) element;
        else if (element instanceof TradeElement)
        {
            TradeElement te = (TradeElement) element;
            return te.isTrade() ? te.getTrade() : null;
        }
        return null;
    }

    /**
     * Helper method to check if an element is a category row
     */
    private static boolean isCategory(Object element)
    {
        return element instanceof TradeElement && ((TradeElement) element).isCategory();
    }

    /**
     * Helper method to extract TradeCategory from a TradeElement
     * 
     * @return the TradeCategory, or null if not a category
     */
    private static TradeCategory asCategory(Object element)
    {
        if (element instanceof TradeElement)
        {
            TradeElement te = (TradeElement) element;
            return te.isCategory() ? te.getCategory() : null;
        }
        return null;
    }

    public Control createViewControl(Composite parent, ViewMode viewMode)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        trades = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(view.getEditorActivationState(), trades);
        ToolTipCustomProviderSupport.enableFor(trades, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(trades);

        support = new ShowHideColumnHelper(
                        SecuritiesPerformanceView.class.getSimpleName() + "@trades@" + viewMode.name(), //$NON-NLS-1$
                        view.getPreferenceStore(), trades, layout);
        createTradesColumns(support, viewMode);
        support.createColumns(true);

        trades.getTable().setHeaderVisible(true);
        trades.getTable().setLinesVisible(true);
        trades.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }

    private void createTradesColumns(ShowHideColumnHelper support, ViewMode viewMode)
    {
        Column column;
        
        if (viewMode == ViewMode.MULTIPLE_SECURITES)
        {
            // Custom name column that handles both trades (showing security name)
            // and categories (showing classification name in bold)
            column = new Column("name", Messages.ColumnName, SWT.LEFT, 300); //$NON-NLS-1$
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object e)
                {
                    Trade trade = asTrade(e);
                    if (trade != null)
                        return trade.getSecurity().getName();

                    TradeCategory category = asCategory(e);
                    if (category != null)
                        return category.getClassification().getName();

                    return null;
                }

                @Override
                public org.eclipse.swt.graphics.Font getFont(Object e)
                {
                    return isCategory(e) ? org.eclipse.jface.resource.JFaceResources.getFontRegistry()
                                    .getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT) : null;
                }

                @Override
                public Image getImage(Object e)
                {
                    Trade trade = asTrade(e);
                    return trade != null ? Images.SECURITY.image() : null;
                }
            });
            column.setSorter(ColumnViewerSorter.create(e -> {
                Trade trade = asTrade(e);
                if (trade != null)
                    return trade.getSecurity().getName();
                TradeCategory category = asCategory(e);
                return category != null ? category.getClassification().getName() : ""; //$NON-NLS-1$
            }));
            support.addColumn(column);
        }

        column = new Column("start", Messages.ColumnStartDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new DateTimeLabelProvider(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getStart() : null;
        }));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getStart() : null;
        }), SWT.DOWN);
        support.addColumn(column);

        column = new Column("end", Messages.ColumnEndDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(
                        new DateTimeLabelProvider(
                                        e -> {
                                            Trade trade = asTrade(e);
                                            return trade != null ? trade.getEnd().orElse(null) : null;
                                        }, Messages.LabelOpenTrade)
                        {
                            @Override
                            public Color getBackground(Object e)
                            {
                                Trade trade = asTrade(e);
                                return trade != null && trade.isClosed() ? null : Colors.theme().warningBackground();
                            }
                        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            if (trade != null)
            {
                Optional<LocalDateTime> date = trade.getEnd();
                return date.isPresent() ? date.get() : LocalDateTime.now().plusYears(1);
            }
            return null;
        }));
        support.addColumn(column);

        column = new Column("tx", Messages.ColumnNumberOfTransactions, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                if (trade != null)
                    return String.valueOf(trade.getTransactions().size());

                TradeCategory category = asCategory(e);
                if (category != null)
                    return String.valueOf(category.getTradeCount());

                return null;
            }

            @Override
            public Image getImage(Object element)
            {
                return asTrade(element) != null ? Images.INFO.image() : null;
            }
        });
        column.setToolTipProvider(e -> {
            Trade trade = asTrade(e);
            if (trade == null)
                return null;

            return new TabularDataSource(Messages.LabelTrades, builder -> {
                builder.addColumns(new TabularDataSource.Column(Messages.ColumnDate, SWT.LEFT, 100) //
                                .withFormatter(o -> Values.DateTime
                                                .format(((TransactionPair<?>) o).getTransaction().getDateTime())), //
                                new TabularDataSource.Column(Messages.ColumnTransactionType, SWT.LEFT), //
                                new TabularDataSource.Column(Messages.ColumnShares) //
                                                .withFormatter(o -> Values.Share.formatNonZero((Long) o)), //
                                new TabularDataSource.Column(Messages.ColumnQuote) //
                                                .withFormatter(o -> Values.CalculatedQuote.format((Quote) o, //
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnAmount) //
                                                .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnFees) //
                                                .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnTaxes) //
                                                .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnTotal).withFormatter(o -> Values.Money
                                                .formatNonZero((Money) o, view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnPortfolio, SWT.LEFT, 220)
                                                .withFormatter(o -> o instanceof Named n ? n.getName() : null)
                                                .withLogo());

                trade.getTransactions().stream().forEach(pair -> {

                    Object[] row = new Object[9];
                    row[0] = pair;
                    pair.withAccountTransaction().ifPresent(t -> row[1] = t.getTransaction().getType().toString());
                    pair.withPortfolioTransaction().ifPresent(t -> row[1] = t.getTransaction().getType().toString());
                    row[2] = pair.getTransaction().getShares();
                    row[3] = pair.getTransaction().getGrossPricePerShare();
                    row[4] = pair.getTransaction().getGrossValue();
                    row[5] = pair.getTransaction().getUnitSum(Unit.Type.FEE);
                    row[6] = pair.getTransaction().getUnitSum(Unit.Type.TAX);
                    row[7] = pair.getTransaction().getMonetaryAmount();
                    row[8] = pair.getOwner();

                    builder.addRow(row);
                });
            });
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getTransactions().size() : 0;
        }));
        support.addColumn(column);

        column = new Column("shares", Messages.ColumnShares, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            public Long getValue(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? trade.getShares() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getShares() : null;
        }));
        support.addColumn(column);

        column = new Column("entryvalue", Messages.ColumnEntryValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? Values.Money.format(trade.getEntryValue(), view.getClient().getBaseCurrency())
                                : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getEntryValue() : null;
        }));
        support.addColumn(column);

        column = new Column("entryvalue-mvavg", //$NON-NLS-1$
                        Messages.ColumnEntryValue + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? Values.Money.format(trade.getEntryValueMovingAverage(), view.getClient().getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getEntryValueMovingAverage() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        Function<Trade, Money> averagePurchasePrice = t -> {
            Money entryValue = t.getEntryValue();
            return Money.of(entryValue.getCurrencyCode(),
                            Math.round(entryValue.getAmount() / (double) t.getShares() * Values.Share.factor()));
        };

        column = new Column("entryvalue-pershare", Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")" + " (" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? Values.Money.format(averagePurchasePrice.apply(trade), view.getClient().getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? averagePurchasePrice.apply(trade) : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        Function<Trade, Money> averagePurchasePriceMovingAverage = t -> {
            Money entryValue = t.getEntryValueMovingAverage();
            return Money.of(entryValue.getCurrencyCode(),
                            Math.round(entryValue.getAmount() / (double) t.getShares() * Values.Share.factor()));
        };

        column = new Column("entryvalue-mvavg-pershare", //$NON-NLS-1$
                        Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")" + " (" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")" + " (" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? Values.Money.format(averagePurchasePriceMovingAverage.apply(trade),
                                view.getClient().getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? averagePurchasePriceMovingAverage.apply(trade) : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("exitvalue", Messages.ColumnExitValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnExitValue);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? Values.Money.format(trade.getExitValue(), view.getClient().getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getExitValue() : null;
        }));
        support.addColumn(column);

        Function<Trade, Money> averageSellPrice = t -> {
            Money exitValue = t.getExitValue();
            return Money.of(exitValue.getCurrencyCode(),
                            Math.round(exitValue.getAmount() / (double) t.getShares() * Values.Share.factor()));
        };

        column = new Column("exitvalue-pershare", Messages.ColumnExitValue + " (" + Messages.ColumnPerShare + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnExitValue);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? Values.Money.format(averageSellPrice.apply(trade), view.getClient().getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? averageSellPrice.apply(trade) : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("pl", Messages.ColumnProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnProfitLoss + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new MoneyColorLabelProvider(element -> {
            Trade trade = asTrade(element);
            if (trade != null)
                return trade.getProfitLoss();
            TradeCategory category = asCategory(element);
            return category != null ? category.getTotalProfitLoss() : null;
        }, view.getClient()));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            if (trade != null)
                return trade.getProfitLoss();
            TradeCategory category = asCategory(e);
            return category != null ? category.getTotalProfitLoss() : null;
        }));
        support.addColumn(column);

        column = new Column("gpl", Messages.ColumnGrossProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnGrossProfitLoss + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new MoneyColorLabelProvider(element -> {
            Trade trade = asTrade(element);
            if (trade != null)
                return trade.getProfitLossWithoutTaxesAndFees();
            TradeCategory category = asCategory(element);
            return category != null ? category.getTotalProfitLossWithoutTaxesAndFees() : null;
        }, view.getClient()));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            if (trade != null)
                return trade.getProfitLossWithoutTaxesAndFees();
            TradeCategory category = asCategory(e);
            return category != null ? category.getTotalProfitLossWithoutTaxesAndFees() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("pl-mvavg", //$NON-NLS-1$
                        Messages.ColumnProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new MoneyColorLabelProvider(element -> {
            Trade trade = asTrade(element);
            return trade != null ? trade.getProfitLossMovingAverage() : null;
        }, view.getClient()));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getProfitLossMovingAverage() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("gpl-mavg", //$NON-NLS-1$
                        Messages.ColumnGrossProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnGrossProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new MoneyColorLabelProvider(element -> {
            Trade trade = asTrade(element);
            return trade != null ? trade.getProfitLossMovingAverageWithoutTaxesAndFees() : null;
        }, view.getClient()));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getProfitLossMovingAverageWithoutTaxesAndFees() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("holdingperiod", Messages.ColumnHoldingPeriod, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                if (trade != null)
                    return Long.toString(trade.getHoldingPeriod());
                TradeCategory category = asCategory(e);
                return category != null ? Long.toString(category.getAverageHoldingPeriod()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            if (trade != null)
                return trade.getHoldingPeriod();
            TradeCategory category = asCategory(e);
            return category != null ? category.getAverageHoldingPeriod() : null;
        }));
        support.addColumn(column);

        column = new Column("latesttrade", Messages.ColumnLatestTrade, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new DateTimeLabelProvider(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getLastTransaction().getTransaction().getDateTime() : null;
        }));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getLastTransaction().getTransaction().getDateTime() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("irr", Messages.ColumnIRR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2, element -> {
            Trade trade = asTrade(element);
            if (trade != null)
                return trade.getIRR();
            TradeCategory category = asCategory(element);
            return category != null ? category.getAverageIRR() : null;
        }));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            if (trade != null)
                return trade.getIRR();
            TradeCategory category = asCategory(e);
            return category != null ? category.getAverageIRR() : null;
        }));
        support.addColumn(column);

        column = new Column("return", Messages.ColumnReturn, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnReturn);
        column.setMenuLabel(Messages.ColumnReturn + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2, element -> {
            Trade trade = asTrade(element);
            if (trade != null)
                return trade.getReturn();
            TradeCategory category = asCategory(element);
            return category != null ? category.getAverageReturn() : null;
        }));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            if (trade != null)
                return trade.getReturn();
            TradeCategory category = asCategory(e);
            return category != null ? category.getAverageReturn() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("return-mavg", //$NON-NLS-1$
                        Messages.ColumnReturn + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnReturn);
        column.setMenuLabel(Messages.ColumnReturn + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2, element -> {
            Trade trade = asTrade(element);
            return trade != null ? trade.getReturnMovingAverage() : null;
        }));
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getReturnMovingAverage() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            private String getRawText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? trade.getLastTransaction().getTransaction().getNote() : null;
            }

            @Override
            public String getText(Object e)
            {
                String note = getRawText(e);
                return note == null || note.isEmpty() ? null : TextUtil.toSingleLine(note);
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getRawText(e);
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getRawText(e);
                return note == null || note.isEmpty() ? null : TextUtil.wordwrap(note);
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getLastTransaction().getTransaction().getNote() : null;
        }));
        support.addColumn(column);

        column = new Column("portfolio", Messages.ColumnPortfolio, SWT.LEFT, 100); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPortfolio);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade trade = asTrade(e);
                return trade != null ? trade.getPortfolio().getName() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getPortfolio().getName() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);

        column = new IsinColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new SymbolColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new WknColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("instrumentCurrency", Messages.ColumnCurrency, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnSecurity);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Trade trade = asTrade(element);
                return trade != null ? trade.getSecurity().getCurrencyCode() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Trade trade = asTrade(e);
            return trade != null ? trade.getSecurity().getCurrencyCode() : null;
        }));
        column.setVisible(false);
        support.addColumn(column);
        
        // Wrap all sorters with TradeElementComparator to maintain taxonomy grouping
        support.getColumns().forEach(col -> {
            if (col.getSorter() != null)
                col.getSorter().wrap(TradeElementComparator::new);
        });
    }

    public void setInput(List<?> items)
    {
        this.trades.setInput(items);
    }

    public Object getInput()
    {
        return this.trades.getInput();
    }

    public TableViewer getTableViewer()
    {
        return trades;
    }

    public ShowHideColumnHelper getShowHideColumnHelper()
    {
        return support;
    }

    /**
     * Comparator that sorts by TradeElement sortOrder first (to keep taxonomy
     * groups together), then by the wrapped comparator (to sort within each
     * group). Similar to StatementOfAssetsViewer.ElementComparator.
     */
    public static class TradeElementComparator implements Comparator<Object>
    {
        private Comparator<Object> comparator;

        public TradeElementComparator(Comparator<Object> wrapped)
        {
            this.comparator = wrapped;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            // Extract sortOrder from TradeElements, otherwise use 0 for plain Trades
            int a = o1 instanceof TradeElement ? ((TradeElement) o1).getSortOrder() : 0;
            int b = o2 instanceof TradeElement ? ((TradeElement) o2).getSortOrder() : 0;

            if (a != b)
            {
                int direction = ColumnViewerSorter.SortingContext.getSortDirection();
                return direction == SWT.UP ? a - b : b - a;
            }

            // Same sortOrder, use wrapped comparator to sort within the group
            return comparator.compare(o1, o2);
        }
    }
}
