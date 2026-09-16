package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IRRCalculationAfterTaxTest
{
    @Test
    public void testDividendPaymentsWithTaxes()
    {
        // same scenario as IRRCalculationTest#testDividendPaymentsWithTaxes,
        // but this time the tax withheld on the dividend and the tax paid on
        // the sale must NOT be added back

        List<CalculationLineItem> tx = new ArrayList<>();

        Portfolio portfolio = new Portfolio();
        Security security = new Security();

        tx.add(CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(LocalDateTime.of(2015, Month.DECEMBER, 31, 0, 0), //
                                        CurrencyUnit.EUR, Values.Amount.factorize(1000), //
                                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, //
                                        Values.Amount.factorize(10), 0)));

        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setDateTime(LocalDateTime.parse("2016-06-01T00:00"));
        t.setSecurity(security);
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));
        t.setShares(Values.Share.factorize(10));
        t.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50))));
        tx.add(CalculationLineItem.of(new Account(), t));

        tx.add(CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(LocalDateTime.of(2016, Month.DECEMBER, 31, 0, 0), //
                                        CurrencyUnit.EUR, Values.Amount.factorize(1200), //
                                        security, Values.Share.factorize(10), PortfolioTransaction.Type.SELL, //
                                        Values.Amount.factorize(10), Values.Amount.factorize(30))));

        IRRCalculationAfterTax calculation = Calculation.perform(IRRCalculationAfterTax.class,
                        new TestCurrencyConverter(), security, tx);

        // Excel verification
        // 31.12.15 -1000 (unchanged: this buy has no taxes)
        // 01.06.16 100 (net dividend, tax NOT added back)
        // 31.12.16 1200 (net sale proceeds, tax NOT added back)
        // =XINTZINSFUSS(B1:B3;A1:A3) = 0,316409186

        assertThat(calculation.getIRR(), IsCloseTo.closeTo(0.316409186d, 0.000001d));
    }

    @Test
    public void testStandaloneTaxTransactionIsIncludedAsCashFlow()
    {
        // a standalone TAXES transaction linked to the security (e.g. a tax
        // adjustment not tied to a specific buy/sell/dividend) must be
        // counted as a real cash outflow in the after-tax IRR, whereas the
        // gross IRRCalculation always ignores it

        List<CalculationLineItem> tx = new ArrayList<>();

        Portfolio portfolio = new Portfolio();
        Security security = new Security();

        tx.add(CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0), //
                                        CurrencyUnit.EUR, Values.Amount.factorize(1000), //
                                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0)));

        AccountTransaction standaloneTax = new AccountTransaction();
        standaloneTax.setType(AccountTransaction.Type.TAXES);
        standaloneTax.setDateTime(LocalDateTime.of(2020, Month.JULY, 1, 0, 0));
        standaloneTax.setSecurity(security);
        standaloneTax.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20)));
        tx.add(CalculationLineItem.of(new Account(), standaloneTax));

        tx.add(CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(LocalDateTime.of(2021, Month.JANUARY, 1, 0, 0), //
                                        CurrencyUnit.EUR, Values.Amount.factorize(1100), //
                                        security, Values.Share.factorize(10), PortfolioTransaction.Type.SELL, 0, 0)));

        IRRCalculationAfterTax afterTax = Calculation.perform(IRRCalculationAfterTax.class,
                        new TestCurrencyConverter(), security, tx);
        IRRCalculation gross = Calculation.perform(IRRCalculation.class, new TestCurrencyConverter(), security, tx);

        // the gross IRR ignores the standalone tax charge entirely
        assertThat(gross.getIRR(), IsCloseTo.closeTo(0.1d, 0.001d));

        // the after-tax IRR must be lower because it counts the 20 EUR tax
        // charge as a real cash outflow
        assertTrue(afterTax.getIRR() < gross.getIRR());
    }
    @Test
    public void testStandaloneTaxRefundTransactionIsIncludedAsCashFlow()
    {
        // symmetric to testStandaloneTaxTransactionIsIncludedAsCashFlow: a standalone
        // TAX_REFUND transaction linked to the security must be counted as a real cash
        // inflow in the after-tax IRR, while the gross IRR calculation ignores it

        List<CalculationLineItem> tx = new ArrayList<>();

        Portfolio portfolio = new Portfolio();
        Security security = new Security();

        tx.add(CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0), //
                                        CurrencyUnit.EUR, Values.Amount.factorize(1000), //
                                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0)));

        AccountTransaction standaloneTaxRefund = new AccountTransaction();
        standaloneTaxRefund.setType(AccountTransaction.Type.TAX_REFUND);
        standaloneTaxRefund.setDateTime(LocalDateTime.of(2020, Month.JULY, 1, 0, 0));
        standaloneTaxRefund.setSecurity(security);
        standaloneTaxRefund.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20)));
        tx.add(CalculationLineItem.of(new Account(), standaloneTaxRefund));

        tx.add(CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(LocalDateTime.of(2021, Month.JANUARY, 1, 0, 0), //
                                        CurrencyUnit.EUR, Values.Amount.factorize(1100), //
                                        security, Values.Share.factorize(10), PortfolioTransaction.Type.SELL, 0, 0)));

        IRRCalculationAfterTax afterTax = Calculation.perform(IRRCalculationAfterTax.class,
                        new TestCurrencyConverter(), security, tx);
        IRRCalculation gross = Calculation.perform(IRRCalculation.class, new TestCurrencyConverter(), security, tx);

        // the gross IRR completely ignores the standalone tax refund
        assertThat(gross.getIRR(), IsCloseTo.closeTo(0.1d, 0.001d));

        // the after-tax IRR must be higher because it includes the 20 EUR refund
        // as a real cash inflow
        assertTrue(afterTax.getIRR() > gross.getIRR());
    }
}
