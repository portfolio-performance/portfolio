package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

public class PortfolioTransactionTest
{
    private static PortfolioTransaction transaction;

    @BeforeClass
    public static void setupTransaction()
    {
        Security security = new Security();
        security.setCurrencyCode(CurrencyUnit.USD);

        transaction = new PortfolioTransaction();
        transaction.setDateTime(LocalDateTime.parse("2015-01-15T00:00")); //$NON-NLS-1$
        transaction.setSecurity(security);
        transaction.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        transaction.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(900)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(818.18)), BigDecimal.valueOf(1.1)));
        transaction.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));
        transaction.setShares(Values.Share.factorize(9));
        transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
    }

    @Test
    public void testGrossPricePerShareWithCurrencyConversion()
    {
        CurrencyConverter converter = new TestCurrencyConverter().with(CurrencyUnit.USD);

        // assert that exchange rate is different from the transaction exchange
        // rate as we want to test that the transaction exchange rate is used

        assertThat(converter.getRate(transaction.getDateTime(), CurrencyUnit.EUR).getValue(), is(not(transaction
                        .getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new).getExchangeRate())));

        assertThat(transaction.getGrossValue(converter),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(818.18))));

        assertThat(transaction.getGrossPricePerShare(converter),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(818.18 / 9))));

        assertThat(transaction.getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(900.0 / 9))));

        assertThat(transaction.getGrossPricePerShare(converter.with(CurrencyUnit.EUR)),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(900.0 / 9))));
    }
}
