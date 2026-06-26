package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.lessThan;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransactionEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransactionEditor;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class AccountPerformanceTaxRefundTestCase
{
    /**
     * Feature: when calculating the performance of an account, do include taxes
     * and tax refunds but only those that are not paid for a security.
     */
    @Test
    public void testAccountPerformanceTaxRefund() throws IOException
    {
        Client client = ClientFactory
                        .load(SecurityTestCase.class.getResourceAsStream("account_performance_tax_refund.xml"));

        Account account = client.getAccounts().get(0);
        Interval period = Interval.of(LocalDate.parse("2013-12-06"), LocalDate.parse("2014-12-06"));

        AccountTransaction deposit = account.getTransactions().get(0);

        // no changes in holdings, ttwror must be:
        double startValue = deposit.getAmount();
        double endValue = account.getCurrentAmount(LocalDateTime.of(2016, 1, 1, 10, 00));
        double ttwror = (endValue / startValue) - 1;

        List<Exception> warnings = new ArrayList<>();
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex accountPerformance = PerformanceIndex.forAccount(client, converter, account, period, warnings);
        assertThat(warnings, empty());

        double calculatedTtwror = accountPerformance.getFinalAccumulatedPercentage();
        assertThat(calculatedTtwror, closeTo(ttwror, 0.0001));

        // if the tax_refund is for a security, it must not be included in the
        // performance of the account
        AccountTransaction taxRefund = account.getTransactions().stream() //
                        .filter(transaction -> transaction.getType() == AccountTransaction.Type.TAX_REFUND) //
                        .findFirst().orElseThrow();
        assertThat(taxRefund.getType(), is(AccountTransaction.Type.TAX_REFUND));
        setSecurity(taxRefund, new Security());

        accountPerformance = PerformanceIndex.forAccount(client, converter, account, period, warnings);
        assertThat(warnings, empty());
        assertThat(accountPerformance.getFinalAccumulatedPercentage(), lessThan(calculatedTtwror));
    }

    private void setSecurity(AccountTransaction transaction, Security security)
    {
        if (transaction instanceof LedgerBackedAccountTransaction ledgerBacked)
        {
            new LedgerAccountTransactionEditor().apply(ledgerBacked,
                            LedgerAccountTransactionEdit.builder().security(security).build());
        }
        else
        {
            transaction.setSecurity(security);
        }
    }
}
