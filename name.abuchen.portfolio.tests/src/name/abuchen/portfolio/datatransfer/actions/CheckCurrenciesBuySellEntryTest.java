package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class CheckCurrenciesBuySellEntryTest
{
    private CheckCurrenciesAction action = new CheckCurrenciesAction();

    @Test
    public void testBuySellEntry()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        Security security = new Security();
        security.setCurrencyCode("USD");

        Portfolio portfolio = new Portfolio();

        BuySellEntry entry = new BuySellEntry();
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(security);
        entry.setMonetaryAmount(Money.of("EUR", 100_00));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 80_00),
                        Money.of("USD", 100_00), BigDecimal.valueOf(0.8)));

        assertThat(action.process(entry, account, portfolio).getCode(), is(Status.Code.OK));
    }
}
