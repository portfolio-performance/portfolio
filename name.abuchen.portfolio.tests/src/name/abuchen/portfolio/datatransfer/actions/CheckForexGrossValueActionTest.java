package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CheckForexGrossValueActionTest
{
    @Test
    public void testCheckLenientlyAcceptsRoundingErrors()
    {
        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setMonetaryAmount(Money.of("CAD", Values.Amount.factorize(95.26)));
        transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("CAD", Values.Amount.factorize(95.25)),
                        Money.of("USD", Values.Amount.factorize(69.05)), BigDecimal.valueOf(1.3795)));

        assertThat(new CheckForexGrossValueAction().process(transaction, new Account()).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testCheckRejectsValuesOutsideRangeOfRoundingErrors()
    {
        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setMonetaryAmount(Money.of("CAD", Values.Amount.factorize(95.00)));
        transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("CAD", Values.Amount.factorize(95.25)),
                        Money.of("USD", Values.Amount.factorize(69.05)), BigDecimal.valueOf(1.3795)));

        assertThat(new CheckForexGrossValueAction().process(transaction, new Account()).getCode(),
                        is(Status.Code.ERROR));
    }
}
