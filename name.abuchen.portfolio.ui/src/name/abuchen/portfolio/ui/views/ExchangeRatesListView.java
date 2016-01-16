package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.htmlchart.HtmlChart;
import name.abuchen.portfolio.ui.util.htmlchart.HtmlChartConfigTimeline;
import name.abuchen.portfolio.ui.util.htmlchart.HtmlChartConfigTimelineSeriesLine;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.SimpleListContentProvider;

public class ExchangeRatesListView extends AbstractListView
{
    @Inject
    private ExchangeRateProviderFactory providerFactory;
    
    private HtmlChart chart;
    private HtmlChartConfigTimeline chartConfig;

    @Override
    protected String getTitle()
    {
        return Messages.LabelCurrencies;
    }

    @Override
    public void setFocus()
    {
        //chart.getAxisSet().adjustRange();
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

        support.createColumns();

        indeces.getTable().setHeaderVisible(true);
        indeces.getTable().setLinesVisible(true);

        indeces.setContentProvider(new SimpleListContentProvider());

        indeces.setInput(providerFactory.getAvailableTimeSeries());
        indeces.refresh();

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
        chartConfig = new HtmlChartConfigTimeline();
        chartConfig.setShowLegend(false)
                .setNumberFormat("0.0000")
                .setNumberFormatLocale("de");
        chart = new HtmlChart(chartConfig);
        chart.createControl(parent);
        refreshChart(null);
    }

    private void refreshChart(ExchangeRateTimeSeries series)
    {
        chartConfig.series().clear();

        if (series == null || series.getRates().isEmpty())
        {
            chartConfig.setTitle(Messages.LabelCurrencies);
            return;
        } else {
            String title = MessageFormat.format("{0}/{1} ({2})", //$NON-NLS-1$
                            series.getTermCurrency(), series.getBaseCurrency(), series.getProvider().getName());
            chartConfig.setTitle(title);
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

        HtmlChartConfigTimelineSeriesLine chartSeries = new HtmlChartConfigTimelineSeriesLine(
                        Messages.LabelConsumerPriceIndex, dates, values,
                        new RGB(Colors.CPI.red(), Colors.CPI.green(), Colors.CPI.blue()), 1);
        
        chartConfig.series().add(chartSeries);
        chart.refreshChart();
    }
}
