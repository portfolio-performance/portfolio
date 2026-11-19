package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityDeliveryModelTest
{
    private final Client client = new Client();
    private final Portfolio portfolio = new Portfolio();
    private final Security bond = new Security("Acme Bond", "EUR");

    public SecurityDeliveryModelTest()
    {
        var account = new Account();
        account.setCurrencyCode("EUR");
        client.addAccount(account);
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);
    }

    /**
     * Creates a delivery like a purchase or sale converted into a delivery:
     * all units (incl. the accrued interest) are kept.
     */
    private PortfolioTransaction delivery(PortfolioTransaction.Type type, double amount)
    {
        var t = new PortfolioTransaction();
        t.setType(type);
        t.setDateTime(LocalDateTime.parse("2024-03-01T00:00"));
        t.setSecurity(bond);
        t.setShares(Values.Share.factorize(1000));
        t.setCurrencyCode("EUR");
        t.setAmount(Values.Amount.factorize(amount));
        t.addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, Money.of("EUR", Values.Amount.factorize(10))));
        t.addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", Values.Amount.factorize(5))));
        portfolio.addTransaction(t);
        return t;
    }

    private SecurityDeliveryModel open(PortfolioTransaction t)
    {
        var model = new SecurityDeliveryModel(client, t.getType());
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        model.setSource(new TransactionPair<>(portfolio, t));
        return model;
    }

    @Test
    public void testInboundDeliveryWithAccruedInterest()
    {
        // 1,000.00 + accrued interest 10.00 + fees 5.00
        var t = delivery(PortfolioTransaction.Type.DELIVERY_INBOUND, 1015);

        var model = open(t);
        assertThat(model.getAccruedInterest(), is(Values.Amount.factorize(10)));
        assertThat(model.getGrossValue(), is(Values.Amount.factorize(1000)));
        assertThat(model.getQuote(), is(BigDecimal.valueOf(100.0)));
        assertThat(model.getTotal(), is(Values.Amount.factorize(1015)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));

        // the accrued interest can be corrected
        model.setAccruedInterest(Values.Amount.factorize(12));
        assertThat(model.getTotal(), is(Values.Amount.factorize(1017)));

        model.applyChanges();

        assertThat(t.getAmount(), is(Values.Amount.factorize(1017)));
        assertThat(t.getAccruedInterest(), is(Money.of("EUR", Values.Amount.factorize(12))));
        assertThat(t.getGrossValueAmount(), is(Values.Amount.factorize(1000)));
    }

    @Test
    public void testOutboundDeliveryWithAccruedInterest()
    {
        // 1,000.00 + accrued interest 10.00 - fees 5.00
        var t = delivery(PortfolioTransaction.Type.DELIVERY_OUTBOUND, 1005);

        var model = open(t);
        assertThat(model.getAccruedInterest(), is(Values.Amount.factorize(10)));
        assertThat(model.getGrossValue(), is(Values.Amount.factorize(1000)));
        assertThat(model.getTotal(), is(Values.Amount.factorize(1005)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));

        model.applyChanges();

        assertThat(t.getAmount(), is(Values.Amount.factorize(1005)));
        assertThat(t.getAccruedInterest(), is(Money.of("EUR", Values.Amount.factorize(10))));
    }
}
