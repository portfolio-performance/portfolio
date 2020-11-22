package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

public class AccountBalanceChart extends TimelineChart // NOSONAR
{

    public AccountBalanceChart(Composite parent)
    {
        super(parent);
        getTitle().setVisible(false);
    }

    public void updateChart(Account account, ExchangeRateProviderFactory exchangeRateProviderFactory)
    {
        try
        {
            suspendUpdate(true);

            for (ISeries s : getSeriesSet().getSeries())
                getSeriesSet().deleteSeries(s.getId());

            if (account == null)
                return;

            List<AccountTransaction> tx = account.getTransactions();

            if (tx.isEmpty())
                return;

            LocalDate now = LocalDate.now();
            LocalDate start = tx.get(0).getDateTime().toLocalDate();
            LocalDate end = tx.get(tx.size() - 1).getDateTime().toLocalDate();

            CurrencyConverter converter = new CurrencyConverterImpl(exchangeRateProviderFactory,
                            account.getCurrencyCode());
            Collections.sort(tx, new Transaction.ByDate());

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
                values[ii] = AccountSnapshot.create(account, converter, start) //
                                .getFunds().getAmount() / Values.Amount.divider();
                dates[ii] = start;
                start = start.plusDays(1);
            }

            addDateSeries(dates, values, Colors.CASH, account.getName());
        }
        finally
        {
            adjustRange();
            suspendUpdate(false);
        }
    }

}
