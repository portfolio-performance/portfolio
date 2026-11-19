package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
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

    private static BuySellEntry purchaseWithAccruedInterest(double amount)
    {
        var rate = BigDecimal.valueOf(0.9);
        var entry = new BuySellEntry();
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setMonetaryAmount(Money.of("EUR", Values.Amount.factorize(amount)));

        // USD 1,100.00 = EUR 990.00 + accrued interest USD 10.00 = EUR 9.00 +
        // fees EUR 5.00
        var t = entry.getPortfolioTransaction();
        t.addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, Money.of("EUR", Values.Amount.factorize(990)),
                        Money.of("USD", Values.Amount.factorize(1100)), rate));
        t.addUnit(new Transaction.Unit(Transaction.Unit.Type.ACCRUED_INTEREST,
                        Money.of("EUR", Values.Amount.factorize(9)), Money.of("USD", Values.Amount.factorize(10)),
                        rate));
        t.addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of("EUR", Values.Amount.factorize(5))));
        return entry;
    }

    @Test
    public void testCheckAcceptsAccruedInterestInForex()
    {
        var entry = purchaseWithAccruedInterest(1004);

        assertThat(new CheckForexGrossValueAction().process(entry, new Account(), new Portfolio()).getCode(),
                        is(Status.Code.OK));
    }

    @Test
    public void testCheckRejectsWrongTotalWithAccruedInterestInForex()
    {
        // total without the accrued interest
        var entry = purchaseWithAccruedInterest(995);

        assertThat(new CheckForexGrossValueAction().process(entry, new Account(), new Portfolio()).getCode(),
                        is(Status.Code.ERROR));
    }
}
