package name.abuchen.portfolio.datatransfer.freedom;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class Freedom24JSONExtractorTest
{
    private Extractor.InputFile createInputFile(String resource) throws IOException
    {
        try (InputStream in = getClass().getResourceAsStream(resource))
        {
            File tempFile = Files.createTempFile("freedom24test", ".json").toFile();
            tempFile.deleteOnExit();
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new Extractor.InputFile(tempFile);
        }
    }

    @Test
    public void testKauf01() throws IOException
    {
        // BUY AAPL.US – commission in EUR (cross-currency) → separate FEES transaction
        // commission: EUR 5.52, rate 0.92 EUR/USD → forex USD 6.00 × 0.92 = EUR 5.52
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Kauf01.json"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));

        assertThat(results, hasItem(security(
                        hasIsin("US0378331005"),
                        hasTicker("AAPL"),
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(purchase(
                        hasDate("2026-02-03T14:22:10"),
                        hasShares(5.0),
                        hasAmount("USD", 1102.50),
                        hasGrossValue("USD", 1102.50),
                        hasTaxes("USD", 0.00),
                        hasFees("USD", 0.00),
                        hasNote("Trade/Order-ID: 700111222 / 600333444"))));

        assertThat(results, hasItem(fee(
                        hasDate("2026-02-03T14:22:10"),
                        hasAmount("EUR", 5.52))));
    }

    @Test
    public void testVerkauf01() throws IOException
    {
        // SELL MSFT.US – commission in EUR (cross-currency) → separate FEES transaction
        // commission: EUR 4.15, rate 0.90 EUR/USD → forex USD 4.61 × 0.90 = EUR 4.15
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Verkauf01.json"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));

        assertThat(results, hasItem(security(
                        hasIsin("US5949181045"),
                        hasTicker("MSFT"),
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(sale(
                        hasDate("2026-02-10T17:45:33"),
                        hasShares(2.0),
                        hasAmount("USD", 830.60),
                        hasGrossValue("USD", 830.60),
                        hasTaxes("USD", 0.00),
                        hasFees("USD", 0.00),
                        hasNote("Trade/Order-ID: 700555666 / 600777888"))));

        assertThat(results, hasItem(fee(
                        hasDate("2026-02-10T17:45:33"),
                        hasAmount("EUR", 4.15))));
    }

    @Test
    public void testEinzahlung01() throws IOException
    {
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Einzahlung01.json"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(deposit(
                        hasDate("2026-02-01T09:15:00"),
                        hasAmount("EUR", 5000.00),
                        hasNote("Transaction-ID: 1234567890 | Top up account through a bank transfer"))));
    }

    @Test
    public void testEntnahme01() throws IOException
    {
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Entnahme01.json"), errors);

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(removal(
                        hasDate("2026-02-14T11:30:00"),
                        hasAmount("EUR", 2500.75),
                        hasNote("Transaction-ID: 9876543210 | Debit. Withdrawal of funds"))));
    }

    @Test
    public void testZinsen01() throws IOException
    {
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Zinsen01.json"), errors);

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(interestCharge(
                        hasDate("2026-02-15T03:00:00"),
                        hasAmount("EUR", 0.03))));
    }

    @Test
    public void testEinlieferung01() throws IOException
    {
        // Promo share NVDA.US – ISIN must come from in_outs_securities.detailed join
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Einlieferung01.json"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));

        assertThat(results, hasItem(security(
                        hasIsin("US67066G1040"),
                        hasTicker("NVDA"),
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(inboundDelivery(
                        hasDate("2026-02-01T08:30:00"),
                        hasShares(1.0),
                        hasAmount("USD", 0.00))));
    }

    @Test
    public void testEinlieferung01_EN() throws IOException
    {
        // English export: in_outs_securities.detailed uses "Free stocks" instead of "Geschenkaktionen"
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Einlieferung01_EN.json"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(results.size(), is(2));

        assertThat(results, hasItem(security(
                        hasIsin("US67066G1040"),
                        hasTicker("NVDA"),
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(inboundDelivery(
                        hasDate("2026-02-01T08:30:00"),
                        hasShares(1.0),
                        hasAmount("USD", 0.00))));
    }

    @Test
    public void testZinsen01_EN() throws IOException
    {
        // English export: type "Interest on the use of funds EUR" → INTEREST_CHARGE
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Zinsen01_EN.json"), errors);

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(interestCharge(
                        hasDate("2026-02-15T03:00:00"),
                        hasAmount("EUR", 0.03))));
    }

    @Test
    public void testWaehrungskonversion01() throws IOException
    {
        // BUY USD/EUR: pay EUR 2731.50, receive USD 3000.00 (rate 0.9105)
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile("Waehrungskonversion01.json"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransfers(results), is(1L));
        assertThat(results.size(), is(1));

        var item = (AccountTransferItem) results.stream()
                        .filter(AccountTransferItem.class::isInstance).findFirst().orElseThrow();
        AccountTransferEntry entry = (AccountTransferEntry) item.getSubject();

        assertThat(entry.getSourceTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2731.50))));
        assertThat(entry.getTargetTransaction().getMonetaryAmount(),
                        is(Money.of("USD", Values.Amount.factorize(3000.00))));
    }

    // -----------------------------------------------------------------------
    // Multi-language helpers
    // -----------------------------------------------------------------------

    private void assertInboundDelivery(String resource) throws IOException
    {
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile(resource), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(results.size(), is(2));

        assertThat(results, hasItem(security(
                        hasIsin("US67066G1040"),
                        hasTicker("NVDA"),
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(inboundDelivery(
                        hasDate("2026-02-01T08:30:00"),
                        hasShares(1.0),
                        hasAmount("USD", 0.00))));
    }

    private void assertInterestCharge(String resource) throws IOException
    {
        var extractor = new Freedom24JSONExtractor(new Client());
        var errors = new ArrayList<Exception>();
        var results = extractor.extract(null, createInputFile(resource), errors);

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(interestCharge(
                        hasDate("2026-02-15T03:00:00"),
                        hasAmount("EUR", 0.03))));
    }

    // -----------------------------------------------------------------------
    // Inbound delivery (gift shares) – all languages
    // -----------------------------------------------------------------------

    @Test public void testEinlieferung01_AR() throws IOException { assertInboundDelivery("Einlieferung01_AR.json"); }
    @Test public void testEinlieferung01_BG() throws IOException { assertInboundDelivery("Einlieferung01_BG.json"); }
    @Test public void testEinlieferung01_CS() throws IOException { assertInboundDelivery("Einlieferung01_CS.json"); }
    @Test public void testEinlieferung01_DA() throws IOException { assertInboundDelivery("Einlieferung01_DA.json"); }
    @Test public void testEinlieferung01_EL() throws IOException { assertInboundDelivery("Einlieferung01_EL.json"); }
    @Test public void testEinlieferung01_ES() throws IOException { assertInboundDelivery("Einlieferung01_ES.json"); }
    @Test public void testEinlieferung01_ET() throws IOException { assertInboundDelivery("Einlieferung01_ET.json"); }
    @Test public void testEinlieferung01_FR() throws IOException { assertInboundDelivery("Einlieferung01_FR.json"); }
    @Test public void testEinlieferung01_HY() throws IOException { assertInboundDelivery("Einlieferung01_HY.json"); }
    @Test public void testEinlieferung01_IT() throws IOException { assertInboundDelivery("Einlieferung01_IT.json"); }
    @Test public void testEinlieferung01_KK() throws IOException { assertInboundDelivery("Einlieferung01_KK.json"); }
    @Test public void testEinlieferung01_LT() throws IOException { assertInboundDelivery("Einlieferung01_LT.json"); }
    @Test public void testEinlieferung01_NL() throws IOException { assertInboundDelivery("Einlieferung01_NL.json"); }
    @Test public void testEinlieferung01_PL() throws IOException { assertInboundDelivery("Einlieferung01_PL.json"); }
    @Test public void testEinlieferung01_PT() throws IOException { assertInboundDelivery("Einlieferung01_PT.json"); }
    @Test public void testEinlieferung01_RO() throws IOException { assertInboundDelivery("Einlieferung01_RO.json"); }
    @Test public void testEinlieferung01_RU() throws IOException { assertInboundDelivery("Einlieferung01_RU.json"); }
    @Test public void testEinlieferung01_TG() throws IOException { assertInboundDelivery("Einlieferung01_TG.json"); }
    @Test public void testEinlieferung01_UK() throws IOException { assertInboundDelivery("Einlieferung01_UK.json"); }
    @Test public void testEinlieferung01_ZH() throws IOException { assertInboundDelivery("Einlieferung01_ZH.json"); }

    // -----------------------------------------------------------------------
    // Interest charge – all languages
    // -----------------------------------------------------------------------

    @Test public void testZinsen01_AR() throws IOException { assertInterestCharge("Zinsen01_AR.json"); }
    @Test public void testZinsen01_BG() throws IOException { assertInterestCharge("Zinsen01_BG.json"); }
    @Test public void testZinsen01_CS() throws IOException { assertInterestCharge("Zinsen01_CS.json"); }
    @Test public void testZinsen01_DA() throws IOException { assertInterestCharge("Zinsen01_DA.json"); }
    @Test public void testZinsen01_EL() throws IOException { assertInterestCharge("Zinsen01_EL.json"); }
    @Test public void testZinsen01_ES() throws IOException { assertInterestCharge("Zinsen01_ES.json"); }
    @Test public void testZinsen01_ET() throws IOException { assertInterestCharge("Zinsen01_ET.json"); }
    @Test public void testZinsen01_FR() throws IOException { assertInterestCharge("Zinsen01_FR.json"); }
    @Test public void testZinsen01_HY() throws IOException { assertInterestCharge("Zinsen01_HY.json"); }
    @Test public void testZinsen01_IT() throws IOException { assertInterestCharge("Zinsen01_IT.json"); }
    @Test public void testZinsen01_KK() throws IOException { assertInterestCharge("Zinsen01_KK.json"); }
    @Test public void testZinsen01_LT() throws IOException { assertInterestCharge("Zinsen01_LT.json"); }
    @Test public void testZinsen01_NL() throws IOException { assertInterestCharge("Zinsen01_NL.json"); }
    @Test public void testZinsen01_PL() throws IOException { assertInterestCharge("Zinsen01_PL.json"); }
    @Test public void testZinsen01_PT() throws IOException { assertInterestCharge("Zinsen01_PT.json"); }
    @Test public void testZinsen01_RO() throws IOException { assertInterestCharge("Zinsen01_RO.json"); }
    @Test public void testZinsen01_RU() throws IOException { assertInterestCharge("Zinsen01_RU.json"); }
    @Test public void testZinsen01_TG() throws IOException { assertInterestCharge("Zinsen01_TG.json"); }
    @Test public void testZinsen01_UK() throws IOException { assertInterestCharge("Zinsen01_UK.json"); }
    @Test public void testZinsen01_ZH() throws IOException { assertInterestCharge("Zinsen01_ZH.json"); }
}
