package name.abuchen.portfolio.ui.views.taxes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AdvanceLumpSumViewTest
{
    private static Locale defaultLocale;

    @BeforeClass
    public static void setupLocale()
    {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @AfterClass
    public static void resetLocale()
    {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testBalanceDateIsCappedAtToday()
    {
        var today = LocalDate.parse("2026-11-19");

        assertThat(AdvanceLumpSumView.balanceDate(2025, today), is(LocalDate.parse("2026-01-01")));
        assertThat(AdvanceLumpSumView.balanceDate(2026, today), is(today));
    }

    @Test
    public void testReferenceBalanceRequiresExchangeRate()
    {
        var client = new Client();
        var date = LocalDate.parse("2015-01-16");
        var converter = new TestCurrencyConverter()
        {
            @Override
            public Optional<ExchangeRate> findRate(LocalDate rateDate, String currencyCode)
            {
                return "ARS".equals(currencyCode) ? Optional.empty() : super.findRate(rateDate, currencyCode);
            }
        };
        var euro = new AccountBuilder().deposit_("2015-01-02", Values.Amount.factorize(100)).addTo(client);
        var dollar = new AccountBuilder(CurrencyUnit.USD).deposit_("2015-01-02", Values.Amount.factorize(100))
                        .addTo(client);
        var peso = new AccountBuilder("ARS").deposit_("2015-01-02", Values.Amount.factorize(100)).addTo(client);

        assertThat(AdvanceLumpSumView.referenceBalance(euro, converter, date),
                        is(Optional.of(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)))));
        assertThat(AdvanceLumpSumView.referenceBalance(dollar, converter, date), is(Optional.of(
                        converter.convert(date, Money.of(CurrencyUnit.USD, Values.Amount.factorize(100))))));
        assertThat(AdvanceLumpSumView.referenceBalance(peso, converter, date), is(Optional.empty()));
    }

    @Test
    public void testReportIsRelevantForGermanLocales()
    {
        assertThat(AdvanceLumpSumView.isRelevantFor(Locale.GERMANY), is(true));
        // language preference preselects the plain language entry
        assertThat(AdvanceLumpSumView.isRelevantFor(Locale.GERMAN), is(true));
        // the tax exists only in Germany
        assertThat(AdvanceLumpSumView.isRelevantFor(Locale.forLanguageTag("de-AT")), is(false));
        assertThat(AdvanceLumpSumView.isRelevantFor(Locale.US), is(false));
        assertThat(AdvanceLumpSumView.isRelevantFor(null), is(false));
    }

    @Test
    public void testCsvExportReadsCurrentReportRoots()
    {
        var provider = new AdvanceLumpSumView.ContentProvider();
        var roots = List.of("Depot A", "Depot B");
        provider.inputChanged(null, null, roots);

        assertThat(provider.getElements(roots), arrayContaining("Depot A", "Depot B"));
        assertThat(provider.getElements(null), arrayContaining("Depot A", "Depot B"));

        provider.inputChanged(null, roots, List.of());
        assertThat(provider.getElements(null), emptyArray());
    }
}
