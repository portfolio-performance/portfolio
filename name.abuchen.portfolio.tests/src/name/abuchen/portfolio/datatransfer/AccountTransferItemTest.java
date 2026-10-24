package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class AccountTransferItemTest
{
    private Client client;

    private Account usd;
    private Account chf;
    private Account eurFromUsd;
    private Account eurFromChf;

    @Before
    public void prepare()
    {
        client = new Client();

        usd = account("USD account", "USD");
        chf = account("CHF account", "CHF");
        eurFromUsd = account("EUR account 1", "EUR");
        eurFromChf = account("EUR account 2", "EUR");
    }

    @Test
    public void testTargetAccountIsSelectedByCurrencyPair()
    {
        var context = new PairContext();
        context.targets.put("USD>EUR", eurFromUsd);
        context.targets.put("CHF>EUR", eurFromChf);

        var fromUsd = new AccountTransferItem(transfer("USD", 2200_00, "EUR", 1840_17), true);
        var fromChf = new AccountTransferItem(transfer("CHF", 1000_00, "EUR", 904_98), true);

        assertThat(fromUsd.apply(new InsertAction(client), context).getCode(), is(Status.Code.OK));
        assertThat(fromChf.apply(new InsertAction(client), context).getCode(), is(Status.Code.OK));

        assertBooking(usd, AccountTransaction.Type.TRANSFER_OUT, Money.of("USD", 2200_00));
        assertBooking(eurFromUsd, AccountTransaction.Type.TRANSFER_IN, Money.of("EUR", 1840_17));

        assertBooking(chf, AccountTransaction.Type.TRANSFER_OUT, Money.of("CHF", 1000_00));
        assertBooking(eurFromChf, AccountTransaction.Type.TRANSFER_IN, Money.of("EUR", 904_98));
    }

    @Test
    public void testTargetAccountFallsBackToTargetCurrency()
    {
        // a context which only knows the target currency (e.g. an existing
        // implementation which does not override the new method)
        var context = new ImportAction.Context()
        {
            @Override
            public Account getAccount(String currencyCode)
            {
                return "USD".equals(currencyCode) ? usd : null;
            }

            @Override
            public Account getSecondaryAccount(String currencyCode)
            {
                return "EUR".equals(currencyCode) ? eurFromUsd : null;
            }

            @Override
            public Portfolio getPortfolio()
            {
                return null;
            }

            @Override
            public Portfolio getSecondaryPortfolio()
            {
                return null;
            }
        };

        var item = new AccountTransferItem(transfer("USD", 2200_00, "EUR", 1840_17), true);

        assertThat(item.apply(new InsertAction(client), context).getCode(), is(Status.Code.OK));

        assertBooking(usd, AccountTransaction.Type.TRANSFER_OUT, Money.of("USD", 2200_00));
        assertBooking(eurFromUsd, AccountTransaction.Type.TRANSFER_IN, Money.of("EUR", 1840_17));
    }

    @Test
    public void testExplicitTargetAccountTakesPrecedence()
    {
        var context = new PairContext();
        context.targets.put("USD>EUR", eurFromUsd);

        var item = new AccountTransferItem(transfer("USD", 2200_00, "EUR", 1840_17), true);
        item.setAccountSecondary(eurFromChf);

        assertThat(item.apply(new InsertAction(client), context).getCode(), is(Status.Code.OK));

        assertBooking(eurFromChf, AccountTransaction.Type.TRANSFER_IN, Money.of("EUR", 1840_17));
        assertThat(eurFromUsd.getTransactions().isEmpty(), is(true));
    }

    @Test
    public void testMissingTargetAccountIsReportedAsError()
    {
        var context = new PairContext();
        context.targets.put("CHF>EUR", eurFromChf);

        var item = new AccountTransferItem(transfer("USD", 2200_00, "EUR", 1840_17), true);

        assertThat(item.apply(new InsertAction(client), context).getCode(), is(Status.Code.ERROR));
        assertThat(eurFromChf.getTransactions().isEmpty(), is(true));
    }

    private Account account(String name, String currencyCode)
    {
        var account = new Account(name);
        account.setCurrencyCode(currencyCode);
        client.addAccount(account);
        return account;
    }

    private static AccountTransferEntry transfer(String sourceCurrency, long sourceAmount, String targetCurrency,
                    long targetAmount)
    {
        var source = Money.of(sourceCurrency, sourceAmount);
        var target = Money.of(targetCurrency, targetAmount);

        var entry = new AccountTransferEntry();
        entry.setDate(LocalDateTime.of(2021, 3, 5, 17, 7, 5));
        entry.getSourceTransaction().setMonetaryAmount(source);
        entry.getTargetTransaction().setMonetaryAmount(target);

        var rate = BigDecimal.valueOf(sourceAmount).divide(BigDecimal.valueOf(targetAmount), 10, RoundingMode.HALF_DOWN);
        entry.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, source, target, rate));

        return entry;
    }

    private static void assertBooking(Account account, AccountTransaction.Type type, Money amount)
    {
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getType(), is(type));
        assertThat(account.getTransactions().get(0).getMonetaryAmount(), is(amount));
    }

    /**
     * Context which selects the target account by currency pair
     */
    private class PairContext implements ImportAction.Context
    {
        private final Map<String, Account> targets = new HashMap<>();

        @Override
        public Account getAccount(String currencyCode)
        {
            return switch (currencyCode)
            {
                case "USD" -> usd;
                case "CHF" -> chf;
                default -> null;
            };
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            throw new UnsupportedOperationException("the currency pair must be used");
        }

        @Override
        public Account getSecondaryAccount(String sourceCurrencyCode, String targetCurrencyCode)
        {
            return targets.get(sourceCurrencyCode + ">" + targetCurrencyCode);
        }

        @Override
        public Portfolio getPortfolio()
        {
            return null;
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return null;
        }
    }
}
