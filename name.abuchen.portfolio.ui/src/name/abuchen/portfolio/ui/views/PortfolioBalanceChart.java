package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

public class PortfolioBalanceChart extends TimelineChart // NOSONAR
{
    private Client client;

    public PortfolioBalanceChart(Composite parent, Client client)
    {
        super(parent);
        this.client = client;
        getTitle().setVisible(false);
    }

    public void updateChart(Portfolio portfolio, ExchangeRateProviderFactory exchangeRateProviderFactory)
    {
        try
        {
            suspendUpdate(true);

            for (ISeries s : getSeriesSet().getSeries())
                getSeriesSet().deleteSeries(s.getId());

            if (portfolio == null)
                return;

            List<PortfolioTransaction> tx = portfolio.getTransactions();

            if (tx.isEmpty())
                return;

            Collections.sort(tx, Transaction.BY_DATE);

            LocalDate now = LocalDate.now();
            LocalDate start = tx.get(0).getDateTime().toLocalDate();
            LocalDate end = tx.get(tx.size() - 1).getDateTime().toLocalDate();

            CurrencyConverter converter = new CurrencyConverterImpl(exchangeRateProviderFactory,
                            client.getBaseCurrency());

            if (now.isAfter(end))
                end = now;
            if (now.isBefore(start))
                start = now;

            int days = (int) ChronoUnit.DAYS.between(start, end) + 2;

            LocalDate[] dates = new LocalDate[days];
            double[] values = new double[days];

            dates[0] = start.minusDays(1);
            values[0] = 0d;

            for (int ii = 1; ii < dates.length; ii++)
            {
                values[ii] = PortfolioSnapshot.create(portfolio, converter, start) //
                                .getValue().getAmount() / Values.Amount.divider();
                dates[ii] = start;
                start = start.plusDays(1);
            }

            addDateSeries(portfolio.getUUID(), dates, values, Colors.CASH, portfolio.getName());
        }
        finally
        {
            adjustRange();
            suspendUpdate(false);
        }
    }

}
