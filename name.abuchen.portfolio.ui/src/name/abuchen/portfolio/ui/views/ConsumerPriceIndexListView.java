package name.abuchen.portfolio.ui.views;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
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

    @Override
    public void notifyModelUpdated()
    {
        indeces.setInput(getClient().getConsumerPriceIndeces());
        refreshChart();
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        indeces = new TableViewer(parent, SWT.FULL_SELECTION);

        TableColumn column = new TableColumn(indeces.getTable(), SWT.None);
        column.setText(Messages.ColumnYear);
        column.setWidth(80);

        column = new TableColumn(indeces.getTable(), SWT.None);
        column.setText(Messages.ColumnMonth);
        column.setWidth(80);

        column = new TableColumn(indeces.getTable(), SWT.RIGHT);
        column.setText(Messages.ColumnIndex);
        column.setWidth(80);

        indeces.getTable().setHeaderVisible(true);
        indeces.getTable().setLinesVisible(true);

        indeces.setLabelProvider(new CPILabelProvider());
        indeces.setContentProvider(new SimpleListContentProvider());

        Collections.sort(getClient().getConsumerPriceIndeces());
        indeces.setInput(getClient().getConsumerPriceIndeces());
        indeces.refresh();

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
                        .amount("index") // //$NON-NLS-1$
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
                refreshChart();
            }
        });
    }

    static class CPILabelProvider extends LabelProvider implements ITableLabelProvider
    {
        private static final String[] MONTHS = new DateFormatSymbols().getMonths();

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            ConsumerPriceIndex p = (ConsumerPriceIndex) element;
            switch (columnIndex)
            {
                case 0:
                    return String.valueOf(p.getYear());
                case 1:
                    return String.valueOf(MONTHS[p.getMonth()]);
                case 2:
                    return String.format("%,10.2f", p.getIndex() / 100d); //$NON-NLS-1$
            }
            return null;
        }

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
            cpis[ii] = (double) index.getIndex() / 100d;
            ii++;
        }

        chart.addDateSeries(dates, cpis, Colors.CPI);

        chart.getAxisSet().adjustRange();

        chart.redraw();
    }

}
