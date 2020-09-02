package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class QuoteFromTransactionExtractorTest
{
    private Client client;
    private Security security;
    private BuySellEntry entry;

    @Before
    public void setup()
    {
        client = new Client();

        security = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        Money amount = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100));

        entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2015-01-15T00:00")); //$NON-NLS-1$
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(10));
        entry.setMonetaryAmount(amount);

        entry.insert();
    }

    @Test
    public void testExtractionOfQuotes()
    {
        assertThat(security.getPrices().size(), is(0));

        QuoteFromTransactionExtractor extractor = new QuoteFromTransactionExtractor(client,
                        new TestCurrencyConverter());

        assertThat(extractor.extractQuotes(security), is(true));

        assertThat(security.getPrices().size(), is(1));

        SecurityPrice price = security.getPrices().get(0);
        assertThat(price.getDate(), is(LocalDate.parse("2015-01-15"))); //$NON-NLS-1$
        assertThat(price.getValue(), is(Values.Quote.factorize(10)));
    }

    @Test
    public void testExtractionOfQuotesWithForex()
    {
        assertThat(security.getPrices().size(), is(0));

        security.setCurrencyCode(CurrencyUnit.USD);

        BigDecimal exchangeRate = BigDecimal.valueOf(1.1708).setScale(10);
        Money amount = entry.getPortfolioTransaction().getMonetaryAmount();
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, amount, Money.of(CurrencyUnit.USD,
                            exchangeRate.multiply(BigDecimal.valueOf(amount.getAmount()))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue()),
                        BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN)));

        QuoteFromTransactionExtractor extractor = new QuoteFromTransactionExtractor(client,
                        new TestCurrencyConverter());

        assertThat(extractor.extractQuotes(security), is(true));

        assertThat(security.getPrices().size(), is(1));

        SecurityPrice price = security.getPrices().get(0);
        assertThat(price.getDate(), is(LocalDate.parse("2015-01-15"))); //$NON-NLS-1$
        assertThat(price.getValue(), is(Values.Quote.factorize(10 * 1.1708)));
    }
}
