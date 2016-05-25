package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

/**
 * Chart of historical quotes for a given security
 */
public class SecuritiesChart
{

    private TimelineChart chart;
    private LocalDate chartPeriod = LocalDate.now().minusYears(2);
    private Security security;
    private Client client;

    public SecuritiesChart(Composite parent, Client client)
    {
        this.client = client;
        chart = new TimelineChart(parent);
        chart.getTitle().setText("..."); //$NON-NLS-1$
        chart.getToolTip().setValueFormat(new DecimalFormat(Values.Quote.pattern()));
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

        Composite buttons = new Composite(parent, SWT.NONE);
        buttons.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridDataFactory.fillDefaults().grab(false, true).applyTo(buttons);
        RowLayoutFactory.fillDefaults().type(SWT.VERTICAL).spacing(2).fill(true).applyTo(buttons);

        addButton(buttons, Messages.SecurityTabChart1M, Period.ofMonths(1));
        addButton(buttons, Messages.SecurityTabChart2M, Period.ofMonths(2));
        addButton(buttons, Messages.SecurityTabChart6M, Period.ofMonths(6));
        addButton(buttons, Messages.SecurityTabChart1Y, Period.ofYears(1));
        addButton(buttons, Messages.SecurityTabChart2Y, Period.ofYears(3));
        addButton(buttons, Messages.SecurityTabChart3Y, Period.ofYears(4));
        addButton(buttons, Messages.SecurityTabChart5Y, Period.ofYears(5));
        addButton(buttons, Messages.SecurityTabChart10Y, Period.ofYears(10));

        Button button = new Button(buttons, SWT.FLAT);
        button.setText(Messages.SecurityTabChartAll);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                chartPeriod = null;
                updateChart();
            }
        });
    }

    private void addButton(Composite buttons, String label, TemporalAmount amountToAdd)
    {
        Button b = new Button(buttons, SWT.FLAT);
        b.setText(label);
        b.addSelectionListener(new ChartPeriodSelectionListener()
        {
            @Override
            protected LocalDate startAt()
            {
                return LocalDate.now().minus(amountToAdd);
            }
        });
    }

    /**
     * ChartPeriodSelectionListener handles the selection of the time which
     * should be displayed in the chart
     */
    private abstract class ChartPeriodSelectionListener implements SelectionListener
    {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
            chartPeriod = startAt();
            updateChart();
        }

        protected abstract LocalDate startAt();

        @Override
        public void widgetDefaultSelected(SelectionEvent e)
        {
            // not used
        }
    }

    public void updateChart(Security security)
    {
        this.security = security;
        updateChart();
    }

    private void updateChart()
    {
        chart.setRedraw(false);

        try
        {
            ISeries series = chart.getSeriesSet().getSeries(Messages.ColumnQuote);
            if (series != null)
            {
                chart.getSeriesSet().deleteSeries(Messages.ColumnQuote);
            }
            chart.clearMarkerLines();

            if (security == null || security.getPrices().isEmpty())
            {
                chart.getTitle().setText(security == null ? "..." : security.getName()); //$NON-NLS-1$
                chart.redraw();
                return;
            }

            chart.getTitle().setText(security.getName());

            List<SecurityPrice> prices = security.getPrices();

            int index;
            LocalDate[] dates;
            double[] values;

            if (chartPeriod == null)
            {
                index = 0;
                dates = new LocalDate[prices.size()];
                values = new double[prices.size()];
            }
            else
            {
                index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(chartPeriod, 0),
                                new SecurityPrice.ByDate()));

                if (index >= prices.size())
                {
                    // no data available
                    chart.redraw();
                    return;
                }

                dates = new LocalDate[prices.size() - index];
                values = new double[prices.size() - index];
            }

            for (int ii = 0; index < prices.size(); index++, ii++)
            {
                SecurityPrice p = prices.get(index);
                dates[ii] = p.getTime();
                values[ii] = p.getValue() / Values.Quote.divider();
            }

            ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.ColumnQuote);
            lineSeries.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
            lineSeries.setLineWidth(2);
            lineSeries.enableArea(true);
            lineSeries.setSymbolType(PlotSymbolType.NONE);
            lineSeries.setYSeries(values);
            lineSeries.setAntialias(SWT.ON);

            chart.adjustRange();

            addChartMarker();

        }
        finally
        {
            chart.setRedraw(true);
            chart.redraw();
        }
    }

    private void addChartMarker()
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getSecurity() == security && (chartPeriod == null || chartPeriod.isBefore(t.getDate())))
                {
                    String label = Values.Share.format(t.getShares());
                    switch (t.getType())
                    {
                        case BUY:
                        case TRANSFER_IN:
                        case DELIVERY_INBOUND:
                            chart.addMarkerLine(t.getDate(), new RGB(0, 128, 0), label);
                            break;
                        case SELL:
                        case TRANSFER_OUT:
                        case DELIVERY_OUTBOUND:
                            chart.addMarkerLine(t.getDate(), new RGB(128, 0, 0), "-" + label); //$NON-NLS-1$
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            }
        }

        for (SecurityEvent event : security.getEvents())
        {
            if (chartPeriod == null || chartPeriod.isBefore(event.getDate()))
            {
                chart.addMarkerLine(event.getDate(), new RGB(255, 140, 0), event.getDetails());
            }
        }
    }
}
