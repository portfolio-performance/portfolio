package name.abuchen.portfolio.ui.views;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.MonthEditingSupport;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ValueEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.ISeries;

public class ConsumerPriceIndexListView extends AbstractListView implements ModificationListener
{
    private TableViewer indeces;
    private TimelineChart chart;

    @Override
    protected String getTitle()
    {
        return Messages.LabelConsumerPriceIndex;
    }

    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        addExportButton(toolBar);
    }

    private void addExportButton(ToolBar toolBar)
    {
        Action export = new Action()
        {
            private Menu menu;

            @Override
            public void run()
            {
                if (menu == null)
                {
                    menu = createContextMenu(getActiveShell(), new IMenuListener()
                    {
                        @Override
                        public void menuAboutToShow(IMenuManager manager)
                        {
                            exportMenuAboutToShow(manager);
                        }
                    });
                }
                menu.setVisible(true);
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_EXPORT));
        export.setToolTipText(Messages.MenuExportData);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    private void exportMenuAboutToShow(IMenuManager manager)
    {
        manager.add(new Action(Messages.MenuExportChartData)
        {
            @Override
            public void run()
            {
                new TimelineChartCSVExporter(chart).export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        });
        manager.add(new Separator());
        chart.exportMenuAboutToShow(manager, getTitle());
    }

    @Override
    public void setFocus()
    {
        chart.getAxisSet().adjustRange();
        super.setFocus();
    }

    @Override
    public void notifyModelUpdated()
    {
        indeces.setInput(getClient().getConsumerPriceIndeces());
        refreshChart();
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        markDirty();
        refreshChart();
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        indeces = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(indeces);

        ShowHideColumnHelper support = new ShowHideColumnHelper(ConsumerPriceIndexListView.class.getSimpleName()
                        + "@bottom", getPreferenceStore(), indeces, layout); //$NON-NLS-1$

        Column column = new Column(Messages.ColumnYear, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return String.valueOf(((ConsumerPriceIndex) element).getYear());
            }
        });
        ColumnViewerSorter.create(ConsumerPriceIndex.class, "year", "month").attachTo(column, SWT.DOWN); //$NON-NLS-1$ //$NON-NLS-2$
        new ValueEditingSupport(ConsumerPriceIndex.class, "year", Values.Year).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnMonth, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            private final String[] MONTHS = new DateFormatSymbols().getMonths();

            @Override
            public String getText(Object element)
            {
                return String.valueOf(MONTHS[((ConsumerPriceIndex) element).getMonth()]);
            }
        });
        ColumnViewerSorter.create(ConsumerPriceIndex.class, "month", "year").attachTo(column); //$NON-NLS-1$ //$NON-NLS-2$
        new MonthEditingSupport(ConsumerPriceIndex.class, "month").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnIndex, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Index.format(((ConsumerPriceIndex) element).getIndex());
            }
        });
        ColumnViewerSorter.create(ConsumerPriceIndex.class, "index").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(ConsumerPriceIndex.class, "index", Values.Index).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        indeces.getTable().setHeaderVisible(true);
        indeces.getTable().setLinesVisible(true);

        indeces.setContentProvider(new SimpleListContentProvider());

        indeces.setInput(getClient().getConsumerPriceIndeces());
        indeces.refresh();
        ViewerHelper.pack(indeces);

        hookContextMenu(indeces.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }

        });
    }

    private void fillContextMenu(IMenuManager manager)
    {
        manager.add(new Action(Messages.ConsumerPriceIndexMenuDelete)
        {
            @Override
            public void run()
            {
                ConsumerPriceIndex index = (ConsumerPriceIndex) ((IStructuredSelection) indeces.getSelection())
                                .getFirstElement();

                if (index == null)
                    return;

                getClient().removeConsumerPriceIndex(index);
                markDirty();

                indeces.setInput(getClient().getConsumerPriceIndeces());
                refreshChart();
            }
        });

        manager.add(new Action(Messages.ConsumerPriceIndexMenuAdd)
        {
            @Override
            public void run()
            {
                ConsumerPriceIndex index = new ConsumerPriceIndex();
                index.setYear(Calendar.getInstance().get(Calendar.YEAR));
                index.setMonth(Calendar.getInstance().get(Calendar.MONTH));

                getClient().addConsumerPriceIndex(index);
                markDirty();

                indeces.setInput(getClient().getConsumerPriceIndeces());
                indeces.editElement(index, 0);
                refreshChart();
            }
        });
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        chart = new TimelineChart(parent);
        chart.getTitle().setText(Messages.LabelConsumerPriceIndex);
        chart.getToolTip().setDateFormat("%1$tB %1$tY"); //$NON-NLS-1$
        refreshChart();
    }

    private void refreshChart()
    {
        for (ISeries s : chart.getSeriesSet().getSeries())
            chart.getSeriesSet().deleteSeries(s.getId());

        if (getClient().getConsumerPriceIndeces() == null || getClient().getConsumerPriceIndeces().isEmpty())
            return;

        List<ConsumerPriceIndex> indeces = new ArrayList<ConsumerPriceIndex>(getClient().getConsumerPriceIndeces());
        Collections.sort(indeces, new ConsumerPriceIndex.ByDate());

        Date[] dates = new Date[indeces.size()];
        double[] cpis = new double[indeces.size()];

        int ii = 0;
        for (ConsumerPriceIndex index : indeces)
        {
            dates[ii] = Dates.date(index.getYear(), index.getMonth(), 1);
            cpis[ii] = (double) index.getIndex() / Values.Index.divider();
            ii++;
        }

        chart.addDateSeries(dates, cpis, Colors.CPI, Messages.LabelConsumerPriceIndex);

        chart.getAxisSet().adjustRange();

        chart.redraw();
    }

}
