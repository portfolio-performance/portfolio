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
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.ISeries;

public class ConsumerPriceIndexListView extends AbstractListView
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
            @Override
            public void run()
            {
                new TimelineChartCSVExporter(chart).export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_EXPORT));
        export.setToolTipText(Messages.MenuExportData);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    @Override
    public void notifyModelUpdated()
    {
        indeces.setInput(getClient().getConsumerPriceIndeces());
        refreshChart();
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        indeces = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(ConsumerPriceIndexListView.class.getSimpleName()
                        + "@bottom", indeces, layout); //$NON-NLS-1$

        Column column = new Column(Messages.ColumnYear, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return String.valueOf(((ConsumerPriceIndex) element).getYear());
            }
        });
        column.setSorter(ColumnViewerSorter.create(ConsumerPriceIndex.class, "year", "month"), SWT.DOWN); //$NON-NLS-1$ //$NON-NLS-2$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(ConsumerPriceIndex.class, "month", "year")); //$NON-NLS-1$ //$NON-NLS-2$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(ConsumerPriceIndex.class, "index")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();

        indeces.getTable().setHeaderVisible(true);
        indeces.getTable().setLinesVisible(true);

        indeces.setContentProvider(new SimpleListContentProvider());

        indeces.setInput(getClient().getConsumerPriceIndeces());
        indeces.refresh();
        ViewerHelper.pack(indeces);

        new CellEditorFactory(indeces, ConsumerPriceIndex.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                indeces.refresh(element);
                                refreshChart();
                            }
                        }) //
                        .editable("year") // //$NON-NLS-1$
                        .month("month") // //$NON-NLS-1$
                        .index("index") // //$NON-NLS-1$
                        .apply();

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

                getClient().getConsumerPriceIndeces().remove(index);
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
        refreshChart();
    }

    private void refreshChart()
    {
        for (ISeries s : chart.getSeriesSet().getSeries())
            chart.getSeriesSet().deleteSeries(s.getId());

        if (getClient().getConsumerPriceIndeces() == null || getClient().getConsumerPriceIndeces().isEmpty())
            return;

        List<ConsumerPriceIndex> indeces = new ArrayList<ConsumerPriceIndex>(getClient().getConsumerPriceIndeces());
        Collections.sort(indeces);

        Date[] dates = new Date[indeces.size()];
        double[] cpis = new double[indeces.size()];

        int ii = 0;
        for (ConsumerPriceIndex index : indeces)
        {
            dates[ii] = Dates.date(index.getYear(), index.getMonth(), 1);
            cpis[ii] = (double) index.getIndex() / Values.Index.divider();
            ii++;
        }

        chart.addDateSeries(dates, cpis, Colors.CPI);

        chart.getAxisSet().adjustRange();

        chart.redraw();
    }

}
