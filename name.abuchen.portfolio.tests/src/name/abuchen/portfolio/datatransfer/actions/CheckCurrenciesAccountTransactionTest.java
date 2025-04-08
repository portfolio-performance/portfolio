package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class CheckCurrenciesAccountTransactionTest
{
    private CheckCurrenciesAction action = new CheckCurrenciesAction();

    @Test
    public void testTransactionCurrencyMatchesAccount()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        AccountTransaction t = new AccountTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));

        t.setMonetaryAmount(Money.of("USD", 1_00));
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTransactionCurrencyMatchesSecurity()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        Security security = new Security("", "EUR");

        AccountTransaction t = new AccountTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));

        security.setCurrencyCode("USD");
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testCheckTransactionUnitIfCurrenciesAreDifferent()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        Security security = new Security("", "USD");

        Unit unit = new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("USD", 2_00), BigDecimal.valueOf(0.5));

        AccountTransaction t = new AccountTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        t.addUnit(unit);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));

        t.removeUnit(unit);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));

        Unit other = new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("JPY", 2_00), BigDecimal.valueOf(0.5));
        t.addUnit(other);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testCheckTransactionUnitIfCurrenciesAreEqual()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        Security security = new Security("", "EUR");

        AccountTransaction t = new AccountTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));

        Unit unit = new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("USD", 2_00), BigDecimal.valueOf(0.5));
        t.addUnit(unit);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTransactionIfSecurityIsIndex()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        Security security = new Security("", null);

        AccountTransaction t = new AccountTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
    }

}
