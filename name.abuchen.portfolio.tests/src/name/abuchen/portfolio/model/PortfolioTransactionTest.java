package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
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

    @Test
    public void testGrossPricePerShareOfPercentageQuotedSecurity()
    {
        Security bond = new Security();
        bond.setCurrencyCode(CurrencyUnit.EUR);
        bond.setPercentageQuoted(true);

        PortfolioTransaction t = new PortfolioTransaction();
        t.setDateTime(LocalDateTime.parse("2015-01-15T00:00")); //$NON-NLS-1$
        t.setSecurity(bond);
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3016.67)));
        t.setShares(Values.Share.factorize(3000));
        t.setType(PortfolioTransaction.Type.BUY);

        // 3,016.67 EUR / 3,000 nominal = 100.5556666...%: the conversion to
        // percent must not cost precision beyond the 10 significant digits of
        // Values.MC (which apply to every quote)
        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100.5556667))));

        CurrencyConverter converter = new TestCurrencyConverter().with(CurrencyUnit.EUR);
        assertThat(t.getGrossPricePerShare(converter),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100.5556667))));
    }

    @Test
    public void testGrossPricePerShareWithoutSecurity()
    {
        PortfolioTransaction t = new PortfolioTransaction();
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        t.setShares(Values.Share.factorize(10));
        t.setType(PortfolioTransaction.Type.BUY);

        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100))));
    }

    @Test
    public void testQuotedGrossPricePerShareWithForex()
    {
        // 1,000 USD nominal at 101 % = USD 1,010 = EUR 909 (exchange rate 0.9)
        Security bond = new Security("Bond", CurrencyUnit.USD); //$NON-NLS-1$
        bond.setPercentageQuoted(true);
        PortfolioTransaction t = createForexPurchase(bond);

        // the percentage refers to the nominal value in USD
        assertThat(t.getQuotedGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(101))));
        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(90.9))));

        // regular securities keep the price in transaction currency
        Security share = new Security("Share", CurrencyUnit.USD); //$NON-NLS-1$
        PortfolioTransaction s = createForexPurchase(share);
        assertThat(s.getQuotedGrossPricePerShare(), is(s.getGrossPricePerShare()));
    }

    private static PortfolioTransaction createForexPurchase(Security security)
    {
        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(PortfolioTransaction.Type.BUY);
        t.setDateTime(LocalDateTime.parse("2015-01-15T00:00")); //$NON-NLS-1$
        t.setSecurity(security);
        t.setShares(Values.Share.factorize(1000));
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(909)));
        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(909)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1010)), BigDecimal.valueOf(0.9)));
        return t;
    }

    @Test
    public void testGrossValueOfPurchaseWithAccruedInterest()
    {
        // amount = gross value + accrued interest + fees + taxes
        PortfolioTransaction t = createWithAccruedInterest(PortfolioTransaction.Type.BUY, 5232.20);

        assertThat(t.getAccruedInterest(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(150.86))));
        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5003.00))));
        assertThat(t.getMonetaryAmountWithoutAccruedInterest(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5081.34))));
        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(100.06))));
    }

    @Test
    public void testGrossValueOfSaleWithAccruedInterest()
    {
        // amount = gross value + accrued interest - fees - taxes
        PortfolioTransaction t = createWithAccruedInterest(PortfolioTransaction.Type.SELL, 5075.52);

        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5003.00))));
        assertThat(t.getMonetaryAmountWithoutAccruedInterest(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4924.66))));

        CurrencyConverter converter = new TestCurrencyConverter().with(CurrencyUnit.USD);
        assertThat(t.getAccruedInterest(converter), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(150.86))));
        assertThat(t.getMonetaryAmountWithoutAccruedInterest(converter),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4924.66))));
    }

    private static PortfolioTransaction createWithAccruedInterest(PortfolioTransaction.Type type, double amount)
    {
        Security bond = new Security("Bond", CurrencyUnit.USD); //$NON-NLS-1$
        bond.setPercentageQuoted(true);

        // 5,000 nominal at 100.06 %, accrued interest 150.86, fees 78.34
        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(type);
        t.setDateTime(LocalDateTime.parse("2025-08-21T00:00")); //$NON-NLS-1$
        t.setSecurity(bond);
        t.setShares(Values.Share.factorize(5000));
        t.setMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(amount)));
        t.addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, Money.of(CurrencyUnit.USD, Values.Amount.factorize(150.86))));
        t.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.USD, Values.Amount.factorize(78.34))));
        return t;
    }
}
