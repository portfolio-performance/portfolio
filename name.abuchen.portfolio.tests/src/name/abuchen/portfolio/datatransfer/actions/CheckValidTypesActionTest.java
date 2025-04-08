package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class CheckValidTypesActionTest
{
    CheckValidTypesAction action = new CheckValidTypesAction();

    @Test
    public void testAccountTransaction()
    {
        Account account = new Account();
        account.setCurrencyCode("EUR");

        AccountTransaction t = new AccountTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));

        t.setType(AccountTransaction.Type.BUY);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
        t.setType(AccountTransaction.Type.SELL);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
        t.setType(AccountTransaction.Type.TRANSFER_IN);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));
        t.setType(AccountTransaction.Type.TRANSFER_OUT);
        assertThat(action.process(t, account).getCode(), is(Status.Code.ERROR));

        t.setType(AccountTransaction.Type.DEPOSIT);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.DIVIDENDS);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.FEES);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.FEES_REFUND);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.INTEREST);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.INTEREST_CHARGE);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.REMOVAL);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.TAX_REFUND);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
        t.setType(AccountTransaction.Type.TAXES);
        assertThat(action.process(t, account).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testPortfolioTransaction()
    {
        Portfolio portfolio = new Portfolio();

        Security security = new Security("", "EUR");

        PortfolioTransaction t = new PortfolioTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);

        t.setType(PortfolioTransaction.Type.BUY);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
        t.setType(PortfolioTransaction.Type.SELL);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
        t.setType(PortfolioTransaction.Type.TRANSFER_IN);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
        t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));

        t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));
        t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));
    }
}
