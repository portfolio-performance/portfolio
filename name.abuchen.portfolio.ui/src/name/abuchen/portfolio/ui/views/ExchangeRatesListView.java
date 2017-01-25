package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.ISeries;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;

public class ExchangeRatesListView extends AbstractListView
{
    @Inject
    private ExchangeRateProviderFactory providerFactory;

    private TimelineChart chart;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelCurrencies;
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

        ShowHideColumnHelper support = new ShowHideColumnHelper(ExchangeRatesListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), indeces, layout);

        Column column = new Column(Messages.ColumnBaseCurrency, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ExchangeRateTimeSeries) element).getBaseCurrency();
            }
        });
        ColumnViewerSorter.create(ExchangeRateTimeSeries.class, "baseCurrency", "termCurrency") //$NON-NLS-1$ //$NON-NLS-2$
                        .attachTo(column, SWT.DOWN);
        support.addColumn(column);

        column = new Column(Messages.ColumnTermCurrency, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ExchangeRateTimeSeries) element).getTermCurrency();
            }
        });
        ColumnViewerSorter.create(ExchangeRateTimeSeries.class, "termCurrency", "baseCurrency") //$NON-NLS-1$ //$NON-NLS-2$
                        .attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnCurrencyProvider, SWT.None, 150);
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

        column = new Column(Messages.ColumnDateLatestExchangeRate, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                ExchangeRateTimeSeries series = (ExchangeRateTimeSeries) element;
                List<ExchangeRate> rates = series.getRates();
                return rates.isEmpty() ? null : Values.Date.format(rates.get(rates.size() - 1).getTime());
            }
        });
        ColumnViewerSorter.create(element -> {
            ExchangeRateTimeSeries series = (ExchangeRateTimeSeries) element;
            List<ExchangeRate> rates = series.getRates();
            return rates.isEmpty() ? null : rates.get(rates.size() - 1).getTime();
        }).attachTo(column);
        support.addColumn(column);

        support.createColumns();

        indeces.getTable().setHeaderVisible(true);
        indeces.getTable().setLinesVisible(true);

        indeces.setContentProvider(ArrayContentProvider.getInstance());

        indeces.setInput(providerFactory.getAvailableTimeSeries());
        indeces.refresh();

        indeces.addSelectionChangedListener(event -> refreshChart(
                        (ExchangeRateTimeSeries) ((IStructuredSelection) event.getSelection()).getFirstElement()));
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
        try
        {
            chart.suspendUpdate(true);
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            if (series == null || series.getRates().isEmpty())
            {
                chart.getTitle().setText(Messages.LabelCurrencies);
                return;
            }

            List<ExchangeRate> rates = series.getRates();

            LocalDate[] dates = new LocalDate[rates.size()];
            double[] values = new double[rates.size()];

            int ii = 0;
            for (ExchangeRate rate : rates)
            {
                dates[ii] = rate.getTime();
                values[ii] = rate.getValue().doubleValue();
                ii++;
            }

            String title = MessageFormat.format("{0}/{1} ({2})", //$NON-NLS-1$
                            series.getBaseCurrency(), series.getTermCurrency(), series.getProvider().getName());

            chart.getTitle().setText(title);
            chart.addDateSeries(dates, values, Colors.TOTALS, title);

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
            chart.redraw();
        }
    }
}
