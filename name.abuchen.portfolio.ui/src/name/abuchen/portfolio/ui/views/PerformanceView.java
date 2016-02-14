package name.abuchen.portfolio.ui.views;

import static name.abuchen.portfolio.ui.util.SWTHelper.clearLabel;
import static name.abuchen.portfolio.ui.util.SWTHelper.placeBelow;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.GroupEarningsByAccount;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.TreeViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.SimpleListContentProvider;
import name.abuchen.portfolio.util.Interval;

public class PerformanceView extends AbstractHistoricView
{
    private static class OverviewTab implements DisposeListener
    {
        private ClientPerformanceSnapshot snapshot;

        private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

        private Font kpiFont;
        private Font boldFont;

        private Composite container;

        private Label ttwror;
        private Label irr;
        private Label absoluteChange;
        private Label delta;

        private Label ttwrorLastDay;
        private Label absoluteChangeLastDay;

        private Label maxDrawdown;
        private Label maxDrawdownDuration;
        private Label volatility;
        private Label semiVolatility;

        private Label[] signs;
        private Label[] labels;
        private Label[] values;

        public void setInput(ClientPerformanceSnapshot snapshot)
        {
            this.snapshot = snapshot;

            PerformanceIndex index = snapshot.getPerformanceIndex();

            if (index.getTotals().length > 1)
            {
                setIndicators(snapshot, index);
                setRiskIndicators(index);
            }
            else
            {
                clearLabel(ttwror, irr, absoluteChange, delta, ttwrorLastDay, absoluteChangeLastDay, maxDrawdown,
                                maxDrawdownDuration, volatility, semiVolatility);
            }

            int ii = 0;
            for (ClientPerformanceSnapshot.Category category : snapshot.getCategories())
            {
                signs[ii].setText(category.getSign());
                labels[ii].setText(category.getLabel());
                values[ii].setText(Values.Money.format(category.getValuation(), index.getClient().getBaseCurrency()));

                if (++ii >= labels.length)
                    break;
            }

            container.layout(true);
        }

        private void setIndicators(ClientPerformanceSnapshot snapshot, PerformanceIndex index)
        {
            int length = index.getTotals().length;
            ttwror.setText(Values.Percent2.format(index.getFinalAccumulatedPercentage()));
            irr.setText(Values.Percent2.format(snapshot.getPerformanceIRR()));
            absoluteChange.setText(Values.Amount.format(index.getTotals()[length - 1] - index.getTotals()[0]));
            delta.setText(Values.Money.format(snapshot.getAbsoluteDelta(), index.getClient().getBaseCurrency()));

            ttwrorLastDay.setText(Values.Percent2.format(index.getDeltaPercentage()[length - 1]));
            absoluteChangeLastDay.setText(
                            Values.Amount.format(index.getTotals()[length - 1] - index.getTotals()[length - 2]));
        }

        private void setRiskIndicators(PerformanceIndex index)
        {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                            .withZone(ZoneId.systemDefault());

            Drawdown drawdown = index.getDrawdown();
            Volatility vola = index.getVolatility();

            maxDrawdown.setText(Values.Percent2.format(drawdown.getMaxDrawdown()));
            InfoToolTip.attach(maxDrawdown,
                            MessageFormat.format(Messages.TooltipMaxDrawdown,
                                            formatter.format(drawdown.getIntervalOfMaxDrawdown().getStart()),
                                            formatter.format(drawdown.getIntervalOfMaxDrawdown().getEnd())));

            // max drawdown duration
            Interval maxDDDuration = drawdown.getMaxDrawdownDuration();
            maxDrawdownDuration.setText(MessageFormat.format(Messages.LabelXDays, maxDDDuration.getDays()));
            boolean isUntilEndOfPeriod = maxDDDuration.getEnd().equals(index.getReportInterval().getEndDate());
            String maxDDSupplement = isUntilEndOfPeriod ? Messages.TooltipMaxDrawdownDurationEndOfPeriod
                            : Messages.TooltipMaxDrawdownDurationFromXtoY;

            // recovery time
            Interval recoveryTime = drawdown.getLongestRecoveryTime();
            isUntilEndOfPeriod = recoveryTime.getEnd().equals(index.getReportInterval().getEndDate());
            String recoveryTimeSupplement = isUntilEndOfPeriod ? Messages.TooltipMaxDrawdownDurationEndOfPeriod
                            : Messages.TooltipMaxDrawdownDurationFromXtoY;

            InfoToolTip.attach(maxDrawdownDuration, Messages.TooltipMaxDrawdownDuration + "\n\n" //$NON-NLS-1$
                            + MessageFormat.format(maxDDSupplement, formatter.format(maxDDDuration.getStart()),
                                            formatter.format(maxDDDuration.getEnd()))
                            + "\n\n" //$NON-NLS-1$
                            + MessageFormat.format(Messages.TooltipMaxDurationLowToHigh, recoveryTime.getDays())
                            + MessageFormat.format(recoveryTimeSupplement, formatter.format(recoveryTime.getStart()),
                                            formatter.format(recoveryTime.getEnd())));

            volatility.setText(Values.Percent2.format(index.getVolatility().getStandardDeviation()));
            InfoToolTip.attach(volatility, Messages.TooltipVolatility);

            semiVolatility.setText(Values.Percent2.format(vola.getSemiDeviation()));
            InfoToolTip.attach(semiVolatility,
                            MessageFormat.format(Messages.TooltipSemiVolatility,
                            Values.Percent5.format(vola.getExpectedSemiDeviation()),
                            vola.getNormalizedSemiDeviationComparison(),
                            Values.Percent5.format(vola.getStandardDeviation()),
                            Values.Percent5.format(vola.getSemiDeviation())));
        }

        public void createTab(CTabFolder folder)
        {
            container = new Composite(folder, SWT.NONE);
            container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            RowLayout layout = new RowLayout();
            layout.marginHeight = layout.marginWidth = 10;
            layout.spacing = 40;
            container.setLayout(layout);
            container.addDisposeListener(this);

            // create fonts
            kpiFont = resourceManager.createFont(
                            FontDescriptor.createFrom(container.getFont()).setStyle(SWT.NORMAL).increaseHeight(10));

            boldFont = resourceManager.createFont(FontDescriptor
                            .createFrom(JFaceResources.getFont(JFaceResources.HEADER_FONT)).setStyle(SWT.BOLD));

            createIndicators(container);
            createRiskIndicators(container);
            createCalculation(container);

            CTabItem item = new CTabItem(folder, SWT.NONE);
            item.setText(Messages.PerformanceTabOverview);
            item.setControl(container);
        }

        private void createIndicators(Composite container)
        {
            Composite composite = new Composite(container, SWT.NONE);
            composite.setLayout(new FormLayout());
            composite.setBackground(container.getBackground());

            Label heading = new Label(composite, SWT.NONE);
            heading.setText(Messages.LabelKeyIndicators);
            heading.setFont(boldFont);
            heading.setForeground(resourceManager.createColor(Colors.HEADINGS.swt()));

            int[] maxWidth = new int[1];

            ttwror = addKPIBelow(Messages.LabelTTWROR, heading, maxWidth);
            irr = addKPIBelow(Messages.LabelIRR, ttwror, maxWidth);
            absoluteChange = addKPIBelow(Messages.LabelAbsoluteChange, irr, maxWidth);
            delta = addKPIBelow(Messages.LabelAbsoluteDelta, absoluteChange, maxWidth);

            Label headingLastDay = new Label(composite, SWT.NONE);
            headingLastDay.setText(Messages.LabelTTWROROneDay);
            headingLastDay.setFont(boldFont);
            headingLastDay.setForeground(resourceManager.createColor(Colors.HEADINGS.swt()));

            ttwrorLastDay = addKPIBelow(Messages.LabelTTWROR, headingLastDay, maxWidth);
            absoluteChangeLastDay = addKPIBelow(Messages.LabelAbsoluteChange, ttwrorLastDay, maxWidth);

            // layout

            FormData data = new FormData();
            data.left = new FormAttachment(0, 5);
            data.width = maxWidth[0];
            heading.setLayoutData(data);

            data = new FormData();
            data.left = new FormAttachment(0, 5);
            data.top = new FormAttachment(delta, 20);
            data.width = maxWidth[0];
            headingLastDay.setLayoutData(data);
        }

        private void createRiskIndicators(Composite container2)
        {
            Composite composite = new Composite(container, SWT.NONE);
            composite.setLayout(new FormLayout());
            composite.setBackground(container.getBackground());

            Label heading = new Label(composite, SWT.NONE);
            heading.setText(Messages.LabelRiskIndicators);
            heading.setFont(boldFont);
            heading.setForeground(resourceManager.createColor(Colors.HEADINGS.swt()));

            int[] maxWidth = new int[1];

            maxDrawdown = addKPIBelow(Messages.LabelMaxDrawdown, heading, maxWidth);
            maxDrawdownDuration = addKPIBelow(Messages.LabelMaxDrawdownDuration, maxDrawdown, maxWidth);
            volatility = addKPIBelow(Messages.LabelVolatility, maxDrawdownDuration, maxWidth);
            semiVolatility = addKPIBelow(Messages.LabelSemiVolatility, volatility, maxWidth);

            // layout

            FormData data = new FormData();
            data.left = new FormAttachment(0, 5);
            data.width = Math.max(maxWidth[0], heading.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
            heading.setLayoutData(data);
        }

        private Label addKPIBelow(String label, Control other, int[] maxWidth)
        {
            Label lblKpi = new Label(other.getParent(), SWT.NONE);
            lblKpi.setText(label);

            Label kpi = new Label(other.getParent(), SWT.NONE);
            kpi.setFont(kpiFont);

            placeBelow(other, lblKpi);
            placeBelow(lblKpi, kpi);

            int width = lblKpi.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            maxWidth[0] = Math.max(width, maxWidth[0]);

            return kpi;
        }

        private void createCalculation(Composite container)
        {
            Composite composite = new Composite(container, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(composite);
            composite.setBackground(container.getBackground());

            Label heading = new Label(composite, SWT.NONE);
            heading.setText(Messages.PerformanceTabCalculation);
            heading.setFont(boldFont);
            heading.setForeground(resourceManager.createColor(Colors.HEADINGS.swt()));
            GridDataFactory.fillDefaults().span(3, 1).applyTo(heading);

            labels = new Label[ClientPerformanceSnapshot.CategoryType.values().length];
            signs = new Label[labels.length];
            values = new Label[labels.length];

            for (int ii = 0; ii < labels.length; ii++)
            {
                signs[ii] = new Label(composite, SWT.NONE);
                labels[ii] = new Label(composite, SWT.NONE);
                values[ii] = new Label(composite, SWT.RIGHT);
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(values[ii]);
            }
        }

        @Override
        public void widgetDisposed(DisposeEvent e)
        {
            resourceManager.dispose();
        }

        public ClientPerformanceSnapshot getSnapshot()
        {
            return snapshot;
        }
    }

    @Inject
    private ExchangeRateProviderFactory factory;

    private OverviewTab overview;
    private TreeViewer calculation;
    private StatementOfAssetsViewer snapshotStart;
    private StatementOfAssetsViewer snapshotEnd;
    private TableViewer earnings;
    private TableViewer earningsByAccount;

    @Override
    protected String getTitle()
    {
        return Messages.LabelPerformanceCalculation;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        new ExportDropDown(toolBar);
    }

    @Override
    public void reportingPeriodUpdated()
    {
        ReportingPeriod period = getReportingPeriod();
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(getClient(), converter, period);

        overview.setInput(snapshot);

        try
        {
            calculation.getTree().setRedraw(false);
            calculation.setInput(snapshot);
            calculation.expandAll();
            calculation.getTree().getParent().layout();
        }
        finally
        {
            calculation.getTree().setRedraw(true);
        }

        snapshotStart.setInput(snapshot.getStartClientSnapshot());
        snapshotEnd.setInput(snapshot.getEndClientSnapshot());

        earnings.setInput(snapshot.getEarnings());
        earningsByAccount.setInput(new GroupEarningsByAccount(snapshot).getItems());
    }

    @Override
    public void notifyModelUpdated()
    {
        reportingPeriodUpdated();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        // result tabs
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        overview = new OverviewTab();
        overview.createTab(folder);

        createCalculationItem(folder, Messages.PerformanceTabCalculation);
        snapshotStart = createStatementOfAssetsItem(folder, Messages.PerformanceTabAssetsAtStart);
        snapshotEnd = createStatementOfAssetsItem(folder, Messages.PerformanceTabAssetsAtEnd);
        createEarningsItem(folder, Messages.PerformanceTabEarnings);
        createEarningsByAccountsItem(folder, Messages.PerformanceTabEarningsByAccount);

        folder.setSelection(0);

        reportingPeriodUpdated();

        return folder;
    }

    private StatementOfAssetsViewer createStatementOfAssetsItem(CTabFolder folder, String title)
    {
        StatementOfAssetsViewer viewer = make(StatementOfAssetsViewer.class);
        Control control = viewer.createControl(folder);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(control);

        return viewer;
    }

    private void createCalculationItem(CTabFolder folder, String title)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        calculation = new TreeViewer(container, SWT.FULL_SELECTION);

        final Font boldFont = JFaceResources.getFontRegistry().getBold(container.getFont().getFontData()[0].getName());

        TreeViewerColumn column = new TreeViewerColumn(calculation, SWT.NONE);
        column.getColumn().setText(Messages.ColumnLable);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                {
                    ClientPerformanceSnapshot.Category cat = (ClientPerformanceSnapshot.Category) element;
                    return cat.getLabel();
                }
                else if (element instanceof ClientPerformanceSnapshot.Position)
                {
                    ClientPerformanceSnapshot.Position pos = (ClientPerformanceSnapshot.Position) element;
                    return pos.getLabel();
                }
                return null;
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                {
                    return Images.CATEGORY.image();
                }
                else if (element instanceof ClientPerformanceSnapshot.Position)
                {
                    ClientPerformanceSnapshot.Position position = (ClientPerformanceSnapshot.Position) element;
                    return position.getSecurity() != null ? Images.SECURITY.image() : null;
                }

                return null;
            }

            @Override
            public Font getFont(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                    return boldFont;
                return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(350));

        column = new TreeViewerColumn(calculation, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnValue);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                {
                    ClientPerformanceSnapshot.Category cat = (ClientPerformanceSnapshot.Category) element;
                    return Values.Money.format(cat.getValuation(), getClient().getBaseCurrency());
                }
                else if (element instanceof ClientPerformanceSnapshot.Position)
                {
                    ClientPerformanceSnapshot.Position pos = (ClientPerformanceSnapshot.Position) element;
                    return Values.Money.format(pos.getValuation(), getClient().getBaseCurrency());
                }
                return null;
            }

            @Override
            public Font getFont(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                    return boldFont;
                return null;
            }
        });

        layout.setColumnData(column.getColumn(), new ColumnPixelData(80));

        calculation.getTree().setHeaderVisible(true);
        calculation.getTree().setLinesVisible(true);

        calculation.setContentProvider(new PerformanceContentProvider());

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(container);

        hookContextMenu(calculation.getTree(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Object selection = ((IStructuredSelection) calculation.getSelection()).getFirstElement();
        if (!(selection instanceof ClientPerformanceSnapshot.Position))
            return;

        Security security = ((ClientPerformanceSnapshot.Position) selection).getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
    }

    private void createEarningsItem(CTabFolder folder, String title)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        earnings = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(PerformanceView.class.getSimpleName() + "@earnings2", //$NON-NLS-1$
                        getPreferenceStore(), earnings, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((Transaction) element).getDate());
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "date"), SWT.UP); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((Transaction) element).getMonetaryAmount(), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "amount")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnSource, SWT.LEFT, 400);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction transaction = (Transaction) element;
                if (transaction.getSecurity() != null)
                    return transaction.getSecurity().getName();

                for (Account account : getClient().getAccounts())
                {
                    if (account.getTransactions().contains(transaction))
                        return account.getName();
                }

                return null;
            }

            @Override
            public Image getImage(Object element)
            {
                Transaction transaction = (Transaction) element;
                return transaction.getSecurity() != null ? Images.SECURITY.image() : Images.ACCOUNT.image();
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "security")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 22); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Transaction) element).getNote();
            }

            @Override
            public Image getImage(Object e)
            {
                String note = ((Transaction) e).getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "note")); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        earnings.getTable().setHeaderVisible(true);
        earnings.getTable().setLinesVisible(true);

        earnings.setContentProvider(new SimpleListContentProvider());

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(container);
    }

    private void createEarningsByAccountsItem(CTabFolder folder, String title)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        earningsByAccount = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(PerformanceView.class.getSimpleName() + "@byaccounts", //$NON-NLS-1$
                        getPreferenceStore(), earningsByAccount, layout);

        Column column = new Column(Messages.ColumnSource, SWT.LEFT, 400);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                GroupEarningsByAccount.Item item = (GroupEarningsByAccount.Item) element;
                return item.getAccount().getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.ACCOUNT.image();
            }
        });
        column.setSorter(ColumnViewerSorter.create(GroupEarningsByAccount.Item.class, "account")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                GroupEarningsByAccount.Item item = (GroupEarningsByAccount.Item) element;
                return Values.Money.format(item.getSum(), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(GroupEarningsByAccount.Item.class, "sum")); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        earningsByAccount.getTable().setHeaderVisible(true);
        earningsByAccount.getTable().setLinesVisible(true);

        earningsByAccount.setContentProvider(new SimpleListContentProvider());

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(container);
    }

    private static class PerformanceContentProvider implements ITreeContentProvider
    {
        private ClientPerformanceSnapshot.Category[] categories;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.categories = new ClientPerformanceSnapshot.Category[0];
            }
            else if (newInput instanceof ClientPerformanceSnapshot)
            {
                this.categories = ((ClientPerformanceSnapshot) newInput).getCategories()
                                .toArray(new ClientPerformanceSnapshot.Category[0]);
            }
            else
            {
                throw new RuntimeException("Unsupported type: " + newInput.getClass().getName()); //$NON-NLS-1$
            }
        }

        public Object[] getElements(Object inputElement)
        {
            return this.categories;
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof ClientPerformanceSnapshot.Category)
                return ((ClientPerformanceSnapshot.Category) parentElement).getPositions()
                                .toArray(new ClientPerformanceSnapshot.Position[0]);
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof ClientPerformanceSnapshot.Position)
            {
                for (ClientPerformanceSnapshot.Category c : categories)
                {
                    if (c.getPositions().contains(element))
                        return c;
                }

            }
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof ClientPerformanceSnapshot.Category
                            && !((ClientPerformanceSnapshot.Category) element).getPositions().isEmpty();
        }

        public void dispose()
        {}

    }

    private final class ExportDropDown extends AbstractDropDown
    {
        private ExportDropDown(ToolBar toolBar)
        {
            super(toolBar, Messages.MenuExportData, Images.EXPORT.image(), SWT.NONE);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabCalculation))
            {
                @Override
                public void run()
                {
                    new TreeViewerCSVExporter(calculation).export(Messages.PerformanceTabCalculation + ".csv"); //$NON-NLS-1$
                }
            });

            manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabAssetsAtStart))
            {
                @Override
                public void run()
                {
                    new TableViewerCSVExporter(snapshotStart.getTableViewer())
                                    .export(Messages.PerformanceTabAssetsAtStart + ".csv"); //$NON-NLS-1$
                }
            });

            manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabAssetsAtEnd))
            {
                @Override
                public void run()
                {
                    new TableViewerCSVExporter(snapshotEnd.getTableViewer())
                                    .export(Messages.PerformanceTabAssetsAtEnd + ".csv"); //$NON-NLS-1$
                }
            });

            manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabEarnings))
            {
                @Override
                public void run()
                {
                    new TableViewerCSVExporter(earnings).export(Messages.PerformanceTabEarnings + ".csv"); //$NON-NLS-1$
                }
            });

            manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabEarningsByAccount))
            {
                @Override
                public void run()
                {
                    new TableViewerCSVExporter(earningsByAccount)
                                    .export(Messages.PerformanceTabEarningsByAccount + ".csv"); //$NON-NLS-1$
                }
            });

            manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.LabelVolatility))
            {
                @Override
                public void run()
                {
                    new AbstractCSVExporter()
                    {
                        @Override
                        protected void writeToFile(File file) throws IOException
                        {
                            ClientPerformanceSnapshot snapshot = overview.getSnapshot();
                            if (snapshot == null)
                                return;

                            PerformanceIndex index = snapshot.getPerformanceIndex();
                            index.exportVolatilityData(file);
                        }

                        @Override
                        protected Control getControl()
                        {
                            return ExportDropDown.this.getToolBar();
                        }
                    }.export(Messages.LabelVolatility + ".csv"); //$NON-NLS-1$
                }
            });
        }
    }

}
