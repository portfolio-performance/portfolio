package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CheckCurrenciesActionTest
{
    private PortfolioTransaction leg(PortfolioTransaction.Type type, Security security, String txCurrency, long amount)
    {
        var t = new PortfolioTransaction();
        t.setType(type);
        t.setDateTime(LocalDateTime.parse("2015-01-09T00:00"));
        t.setSecurity(security);
        t.setCurrencyCode(txCurrency);
        t.setAmount(amount);
        return t;
    }

    @Test
    public void testSourceLegInEventCurrencyNeedsNoForex()
    {
        // source (CHF) == security currency (CHF): no GROSS_VALUE expected
        Security parent = new Security("CHF parent", "CHF");
        var source = leg(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND, parent, "CHF", Values.Amount.factorize(2000));
        source.setShares(0);

        assertThat(new CheckCurrenciesAction().process(source, new Portfolio()).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testTargetLegWithMatchingForexIsValid()
    {
        // target (CHF tx) != security (USD): a GROSS_VALUE in USD is required
        Security spinco = new Security("USD spinco", CurrencyUnit.USD);
        var target = leg(PortfolioTransaction.Type.DISTRIBUTION_INBOUND, spinco, "CHF", Values.Amount.factorize(2000));
        target.setShares(Values.Share.factorize(100));
        // forex chosen so amount == forex * exchangeRate within Unit's own
        // rounding tolerance (2000 CHF == 1818.18 USD * 1.10)
        target.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of("CHF", Values.Amount.factorize(2000)), //
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1818.18)), //
                        new BigDecimal("1.10")));

        assertThat(new CheckCurrenciesAction().process(target, new Portfolio()).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testTargetLegMissingForexIsRejected()
    {
        // target (CHF tx) != security (USD) but no GROSS_VALUE -> ERROR
        Security spinco = new Security("USD spinco", CurrencyUnit.USD);
        var target = leg(PortfolioTransaction.Type.DISTRIBUTION_INBOUND, spinco, "CHF", Values.Amount.factorize(2000));
        target.setShares(Values.Share.factorize(100));

        assertThat(new CheckCurrenciesAction().process(target, new Portfolio()).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTargetLegWithMismatchedForexIsRejected()
    {
        // GROSS_VALUE forex currency (GBP) != security currency (USD) -> ERROR
        Security spinco = new Security("USD spinco", CurrencyUnit.USD);
        var target = leg(PortfolioTransaction.Type.DISTRIBUTION_INBOUND, spinco, "CHF", Values.Amount.factorize(2000));
        target.setShares(Values.Share.factorize(100));
        // forex chosen so amount == forex * exchangeRate within Unit's own
        // rounding tolerance (2000 CHF == 2352.94 GBP * 0.85); the currency
        // mismatch (GBP vs. required USD) is what triggers the ERROR
        target.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of("CHF", Values.Amount.factorize(2000)), //
                        Money.of("GBP", Values.Amount.factorize(2352.94)), //
                        new BigDecimal("0.85")));

        assertThat(new CheckCurrenciesAction().process(target, new Portfolio()).getCode(), is(Status.Code.ERROR));
    }
}
