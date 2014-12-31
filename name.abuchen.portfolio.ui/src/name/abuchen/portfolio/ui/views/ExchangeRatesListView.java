package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import name.abuchen.portfolio.model.ExchangeRate;
import name.abuchen.portfolio.model.ExchangeRateProviderFactory;
import name.abuchen.portfolio.model.ExchangeRateTimeSeries;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.ISeries;

public class ExchangeRatesListView extends AbstractListView
{
    @Inject
    private ExchangeRateProviderFactory providerFactory;

    private TimelineChart chart;

    @Override
    protected String getTitle()
    {
        return "Währungen";
    }

    @Override
    public void setFocus()
    {
        chart.getAxisSet().adjustRange();
        super.setFocus();
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        TableViewer indeces = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        ExchangeRatesListView.class.getSimpleName() + "@top", getPreferenceStore(), indeces, layout); //$NON-NLS-1$

        Column column = new Column("Base Currency", SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ExchangeRateTimeSeries) element).getBaseCurrency();
            }
        });
        ColumnViewerSorter.create(ExchangeRateTimeSeries.class, "baseCurrency", "termCurrency").attachTo(column,
                        SWT.DOWN);
        support.addColumn(column);

        column = new Column("Term Currency", SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ExchangeRateTimeSeries) element).getTermCurrency();
            }
        });
        ColumnViewerSorter.create(ExchangeRateTimeSeries.class, "termCurrency", "baseCurrency").attachTo(column); //$NON-NLS-1$ //$NON-NLS-2$
        support.addColumn(column);

        column = new Column("Provider", SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ExchangeRateTimeSeries) element).getProvider().getName();
            }
        });
        ColumnViewerSorter.create(ExchangeRateTimeSeries.class, "provider").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        indeces.getTable().setHeaderVisible(true);
        indeces.getTable().setLinesVisible(true);

        indeces.setContentProvider(new SimpleListContentProvider());

        indeces.setInput(providerFactory.getAvailableTimeSeries());
        indeces.refresh();
        ViewerHelper.pack(indeces);

        indeces.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                ExchangeRateTimeSeries series = (ExchangeRateTimeSeries) ((IStructuredSelection) event.getSelection())
                                .getFirstElement();
                refreshChart(series);
            }
        });
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        chart = new TimelineChart(parent);
        chart.getToolTip().setValueFormat(new DecimalFormat("0.0000")); //$NON-NLS-1$
        refreshChart(null);
    }

    private void refreshChart(ExchangeRateTimeSeries series)
    {
        for (ISeries s : chart.getSeriesSet().getSeries())
            chart.getSeriesSet().deleteSeries(s.getId());

        if (series == null || series.getRates().isEmpty())
        {
            chart.getTitle().setText("Währungen");
            return;
        }

        List<ExchangeRate> rates = series.getRates();

        Date[] dates = new Date[rates.size()];
        double[] values = new double[rates.size()];

        int ii = 0;
        for (ExchangeRate rate : rates)
        {
            dates[ii] = rate.getTime();
            values[ii] = (double) rate.getValue() / Values.ExchangeRate.divider();
            ii++;
        }

        String title = MessageFormat.format("{0}/{1} ({2})", series.getBaseCurrency(), series.getTermCurrency(), series
                        .getProvider().getName());

        chart.getTitle().setText(title);
        chart.addDateSeries(dates, values, Colors.TOTALS, title);

        chart.getAxisSet().adjustRange();

        chart.redraw();
    }
}
