package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ClientSnapshotIteratorTest
{
    /**
     * Counts the exchange rate lookups per currency and date.
     */
    private static class CountingConverter implements CurrencyConverter
    {
        private final CurrencyConverter delegate = new TestCurrencyConverter();
        private final Map<String, Integer> lookups = new HashMap<>();

        @Override
        public String getTermCurrency()
        {
            return delegate.getTermCurrency();
        }

        @Override
        public ExchangeRate getRate(LocalDate date, String currencyCode)
        {
            lookups.merge(currencyCode + "@" + date, 1, Integer::sum);
            return delegate.getRate(date, currencyCode);
        }

        @Override
        public CurrencyConverter with(String currencyCode)
        {
            return delegate.with(currencyCode);
        }

        int lookups(String currencyCode, LocalDate date)
        {
            return lookups.getOrDefault(currencyCode + "@" + date, 0);
        }
    }

    @Test
    public void testSameDayRateConverterReturnsRatesOfDelegate()
    {
        var delegate = new TestCurrencyConverter();
        var converter = new ClientSnapshotIterator.SameDayRateConverter(delegate);

        // interleaved currencies, repeated and decreasing dates (incl.
        // weekends and dates outside of the available rates)
        var dates = List.of("2014-12-30", "2014-12-31", "2015-01-03", "2015-01-03", "2015-01-15",
                        "2015-01-05", "2015-01-16", "2015-01-20");
        var currencies = List.of(CurrencyUnit.USD, "CHF", CurrencyUnit.EUR, CurrencyUnit.USD);

        for (var d : dates)
        {
            var date = LocalDate.parse(d);

            for (var currency : currencies)
            {
                assertThat(converter.getRate(date, currency).getValue(),
                                is(delegate.getRate(date, currency).getValue()));

                var amount = Money.of(currency, Values.Amount.factorize(1234.56));
                assertThat(converter.convert(date, amount), is(delegate.convert(date, amount)));
            }
        }
    }

    @Test
    public void testSameDayRateConverterLooksUpRateOncePerCurrencyAndDay()
    {
        var counting = new CountingConverter();
        var converter = new ClientSnapshotIterator.SameDayRateConverter(counting);

        var day1 = LocalDate.parse("2015-01-05");
        var day2 = LocalDate.parse("2015-01-06");

        converter.getRate(day1, CurrencyUnit.USD);
        converter.getRate(day1, "CHF");
        converter.getRate(day1, CurrencyUnit.USD);
        converter.getRate(day2, CurrencyUnit.USD);
        converter.getRate(day2, CurrencyUnit.USD);

        assertThat(counting.lookups(CurrencyUnit.USD, day1), is(1));
        assertThat(counting.lookups("CHF", day1), is(1));
        assertThat(counting.lookups(CurrencyUnit.USD, day2), is(1));
    }

    @Test
    public void testValuationLooksUpRateOncePerCurrencyAndDay()
    {
        var client = new Client();

        var usd1 = new SecurityBuilder(CurrencyUnit.USD).addPrice("2015-01-02", Values.Quote.factorize(100))
                        .addTo(client);
        var usd2 = new SecurityBuilder(CurrencyUnit.USD).addPrice("2015-01-02", Values.Quote.factorize(50))
                        .addTo(client);
        var chf = new SecurityBuilder("CHF").addPrice("2015-01-02", Values.Quote.factorize(20))
                        .addTo(client);

        var usdAccount = new AccountBuilder(CurrencyUnit.USD)
                        .deposit_("2015-01-02", Values.Amount.factorize(1000)).addTo(client);

        new PortfolioBuilder(usdAccount) //
                        .inbound_delivery(usd1, "2015-01-02", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .inbound_delivery(usd2, "2015-01-02", Values.Share.factorize(20),
                                        Values.Amount.factorize(1000)) //
                        .inbound_delivery(chf, "2015-01-02", Values.Share.factorize(30),
                                        Values.Amount.factorize(600)) //
                        .addTo(client);

        var counting = new CountingConverter();
        var iterator = new ClientSnapshotIterator(client, counting);

        for (LocalDate date = LocalDate.parse("2015-01-02"); !date.isAfter(LocalDate.parse("2015-01-16")); date = date
                        .plusDays(1))
        {
            long valuation = iterator.nextValuation(date);

            // account + two securities in USD, one security in CHF
            assertThat(counting.lookups(CurrencyUnit.USD, date), is(1));
            assertThat(counting.lookups("CHF", date), is(1));

            assertThat(valuation > 0, is(true));
        }
    }

    @Test
    public void testPriceCursorReturnsSamePriceAsSecurity()
    {
        var random = new Random(7);
        var base = LocalDate.parse("2020-01-01");

        for (int run = 0; run < 2_000; run++)
        {
            var security = new Security("S", CurrencyUnit.EUR);

            int count = random.nextInt(12);
            for (int ii = 0; ii < count; ii++)
                security.addPrice(new SecurityPrice(base.plusDays(random.nextInt(60)), random.nextInt(100) + 1L));

            if (random.nextInt(3) > 0)
                security.setLatest(new LatestSecurityPrice(base.plusDays(random.nextInt(70)), 999));

            var cursor = new ClientSnapshotIterator.PriceCursor(security);

            var date = base.minusDays(5);
            for (int step = 0; step < 90; step++)
            {
                // modify prices while iterating (like a running price update)
                int modification = random.nextInt(20);
                if (modification == 0)
                {
                    security.addPrice(new SecurityPrice(base.plusDays(random.nextInt(70)), random.nextInt(100) + 1L));
                }
                else if (modification == 1 && !security.getPrices().isEmpty())
                {
                    List<SecurityPrice> prices = security.getPrices();
                    security.removePrice(prices.get(random.nextInt(prices.size())));
                }
                else if (modification == 2)
                {
                    security.setLatest(random.nextBoolean() ? null
                                    : new LatestSecurityPrice(base.plusDays(random.nextInt(70)), 998));
                }

                // mostly increasing dates, sometimes jumping back
                date = random.nextInt(25) == 0 ? date.minusDays(random.nextInt(10)) : date.plusDays(1);

                var expected = security.getSecurityPrice(date);
                var actual = cursor.getSecurityPrice(date);

                if (security.getPrices().isEmpty() && security.getLatest() == null)
                {
                    // Security#getSecurityPrice creates a new zero price
                    assertThat(actual.getDate(), is(expected.getDate()));
                    assertThat(actual.getValue(), is(expected.getValue()));
                }
                else
                {
                    assertThat(actual, is(sameInstance(expected)));
                }
            }
        }
    }
}
