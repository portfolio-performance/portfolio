package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class CheckCurrenciesPortfolioTransactionTest
{
    private CheckCurrenciesAction action = new CheckCurrenciesAction();

    @Test
    public void testTransactionCurrencyMatchesSecurity()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "EUR");

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_INBOUND);
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));

        security.setCurrencyCode("USD");
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testNoForexGrossValueExistsIfCurrenciesMatch()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "EUR");

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_INBOUND);
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("USD", 2_00),
                        BigDecimal.valueOf(0.5)));
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testNoForexGrossValueExistsIfCurrenciesMatchEvenIfNoForex()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "EUR");

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_INBOUND);
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("EUR", 2_00),
                        BigDecimal.valueOf(0.5)));
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTransactionHasGrossValueMatchingSecurityCurrency()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "USD");

        Unit unit = new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("USD", 2_00),
                        BigDecimal.valueOf(0.5));

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_INBOUND);
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setSecurity(security);
        t.addUnit(unit);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));

        t.removeUnit(unit);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));

        Unit other = new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1_00), Money.of("JPY", 2_00),
                        BigDecimal.valueOf(0.5));
        t.addUnit(other);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTransactionTaxesAndFeesAddUpForOutboundDeliveries()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "EUR");

        Unit tax = new Unit(Unit.Type.TAX, Money.of("EUR", 5_00));
        Unit fee = new Unit(Unit.Type.FEE, Money.of("EUR", 5_00));

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_OUTBOUND);
        t.setMonetaryAmount(Money.of("EUR", 20_00));
        t.setSecurity(security);

        t.addUnit(fee);
        t.addUnit(tax);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));

        t.setType(Type.DELIVERY_INBOUND);
        t.setMonetaryAmount(Money.of("EUR", 7_00));
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));

        t.setType(Type.DELIVERY_OUTBOUND);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));

        t.setType(Type.DELIVERY_INBOUND);
        t.removeUnit(tax);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));

        t.setMonetaryAmount(Money.of("EUR", 3_00));
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));

        t.setType(Type.DELIVERY_OUTBOUND);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testTransactionForexTaxesAndFees()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "USD");

        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 10_00), Money.of("USD", 20_00),
                        BigDecimal.valueOf(0.5));
        Unit tax = new Unit(Unit.Type.TAX, Money.of("EUR", 5_00), Money.of("USD", 10_00), BigDecimal.valueOf(0.5));
        Unit tax2 = new Unit(Unit.Type.TAX, Money.of("EUR", 1_00));
        Unit fee = new Unit(Unit.Type.FEE, Money.of("EUR", 5_00), Money.of("USD", 10_00), BigDecimal.valueOf(0.5));

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_OUTBOUND);
        t.setMonetaryAmount(Money.of("EUR", 20_00));
        t.setSecurity(security);

        t.addUnit(grossValue);
        t.addUnit(fee);
        t.addUnit(tax);
        t.addUnit(tax2);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.OK));

        t.removeUnit(fee);
        t.addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", 5_00), Money.of("JPY", 10_00), BigDecimal.valueOf(0.5)));
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTransactionForexTaxesAndFeesIfCurrenciesMatch()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", "EUR");

        Unit tax = new Unit(Unit.Type.TAX, Money.of("EUR", 5_00), Money.of("USD", 10_00), BigDecimal.valueOf(0.5));

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(Type.DELIVERY_OUTBOUND);
        t.setMonetaryAmount(Money.of("EUR", 20_00));
        t.setSecurity(security);

        t.addUnit(tax);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testTransactionIfSecurityIsIndex()
    {
        Portfolio portfolio = new Portfolio();
        Security security = new Security("", null);

        PortfolioTransaction t = new PortfolioTransaction();
        t.setMonetaryAmount(Money.of("EUR", 1_00));
        t.setType(Type.DELIVERY_OUTBOUND);
        t.setSecurity(security);
        assertThat(action.process(t, portfolio).getCode(), is(Status.Code.ERROR));
    }
}
