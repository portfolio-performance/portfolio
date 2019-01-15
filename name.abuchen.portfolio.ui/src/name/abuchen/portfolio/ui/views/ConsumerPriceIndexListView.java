package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.MonthEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;

public class ConsumerPriceIndexListView extends AbstractListView implements ModificationListener
{
    private TableViewer indices;
    private TimelineChart chart;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelConsumerPriceIndex;
    }

    @Override
    protected int getSashStyle()
    {
        return SWT.VERTICAL | SWT.BEGINNING;
    }

    @Override
    protected void addButtons(ToolBarManager manager)
    {
        super.addButtons(manager);
        addExportButton(manager);
    }

    private void addExportButton(ToolBarManager manager)
    {
        Action export = new Action()
        {
            private Menu menu;

            @Override
            public void run()
            {
                if (menu == null)
                    menu = createContextMenu(getActiveShell(), ConsumerPriceIndexListView.this::exportMenuAboutToShow);

                menu.setVisible(true);
            }
        };
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);

        manager.add(export);
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
        indices.setInput(getClient().getConsumerPriceIndices());
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

        indices = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(indices);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        ConsumerPriceIndexListView.class.getSimpleName() + "@bottom", getPreferenceStore(), indices, //$NON-NLS-1$
                        layout);

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
            @Override
            public String getText(Object element)
            {
                int month = ((ConsumerPriceIndex) element).getMonth();
                return Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault());
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

        indices.getTable().setHeaderVisible(true);
        indices.getTable().setLinesVisible(true);

        indices.setContentProvider(ArrayContentProvider.getInstance());

        indices.setInput(getClient().getConsumerPriceIndices());
        indices.refresh();

        hookContextMenu(indices.getTable(), this::fillContextMenu);
    }

    private void fillContextMenu(IMenuManager manager)
    {
        manager.add(new Action(Messages.ConsumerPriceIndexMenuDelete)
        {
            @Override
            public void run()
            {
                ConsumerPriceIndex index = (ConsumerPriceIndex) ((IStructuredSelection) indices.getSelection())
                                .getFirstElement();

                if (index == null)
                    return;

                getClient().removeConsumerPriceIndex(index);
                markDirty();

                indices.setInput(getClient().getConsumerPriceIndices());
                refreshChart();
            }
        });

        manager.add(new Action(Messages.ConsumerPriceIndexMenuAdd)
        {
            @Override
            public void run()
            {
                ConsumerPriceIndex index = new ConsumerPriceIndex();
                LocalDate now = LocalDate.now();
                index.setYear(now.getYear());
                index.setMonth(now.getMonthValue());

                getClient().addConsumerPriceIndex(index);
                markDirty();

                indices.setInput(getClient().getConsumerPriceIndices());
                indices.editElement(index, 0);
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
        try
        {
            chart.suspendUpdate(true);
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            if (getClient().getConsumerPriceIndices() == null || getClient().getConsumerPriceIndices().isEmpty())
                return;

            List<ConsumerPriceIndex> data = new ArrayList<>(getClient().getConsumerPriceIndices());
            Collections.sort(data, new ConsumerPriceIndex.ByDate());

            LocalDate[] dates = new LocalDate[data.size()];
            double[] cpis = new double[data.size()];

            int ii = 0;
            for (ConsumerPriceIndex index : data)
            {
                dates[ii] = LocalDate.of(index.getYear(), index.getMonth(), 1);
                cpis[ii] = (double) index.getIndex() / Values.Index.divider();
                ii++;
            }

            chart.addDateSeries(dates, cpis, Colors.CPI, Messages.LabelConsumerPriceIndex);

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
            chart.redraw();
        }
    }

}
