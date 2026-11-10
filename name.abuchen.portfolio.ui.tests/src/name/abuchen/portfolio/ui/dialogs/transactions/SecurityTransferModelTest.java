package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("nls")
public class SecurityTransferModelTest
{
    @Test
    public void testPresetFromPositionWithPercentQuoting()
    {
        var client = new Client();
        Security bond = new SecurityBuilder() //
                        .addPrice(LocalDate.now().toString(), Values.Quote.factorize(101)) //
                        .addTo(client);
        bond.setPercentageQuoted(true);
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(bond, "2024-01-02", Values.Share.factorize(1000), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var model = new SecurityTransferModel(client);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        model.setSourcePortfolio(portfolio);
        model.setSecurity(bond);

        // 1,000 nominal x 101 % = 1,010.00 EUR
        assertThat(model.getShares(), is(Values.Share.factorize(1000)));
        assertThat(model.getAmount(), is(Values.Amount.factorize(1010)));
        assertThat(model.getQuote(), is(BigDecimal.valueOf(101.0)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @Test
    public void testChangedQuoteAndAmountWithPercentQuoting()
    {
        var client = new Client();
        Security bond = new SecurityBuilder().addTo(client);
        bond.setPercentageQuoted(true);

        var model = new SecurityTransferModel(client);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));
        model.setSecurity(bond);
        model.setShares(Values.Share.factorize(1000));

        // quote in percent of the nominal value
        model.setQuote(BigDecimal.valueOf(99.0));
        assertThat(model.getAmount(), is(Values.Amount.factorize(990)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));

        // amount -> quote in percent
        model.setAmount(Values.Amount.factorize(1000));
        assertThat(model.getQuote(), is(BigDecimal.valueOf(100.0)));
        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @Test
    public void testQuotationAndStatusWithPercentQuoting()
    {
        var client = new Client();
        Security bond = new SecurityBuilder().addTo(client);
        bond.setPercentageQuoted(true);
        Security share = new SecurityBuilder().addTo(client);

        var model = new SecurityTransferModel(client);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(client));

        // percentage-quoted: quote in percent, nominal instead of shares
        model.setSecurity(bond);
        model.setShares(0L);
        assertThat(model.getSecurityQuotation(), is("%"));
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnNominal))));

        // regular security: quote in currency, shares
        model.setSecurity(share);
        model.setShares(0L);
        assertThat(model.getSecurityQuotation(), is("EUR"));
        assertThat(model.getCalculationStatus(), is(ValidationStatus
                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnShares))));
    }
}
