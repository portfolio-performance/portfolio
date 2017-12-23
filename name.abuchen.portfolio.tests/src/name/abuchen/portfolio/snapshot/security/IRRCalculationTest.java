package name.abuchen.portfolio.snapshot.security;

import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IRRCalculationTest
{

    @Test
    public void testDividendPaymentsWithTaxes()
    {
        List<Transaction> tx = new ArrayList<>();

        Security security = new Security();

        tx.add(new PortfolioTransaction(LocalDateTime.of(2015, Month.DECEMBER, 31, 0, 0), //
                        CurrencyUnit.EUR, Values.Amount.factorize(1000), //
                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, //
                        Values.Amount.factorize(10), 0));

        DividendTransaction t = new DividendTransaction();
        t.setDateTime(LocalDateTime.parse("2016-06-01T00:00"));
        t.setSecurity(security);
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));
        t.setShares(Values.Share.factorize(10));
        t.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50))));
        tx.add(t);

        tx.add(new PortfolioTransaction(LocalDateTime.of(2016, Month.DECEMBER, 31, 0, 0), //
                        CurrencyUnit.EUR, Values.Amount.factorize(1200), //
                        security, Values.Share.factorize(10), PortfolioTransaction.Type.SELL, //
                        Values.Amount.factorize(10), Values.Amount.factorize(30)));

        IRRCalculation calculation = Calculation.perform(IRRCalculation.class, new TestCurrencyConverter(), tx);

        // Excel verification
        // 31.12.15 -1000
        // 01.06.16 150
        // 31.12.16 1230
        // =XINTZINSFUSS(B1:B3;A1:A3) = 0,412128788

        assertThat(calculation.getIRR(), IsCloseTo.closeTo(0.412128788d, 0.00000001d));
    }
}
