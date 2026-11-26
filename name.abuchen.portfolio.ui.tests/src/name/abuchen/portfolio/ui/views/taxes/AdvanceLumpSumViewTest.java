package name.abuchen.portfolio.ui.views.taxes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.text.MessageFormat;
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
import name.abuchen.portfolio.snapshot.advancelumpsum.AdvanceLumpSumCalculation.Result;
import name.abuchen.portfolio.snapshot.advancelumpsum.BaseInterestRate;
import name.abuchen.portfolio.snapshot.advancelumpsum.BaseInterestRate.Source;
import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("nls")
public class AdvanceLumpSumViewTest
{
    @Test
    public void testSummaryHighlightsBaseRateAndSeparatesFiguresFromNotes()
    {
        var rate = new BaseInterestRate(2025, new BigDecimal("2.53"), Source.OFFICIAL);
        var result = new Result(CurrencyUnit.EUR, List.of());
        var zero = "<strong>" + Values.Money.format(Money.of(CurrencyUnit.EUR, 0)) + "</strong>";
        var expected = Messages.LabelBaseInterestRate + " 2025: <strong>2,53 %</strong> ("
                        + Messages.LabelBaseInterestRateSourceOfficial + ") | " + Messages.LabelAdvanceLumpSum + ": "
                        + zero + " | " + Messages.LabelTaxableBeforeDeductions + ": " + zero + "\n\n<em>"
                        + Messages.MsgAdvanceLumpSumInfo + "</em>";

        assertThat(AdvanceLumpSumView.buildSummary(2025, rate, result), is(expected));
    }

    @Test
    public void testSummaryShowsTaxableAmountBeforeListingUsedDeductions()
    {
        var zero = Money.of(CurrencyUnit.EUR, 0);
        var taxable = Money.of(CurrencyUnit.EUR, 10000);
        var loss = Money.of(CurrencyUnit.EUR, 3000);
        var exemption = Money.of(CurrencyUnit.EUR, 2000);
        var separator = " | ";
        var taxableLabel = separator + Messages.LabelTaxableBeforeDeductions + ": <strong>"
                        + Values.Money.format(taxable) + "</strong>";
        var lossLabel = MessageFormat.format(Messages.MsgAdvanceLumpSumLossUsed,
                        "<strong>" + Values.Money.format(loss) + "</strong>");
        var exemptionLabel = MessageFormat.format(Messages.MsgAdvanceLumpSumExemptionOrderUsed,
                        "<strong>" + Values.Money.format(exemption) + "</strong>");

        assertThat(AdvanceLumpSumView.formatTaxableSummary(taxable, zero, zero), is(taxableLabel));
        assertThat(AdvanceLumpSumView.formatTaxableSummary(Money.of(CurrencyUnit.EUR, 7000), loss, zero),
                        is(taxableLabel + separator + lossLabel));
        assertThat(AdvanceLumpSumView.formatTaxableSummary(Money.of(CurrencyUnit.EUR, 8000), zero, exemption),
                        is(taxableLabel + separator + exemptionLabel));
        assertThat(AdvanceLumpSumView.formatTaxableSummary(Money.of(CurrencyUnit.EUR, 5000), loss, exemption),
                        is(taxableLabel + separator + lossLabel + separator + exemptionLabel));
    }

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
    public void testParseAmount()
    {
        assertThat(AdvanceLumpSumView.parseAmount("1.000,00"), is(Values.Amount.factorize(1000)));
        assertThat(AdvanceLumpSumView.parseAmount(" 2.500,50 "), is(Values.Amount.factorize(2500.5)));
        assertThat(AdvanceLumpSumView.parseAmount("0"), is(0L));
    }

    @Test
    public void testParseAmountRejectsInvalidInput()
    {
        assertThat(AdvanceLumpSumView.parseAmount(""), is(nullValue()));
        assertThat(AdvanceLumpSumView.parseAmount("  "), is(nullValue()));
        assertThat(AdvanceLumpSumView.parseAmount("abc"), is(nullValue()));
        assertThat(AdvanceLumpSumView.parseAmount("100x"), is(nullValue()));
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
