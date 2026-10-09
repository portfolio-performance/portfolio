package name.abuchen.portfolio.datatransfer.pdf.degiro;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;

public class DegiroPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-08-02"), hasAmount("EUR", 350.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Einzahlung"))));
    }

    @Test
    public void testKontoauszug02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C27U079"), hasWkn(null), hasTicker(null), //
                        hasName("ODX1 C11500.00 01MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C25KFF5"), hasWkn(null), hasTicker(null), //
                        hasName("ODX2 P11000.00 08FEB19"), //
                        hasCurrencyCode("EUR"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-07T11:53"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-01T11:44"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug02.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-03-01T13:21"), //
                        hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Gebühr für Ausübung/Zuteilung"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-02-08T13:27"), //
                        hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Gebühr für Ausübung/Zuteilung"), //
                        hasAmount("EUR", 2.00), hasGrossValue("EUR", 2.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-03-05T15:37"), hasAmount("EUR", 18.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Gutschrift für die Neukundenaktion"))));
    }

    @Test
    public void testKontoauszug03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("IS.DJ U.S.SELEC.DIV.U.ETF"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0002635299"), hasWkn(null), hasTicker(null), //
                        hasName("ISH.S.EU.SEL.DIV.30 U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("IS.S.GL.SE.D.100 U.ETF A"), //
                        hasCurrencyCode("EUR"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-08-01T11:32"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-07-07T11:28"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-07-05T11:36"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-23T12:24"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-05-24T12:16"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Einzahlung"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-07-17"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.75), hasGrossValue("EUR", 0.75), //
                        hasForexGrossValue("USD", 0.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8674531575, 0.000001))))));

        // @formatter:off
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-07-17"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.07 - 0.55), hasGrossValue("EUR", 2.07), //
                        hasTaxes("EUR", 0.55), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-07-17"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.64), hasGrossValue("EUR", 22.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // @formatter:off
        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividende EUR 0,09 EUR 497,25
        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividendensteuer EUR -0,02 EUR 497,16
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-07-17"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.02), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-07-17"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.74), hasGrossValue("EUR", 1.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2017-07-31"), hasAmount("EUR", 0.07), //
                        hasSource("Kontoauszug03.txt"), hasNote("Zinsen"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2017-06-30"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug03.txt"), hasNote("Einrichtung von Handelsmodalitäten 2017"))));
    }

    @Test
    public void testKontoauszug04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2017-08-31"), hasAmount("EUR", 0.89), //
                        hasSource("Kontoauszug04.txt"), hasNote("Einrichtung von Handelsmodalitäten 2017"))));
    }

    @Test
    public void testKontoauszug05()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(33L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(38));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-06-06T15:34"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-22T18:40"), hasAmount("EUR", 27.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-22T18:40"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-07-03T11:47"), hasAmount("EUR", 0.54), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-07-03T11:47"), hasAmount("EUR", 0.54), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-14T07:55"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.36), //
                        hasForexGrossValue("USD", 0.40), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8913450397, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-06T09:00"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.23), hasGrossValue("EUR", 0.33), //
                        hasForexGrossValue("USD", 0.37), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.04), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8859750155, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-16T08:21"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.58), hasGrossValue("EUR", 0.69), //
                        hasForexGrossValue("USD", 0.77), //
                        hasTaxes("EUR", 0.11), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8939746111, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-05T12:04"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-11T03:21"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-04T00:58"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-01-05T02:29"), hasAmount("EUR", 55.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-01-04T20:14"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-01-04T16:55"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Einzahlung"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-21T09:13"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-09T07:39"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.33), //
                        hasForexGrossValue("USD", 0.37), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-03-22T07:14"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.35), //
                        hasForexGrossValue("USD", 0.40), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-14T08:38"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.45), hasGrossValue("EUR", 0.65), //
                        hasForexGrossValue("USD", 0.73), //
                        hasTaxes("EUR", 0.20), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-11T03:21"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-04T00:58"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-01-05T02:29"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-01-04T20:14"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-06-04T16:11"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-06-04T16:11"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-05-03T13:46"), hasAmount("EUR", 0.13), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-05-03T13:46"), hasAmount("EUR", 0.13), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-04-01T18:24"), hasAmount("EUR", 0.04), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-04-01T18:24"), hasAmount("EUR", 0.04), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-03-04T09:09"), hasAmount("EUR", 1.17), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-03-04T09:09"), hasAmount("EUR", 0.64), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-02-01T16:25"), hasAmount("EUR", 0.53), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:35"), hasAmount("EUR", 0.53), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:34"), hasAmount("EUR", 0.53), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn(null), hasTicker(null), //
                        hasName("WIRECARD AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5007541064"), hasWkn(null), hasTicker(null), //
                        hasName("THE KRAFT HEINZ COMPAN"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8356993076"), hasWkn(null), hasTicker(null), //
                        hasName("SONY CORPORATION COMMO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00507V1098"), hasWkn(null), hasTicker(null), //
                        hasName("ACTIVISION BLIZZARD I"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug06()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-25T13:06"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2019-08-05T00:09"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Auszahlung"))));

        // check transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-26T13:52"), //
                        hasShares(0), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Gebühr für Ausübung/Zuteilung"), //
                        hasAmount("EUR", 2.00), hasGrossValue("EUR", 2.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3727R2"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 P12400.00 26JUL19"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testKontoauszug07()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(47L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(13L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(50));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-08-02T16:21"), hasAmount("EUR", 6695.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-01T16:17"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-06-03T16:29"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-05-31T14:10"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-04-01T18:15"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-04T18:05"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-01-23T13:49"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-31T12:24"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-11-30T17:12"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-09-28T20:58"), hasAmount("EUR", 1001.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-08-01T11:53"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-07-16T17:55"), hasAmount("EUR", 11000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-07-06T15:23"), hasAmount("EUR", 2502.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-06-29T15:25"), hasAmount("EUR", 1600.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-06-06T12:03"), hasAmount("EUR", 1870.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-06-01T17:46"), hasAmount("EUR", 800.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-05-01T13:06"), hasAmount("EUR", 802.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-23T15:15"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-03-15T18:31"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-02-21T16:01"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Einzahlung"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-08-05T14:12"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 1.64), hasGrossValue("USD", 1.64), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-07-04T10:22"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.12), hasGrossValue("EUR", 10.12), //
                        hasForexGrossValue("USD", 11.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-06-05T10:24"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 15.15), hasGrossValue("USD", 15.15), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-05-08T17:52"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 13.44), hasGrossValue("USD", 13.44), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-04-02T14:03"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 11.36), hasGrossValue("USD", 11.36), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-03-29T04:13"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 2.55), hasGrossValue("USD", 3.00), //
                                        hasTaxes("USD", 0.45), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-03-05T09:10"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 0.60), hasGrossValue("USD", 0.60), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2019-01-02T16:16"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 0.03), hasGrossValue("USD", 0.03), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2018-12-05T09:47"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 0.31), hasGrossValue("USD", 0.31), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2018-11-09T06:28"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 7.75), hasGrossValue("USD", 9.12), //
                                        hasTaxes("USD", 1.37), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2018-11-05T17:31"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 2.15), hasGrossValue("USD", 2.15), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2018-10-05T17:10"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 0.29), hasGrossValue("USD", 0.29), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2018-09-21T11:00"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 0.11), hasGrossValue("USD", 0.11), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2018-08-09T06:20"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug07.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 2.45), hasGrossValue("USD", 2.88), //
                                        hasTaxes("USD", 0.43), hasFees("USD", 0.00)))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2018-12-05T16:07"), hasAmount("EUR", 0.04), //
                        hasSource("Kontoauszug07.txt"), hasNote("Zinsen für Leerverkauf"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-11-30T17:12"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-09-28T20:58"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-07-06T15:23"), hasAmount("EUR", 2.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-06-29T15:25"), hasAmount("EUR", 2.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-05-01T13:06"), hasAmount("EUR", 2.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-03-15T18:31"), hasAmount("EUR", 2.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-03-04T09:09"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-02-01T16:31"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:35"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:34"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-07-04T17:31"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2018 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-05-04T16:17"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2018 (NASDAQ - NDQ)"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0904783114"), hasWkn(null), hasTicker(null), //
                        hasName("MORGAN STANLEY USD LIQUIDITY FUND"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US45778Q1076"), hasWkn(null), hasTicker(null), //
                        hasName("INSPERITY INC. COMMON"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US58470H1014"), hasWkn(null), hasTicker(null), //
                        hasName("MEDIFAST INC COMMON ST"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug08_minimal_example_two_currencies()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Kontoauszug08_extract_two_currencies.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // @formatter:off
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-03T06:09"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug08_extract_two_currencies.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (1.52 - 0.23) * 0.8992805755), hasGrossValue("EUR", 1.37), //
                        hasForexGrossValue("USD", 1.52), //
                        hasTaxes("EUR", 0.21), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8992805755, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-02T07:38"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug08_extract_two_currencies.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.08), hasGrossValue("EUR", 4.08), //
                        hasForexGrossValue("GBP", 3.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1.0938525487, 0.000001))))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3448491049"), hasWkn(null), hasTicker(null), //
                        hasName("FOOT LOCKER INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00BH4HKS39"), hasWkn(null), hasTicker(null), //
                        hasName("VODAFONE GROUP PLC"), //
                        hasCurrencyCode("GBP"))));
    }

    @Test
    public void testKontoauszug08()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(53L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(138L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(5L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(191));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US91913Y1001"), hasWkn(null), hasTicker(null), //
                        hasName("VALERO ENERGY CORPORAT"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4103451021"), hasWkn(null), hasTicker(null), //
                        hasName("HANESBRANDS INC. COMMO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8326964058"), hasWkn(null), hasTicker(null), //
                        hasName("J.M. SMUCKER COMPANY ("), //
                        hasCurrencyCode("USD"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-05T13:44"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-05-24T19:51"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-26T13:53"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-08-09T11:33"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-25T09:58"), hasAmount("EUR", 800.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Auszahlung"))));

        // @formatter:off
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
        // [...]
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-03T06:09"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (1.52 - 0.23) / 1.1120), hasGrossValue("EUR", 1.37), //
                        hasForexGrossValue("USD", 1.52), //
                        hasTaxes("EUR", 0.21), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1 / 1.1120, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-02T07:38"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.08), hasGrossValue("EUR", 4.08), //
                        hasForexGrossValue("GBP", 3.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1.0938525487, 0.000001))))));

        // @formatter:off
        // 13-06-2019 06:46 12-06-2019 Währungswechsel (Einbuchung) EUR 2,08 EUR 263,16
        // 13-06-2019 06:46 12-06-2019 Währungswechsel (Ausbuchung) 1,1298 USD -2,36 USD -0,00
        // [...]
        // 12-06-2019 07:54 12-06-2019 3M COMPANY COMMON STOC US88579Y1010 Dividende USD 1,44 USD 1,22
        // 12-06-2019 07:54 12-06-2019 3M COMPANY COMMON STOC US88579Y1010 Dividendensteuer USD -0,22 USD -0,22
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-12T07:54"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (1.44 - 0.22) / 1.1298), hasGrossValue("EUR", 1.27), //
                        hasForexGrossValue("USD", 1.44), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1 / 1.1298, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2019-05-02T08:20"), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Dividendensteuer: NL0011821202"), //
                        hasAmount("EUR", 2.20), hasGrossValue("EUR", 2.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2019-05-02T08:20"), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Dividendensteuer: NL0011821202"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2019-01-28T16:45"), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Dividendensteuer: US6907321029"), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.30), //
                        hasForexGrossValue("USD", 0.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // @formatter:off
        // 22-08-2019 11:28 20-08-2019 BAIDU INC. - AMERICAN US0567521085 ADR/GDR Weitergabegebühr USD -0,01 USD -0,01
        //
        // The document does not contain an exchange rate for this fee, therefore
        // the transaction cannot be imported and is reported to the user.
        // @formatter:on
        // check transaction without exchange rate
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        fee( //
                                        hasDate("2019-08-22T11:28"), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug08.txt"), //
                                        hasNote("US0567521085: ADR/GDR Weitergabegebühr"), //
                                        hasAmount("USD", 0.01), hasGrossValue("USD", 0.01), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-06-24T17:20"), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("US47215P1066: ADR/GDR Weitergabegebühr"), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasForexGrossValue("USD", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-05-21T08:34"), //
                        hasShares(0), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("US01609W1027: ADR/GDR Weitergabegebühr"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasForexGrossValue("USD", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-02-01T16:32"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-02-01T16:32"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:35"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:35"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:34"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-02-01T13:34"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-09-05T11:34"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2018 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-09-05T11:34"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2018 (NASDAQ - NDQ)"))));

        // check 1st cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2019-08-01T11:16"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug08.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 8.27), hasGrossValue("EUR", 8.27), //
                                        hasForexGrossValue("USD", 9.18), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2nd cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2019-08-01T11:16"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug08.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 8.27), hasGrossValue("EUR", 8.27), //
                                        hasForexGrossValue("USD", 9.18), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoauszug09()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US20030N1019"), hasWkn(null), hasTicker(null), //
                        hasName("COMCAST CORPORATION -"), //
                        hasCurrencyCode("USD"))));

        // @formatter:off
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Einbuchung) EUR 1,29 EUR 691,18
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Ausbuchung) 1,1141 USD -1,44 USD 0,00
        // 24-10-2019 06:13 23-10-2019 COMCAST CORPORATION - US20030N1019 Dividende USD 0,42 USD 1,44
        // 24-10-2019 06:13 23-10-2019 COMCAST CORPORATION - US20030N1019 Dividendensteuer USD -0,06 USD 1,02
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-10-24T06:13"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.33), hasGrossValue("EUR", 0.42 / 1.1141), //
                        hasForexGrossValue("USD", 0.42), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1 / 1.1141, 0.000001))))));

        // @formatter:off
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Einbuchung) EUR 1,29 EUR 691,18
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Ausbuchung) 1,1141 USD -1,44 USD 0,00
        // ...
        // 23-10-2019 07:46 23-10-2019 CISCO SYSTEMS INC. - US17275R1023 Dividende USD 1,40 USD 1,19
        // 23-10-2019 07:46 23-10-2019 CISCO SYSTEMS INC. - US17275R1023 Dividendensteuer USD -0,21 USD -0,21
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-10-23T07:46"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (1.4 - 0.21) / 1.1141), hasGrossValue("EUR", 1.4 / 1.1141), //
                        hasForexGrossValue("USD", 1.4), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1 / 1.1141, 0.000001))))));

        // @formatter:off
        // 17-10-2019 07:09 16-10-2019 Währungswechsel (Einbuchung) EUR 2,80 EUR 506,43
        // 17-10-2019 07:09 16-10-2019 Währungswechsel (Ausbuchung) 1,1083 USD -3,11 USD 0,00
        // 17-10-2019 06:25 16-10-2019 LAM RESEARCH CORPORATI US5128071082 Dividende USD 2,30 USD 3,11
        // 17-10-2019 06:25 16-10-2019 LAM RESEARCH CORPORATI US5128071082 Dividendensteuer USD -0,35 USD 0,81
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-10-17T06:25"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (2.3 - 0.35) / 1.1083), hasGrossValue("EUR", 2.3 / 1.1083), //
                        hasForexGrossValue("USD", 2.3), //
                        hasTaxes("EUR", 0.32), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1 / 1.1083, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-10-23T14:59"), //
                        hasShares(0), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("US9485961018: ADR/GDR Weitergabegebühr"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasForexGrossValue("USD", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US17275R1023"), hasWkn(null), hasTicker(null), //
                        hasName("CISCO SYSTEMS INC. -"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5128071082"), hasWkn(null), hasTicker(null), //
                        hasName("LAM RESEARCH CORPORATI"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9485961018"), hasWkn(null), hasTicker(null), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug10()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(12L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(19L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3755581036"), hasWkn(null), hasTicker(null), //
                        hasName("GILEAD SCIENCES INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B14X4T88"), hasWkn(null), hasTicker(null), //
                        hasName("ISHS-ASIA PAC.DIV.DL D"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5894001008"), hasWkn(null), hasTicker(null), //
                        hasName("MERCURY GENERAL CORPOR"), //
                        hasCurrencyCode("USD"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-10-28T08:26"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-10-28T07:49"), hasAmount("EUR", 7000.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-28T04:09"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.27), //
                        hasForexGrossValue("USD", 0.30), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.9082652134, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-23T05:32"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.46), hasGrossValue("EUR", 0.54), //
                        hasForexGrossValue("USD", 0.60), //
                        hasTaxes("EUR", 0.08), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.9064539521, 0.000001))))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-10-02T16:39"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("SOFORT Zahlungsgebühr"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-11-01T09:35"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-10-02T16:39"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("SOFORT Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-10-01T19:08"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("SOFORT Einzahlung"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-30T08:21"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.48), hasGrossValue("EUR", 0.56), //
                        hasForexGrossValue("USD", 0.63), //
                        hasTaxes("EUR", 0.08), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-27T08:58"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.19), hasGrossValue("EUR", 4.19), //
                        hasForexGrossValue("USD", 4.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-27T04:09"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.41), hasGrossValue("EUR", 2.84), //
                        hasForexGrossValue("USD", 3.15), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-18T09:23"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.60), hasGrossValue("EUR", 4.23), //
                        hasTaxes("EUR", 0.63), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-17T06:18"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.77), hasGrossValue("EUR", 0.90), //
                        hasForexGrossValue("USD", 1.00), //
                        hasTaxes("EUR", 0.13), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-16T07:50"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.05), hasGrossValue("EUR", 3.59), //
                        hasForexGrossValue("USD", 4.00), //
                        hasTaxes("EUR", 0.54), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-13T07:40"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.21), hasGrossValue("EUR", 14.37), //
                        hasForexGrossValue("USD", 16.00), //
                        hasTaxes("EUR", 2.16), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-03T14:40"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.45), hasGrossValue("EUR", 0.54), //
                        hasForexGrossValue("USD", 0.60), //
                        hasTaxes("EUR", 0.09), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-28T08:49"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.12), hasGrossValue("EUR", 4.12), //
                        hasForexGrossValue("USD", 4.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15T07:58"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.83), hasGrossValue("EUR", 0.97), //
                        hasForexGrossValue("USD", 1.07), //
                        hasTaxes("EUR", 0.14), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2019-11-01T09:35"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELL A EO-07"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6802231042"), hasWkn(null), hasTicker(null), //
                        hasName("OLD REPUBLIC INTERNATI"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA COMPANY (THE"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5007541064"), hasWkn(null), hasTicker(null), //
                        hasName("THE KRAFT HEINZ COMPAN"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US2480191012"), hasWkn(null), hasTicker(null), //
                        hasName("DELUXE CORPORATION COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B1FZS350"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSII-DEV.MKT.PR.Y.DLDIS"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US01973R1014"), hasWkn(null), hasTicker(null), //
                        hasName("ALLISON TRANSMISSION H"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4228191023"), hasWkn(null), hasTicker(null), //
                        hasName("HEIDRICK & STRUGGLES I"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00287Y1091"), hasWkn(null), hasTicker(null), //
                        hasName("ABBVIE INC. COMMON STO"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug11()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(24L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(4L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(35));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELL PLC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA COMPANY (THE"), //
                        hasCurrencyCode("USD"))));

        // @formatter:off
        // check deposit transaction
        // 26-10-2020 15:00 26-10-2020 flatex Einzahlung EUR 500,00 EUR 512,88
        // @formatter:on
        assertThat(results, hasItem(deposit(hasDate("2020-10-26T15:00"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug11.txt"), hasNote("flatex Einzahlung"))));

        // @formatter:off
        // check dividends transaction EUR --> EUR
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELL PLC GB00B03MLX29 Dividende EUR 1,52 EUR 651,96
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELL PLC GB00B03MLX29 Dividendensteuer EUR -0,23 EUR 650,44
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-16T11:33"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.52 - 0.23), hasGrossValue("EUR", 1.52), //
                        hasTaxes("EUR", 0.23), hasFees("EUR", 0.00))));

        // @formatter:off
        // check dividends transaction USD --> EUR
        // 17-12-2020 08:06 16-12-2020 Währungswechsel (Ausbuchung) EUR 5,87 EUR 657,83
        // 17-12-2020 08:06 16-12-2020 Währungswechsel (Ausbuchung) 1,2212 USD -7,18 USD 0,00
        // [...]
        // 16-12-2020 08:38 15-12-2020 COCA-COLA COMPANY (THE US1912161007 Dividende USD 1,23 USD 7,18
        // 16-12-2020 08:38 15-12-2020 COCA-COLA COMPANY (THE US1912161007 Dividendensteuer USD -0,37 USD 5,95
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-16T08:38"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.71), hasGrossValue("EUR", 1.23 / 1.2212), //
                        hasForexGrossValue("USD", 1.23), //
                        hasTaxes("EUR", 0.30), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(1 / 1.2212, 0.000001))))));

        // check tax refund of a corrected dividend
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2020-10-15T11:35"), //
                                        hasShares(0.00), //
                                        hasSource("Kontoauszug11.txt"), //
                                        hasNote("Dividendensteuer: US7181721090"), //
                                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.30), //
                                        hasForexGrossValue("USD", 0.35), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-16T08:32"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.17), hasGrossValue("EUR", 7.39), //
                        hasForexGrossValue("USD", 9.03), //
                        hasTaxes("EUR", 2.22), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-11T08:35"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.61), hasGrossValue("EUR", 2.31), //
                        hasForexGrossValue("USD", 2.80), //
                        hasTaxes("EUR", 0.70), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-02T08:10"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.74), hasGrossValue("EUR", 1.06), //
                        hasForexGrossValue("USD", 1.28), //
                        hasTaxes("EUR", 0.32), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-21T06:30"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.03), hasGrossValue("EUR", 1.22), //
                        hasForexGrossValue("USD", 1.45), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-13T09:08"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.30), hasGrossValue("EUR", 3.29), //
                        hasForexGrossValue("USD", 3.90), //
                        hasTaxes("EUR", 0.99), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-10T10:43"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.97), hasGrossValue("EUR", 1.32), //
                        hasTaxes("EUR", 0.35), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-10T10:02"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.15), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-10T07:15"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.94), hasGrossValue("EUR", 1.35), //
                        hasForexGrossValue("USD", 1.60), //
                        hasTaxes("EUR", 0.41), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-15T11:35"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.99), hasGrossValue("EUR", 0.99), //
                        hasForexGrossValue("USD", 1.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-10-15T11:35"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.99), hasGrossValue("EUR", 0.99), //
                                        hasForexGrossValue("USD", 1.16), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-14T08:00"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.03), //
                        hasForexGrossValue("USD", 0.04), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-14T08:00"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.99), //
                        hasForexGrossValue("USD", 1.16), //
                        hasTaxes("EUR", 0.30), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T12:30"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.73), hasGrossValue("EUR", 1.05), //
                        hasForexGrossValue("USD", 1.23), //
                        hasTaxes("EUR", 0.32), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T08:16"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.21), //
                        hasForexGrossValue("USD", 0.25), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-21T16:54"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.81), //
                        hasTaxes("EUR", 0.12), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-16T07:53"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.17), hasGrossValue("EUR", 7.40), //
                        hasForexGrossValue("USD", 8.75), //
                        hasTaxes("EUR", 2.23), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-11T08:21"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.50), hasGrossValue("EUR", 2.15), //
                        hasForexGrossValue("USD", 2.55), //
                        hasTaxes("EUR", 0.65), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-10T06:19"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.01), hasGrossValue("EUR", 1.19), //
                        hasForexGrossValue("USD", 1.41), //
                        hasTaxes("EUR", 0.18), hasFees("EUR", 0.00))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        taxes( //
                                        hasDate("2020-11-18T18:47"), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug11.txt"), //
                                        hasNote("Dividendensteuer: US5949181045"), //
                                        hasAmount("USD", 0.01), hasGrossValue("USD", 0.01), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        taxRefund( //
                                        hasDate("2020-11-18T18:47"), //
                                        hasShares(0), //
                                        hasSource("Kontoauszug11.txt"), //
                                        hasNote("Dividendensteuer: US92826C8394"), //
                                        hasAmount("USD", 0.01), hasGrossValue("USD", 0.01), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5801351017"), hasWkn(null), hasTicker(null), //
                        hasName("MCDONALDS CORPORATION"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("MICROSOFT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92826C8394"), hasWkn(null), hasTicker(null), //
                        hasName("VISA INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9047847093"), hasWkn(null), hasTicker(null), //
                        hasName("UNILEVER NV COMMON STO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006013006"), hasWkn(null), hasTicker(null), //
                        hasName("HAMBORNER REIT AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US57636Q1040"), hasWkn(null), hasTicker(null), //
                        hasName("MASTERCARD INCORPORATE"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7181721090"), hasWkn(null), hasTicker(null), //
                        hasName("PHILIP MORRIS INTERNAT"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn(null), hasTicker(null), //
                        hasName("NIKE INC. COMMON STOC"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug12()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELLA"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007231334"), hasWkn(null), hasTicker(null), //
                        hasName("SIXT SE"), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-28T16:50"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-26T08:50"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2018-10-08T11:25"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Einzahlung"))));

        // @formatter:off
        // 29-03-2021 09:06 29-03-2021 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,79 EUR 118,19
        // 29-03-2021 09:06 29-03-2021 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,42 EUR 115,40
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-29T09:06"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.79 - 0.42), hasGrossValue("EUR", 2.79), //
                        hasTaxes("EUR", 0.42), hasFees("EUR", 0.00))));

        // @formatter:off
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,77 EUR 117,19
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,41 EUR 114,42
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-16T11:33"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.77 - 0.41), hasGrossValue("EUR", 2.77), //
                        hasTaxes("EUR", 0.41), hasFees("EUR", 0.00))));

        // @formatter:off
        // 21-09-2020 16:54 21-09-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,71 EUR 114,96
        // 21-09-2020 16:54 21-09-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,41 EUR 112,25
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-21T16:54"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.71 - 0.41), hasGrossValue("EUR", 2.71), //
                        hasTaxes("EUR", 0.41), hasFees("EUR", 0.00))));

        // @formatter:off
        // 30-06-2020 06:25 29-06-2020 SIXT SE DE0007231334 Dividende EUR 0,05 EUR 112,81
        // 30-06-2020 06:25 29-06-2020 SIXT SE DE0007231334 Dividendensteuer EUR -0,01 EUR 112,76
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T06:25"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.05 - 0.01), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // @formatter:off
        // 24-06-2020 11:42 22-06-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,84 EUR 112,77
        // 24-06-2020 11:42 22-06-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,43 EUR 109,93
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T11:42"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.84 - 0.43), hasGrossValue("EUR", 2.84), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-04-01T21:00"), hasAmount("EUR", 0.15), //
                        hasSource("Kontoauszug12.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2020-12-31T03:42"), hasAmount("EUR", 0.05), //
                        hasSource("Kontoauszug12.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-05-03T17:14"), hasAmount("EUR", 1.12), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-04-01T12:03"), hasAmount("EUR", 0.06), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-03-01T11:21"), hasAmount("EUR", 0.02), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:16"), hasAmount("EUR", 1.30), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-05T18:46"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2020 (Euronext Amsterdam - EAM)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2020-04-01T11:20"), hasAmount("EUR", 1.26), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2020 (Euronext Amsterdam - EAM)"))));

        // check transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-08-08T15:59"), hasAmount("EUR", 2.02), //
                        hasSource("Kontoauszug12.txt"), hasNote("Gutschrift für die Neukundenaktion"))));
    }

    @Test
    public void testKontoauszug13()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-26T09:01"), hasAmount("CHF", 569.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-10-26T09:26"), hasAmount("CHF", 569.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("Einzahlung"))));

        // @formatter:off
        // 17-11-2021 07:38 16-11-2021 Währungswechsel (Einbuchung) CHF 2.02 CHF 1'185.08
        // 17-11-2021 07:38 16-11-2021 Währungswechsel (Ausbuchung) 1.0760 USD -2.18 USD 0.00
        // [...]
        // 16-11-2021 09:52 15-11-2021 ACCENTURE PLC. CLASS A IE00B4BNMY34 Dividende USD 2.91 USD 2.18
        // 16-11-2021 09:52 15-11-2021 ACCENTURE PLC. CLASS A IE00B4BNMY34 Dividendensteuer USD -0.73 USD -0.73
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-16T09:52"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2.02), hasGrossValue("CHF", 2.91 / 1.076), //
                        hasForexGrossValue("USD", 2.91), //
                        hasTaxes("CHF", 0.68), hasFees("CHF", 0.00))));

        // @formatter:off
        // 13-11-2021 07:45 12-11-2021 Währungswechsel (Einbuchung) CHF 1.54 CHF 1'183.06
        // 13-11-2021 07:45 12-11-2021 Währungswechsel (Ausbuchung) 1.0866 USD -1.68 USD 0.00
        // 12-11-2021 07:31 11-11-2021 APPLE INC US0378331005 Dividende USD 1.98 USD 1.68
        // 12-11-2021 07:31 11-11-2021 APPLE INC US0378331005 Dividendensteuer USD -0.30 USD -0.30
        // @formatter:on
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-12T07:31"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1.54), hasGrossValue("CHF", 1.98 / 1.0866), //
                        hasForexGrossValue("USD", 1.98), //
                        hasTaxes("CHF", 0.28), hasFees("CHF", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4BNMY34"), hasWkn(null), hasTicker(null), //
                        hasName("ACCENTURE PLC. CLASS A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug14()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn(null), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000644505"), hasWkn(null), hasTicker(null), //
                        hasName("LENZING AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("SE0006422390"), hasWkn(null), hasTicker(null), //
                        hasName("THULE GROUP"), //
                        hasCurrencyCode("SEK"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US68629Y1038"), hasWkn(null), hasTicker(null), //
                        hasName("ORION OFFICE REIT INC. COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("REALTY INCOME CORPORAT"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8621211007"), hasWkn(null), hasTicker(null), //
                        hasName("STORE CAPITAL CORPORAT"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7181721090"), hasWkn(null), hasTicker(null), //
                        hasName("PHILIP MORRIS INTERNAT"), //
                        hasCurrencyCode("USD"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-04T10:50"), hasAmount("EUR", 1692.01), //
                        hasSource("Kontoauszug14.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-02T10:50"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug14.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-04-25T10:50"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug14.txt"), hasNote("flatex Einzahlung"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-05T10:54"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 97.63), hasGrossValue("EUR", 132.60), //
                        hasTaxes("EUR", 34.97), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-04T08:37"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 116.69), hasGrossValue("EUR", 160.95), //
                        hasTaxes("EUR", 44.26), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-04T07:41"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.15), hasGrossValue("EUR", 18.79), //
                        hasForexGrossValue("SEK", 195.00), //
                        hasTaxes("EUR", 5.64), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-22T09:42"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.37), //
                        hasForexGrossValue("USD", 0.40), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-21T11:20"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.70), hasGrossValue("EUR", 10.24), //
                        hasForexGrossValue("USD", 11.12), //
                        hasTaxes("EUR", 1.54), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-21T10:35"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 31.94), hasGrossValue("EUR", 37.58), //
                        hasForexGrossValue("USD", 40.81), //
                        hasTaxes("EUR", 5.64), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-14T08:53"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 24.00), hasGrossValue("EUR", 24.18), //
                        hasForexGrossValue("USD", 26.25), //
                        hasTaxes("EUR", 0.18), hasFees("EUR", 0.00))));

        // check 1st fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-04-29T17:34"), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote("US3682872078: ADR/GDR Weitergabegebühr"), //
                        hasAmount("EUR", 6.77), hasGrossValue("EUR", 6.77), //
                        hasForexGrossValue("USD", 7.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-04-29T17:30"), //
                        hasShares(0), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote("US3682872078: ADR/GDR Weitergabegebühr"), //
                        hasAmount("EUR", 4.57), hasGrossValue("EUR", 4.57), //
                        hasForexGrossValue("USD", 4.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3682872078"), hasWkn(null), hasTicker(null), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug15()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(25L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(30L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(55));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US30231G1022"), hasWkn(null), hasTicker(null), //
                        hasName("EXXON MOBIL CORPORATIO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("MICROSOFT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0000009538"), hasWkn(null), hasTicker(null), //
                        hasName("PHILIPS KON"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A1ML7J1"), hasWkn(null), hasTicker(null), //
                        hasName("VONOVIA SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4781601046"), hasWkn(null), hasTicker(null), //
                        hasName("JOHNSON & JOHNSON COMM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4581401001"), hasWkn(null), hasTicker(null), //
                        hasName("INTEL CORPORATION - CO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007164600"), hasWkn(null), hasTicker(null), //
                        hasName("SAP SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007664039"), hasWkn(null), hasTicker(null), //
                        hasName("VOLKSWAGEN AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006047004"), hasWkn(null), hasTicker(null), //
                        hasName("HEIDELBERGCEMENT AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn(null), hasTicker(null), //
                        hasName("PROCTER & GAMBLE COMPA"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120644"), hasWkn(null), hasTicker(null), //
                        hasName("DANONE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn(null), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007100000"), hasWkn(null), hasTicker(null), //
                        hasName("MERCEDES-BENZ GROUP AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BAY0017"), hasWkn(null), hasTicker(null), //
                        hasName("BAYER AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008430026"), hasWkn(null), hasTicker(null), //
                        hasName("MUENCHENER RUECKVERSICHERUNGS"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US2358511028"), hasWkn(null), hasTicker(null), //
                        hasName("DANAHER CORPORATION CO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US17275R1023"), hasWkn(null), hasTicker(null), //
                        hasName("CISCO SYSTEMS INC. -"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005200000"), hasWkn(null), hasTicker(null), //
                        hasName("BEIERSDORF AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005557508"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE TELEKOM AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006048432"), hasWkn(null), hasTicker(null), //
                        hasName("HENKEL AG & CO. KGAA VZ"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA COMPANY (THE"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn(null), hasTicker(null), //
                        hasName("NIKE INC. COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92556H2067"), hasWkn(null), hasTicker(null), //
                        hasName("PARAMOUNT GLOBAL"), //
                        hasCurrencyCode("USD"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-20T08:50"), hasAmount("EUR", 600.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-03T09:00"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-02T08:50"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("flatex Einzahlung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-04-08T08:50"), hasAmount("EUR", 600.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("flatex Einzahlung"))));

        // check 1st dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-10T09:04"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.97), hasGrossValue("EUR", 5.84), //
                        hasForexGrossValue("USD", 6.16), //
                        hasTaxes("EUR", 0.87), hasFees("EUR", 0.00))));

        // check 2nd dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-09T09:44"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.99), hasGrossValue("EUR", 1.17), //
                        hasForexGrossValue("USD", 1.24), //
                        hasTaxes("EUR", 0.18), hasFees("EUR", 0.00))));

        // check 3rd dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-09T09:37"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.45), hasGrossValue("EUR", 17.00), //
                        hasTaxes("EUR", 2.55), hasFees("EUR", 0.00))));

        // check 4th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-09T09:20"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.82), hasGrossValue("EUR", 44.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-07T09:08"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.79), hasGrossValue("EUR", 2.11), //
                        hasForexGrossValue("USD", 2.26), //
                        hasTaxes("EUR", 0.32), hasFees("EUR", 0.00))));

        // check 6th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-01T08:23"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.40), hasGrossValue("EUR", 7.52), //
                        hasForexGrossValue("USD", 8.03), //
                        hasTaxes("EUR", 1.12), hasFees("EUR", 0.00))));

        // check 7th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-23T07:58"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 18.04), hasGrossValue("EUR", 24.50), //
                        hasTaxes("EUR", 6.46), hasFees("EUR", 0.00))));

        // check 8th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-17T11:36"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.26), hasGrossValue("EUR", 30.24), //
                        hasTaxes("EUR", 7.98), hasFees("EUR", 0.00))));

        // check 9th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-17T11:35"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.53), hasGrossValue("EUR", 4.80), //
                        hasTaxes("EUR", 1.27), hasFees("EUR", 0.00))));

        // check 10th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-17T07:40"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.20), hasGrossValue("EUR", 2.59), //
                        hasForexGrossValue("USD", 2.74), //
                        hasTaxes("EUR", 0.39), hasFees("EUR", 0.00))));

        // check 11th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-13T16:03"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.91), hasGrossValue("EUR", 3.88), //
                        hasTaxes("EUR", 0.97), hasFees("EUR", 0.00))));

        // check 12th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-12T10:34"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.37), hasGrossValue("EUR", 0.44), //
                        hasForexGrossValue("USD", 0.46), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));

        // check 13th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-05T10:54"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.54), hasGrossValue("EUR", 44.20), //
                        hasTaxes("EUR", 11.66), hasFees("EUR", 0.00))));

        // check 14th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-05T10:19"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 40.49), hasGrossValue("EUR", 55.00), //
                        hasTaxes("EUR", 14.51), hasFees("EUR", 0.00))));

        // check 15th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-05T08:41"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.14), hasGrossValue("EUR", 26.00), //
                        hasTaxes("EUR", 6.86), hasFees("EUR", 0.00))));

        // check 16th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-03T07:27"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.39), hasGrossValue("EUR", 44.00), //
                        hasTaxes("EUR", 11.61), hasFees("EUR", 0.00))));

        // check 17th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-02T08:05"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.60), hasGrossValue("EUR", 0.71), //
                        hasForexGrossValue("USD", 0.75), //
                        hasTaxes("EUR", 0.11), hasFees("EUR", 0.00))));

        // check 18th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-28T08:10"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.62), hasGrossValue("EUR", 0.72), //
                        hasForexGrossValue("USD", 0.76), //
                        hasTaxes("EUR", 0.10), hasFees("EUR", 0.00))));

        // check 19th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-22T12:14"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.61), hasGrossValue("EUR", 4.90), //
                        hasTaxes("EUR", 1.29), hasFees("EUR", 0.00))));

        // check 20th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-13T14:02"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.00), hasGrossValue("EUR", 16.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-08T11:45"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.09), hasGrossValue("EUR", 5.55), //
                        hasTaxes("EUR", 1.46), hasFees("EUR", 0.00))));

        // check 22th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-04T08:17"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.36), hasGrossValue("EUR", 1.60), //
                        hasForexGrossValue("USD", 1.76), //
                        hasTaxes("EUR", 0.24), hasFees("EUR", 0.00))));

        // check 23th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-04T07:31"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.13), hasGrossValue("EUR", 2.50), //
                        hasForexGrossValue("USD", 2.75), //
                        hasTaxes("EUR", 0.37), hasFees("EUR", 0.00))));

        // check 24th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-04T07:04"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.34), hasGrossValue("EUR", 3.93), //
                        hasForexGrossValue("USD", 4.32), //
                        hasTaxes("EUR", 0.59), hasFees("EUR", 0.00))));

        // check 1st interest charge transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-04-02T08:10"), hasAmount("EUR", 0.88), //
                        hasSource("Kontoauszug15.txt"), hasNote("Flatex Interest"))));

        // check 1st fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-04-28T15:33"), //
                        hasShares(0), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote("US47759T1007: ADR/GDR Weitergabegebühr"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasForexGrossValue("USD", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US47759T1007"), hasWkn(null), hasTicker(null), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testKontoauszug16()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(18L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(22));
        new AssertImportActions().check(results, "CHF", "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3XXRP09"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD S&P 500 UCITS ETF USD"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B8GKDB10"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD HIGH DIV"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B945VV12"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE DEVELOPED EUROPE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn(null), hasTicker(null), //
                        hasName("NOVO NORDISK A/S"), //
                        hasCurrencyCode("DKK"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-03T07:32"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 85.89), hasGrossValue("CHF", 85.89), //
                        hasForexGrossValue("USD", 108.27), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-03T07:24"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 215.29), hasGrossValue("CHF", 215.29), //
                        hasForexGrossValue("USD", 271.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-03T07:17"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 213.65), hasGrossValue("EUR", 213.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-03T08:57"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 90.41), hasGrossValue("CHF", 90.41), //
                        hasForexGrossValue("USD", 105.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-03T07:33"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 90.41), hasGrossValue("CHF", 90.41), //
                        hasForexGrossValue("USD", 105.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-03T07:31"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 90.41), hasGrossValue("CHF", 90.41), //
                        hasForexGrossValue("USD", 105.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-03T07:30"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 35.37), hasGrossValue("EUR", 35.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-03T07:25"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 100.99), hasGrossValue("CHF", 100.99), //
                        hasForexGrossValue("USD", 117.83), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-02T06:10"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 36.76), hasGrossValue("CHF", 50.35), //
                        hasForexGrossValue("DKK", 395.00), //
                        hasTaxes("CHF", 13.59), hasFees("CHF", 0.00))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2025-04-03T08:47"), hasExDate(null), //
                                        hasShares(0.00), //
                                        hasSource("Kontoauszug16.txt"), //
                                        hasNote(null), //
                                        hasAmount("CHF", 180.82), hasGrossValue("CHF", 180.82), //
                                        hasForexGrossValue("USD", 210.98), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-26T08:39"), hasAmount("CHF", 1500.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Einzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2025-05-26T08:39"), hasAmount("CHF", 1500.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Einzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2025-04-28T08:42"), hasAmount("CHF", 1500.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Einzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2025-03-26T08:39"), hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Einzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2025-02-26T08:56"), hasAmount("CHF", 1500.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Einzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2025-01-28T10:54"), hasAmount("CHF", 1500.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Einzahlung"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-02-06T17:17"), //
                        hasShares(0), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2025 (Tradegate AG - TDG)"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2025-02-06T17:17"), //
                        hasShares(0), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote("Einrichtung von Handelsmodalitäten 2025 (Xetra - XET)"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug17()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, "CHF", "EUR", "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A13SUL5"), hasWkn(null), hasTicker(null), //
                        hasName("DEFAMA DEUTSCHE FACHMARKT AG"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn(null), hasTicker(null), //
                        hasName("NIKE INC 'B'"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7134481081"), hasWkn(null), hasTicker(null), //
                        hasName("PEPSICO INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120321"), hasWkn(null), hasTicker(null), //
                        hasName("L'ORÉAL"), //
                        hasCurrencyCode("EUR"))));

        // check transactions
        assertThat(results, hasItem(deposit(hasDate("2024-07-01T08:43"), hasAmount("CHF", 1500.00), //
                        hasSource("Kontoauszug17.txt"), hasNote("Einzahlung"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-11T07:21"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.79), hasGrossValue("EUR", 22.80), //
                        hasTaxes("EUR", 6.01), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-02T07:35"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 6.51), hasGrossValue("CHF", 7.66), //
                        hasForexGrossValue("USD", 8.51), //
                        hasTaxes("CHF", 1.15), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-02T07:07"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 23.02), hasGrossValue("CHF", 27.08), //
                        hasForexGrossValue("USD", 30.07), //
                        hasTaxes("CHF", 4.06), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-01T07:29"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 8.29), hasGrossValue("CHF", 9.76), //
                        hasForexGrossValue("USD", 10.84), //
                        hasTaxes("CHF", 1.47), hasFees("CHF", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-21T10:59"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.13), hasGrossValue("EUR", 13.20), //
                        hasTaxes("EUR", 3.07), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2024-06-21T10:57"), hasExDate(null), //
                                        hasShares(0.00), //
                                        hasSource("Kontoauszug17.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 13.20), hasGrossValue("EUR", 13.20), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2024-06-21T10:57"), //
                                        hasShares(0.00), //
                                        hasSource("Kontoauszug17.txt"), //
                                        hasNote("Dividendensteuer: FR0000120321"), //
                                        hasAmount("EUR", 3.30), hasGrossValue("EUR", 3.30), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-07-11T07:21"), //
                        hasShares(0), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote("Gebühr für Kapitalmaßnahme"), //
                        hasAmount("EUR", 1.50), hasGrossValue("EUR", 1.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-07-02T07:36"), //
                        hasShares(0), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote("Gebühr für Kapitalmaßnahme"), //
                        hasAmount("USD", 0.72), hasGrossValue("USD", 0.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-07-02T07:08"), //
                        hasShares(0), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote("Gebühr für Kapitalmaßnahme"), //
                        hasAmount("USD", 1.84), hasGrossValue("USD", 1.84), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-07-01T07:30"), //
                        hasShares(0), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote("Gebühr für Kapitalmaßnahme"), //
                        hasAmount("USD", 0.92), hasGrossValue("USD", 0.92), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-06-21T10:59"), //
                        hasShares(0), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote("Gebühr für Kapitalmaßnahme"), //
                        hasAmount("EUR", 1.01), hasGrossValue("EUR", 1.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(feeRefund( //
                        hasDate("2024-06-21T10:57"), //
                        hasShares(0), //
                        hasSource("Kontoauszug17.txt"), //
                        hasNote("Gebühr für Kapitalmaßnahme"), //
                        hasAmount("EUR", 0.99), hasGrossValue("EUR", 0.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRekeningoverzicht01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-25T08:42"), hasAmount("EUR", 123.00), //
                        hasSource("Rekeningoverzicht01.txt"), hasNote("iDEAL Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-25T08:41"), hasAmount("EUR", 1123.00), //
                        hasSource("Rekeningoverzicht01.txt"), hasNote("iDEAL Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-27T08:43"), hasAmount("EUR", 123.00), //
                        hasSource("Rekeningoverzicht01.txt"), hasNote("iDEAL Deposit"))));
    }

    @Test
    public void testRekeningoverzicht02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-26T08:41"), hasAmount("EUR", 20.00), //
                        hasSource("Rekeningoverzicht02.txt"), hasNote("iDEAL Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-25T08:42"), hasAmount("EUR", 10.00), //
                        hasSource("Rekeningoverzicht02.txt"), hasNote("iDEAL Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-25T08:41"), hasAmount("EUR", 1200.00), //
                        hasSource("Rekeningoverzicht02.txt"), hasNote("iDEAL Deposit"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-09-02T21:29"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht02.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2021 (Borsa Italiana S.p.A. - MIL)"))));
    }

    /**
     * Test reading Dividends in USD from DeGiro AccountStatement in Dutch and
     * converting to EUR.
     */
    @Test
    public void testRekeningoverzicht03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-26T08:08"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Rekeningoverzicht03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.53), hasGrossValue("EUR", 0.53), //
                        hasForexGrossValue("USD", 0.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasName("ISHARES INFRA GLO"), hasIsin("IE00B1FZS467")), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.92954, 0.000001))), //
                        check(tx -> assertNull(tx.getCrossEntry())))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B1FZS467"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES INFRA GLO"), //
                        hasCurrencyCode("USD"))));
    }

    /**
     * Test reading Dividend and Dividend Tax in EUR from DeGiro
     * AccountStatement in Dutch. Dividendbelasting is Dutch for Dividend Tax.
     */
    @Test
    public void testRekeningoverzicht04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0007052782"), hasWkn(null), hasTicker(null), //
                        hasName("LYXOR ETF CAC 40"), //
                        hasCurrencyCode("EUR"))));

        // check dividend transaction including tax
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-07-11T07:45"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("Rekeningoverzicht04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.12), hasGrossValue("EUR", 1.50), //
                        hasTaxes("EUR", 0.38), hasFees("EUR", 0.00), //
                        hasSecurity(hasName("LYXOR ETF CAC 40"), hasIsin("FR0007052782")), //
                        check(tx -> assertNull(tx.getCrossEntry())))));
    }

    /**
     * Test reading Exchange Connection Fee in EUR from DeGiro AccountStatement
     * in Dutch. These are fees that DeGiro charges for trading on foreign stock
     * exchanges.
     */
    @Test
    public void testRekeningoverzicht05()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        assertThat(results, hasItem(fee( //
                        hasDate("2022-06-01T19:55"), //
                        hasShares(0.00), //
                        hasSource("Rekeningoverzicht05.txt"), //
                        hasNote("Giro Exchange Connection Fee 2022"), //
                        hasAmount("EUR", 1.01), hasGrossValue("EUR", 1.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            assertNull(tx.getSecurity());
                            assertNull(tx.getCrossEntry());
                        }))));
    }

    /**
     * Test reading bank deposit in EUR from DeGiro AccountStatement in Dutch.
     * This is shown on the account statement as "flatex Storting".
     */
    @Test
    public void testRekeningoverzicht06()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        assertThat(results, hasItem(deposit( //
                        hasDate("2022-07-25T10:50"), //
                        hasShares(0.00), //
                        hasSource("Rekeningoverzicht06.txt"), //
                        hasNote("flatex Storting"), //
                        hasAmount("EUR", 123.45), hasGrossValue("EUR", 123.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            assertNull(tx.getSecurity());
                            assertNull(tx.getCrossEntry());
                        }))));
    }

    @Test
    public void testRekeningoverzicht07()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(45L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(48));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9229087690"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD TOTAL STOCK M"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9219097683"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD TOTAL INTERNA"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE AW"), //
                        hasCurrencyCode("USD"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-31T09:01"), hasAmount("EUR", 1250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-01T18:27"), hasAmount("EUR", 1250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-06-02T13:29"), hasAmount("EUR", 1250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-29T02:40"), hasAmount("EUR", 1250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-01T08:40"), hasAmount("EUR", 1250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-02T07:43"), hasAmount("EUR", 1250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-30T06:42"), hasAmount("EUR", 750.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("iDEAL Deposit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-06-30T18:20"), hasAmount("EUR", 2900.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("flatex terugstorting"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-09-10T02:58"), hasAmount("EUR", 38100.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("flatex terugstorting"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-13T14:30"), hasAmount("EUR", 47250.00), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("flatex terugstorting"))));

        // check 1st dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-29T03:53"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 328.78), hasGrossValue("EUR", 469.67), //
                        hasForexGrossValue("USD", 499.68), //
                        hasTaxes("EUR", 140.89), hasFees("EUR", 0.00))));

        // check 2nd dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-23T04:04"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 528.85), hasGrossValue("EUR", 755.49), //
                        hasForexGrossValue("USD", 802.48), //
                        hasTaxes("EUR", 226.64), hasFees("EUR", 0.00))));

        // check 3rd dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-29T05:13"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 306.37), hasGrossValue("EUR", 437.68), //
                        hasForexGrossValue("USD", 427.18), //
                        hasTaxes("EUR", 131.31), hasFees("EUR", 0.00))));

        // check 4th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-23T05:31"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 253.97), hasGrossValue("EUR", 362.83), //
                        hasForexGrossValue("USD", 357.79), //
                        hasTaxes("EUR", 108.86), hasFees("EUR", 0.00))));

        // check 5th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-29T04:35"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 266.98), hasGrossValue("EUR", 381.41), //
                        hasForexGrossValue("USD", 402.27), //
                        hasTaxes("EUR", 114.43), hasFees("EUR", 0.00))));

        // check 6th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-26T06:41"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 497.42), hasGrossValue("EUR", 710.59), //
                        hasForexGrossValue("USD", 751.95), //
                        hasTaxes("EUR", 213.17), hasFees("EUR", 0.00))));

        // check 7th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-29T07:03"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 239.54), hasGrossValue("EUR", 342.21), //
                        hasForexGrossValue("USD", 380.30), //
                        hasTaxes("EUR", 102.67), hasFees("EUR", 0.00))));

        // check 8th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-25T06:14"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 81.33), hasGrossValue("EUR", 116.20), //
                        hasForexGrossValue("USD", 128.11), //
                        hasTaxes("EUR", 34.87), hasFees("EUR", 0.00))));

        // check 9th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-31T08:18"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 343.98), hasGrossValue("EUR", 404.69), //
                        hasForexGrossValue("USD", 461.39), //
                        hasTaxes("EUR", 60.71), hasFees("EUR", 0.00))));

        // check 10th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-24T05:20"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 898.93), hasGrossValue("EUR", 1057.56), //
                        hasForexGrossValue("USD", 1200.97), //
                        hasTaxes("EUR", 158.63), hasFees("EUR", 0.00))));

        // check 11th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-30T05:08"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 284.73), hasGrossValue("EUR", 334.97), //
                        hasForexGrossValue("USD", 388.90), //
                        hasTaxes("EUR", 50.24), hasFees("EUR", 0.00))));

        // check 12th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-24T04:07"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 329.06), hasGrossValue("EUR", 387.14), //
                        hasForexGrossValue("USD", 454.89), //
                        hasTaxes("EUR", 58.08), hasFees("EUR", 0.00))));

        // check 13th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-01T12:23"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 280.28), hasGrossValue("EUR", 280.28), //
                        hasForexGrossValue("USD", 332.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-30T08:49"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 315.33), hasGrossValue("EUR", 370.97), //
                        hasForexGrossValue("USD", 440.30), //
                        hasTaxes("EUR", 55.64), hasFees("EUR", 0.00))));

        // check 15th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-25T07:39"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 467.31), hasGrossValue("EUR", 549.78), //
                        hasForexGrossValue("USD", 656.88), //
                        hasTaxes("EUR", 82.47), hasFees("EUR", 0.00))));

        // check 16th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-04-01T15:26"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 151.09), hasGrossValue("EUR", 151.09), //
                        hasForexGrossValue("USD", 178.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-31T04:12"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 317.39), hasGrossValue("EUR", 373.39), //
                        hasForexGrossValue("USD", 437.88), //
                        hasTaxes("EUR", 56.00), hasFees("EUR", 0.00))));

        // check 18th dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-26T04:26"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 143.04), hasGrossValue("EUR", 168.29), //
                        hasForexGrossValue("USD", 198.16), //
                        hasTaxes("EUR", 25.25), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-12-16T18:26"), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("Dividendbelasting: US9219097683"), //
                        hasAmount("EUR", 9.23), hasGrossValue("EUR", 9.23), //
                        hasForexGrossValue("USD", 10.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-12-16T18:26"), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("Dividendbelasting: US9219097683"), //
                        hasAmount("EUR", 5.29), hasGrossValue("EUR", 5.29), //
                        hasForexGrossValue("USD", 5.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-12-16T18:26"), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("Dividendbelasting: US9219097683"), //
                        hasAmount("EUR", 3.95), hasGrossValue("EUR", 3.95), //
                        hasForexGrossValue("USD", 4.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-12-16T18:26"), //
                        hasShares(0), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("Dividendbelasting: US9219097683"), //
                        hasAmount("EUR", 1.65), hasGrossValue("EUR", 1.65), //
                        hasForexGrossValue("USD", 1.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-10-01T15:01"), hasAmount("EUR", 0.01), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-07-01T15:00"), hasAmount("EUR", 2.72), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-04-02T09:20"), hasAmount("EUR", 2.29), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-12-30T22:40"), hasAmount("EUR", 0.86), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-10-01T21:20"), hasAmount("EUR", 3.53), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-07-02T05:40"), hasAmount("EUR", 0.26), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-04-01T21:00"), hasAmount("EUR", 0.29), //
                        hasSource("Rekeningoverzicht07.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-03T14:00"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2023 (NYSE Arca - NYA)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-03T14:00"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2023 (NASDAQ - NDQ)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-02-03T10:06"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2022 (NYSE Arca - NYA)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-02-03T10:06"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2022 (NASDAQ - NDQ)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:18"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2021 (NYSE Arca - NYA)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:18"), hasAmount("EUR", 2.50), //
                        hasSource("Rekeningoverzicht07.txt"), //
                        hasNote("DEGIRO Aansluitingskosten 2021 (NASDAQ - NDQ)"))));
    }

    @Test
    public void testAccountStatement01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA56501R1064"), hasWkn(null), hasTicker(null), //
                        hasName("MANULIFE FINANCIAL COR"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9024941034"), hasWkn(null), hasTicker(null), //
                        hasName("TYSON FOODS INC. COMM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7443201022"), hasWkn(null), hasTicker(null), //
                        hasName("PRUDENTIAL FINANCIAL"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA29250N1050"), hasWkn(null), hasTicker(null), //
                        hasName("ENBRIDGE INC COMMON ST"), //
                        hasCurrencyCode("CAD"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-22T07:39"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.12), hasGrossValue("EUR", 12.17), //
                        hasForexGrossValue("CAD", 18.20), //
                        hasTaxes("EUR", 3.05), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.6685385747, 0.000001))))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-16T03:39"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 31.66), hasGrossValue("EUR", 37.26), //
                        hasForexGrossValue("USD", 44.50), //
                        hasTaxes("EUR", 5.60), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8373806733, 0.000001))))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-12T08:05"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.59), hasGrossValue("EUR", 38.34), //
                        hasForexGrossValue("USD", 46.00), //
                        hasTaxes("EUR", 5.75), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.8335417188, 0.000001))))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-02T14:53"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 38.09), hasGrossValue("EUR", 50.78), //
                        hasForexGrossValue("CAD", 77.66), //
                        hasTaxes("EUR", 12.69), hasFees("EUR", 0.00), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.6539366989, 0.000001))))));

        // check 1th interest transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-03-01T18:47"), hasAmount("EUR", 0.88), //
                        hasSource("AccountStatement01.txt"), hasNote("Interest"))));
    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(73L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(74));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("PLOPTTC00011"), hasWkn(null), hasTicker(null), //
                        hasName("CD PROJEKT SA"), //
                        hasCurrencyCode("PLN"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-28T04:43"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 2.31), //
                        hasForexGrossValue("PLN", 10.00), //
                        hasTaxes("EUR", 0.44), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-21T06:26"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.82), hasGrossValue("EUR", 2.25), //
                        hasForexGrossValue("PLN", 10.00), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2022-07-14T08:47"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.68), hasGrossValue("EUR", 2.07), //
                        hasForexGrossValue("PLN", 10.00), //
                        hasTaxes("EUR", 0.39), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-09T07:31"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.06), hasGrossValue("EUR", 11.19), //
                        hasForexGrossValue("PLN", 50.00), //
                        hasTaxes("EUR", 2.13), hasFees("EUR", 0.00))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2021-07-23T15:02"), hasExDate(null), //
                                        hasShares(0.00), //
                                        hasSource("AccountStatement02.txt"), //
                                        hasNote(null), //
                                        hasAmount("PLN", 50.00), hasGrossValue("PLN", 50.00), //
                                        hasTaxes("PLN", 0.00), hasFees("PLN", 0.00)))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2021-07-23T15:02"), //
                                        hasShares(0), //
                                        hasSource("AccountStatement02.txt"), //
                                        hasNote("Dividend Tax: PLOPTTC00011"), //
                                        hasAmount("PLN", 9.50), hasGrossValue("PLN", 9.50), //
                                        hasTaxes("PLN", 0.00), hasFees("PLN", 0.00)))));

        // check transaction without exchange rate
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2021-07-23T15:03"), hasExDate(null), //
                                        hasShares(0.00), //
                                        hasSource("AccountStatement02.txt"), //
                                        hasNote(null), //
                                        hasAmount("PLN", 40.50), hasGrossValue("PLN", 50.00), //
                                        hasTaxes("PLN", 9.50), hasFees("PLN", 0.00)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-30T09:10"), hasAmount("EUR", 2550.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-04-08T09:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-02-29T11:30"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-01-30T11:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-01-03T11:30"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-12-08T11:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-11-06T11:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-10-03T11:41"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-09-14T11:21"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-08-01T11:31"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-07-18T11:21"), hasAmount("EUR", 3000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-07-14T11:11"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-07-12T17:27"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-03-30T21:11"), hasAmount("EUR", 2300.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-02-28T16:03"), hasAmount("EUR", 2200.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-02-03T15:11"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-01-05T09:21"), hasAmount("EUR", 4000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-11-08T11:10"), hasAmount("EUR", 2700.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-10-06T14:50"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-09-08T20:50"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-08-17T20:50"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-07-20T08:50"), hasAmount("EUR", 3000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-06-01T20:50"), hasAmount("EUR", 2200.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-02T15:50"), hasAmount("EUR", 3000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-04-01T15:50"), hasAmount("EUR", 4000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-03-01T22:20"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-02-14T14:50"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-01-24T20:50"), hasAmount("EUR", 5000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-11-12T23:20"), hasAmount("EUR", 2900.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-09-29T20:50"), hasAmount("EUR", 730.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-07-06T15:50"), hasAmount("EUR", 1500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-06-07T14:50"), hasAmount("EUR", 1000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-05-06T20:50"), hasAmount("EUR", 3105.70), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-03-30T18:56"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-03-05T18:55"), hasAmount("EUR", 6185.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-01-27T12:11"), hasAmount("EUR", 13041.39), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-01-25T14:09"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-12-03T14:45"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-11-17T16:11"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-10-30T19:02"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-07-10T18:41"), hasAmount("EUR", 7908.40), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-07-10T14:48"), hasAmount("EUR", 2146.92), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-06-11T18:43"), hasAmount("EUR", 3308.58), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-05-07T18:54"), hasAmount("EUR", 4609.20), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-04-14T13:55"), hasAmount("EUR", 6321.60), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-04-14T13:55"), hasAmount("EUR", 2509.20), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-03-06T12:36"), hasAmount("EUR", 1796.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-02-10T18:29"), hasAmount("EUR", 1322.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-02-03T15:34"), hasAmount("EUR", 1465.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2019-12-10T11:08"), hasAmount("EUR", 50.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-22T12:26"), hasAmount("EUR", 0.01), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2018-06-06T16:56"), hasAmount("EUR", 20.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        // check interest charge transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-10-01T14:01"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-07-02T14:12"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.53), hasGrossValue("EUR", 0.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-04-02T15:20"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-12-31T03:00"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-10-01T23:30"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-07-02T15:40"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-02-05T07:17"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2024"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-02-05T07:17"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2024"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-02-05T07:17"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2024"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2023-03-01T15:07"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2023"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2023-01-03T13:59"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2023"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2023-01-03T13:59"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2023"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-02-03T10:00"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2022"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-02-03T10:00"), //
                        hasShares(0), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2022"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAccountStatement03()
    {
        // @formatter:off
        // Synthetic test case derived from AccountStatement02.txt: the dividend tax is
        // booked one minute before its dividend and printed above it. The tax must be
        // added to the dividend and must not be imported a second time.
        // @formatter:on
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement03_synthetic.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-28T04:43"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement03_synthetic.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 2.31), //
                        hasForexGrossValue("PLN", 10.00), //
                        hasTaxes("EUR", 0.44), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("PLOPTTC00011"), hasWkn(null), hasTicker(null), //
                        hasName("CD PROJEKT SA"), //
                        hasCurrencyCode("PLN"))));
    }

    @Test
    public void testAccountStatement_french01()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement_french01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("SE0020050417"), hasWkn(null), hasTicker(null), //
                        hasName("BOLIDEN AB"), //
                        hasCurrencyCode("SEK"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FI0009003727"), hasWkn(null), hasTicker(null), //
                        hasName("WARTSILA OYJ ABP"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker(null), //
                        hasName("ALPHABET INC CLASS A"), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-07T07:10"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.59), hasGrossValue("EUR", 32.27), //
                        hasForexGrossValue("SEK", 352.00), //
                        hasTaxes("EUR", 9.68), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-24T07:04"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.92), hasGrossValue("EUR", 24.49), //
                        hasTaxes("EUR", 8.57), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-16T07:13"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.06), hasGrossValue("EUR", 1.25), //
                        hasForexGrossValue("USD", 1.47), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-08T14:50"), hasAmount("EUR", 9500.00), //
                        hasSource("AccountStatement_french01.txt"), hasNote("Dépôt flatex"))));

        assertThat(results, hasItem(deposit(hasDate("2025-10-07T21:51"), hasAmount("EUR", 500.00), //
                        hasSource("AccountStatement_french01.txt"), hasNote("Dépôt flatex"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-04-07T09:44"), //
                        hasShares(0), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote("Frais de connexion aux places boursières 2026 (London Stock Exchange (LSE) - LSE)"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2026-02-25T16:19"), //
                        hasShares(0), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote("Remboursement offre promotionnelle"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAccountStatement_french02()
    {
        // @formatter:off
        // Synthetic test case derived from AccountStatement_french01.txt: the dividend
        // tax is booked without a dividend. The tax must be imported on its own.
        // @formatter:on
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement_french02_synthetic.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2026-03-24T07:04"), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french02_synthetic.txt"), //
                        hasNote("Impôts sur dividende: FI0009003727"), //
                        hasAmount("EUR", 8.57), hasGrossValue("EUR", 8.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FI0009003727"), hasWkn(null), hasTicker(null), //
                        hasName("WARTSILA OYJ ABP"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testTransaktionsuebersicht01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(17L));
        assertThat(countBuySell(results), is(28L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(49));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C21EMZ1"), hasWkn(null), hasTicker(null), //
                        hasName("ODX5 C11500.00 29MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C21EMV0"), hasWkn(null), hasTicker(null), //
                        hasName("ODX5 C11400.00 29MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C21EMX6"), hasWkn(null), hasTicker(null), //
                        hasName("ODX5 C11450.00 29MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C21EMY4"), hasWkn(null), hasTicker(null), //
                        hasName("ODX5 P11450.00 29MAR19"), //
                        hasCurrencyCode("EUR"))));

        // @formatter:off
        // 29-03-2019 12:18 29-03-2019 ODX5 C11500.00 29MAR19 DE000C21EMZ1
        // Verkauf 3 zu je 30 EUR (DE000C21EMZ1) EUR 450,00 EUR 2.604,06
        // @formatter:on
        // check first buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-29T12:18"), hasShares(3), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 447.30), hasGrossValue("EUR", 450.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.70))));

        // check xx buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-08T12:44"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.90), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check xx buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-08T10:54"), hasShares(2), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 132.80), hasGrossValue("EUR", 131.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.80))));

        // check xx buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-07T13:02"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 100.90), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check outbound delivery (option expired worthless)
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2019-02-08T13:27"), hasShares(3.00), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000C25KFE8")))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-29T12:18"), hasShares(8), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1192.80), hasGrossValue("EUR", 1200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.20))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-29T12:16"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 549.10), hasGrossValue("EUR", 550.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-29T10:50"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 199.10), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-29T10:44"), hasShares(10), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 409.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-29T10:44"), hasShares(10), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 409.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-28T17:15"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 113.40), hasGrossValue("EUR", 112.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-28T17:05"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 113.40), hasGrossValue("EUR", 112.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-28T09:00"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 225.90), hasGrossValue("EUR", 225.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-27T16:35"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 250.90), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-27T14:58"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 175.90), hasGrossValue("EUR", 175.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-26T12:00"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 300.90), hasGrossValue("EUR", 300.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-22T14:10"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 411.90), hasGrossValue("EUR", 411.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-22T14:10"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 161.90), hasGrossValue("EUR", 161.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2019-03-22T14:10"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2019-03-22T14:10"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-22T09:00"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.90), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-21T09:00"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 49.90), hasGrossValue("EUR", 49.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-18T14:06"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 300.90), hasGrossValue("EUR", 300.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-18T13:20"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 350.90), hasGrossValue("EUR", 350.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-02-14T14:42"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.60), hasGrossValue("EUR", 12.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-02-14T09:02"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1754.10), hasGrossValue("EUR", 1755.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-08T16:40"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 500.90), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-08T16:27"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 498.40), hasGrossValue("EUR", 497.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-02-08T13:27"), hasShares(2), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 638.00), hasGrossValue("EUR", 638.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2019-02-08T13:27"), hasShares(2), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-08T11:42"), hasShares(2), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 201.80), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.80))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-07T12:22"), hasShares(1), //
                        hasSource("Transaktionsuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 150.90), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C21EMW8"), hasWkn(null), hasTicker(null), //
                        hasName("ODX5 P11400.00 29MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C21EMU2"), hasWkn(null), hasTicker(null), //
                        hasName("ODX5 P11350.00 29MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C2ZNL25"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 P11550.00 22MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C2ZNL09"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 P11500.00 22MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C2ZNL33"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 C11600.00 22MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C2ZNL74"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 C11700.00 22MAR19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C271SC3"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 P11250.00 22FEB19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C271SD1"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 C11300.00 22FEB19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C2XKH91"), hasWkn(null), hasTicker(null), //
                        hasName("ODAX P10900.00 15FEB19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C2XKH83"), hasWkn(null), hasTicker(null), //
                        hasName("ODAX C10900.00 15FEB19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C25KFF5"), hasWkn(null), hasTicker(null), //
                        hasName("ODX2 P11000.00 08FEB19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C25KFN9"), hasWkn(null), hasTicker(null), //
                        hasName("ODX2 C11200.00 08FEB19"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C25KFE8"), hasWkn(null), hasTicker(null), //
                        hasName("ODX2 C11000.00 08FEB19"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testTransaktionsuebersicht02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005140008"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE BANK AG NA O.N"), //
                        hasCurrencyCode("EUR"))));

        // check first buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-04-01T15:35"), hasShares(136), //
                        hasSource("Transaktionsuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1010.94), hasGrossValue("EUR", 1013.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.26))));

        // check xx buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-01T12:20"), hasShares(18), //
                        hasSource("Transaktionsuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 132.38), hasGrossValue("EUR", 132.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check xx buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-01T12:20"), hasShares(118), //
                        hasSource("Transaktionsuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 869.88), hasGrossValue("EUR", 867.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.23))));
    }

    @Test
    public void testTransaktionsuebersicht03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("TESLA MOTORS INC. - C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000C34JCK6"), hasWkn(null), hasTicker(null), //
                        hasName("ODX1 P12300.00 03MAY19"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-04-29T16:11"), hasShares(3), //
                        hasSource("Transaktionsuebersicht03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 644.53), hasGrossValue("EUR", 645.04), //
                        hasForexGrossValue("USD", 720.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T20:23"), hasShares(1), //
                        hasSource("Transaktionsuebersicht03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 210.42), hasGrossValue("EUR", 209.92), //
                        hasForexGrossValue("USD", 234.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 3rd buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T17:52"), hasShares(2), //
                        hasSource("Transaktionsuebersicht03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 431.20), hasGrossValue("EUR", 430.69), //
                        hasForexGrossValue("USD", 480.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-29T09:16"), hasShares(1), //
                        hasSource("Transaktionsuebersicht03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 250.90), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));
    }

    @Test
    public void testTransaktionsuebersicht04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0394831020"), hasWkn(null), hasTicker(null), //
                        hasName("ARCHER-DANIELS-MIDLAND"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-25T19:03"), hasShares(2), //
                        hasSource("Transaktionsuebersicht04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 75.91), hasGrossValue("EUR", 75.90), //
                        hasForexGrossValue("USD", 84.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-25T19:03"), hasShares(48), //
                        hasSource("Transaktionsuebersicht04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1822.39), hasGrossValue("EUR", 1821.72), //
                        hasForexGrossValue("USD", 2028.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.67))));
    }

    @Test
    public void testTransaktionsuebersicht05()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(66L));
        assertThat(countBuySell(results), is(95L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(161));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1491231015"), hasWkn(null), hasTicker(null), //
                        hasName("CATERPILLAR INC. COMM"), //
                        hasCurrencyCode("USD"))));

        // check first transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-16T19:04"), hasShares(1), //
                        hasSource("Transaktionsuebersicht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 105.50), hasGrossValue("EUR", 105.00), //
                        hasForexGrossValue("USD", 116.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 5th transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-06T20:20"), hasShares(1), //
                        hasSource("Transaktionsuebersicht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 58.35), hasGrossValue("EUR", 57.85), //
                        hasForexGrossValue("USD", 64.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 17th transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-23T15:30"), hasShares(3), //
                        hasSource("Transaktionsuebersicht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 163.32), hasGrossValue("EUR", 163.83), //
                        hasForexGrossValue("USD", 183.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 94th transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-14T16:08"), hasShares(15), //
                        hasSource("Transaktionsuebersicht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 200.33), hasGrossValue("EUR", 199.78), //
                        hasForexGrossValue("USD", 227.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.55))));
    }

    @Test
    public void testTransaktionsuebersicht06()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(17L));
        assertThat(countBuySell(results), is(36L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(53));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000GA2N3L5"), hasWkn(null), hasTicker(null), //
                        hasName("CALL 20.09.19 TRADDESK 180"), //
                        hasCurrencyCode("EUR"))));

        // @formatter:off
        // 20-08-2019 19:03 CALL 20.09.19 TRADDESK 180 DE000GA2N3L5 FRA -114 EUR
        // 6,60 EUR 752,40 EUR 752,40 EUR -2,89 EUR 749,51
        // @formatter:off
        // check first transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-08-20T19:03"), hasShares(114), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 749.51), hasGrossValue("EUR", 752.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.89))));

        // check 29th transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-22T19:16"), hasShares(1), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 76.42), hasGrossValue("EUR", 76.42), //
                        hasForexGrossValue("USD", 85.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 34th transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-12T20:27"), hasShares(50), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3183.28), hasGrossValue("EUR", 3183.96), //
                        hasForexGrossValue("USD", 3588.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.68))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-19T15:30"), hasShares(40), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1405.84), hasGrossValue("EUR", 1405.20), //
                        hasForexGrossValue("USD", 1559.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.64))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-08-15T15:40"), hasShares(25), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1512.25), hasGrossValue("EUR", 1512.84), //
                        hasForexGrossValue("USD", 1685.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.59))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-14T19:04"), hasShares(57), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 367.23), hasGrossValue("EUR", 364.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.43))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-14T18:30"), hasShares(57), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 368.94), hasGrossValue("EUR", 366.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.43))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-08-13T12:08"), hasShares(14), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 662.55), hasGrossValue("EUR", 664.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.17))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-09T09:00"), hasShares(14), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 698.26), hasGrossValue("EUR", 696.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.18))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-08T16:50"), hasShares(16), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1222.16), hasGrossValue("EUR", 1221.61), //
                        hasForexGrossValue("USD", 1368.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-08T15:30"), hasShares(25), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1627.12), hasGrossValue("EUR", 1626.53), //
                        hasForexGrossValue("USD", 1820.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.59))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-08-08T13:48"), hasShares(100), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 760.10), hasGrossValue("EUR", 763.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-07T15:30"), hasShares(19), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1449.83), hasGrossValue("EUR", 1449.26), //
                        hasForexGrossValue("USD", 1624.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.57))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-08-06T15:38"), hasShares(25), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 536.11), hasGrossValue("EUR", 538.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.64))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-06T15:30"), hasShares(40), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2734.13), hasGrossValue("EUR", 2733.49), //
                        hasForexGrossValue("USD", 3054.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.64))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-06T15:29"), hasShares(100), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 725.85), hasGrossValue("EUR", 723.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.85))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-06T08:35"), hasShares(25), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 540.13), hasGrossValue("EUR", 537.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.63))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-02T21:35"), hasShares(12), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1928.52), hasGrossValue("EUR", 1927.98), //
                        hasForexGrossValue("USD", 2139.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.54))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-02T21:30"), hasShares(22), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2642.31), hasGrossValue("EUR", 2641.73), //
                        hasForexGrossValue("USD", 2931.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.58))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-31T20:55"), hasShares(45), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5555.57), hasGrossValue("EUR", 5554.91), //
                        hasForexGrossValue("USD", 6151.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.66))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-31T16:01"), hasShares(16), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 442.24), hasGrossValue("EUR", 442.29), //
                        hasForexGrossValue("USD", 492.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-31T16:01"), hasShares(30), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 829.19), hasGrossValue("EUR", 829.30), //
                        hasForexGrossValue("USD", 924.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.11))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-31T16:01"), hasShares(100), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2763.96), hasGrossValue("EUR", 2764.32), //
                        hasForexGrossValue("USD", 3080.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.36))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-31T16:01"), hasShares(39), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1077.44), hasGrossValue("EUR", 1078.08), //
                        hasForexGrossValue("USD", 1201.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.64))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-30T19:32"), hasShares(50), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2884.90), hasGrossValue("EUR", 2885.58), //
                        hasForexGrossValue("USD", 3218.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.68))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-30T17:21"), hasShares(50), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2870.95), hasGrossValue("EUR", 2871.63), //
                        hasForexGrossValue("USD", 3201.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.68))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-29T16:05"), hasShares(10), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2371.00), hasGrossValue("EUR", 2370.46), //
                        hasForexGrossValue("USD", 2638.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.54))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-23T21:34"), hasShares(70), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1994.65), hasGrossValue("EUR", 1993.90), //
                        hasForexGrossValue("USD", 2223.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-23T19:45"), hasShares(22), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1256.88), hasGrossValue("EUR", 1256.30), //
                        hasForexGrossValue("USD", 1401.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.58))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-22T19:16"), hasShares(29), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2216.13), hasGrossValue("EUR", 2216.24), //
                        hasForexGrossValue("USD", 2486.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.11))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-22T19:16"), hasShares(8), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 611.35), hasGrossValue("EUR", 611.38), //
                        hasForexGrossValue("USD", 685.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-22T19:16"), hasShares(1), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 76.42), hasGrossValue("EUR", 76.42), //
                        hasForexGrossValue("USD", 85.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-22T19:16"), hasShares(1), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 75.93), hasGrossValue("EUR", 76.43), //
                        hasForexGrossValue("USD", 85.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-22T16:06"), hasShares(5), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2842.20), hasGrossValue("EUR", 2842.72), //
                        hasForexGrossValue("USD", 3190.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-12T20:31"), hasShares(55), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3128.91), hasGrossValue("EUR", 3128.21), //
                        hasForexGrossValue("USD", 3525.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.70))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-08T19:17"), hasShares(18), //
                        hasSource("Transaktionsuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2026.48), hasGrossValue("EUR", 2025.92), //
                        hasForexGrossValue("USD", 2269.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.56))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US2545431015"), hasWkn(null), hasTicker(null), //
                        hasName("DIODES INCORPORATED -"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US72703H1014"), hasWkn(null), hasTicker(null), //
                        hasName("PLANET FITNESS INC. C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005419105"), hasWkn(null), hasTicker(null), //
                        hasName("CANCOM SE O.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US57665R1068"), hasWkn(null), hasTicker(null), //
                        hasName("MATCH GROUP INC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DF1TTZ4"), hasWkn(null), hasTicker(null), //
                        hasName("TURBOP O.END DAX 13831,17"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US58506Q1094"), hasWkn(null), hasTicker(null), //
                        hasName("MEDPACE HOLDINGS INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5500211090"), hasWkn(null), hasTicker(null), //
                        hasName("LULULEMON ATHLETICA IN"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IL0011334468"), hasWkn(null), hasTicker(null), //
                        hasName("CYBERARK SOFTWARE LTD."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("MICROSOFT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0079031078"), hasWkn(null), hasTicker(null), //
                        hasName("ADVANCED MICRO DEVICES"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8753722037"), hasWkn(null), hasTicker(null), //
                        hasName("TANDEM DIABETES CARE INC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88339J1051"), hasWkn(null), hasTicker(null), //
                        hasName("THE TRADE DESK CL A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US47215P1066"), hasWkn(null), hasTicker(null), //
                        hasName("JD.COM INC. - AMERICA"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US50212V1008"), hasWkn(null), hasTicker(null), //
                        hasName("LPL FINANCIAL HOLDINGS"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US58733R1023"), hasWkn(null), hasTicker(null), //
                        hasName("MERCADOLIBRE INC. - C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5893781089"), hasWkn(null), hasTicker(null), //
                        hasName("MERCURY SYSTEMS INC -"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransaktionsuebersicht07()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VA5DDR3"), hasWkn(null), hasTicker(null), //
                        hasName("TURBOC O.END GOLD 1109,22"), //
                        hasCurrencyCode("EUR"))));

        // @formatter:off
        // 09-01-2020 09:31 TURBOC O.END GOLD 1109,22 DE000VA5DDR3 FRA -55 EUR
        // 42,46 EUR 2.335,30 EUR 2.335,30 EUR -4,76 EUR 2.330,54
        // @formatter:on
        // check first transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-09T09:31"), hasShares(55), //
                        hasSource("Transaktionsuebersicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2330.54), hasGrossValue("EUR", 2335.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.76))));

        // check 2nd transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-08T09:30"), hasShares(25), //
                        hasSource("Transaktionsuebersicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2342.61), hasGrossValue("EUR", 2340.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.61))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0TGJ55"), hasWkn(null), hasTicker(null), //
                        hasName("VARTA AG"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testTransaktionsuebersicht08()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(17L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US79466L3024"), hasWkn(null), hasTicker(null), //
                        hasName("SALESFORCE.COM INC COM"), //
                        hasCurrencyCode("USD"))));

        // @formatter:off
        // 23-12-2020 21:51 SALESFORCE.COM INC COM US79466L3024 NSY 3 USD 228,00
        // USD -684,00 EUR -561,44 1,2171 EUR -0,51 EUR -561,95
        // @formatter:off
        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-23T21:51"), hasShares(3), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 561.95), hasGrossValue("EUR", 561.44), //
                        hasForexGrossValue("USD", 684.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-02T15:34"), hasShares(2), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 899.55), hasGrossValue("EUR", 900.06), //
                        hasForexGrossValue("USD", 1088.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-07T18:21"), hasShares(100), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 275.79), hasGrossValue("EUR", 268.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.74))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-30T13:43"), hasShares(3), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 152.19), hasGrossValue("EUR", 152.19), //
                        hasForexGrossValue("USD", 181.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-30T13:43"), hasShares(3), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 152.19), hasGrossValue("EUR", 152.19), //
                        hasForexGrossValue("USD", 181.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-26T17:32"), hasShares(1), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 204.43), hasGrossValue("EUR", 203.93), //
                        hasForexGrossValue("USD", 241.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-26T17:00"), hasShares(3), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 289.95), hasGrossValue("EUR", 289.44), //
                        hasForexGrossValue("USD", 342.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-10-26T10:53"), hasShares(4), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2020-10-09"), hasShares(4), //
                                        hasSource("Transaktionsuebersicht08.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-09-14T10:28"), hasShares(6), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-01T12:25"), hasShares(29), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 91.68), hasGrossValue("EUR", 84.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.58))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-01T12:05"), hasShares(5), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 63.51), hasGrossValue("EUR", 61.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.01))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-01T11:39"), hasShares(5), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 194.53), hasGrossValue("EUR", 192.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.03))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2020-09-01"), hasShares(6), //
                                        hasSource("Transaktionsuebersicht08.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-31T16:32"), hasShares(3), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1174.15), hasGrossValue("EUR", 1173.64), //
                        hasForexGrossValue("USD", 1402.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-31"), hasShares(16), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1682.41), hasGrossValue("EUR", 1682.41), //
                        hasForexGrossValue("USD", 1996.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-31"), hasShares(4), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1682.41), hasGrossValue("EUR", 1682.41), //
                        hasForexGrossValue("USD", 1996.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-28T19:06"), hasShares(1), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 421.86), hasGrossValue("EUR", 421.36), //
                        hasForexGrossValue("USD", 501.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-28T15:50"), hasShares(4), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 292.49), hasGrossValue("EUR", 293.00), //
                        hasForexGrossValue("USD", 348.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-17T19:08"), hasShares(1), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1500.67), hasGrossValue("EUR", 1501.17), //
                        hasForexGrossValue("USD", 1785.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-23T16:07"), hasShares(1), //
                        hasSource("Transaktionsuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1370.61), hasGrossValue("EUR", 1370.11), //
                        hasForexGrossValue("USD", 1587.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("KYG9830T1067"), hasWkn(null), hasTicker(null), //
                        hasName("XIAOMI CORP. CL.B"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("TESLA MOTORS INC. - C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9047847093"), hasWkn(null), hasTicker(null), //
                        hasName("UNILEVER NV COMMON STO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9047677045"), hasWkn(null), hasTicker(null), //
                        hasName("UNILEVER PLC COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A289V94"), hasWkn(null), hasTicker(null), //
                        hasName("HAMBORNER REIT AG - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A289WQ9"), hasWkn(null), hasTicker(null), //
                        hasName("INSTONE REAL ESTATE GROUP AG - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELL PLC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SHL1006"), hasWkn(null), hasTicker(null), //
                        hasName("SIEMENS HEALTHINEERS AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7835132033"), hasWkn(null), hasTicker(null), //
                        hasName("RYANAIR HOLDINGS PLC-SP ADR"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransaktionsuebersicht09()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US2546871060"), hasWkn(null), hasTicker(null), //
                        hasName("WALT DISNEY COMPANY (T"), //
                        hasCurrencyCode("USD"))));

        // check 1st sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-07T16:54"), hasShares(2), //
                        hasSource("Transaktionsuebersicht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 289.64), hasGrossValue("EUR", 290.15), //
                        hasForexGrossValue("USD", 356.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 2nd buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-07T09:00"), hasShares(20), //
                        hasSource("Transaktionsuebersicht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 197.00), hasGrossValue("EUR", 194.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check 3rd sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-07T09:00"), hasShares(15), //
                        hasSource("Transaktionsuebersicht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 230.34), hasGrossValue("EUR", 228.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check 4rd sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-07T09:00"), hasShares(25), //
                        hasSource("Transaktionsuebersicht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 973.16), hasGrossValue("EUR", 972.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.63))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000KSAG888"), hasWkn(null), hasTicker(null), //
                        hasName("K&S AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005557508"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE TELEKOM AG"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testTransaktionsuebersicht10()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DV0VC30"), hasWkn(null), hasTicker(null), //
                        hasName("CALL 16.12.22 LEONI 18"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-11T15:41"), hasShares(79), //
                        hasSource("Transaktionsuebersicht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 251.12), hasGrossValue("EUR", 248.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.27))));

        // check 2nd buy transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-11T15:30"), hasShares(34), //
                        hasSource("Transaktionsuebersicht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 508.26), hasGrossValue("EUR", 508.87), //
                        hasForexGrossValue("USD", 618.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.61))));

        // check 3rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-11T15:30"), hasShares(2), //
                        hasSource("Transaktionsuebersicht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 120.05), hasGrossValue("EUR", 120.06), //
                        hasForexGrossValue("USD", 146.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));

        // check 4rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-11T15:30"), hasShares(5), //
                        hasSource("Transaktionsuebersicht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 299.64), hasGrossValue("EUR", 300.16), //
                        hasForexGrossValue("USD", 365.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US05368M1062"), hasWkn(null), hasTicker(null), //
                        hasName("AVID BIOSERVICES INC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US69343T1079"), hasWkn(null), hasTicker(null), //
                        hasName("PJT PARTNERS INC. CLAS"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransaktionsuebersicht11()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0TGJ55"), hasWkn(null), hasTicker(null), //
                        hasName("VARTA AG"), //
                        hasCurrencyCode("EUR"))));

        // check 1st sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-21T10:33"), hasShares(11), //
                        hasSource("Transaktionsuebersicht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1225.38), hasGrossValue("EUR", 1227.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.22))));
    }

    @Test
    public void testTransaktionsuebersicht12()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0TGJ55"), hasWkn(null), hasTicker(null), //
                        hasName("VARTA AG"), //
                        hasCurrencyCode("EUR"))));

        // check 1st sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-21T10:33"), hasShares(11), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1225.38), hasGrossValue("EUR", 1227.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.22))));

        // check 2nd buy transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-13T13:37"), hasShares(120), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1571.88), hasGrossValue("EUR", 1574.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.28))));

        // check 3rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-13T13:36"), hasShares(22), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 819.55), hasGrossValue("EUR", 821.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.15))));

        // check 4rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-09T15:34"), hasShares(7), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1261.66), hasGrossValue("EUR", 1262.18), //
                        hasForexGrossValue("USD", 1505.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 5rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-09T13:16"), hasShares(11), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1999.64), hasGrossValue("EUR", 2002.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.36))));

        // check 6rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-09T09:06"), hasShares(10), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1907.86), hasGrossValue("EUR", 1910.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.34))));

        // check 7rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-22T16:35"), hasShares(40), //
                        hasSource("Transaktionsuebersicht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1053.69), hasGrossValue("EUR", 1054.33), //
                        hasForexGrossValue("USD", 1248.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.64))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELL PLC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005785604"), hasWkn(null), hasTicker(null), //
                        hasName("FRESENIUS SE & CO KGAA"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92826C8394"), hasWkn(null), hasTicker(null), //
                        hasName("VISA INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("ALLIANZ SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("MICROSOFT CORP"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5007541064"), hasWkn(null), hasTicker(null), //
                        hasName("THE KRAFT HEINZ COMPAN"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransaktionsuebersicht13()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht13.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(9L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4627261005"), hasWkn(null), hasTicker(null), //
                        hasName("IROBOT CORPORATION - C"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-03-16T20:45"), hasShares(9), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 511.82), hasGrossValue("EUR", 511.29), //
                        hasForexGrossValue("USD", 628.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.53))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-03-13"), hasShares(175), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 644.00), hasGrossValue("EUR", 644.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-03-16T10:40"), hasShares(161), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 500.75), hasGrossValue("EUR", 500.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.04))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-03-16T10:40"), hasShares(14), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 41.89), hasGrossValue("EUR", 43.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-03-13"), hasShares(350), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 644.00), hasGrossValue("EUR", 644.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-03-02"), hasShares(2500), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 72.50), hasGrossValue("EUR", 72.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-03-02"), hasShares(250), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 72.50), hasGrossValue("EUR", 72.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-03-01T11:26"), hasShares(350), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 824.57), hasGrossValue("EUR", 822.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.07))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-03-01T11:23"), hasShares(56), //
                        hasSource("Transaktionsuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 802.94), hasGrossValue("EUR", 805.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.06))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A2G8Y89"), hasWkn(null), hasTicker(null), //
                        hasName("BAUMOT GROUP AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A2DAM11"), hasWkn(null), hasTicker(null), //
                        hasName("BAUMOT GROUP AG O.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA65542J1066"), hasWkn(null), hasTicker(null), //
                        hasName("NORAM VENTURES INC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA65542J2056"), hasWkn(null), hasTicker(null), //
                        hasName("NORAM VENTURES INC."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005664809"), hasWkn(null), hasTicker(null), //
                        hasName("EVOTEC SE"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testTransaktionsuebersicht14()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005557508"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE TELEKOM AG"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-08-01T17:21"), hasShares(50), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 782.56), hasGrossValue("EUR", 780.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.06))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-08-01T16:32"), hasShares(72), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 682.45), hasGrossValue("EUR", 680.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.05))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-08-01T16:15"), hasShares(29), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1452.82), hasGrossValue("EUR", 1453.42), //
                        hasForexGrossValue("USD", 1715.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.60))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-30T15:43"), hasShares(29), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1465.90), hasGrossValue("EUR", 1465.30), //
                        hasForexGrossValue("USD", 1672.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.60))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-06-30T11:37"), hasShares(13), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1469.48), hasGrossValue("EUR", 1471.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.12))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-05-18T11:03"), hasShares(9), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 476.61), hasGrossValue("EUR", 474.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-05-18T10:52"), hasShares(66), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 470.52), hasGrossValue("EUR", 472.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-05-05T16:33"), hasShares(66), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 467.01), hasGrossValue("EUR", 464.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-05-05T15:11"), hasShares(87), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 446.01), hasGrossValue("EUR", 448.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-02-08T11:14"), hasShares(87), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 490.55), hasGrossValue("EUR", 488.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-02-08T10:56"), hasShares(25), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 481.71), hasGrossValue("EUR", 483.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-01-24T16:41"), hasShares(25), //
                        hasSource("Transaktionsuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 493.29), hasGrossValue("EUR", 491.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007257503"), hasWkn(null), hasTicker(null), //
                        hasName("CECONOMY AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn(null), hasTicker(null), //
                        hasName("NIKE INC. COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BAY0017"), hasWkn(null), hasTicker(null), //
                        hasName("BAYER AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn(null), hasTicker(null), //
                        hasName("WIRECARD AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ENAG999"), hasWkn(null), hasTicker(null), //
                        hasName("E.ON SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CNE100000296"), hasWkn(null), hasTicker(null), //
                        hasName("BYD CO. LTD H        YC 1"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D6554"), hasWkn(null), hasTicker(null), //
                        hasName("NORDEX SE"), //
                        hasCurrencyCode("EUR"))));
    }

    @Test
    public void testTransaktionsuebersicht15()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3696043013"), hasWkn(null), hasTicker(null), //
                        hasName("GENERAL ELECTRIC COMPANY COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(2), //
                        hasSource("Transaktionsuebersicht15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 175.97), hasGrossValue("EUR", 175.97), //
                        hasForexGrossValue("USD", 207.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-08-02"), hasShares(20), //
                        hasSource("Transaktionsuebersicht15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 219.97), hasGrossValue("EUR", 219.97), //
                        hasForexGrossValue("USD", 259.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3696041033"), hasWkn(null), hasTicker(null), //
                        hasName("GENERAL ELECTRIC COMPA"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransaktionsuebersicht16()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht16.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(186L));
        assertThat(countBuySell(results), is(451L));
        assertThat(countAccountTransactions(results), is(20L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(657));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("PLOPTTC00011"), hasWkn(null), hasTicker(null), //
                        hasName("CD PROJEKT RED SA"), //
                        hasCurrencyCode("PLN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US2786421030"), hasWkn(null), hasTicker(null), //
                        hasName("EBAY INC"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-12T15:34"), hasShares(16), //
                        hasSource("Transaktionsuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 691.80), hasGrossValue("EUR", 697.92), //
                        hasForexGrossValue("PLN", 3200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.12))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-08T16:07"), hasShares(8), //
                        hasSource("Transaktionsuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 517.91), hasGrossValue("EUR", 520.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.09))));

        // check 455th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-27T09:48"), hasShares(30), //
                        hasSource("Transaktionsuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 74.24), hasGrossValue("EUR", 70.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.04))));
    }

    @Test
    public void testTransaktionsuebersicht17()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht17.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(20L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US72919P2020"), hasWkn(null), hasTicker(null), //
                        hasName("PLUG POWER INC. - COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541101050"), hasWkn(null), hasTicker(null), //
                        hasName("NIKOLA CORP"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US62914V1061"), hasWkn(null), hasTicker(null), //
                        hasName("NIO INC - ADR"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA0585861085"), hasWkn(null), hasTicker(null), //
                        hasName("BALLARD POWER SYSTEMS"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("TESLA MOTORS INC. - C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker(null), //
                        hasName("ALPHABET INC. - CLASS A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00724F1012"), hasWkn(null), hasTicker(null), //
                        hasName("ADOBE SYSTEMS INCORPOR"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US79466L3024"), hasWkn(null), hasTicker(null), //
                        hasName("SALESFORCE.COM INC COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008232125"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE LUFTHANSA AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007664039"), hasWkn(null), hasTicker(null), //
                        hasName("VOLKSWAGEN AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9311421039"), hasWkn(null), hasTicker(null), //
                        hasName("WAL-MART STORES INC."), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-26T17:55"), hasShares(50), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 613.90), hasGrossValue("CHF", 613.18), //
                        hasForexGrossValue("USD", 675.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.72))));

        // check 6th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-14T21:13"), hasShares(2), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2965.31), hasGrossValue("CHF", 2964.76), //
                        hasForexGrossValue("USD", 3260.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check 19th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T10:29"), hasShares(7), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1006.58), hasGrossValue("CHF", 1001.76), //
                        hasForexGrossValue("EUR", 927.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.82))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24T17:22"), hasShares(15), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 517.84), hasGrossValue("CHF", 518.43), //
                        hasForexGrossValue("USD", 570.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.59))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-21T21:50"), hasShares(30), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 352.60), hasGrossValue("CHF", 353.25), //
                        hasForexGrossValue("USD", 388.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.65))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-19T20:15"), hasShares(30), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 387.62), hasGrossValue("CHF", 386.97), //
                        hasForexGrossValue("USD", 423.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.65))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-17T18:34"), hasShares(25), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 338.75), hasGrossValue("CHF", 339.38), //
                        hasForexGrossValue("USD", 375.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.63))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-13T20:28"), hasShares(2), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2940.94), hasGrossValue("CHF", 2941.49), //
                        hasForexGrossValue("USD", 3230.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-13T20:19"), hasShares(30), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 307.88), hasGrossValue("CHF", 307.23), //
                        hasForexGrossValue("USD", 337.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.65))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-13T20:11"), hasShares(2), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2957.87), hasGrossValue("CHF", 2957.32), //
                        hasForexGrossValue("USD", 3250.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-13T19:46"), hasShares(2), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2951.90), hasGrossValue("CHF", 2952.45), //
                        hasForexGrossValue("USD", 3251.42), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-11T18:55"), hasShares(15), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 601.73), hasGrossValue("CHF", 601.14), //
                        hasForexGrossValue("USD", 656.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.59))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T21:14"), hasShares(25), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 336.38), hasGrossValue("CHF", 335.75), //
                        hasForexGrossValue("USD", 367.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.63))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T19:46"), hasShares(1), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1366.31), hasGrossValue("CHF", 1365.77), //
                        hasForexGrossValue("USD", 1495.01), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.54))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T19:22"), hasShares(20), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 242.94), hasGrossValue("CHF", 242.33), //
                        hasForexGrossValue("USD", 265.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.61))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T19:12"), hasShares(2), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2624.05), hasGrossValue("CHF", 2623.50), //
                        hasForexGrossValue("USD", 2870.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T19:04"), hasShares(2), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 821.61), hasGrossValue("CHF", 821.06), //
                        hasForexGrossValue("USD", 898.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T16:20"), hasShares(5), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 933.87), hasGrossValue("CHF", 933.31), //
                        hasForexGrossValue("USD", 1020.05), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.56))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T10:34"), hasShares(60), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 507.00), hasGrossValue("CHF", 502.42), //
                        hasForexGrossValue("EUR", 465.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.58))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-06T18:54"), hasShares(5), //
                        hasSource("Transaktionsuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 588.21), hasGrossValue("CHF", 587.65), //
                        hasForexGrossValue("USD", 645.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.56))));
    }

    @Test
    public void testTransaktionsuebersicht18()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht18.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3696043013"), hasWkn(null), hasTicker(null), //
                        hasName("GENERAL ELECTRIC COMPANY COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3696041033"), hasWkn(null), hasTicker(null), //
                        hasName("GENERAL ELECTRIC COMPA"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(2), //
                        hasSource("Transaktionsuebersicht18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 175.97), hasGrossValue("EUR", 175.97), //
                        hasForexGrossValue("USD", 207.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-08-02"), hasShares(20), //
                        hasSource("Transaktionsuebersicht18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 219.97), hasGrossValue("EUR", 219.97), //
                        hasForexGrossValue("USD", 259.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTransaktionsuebersicht19()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht19.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3D14"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES MSCI EUROPE ESG SCREENED UCITS ETF EUR ACC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B52VJ196"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES DJ EUROPE SUSTAINABILITY (BLACKROCK ASSET MA..."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5KQNG97"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC SP 500 ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681040223"), hasWkn(null), hasTicker(null), //
                        hasName("AMUNDI ETF STOX600"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("KYG9830T1067"), hasWkn(null), hasTicker(null), //
                        hasName("XIAOMI CORP. CL.B"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US36467W1099"), hasWkn(null), hasTicker(null), //
                        hasName("GAMESTOP CORPORATION C"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-06T09:06"), hasShares(40), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 290.78), hasGrossValue("CHF", 290.78), //
                        hasForexGrossValue("EUR", 270.24), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-25T13:42"), hasShares(4), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 272.37), hasGrossValue("CHF", 270.14), //
                        hasForexGrossValue("EUR", 251.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.23))));

        // check 3rd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-20T09:22"), hasShares(35), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 262.01), hasGrossValue("CHF", 262.01), //
                        hasForexGrossValue("EUR", 244.62), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 4th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-19T09:05"), hasShares(36), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1453.29), hasGrossValue("CHF", 1453.29), //
                        hasForexGrossValue("EUR", 1356.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 5th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-19T09:05"), hasShares(14), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 565.17), hasGrossValue("CHF", 565.17), //
                        hasForexGrossValue("EUR", 527.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 6th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-22T13:26"), hasShares(170), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1256.66), hasGrossValue("CHF", 1256.66), //
                        hasForexGrossValue("EUR", 1161.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 7th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-22T10:18"), hasShares(20), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2234.69), hasGrossValue("CHF", 2234.69), //
                        hasForexGrossValue("EUR", 2064.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 8th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-18T12:46"), hasShares(28), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1097.86), hasGrossValue("CHF", 1097.86), //
                        hasForexGrossValue("EUR", 1002.79), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 9th buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-26T17:41"), hasShares(300), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 989.88), hasGrossValue("CHF", 999.01), //
                        hasForexGrossValue("EUR", 913.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 9.13))));

        // check 10th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-05T15:42"), hasShares(33), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1168.89), hasGrossValue("CHF", 1168.89), //
                        hasForexGrossValue("EUR", 1056.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 11th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-05T15:34"), hasShares(45), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 303.44), hasGrossValue("CHF", 301.13), //
                        hasForexGrossValue("EUR", 272.07), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.31))));

        // check 12th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-12T14:12"), hasShares(9), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1087.01), hasGrossValue("CHF", 1082.14), //
                        hasForexGrossValue("EUR", 1001.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.87))));

        // check 13th buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-29T19:31"), hasShares(1), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 275.33), hasGrossValue("CHF", 275.87), //
                        hasForexGrossValue("USD", 310.32), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.54))));

        // check 14th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-29T15:30"), hasShares(1), //
                        hasSource("Transaktionsuebersicht19.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 338.00), hasGrossValue("CHF", 337.46), //
                        hasForexGrossValue("USD", 380.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.54))));
    }

    @Test
    public void testTransaktionsuebersicht20()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht20.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(18L));
        assertThat(countBuySell(results), is(61L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(79));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B52VJ196"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES DJ EUROPE SUSTAINABILITY (BLACKROCK ASSET MA..."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681040223"), hasWkn(null), hasTicker(null), //
                        hasName("AMUNDI ETF STOX600"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3D14"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES MSCI EUROPE ESG SCREENED UCITS ETF EUR ACC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5KQNG97"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC SP 500 ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("KYG9830T1067"), hasWkn(null), hasTicker(null), //
                        hasName("XIAOMI CORP. CL.B"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US36467W1099"), hasWkn(null), hasTicker(null), //
                        hasName("GAMESTOP CORPORATION C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0000235190"), hasWkn(null), hasTicker(null), //
                        hasName("AIRBUS SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007037129"), hasWkn(null), hasTicker(null), //
                        hasName("RWE AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B88DZ566"), hasWkn(null), hasTicker(null), //
                        hasName("ETF ISHARES S&P 500 CHF HEDGED (ISHARES)"), //
                        hasCurrencyCode("CHF"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US81762P1021"), hasWkn(null), hasTicker(null), //
                        hasName("SERVICENOW INC. COMMO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4BNMY34"), hasWkn(null), hasTicker(null), //
                        hasName("ACCENTURE PLC. CLASS A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0832435464"), hasWkn(null), hasTicker(null), //
                        hasName("LYXOR UCITS ETF S&P500 VIX FU EN ROL LUX"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn(null), hasTicker(null), //
                        hasName("WIRECARD AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("ALLIANZ SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0237935652"), hasWkn(null), hasTicker(null), //
                        hasName("CSAM ISHARES SPI (CH) (CREDIT SUISSE ASSET MANAGEMENT)"), //
                        hasCurrencyCode("CHF"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46429B6974"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES MSCI USA MINIM"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-29T16:46"), hasShares(6), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 390.17), hasGrossValue("CHF", 387.96), //
                        hasForexGrossValue("EUR", 372.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.21))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-29T16:45"), hasShares(12), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1328.58), hasGrossValue("CHF", 1328.58), //
                        hasForexGrossValue("EUR", 1275.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 3rd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-06T09:06"), hasShares(40), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 290.78), hasGrossValue("CHF", 290.78), //
                        hasForexGrossValue("EUR", 270.24), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 4th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-25T13:42"), hasShares(4), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 272.37), hasGrossValue("CHF", 270.14), //
                        hasForexGrossValue("EUR", 251.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.23))));

        // check 5th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-20T09:22"), hasShares(35), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 262.01), hasGrossValue("CHF", 262.01), //
                        hasForexGrossValue("EUR", 244.62), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 6th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-19T09:05"), hasShares(36), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1453.29), hasGrossValue("CHF", 1453.29), //
                        hasForexGrossValue("EUR", 1356.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 7th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-19T09:05"), hasShares(14), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 565.17), hasGrossValue("CHF", 565.17), //
                        hasForexGrossValue("EUR", 527.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 8th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-22T13:26"), hasShares(170), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1256.66), hasGrossValue("CHF", 1256.66), //
                        hasForexGrossValue("EUR", 1161.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 9th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-22T10:18"), hasShares(20), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 2234.69), hasGrossValue("CHF", 2234.69), //
                        hasForexGrossValue("EUR", 2064.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 23th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-26T09:26"), hasShares(20), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1162.29), hasGrossValue("CHF", 1159.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.49))));

        // check 29th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-04T09:04"), hasShares(52), //
                        hasSource("Transaktionsuebersicht20.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 300.47), hasGrossValue("CHF", 298.22), //
                        hasForexGrossValue("EUR", 276.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.25))));
    }

    @Test
    public void testTransaktionsuebersicht21()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn(null), hasTicker(null), //
                        hasName("WIRECARD AG"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES DOW JONES U.S. SELECT DIVIDEND UCITS (DE) ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C5F3ZF0"), hasWkn(null), hasTicker(null), //
                        hasName("ODX1 C12700.00 05JUN20"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C5F3ZG8"), hasWkn(null), hasTicker(null), //
                        hasName("ODX1 P12700.00 05JUN20"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-07T20:36"), hasShares(90.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 97.92), hasGrossValue("EUR", 97.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE0007472060")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-07T20:36"), hasShares(90.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 97.92), hasGrossValue("EUR", 97.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE0007472060")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-24T16:26"), hasShares(30.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 347.06), hasGrossValue("EUR", 345.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.06), //
                        hasSecurity(hasIsin("DE0007472060")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-19T11:24"), hasShares(20.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 442.08), hasGrossValue("EUR", 440.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.08), //
                        hasSecurity(hasIsin("DE0007472060")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-18T14:04"), hasShares(10.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 452.08), hasGrossValue("EUR", 450.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.08), //
                        hasSecurity(hasIsin("DE0007472060")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-18T10:57"), hasShares(18.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1074.27), hasGrossValue("EUR", 1072.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.19), //
                        hasSecurity(hasIsin("DE0007472060")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-10T16:18"), hasShares(4.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 222.07), hasGrossValue("EUR", 220.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.07), //
                        hasSecurity(hasIsin("DE000A0D8Q49")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-06-05T13:30"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000C5F3ZF0")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-05T11:32"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 499.25), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75), //
                        hasSecurity(hasIsin("DE000C5F3ZG8")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-05T10:43"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 150.75), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75), //
                        hasSecurity(hasIsin("DE000C5F3ZG8")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-05T10:22"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 150.75), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75), //
                        hasSecurity(hasIsin("DE000C5F3ZF0")))));
    }

    @Test
    public void testTransaktionsuebersicht22()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(11L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("TESLA INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C34JCK6"), hasWkn(null), hasTicker(null), //
                        hasName("ODX1 P12300.00 03MAY19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3311N4"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 C12300.00 26APR19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3311K0"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 P12200.00 26APR19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3311P9"), hasWkn(null), hasTicker(null), //
                        hasName("ODX4 P12300.00 26APR19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C34JCJ8"), hasWkn(null), hasTicker(null), //
                        hasName("ODX1 C12300.00 03MAY19"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-04-29T16:11"), hasShares(3.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 645.18), hasGrossValue("EUR", 645.69), //
                        hasForexGrossValue("USD", 720.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-29T09:16"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 250.90), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C34JCK6")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T20:23"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 210.21), hasGrossValue("EUR", 209.71), //
                        hasForexGrossValue("USD", 234.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T17:52"), hasShares(2.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 430.77), hasGrossValue("EUR", 430.26), //
                        hasForexGrossValue("USD", 480.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2019-04-26T13:39"), hasShares(5.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 89.00), hasGrossValue("EUR", 89.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000C3311N4")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2019-04-26T13:39"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000C3311K0")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2019-04-26T13:39"), hasShares(4.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000C3311P9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T13:01"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 375.90), hasGrossValue("EUR", 375.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C34JCK6")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T13:01"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 375.90), hasGrossValue("EUR", 375.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C34JCJ8")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T12:30"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.90), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C3311P9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T10:40"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.90), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C3311N4")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T10:00"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 100.90), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C3311P9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-26T09:11"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.90), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90), //
                        hasSecurity(hasIsin("DE000C3311N4")))));
    }

    @Test
    public void testTransaktionsuebersicht23()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(9L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0213691035"), hasWkn(null), hasTicker(null), //
                        hasName("ALTAIR ENGINEERING INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE0008232125"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE LUFTHANSA AG"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE0002635307"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES STOXX EUROPE 600 UCITS ETF (DE)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0S9GB0"), hasWkn(null), hasTicker(null), //
                        hasName("DEUTSCHE BOERSE COMMODITIES GMBH"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000TUAG1E4"), hasWkn(null), hasTicker(null), //
                        hasName("TUI AG - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000TUAG505"), hasWkn(null), hasTicker(null), //
                        hasName("TUI AG"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000TUAG000"), hasWkn(null), hasTicker(null), //
                        hasName("TUI AG"), //
                        hasCurrencyCode("EUR"))));

        // check transactions
        assertThat(results, hasItem(sale( //
                        hasDate("2023-08-04T17:28"), hasShares(9.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 523.55), hasGrossValue("EUR", 525.55), //
                        hasForexGrossValue("USD", 580.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00), //
                        hasSecurity(hasIsin("US0213691035")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-12T09:30"), hasShares(80.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 692.78), hasGrossValue("EUR", 697.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.90), //
                        hasSecurity(hasIsin("DE0008232125")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-22T09:04"), hasShares(35.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1571.20), hasGrossValue("EUR", 1572.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00), //
                        hasSecurity(hasIsin("DE0002635307")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-20T15:37"), hasShares(58.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3303.00), hasGrossValue("EUR", 3306.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("DE000A0S9GB0")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-04-26T09:25"), hasShares(104.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 577.20), hasGrossValue("EUR", 577.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG1E4")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-26T09:25"), hasShares(104.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 577.20), hasGrossValue("EUR", 577.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG505")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2023-04-20T11:33"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG1E4")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2023-04-13T00:00"), hasShares(39.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG1E4")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-13T00:00"), hasShares(104.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 577.20), hasGrossValue("EUR", 577.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG1E4")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2023-03-28T08:53"), hasShares(40.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG1E4")))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2023-03-28T08:53"), hasShares(40.00), //
                                        hasSource("Transaktionsuebersicht23.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        hasSecurity(hasIsin("DE000TUAG1E4"))))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2023-03-28T00:00"), hasShares(40.00), //
                                        hasSource("Transaktionsuebersicht23.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        hasSecurity(hasIsin("DE000TUAG1E4"))))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-02-24T00:00"), hasShares(400.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 743.20), hasGrossValue("EUR", 743.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG000")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-24T00:00"), hasShares(40.00), //
                        hasSource("Transaktionsuebersicht23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 743.20), hasGrossValue("EUR", 743.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("DE000TUAG505")))));
    }

    @Test
    public void testTransaktionsuebersicht24()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000HY30YW6"), hasWkn(null), hasTicker(null), //
                        hasName("XTRACKERS S&P 500 SWAP II UCITS 1C ETF"), //
                        hasCurrencyCode("CHF"))));

        // check transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-17T09:41"), hasShares(725.00), //
                        hasSource("Transaktionsuebersicht24.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 25211.07), hasGrossValue("CHF", 25208.25), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.82), //
                        hasSecurity(hasIsin("IE000HY30YW6")))));
    }

    @Test
    public void testTransaktionsuebersicht25()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL- WORLD UCITS ETF - (USD) ACCUMULATING"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00B8W67662"), hasWkn(null), hasTicker(null), //
                        hasName("LIBERTY GLOBAL PLC - C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0244767585"), hasWkn(null), hasTicker(null), //
                        hasName("UBS GROUP AG"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0012138530"), hasWkn(null), hasTicker(null), //
                        hasName("CREDIT SUISSE GROUP"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin("SE0000806994"), hasWkn(null), hasTicker(null), //
                        hasName("JM AB"), //
                        hasCurrencyCode("SEK"))));

        // check transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-17T09:50"), hasShares(5.00), //
                        hasSource("Transaktionsuebersicht25.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 479.51), hasGrossValue("CHF", 478.55), //
                        hasForexGrossValue("EUR", 500.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.96), //
                        hasSecurity(hasIsin("IE00BK5BQT80")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-20T09:04"), hasShares(3.00), //
                        hasSource("Transaktionsuebersicht25.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 291.80), hasGrossValue("CHF", 290.84), //
                        hasForexGrossValue("EUR", 302.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.96), //
                        hasSecurity(hasIsin("IE00BK5BQT80")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-28T16:45"), hasShares(12.00), //
                        hasSource("Transaktionsuebersicht25.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 183.45), hasGrossValue("CHF", 181.49), //
                        hasForexGrossValue("USD", 202.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.96), //
                        hasSecurity(hasIsin("GB00B8W67662")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-13T00:00"), hasShares(2.00), //
                        hasSource("Transaktionsuebersicht25.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 36.73), hasGrossValue("CHF", 36.73), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("CH0244767585")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-13T00:00"), hasShares(50.00), //
                        hasSource("Transaktionsuebersicht25.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 40.85), hasGrossValue("CHF", 40.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("CH0012138530")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-09T13:03"), hasShares(8.00), //
                        hasSource("Transaktionsuebersicht25.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 100.57), hasGrossValue("CHF", 95.80), //
                        hasForexGrossValue("SEK", 1152.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.77), //
                        hasSecurity(hasIsin("SE0000806994")))));
    }

    @Test
    public void testTransaktionsuebersicht26()
    {
        // @formatter:off
        // Synthetic test case derived from Transaktionsuebersicht24.txt: the Swiss
        // document uses the apostrophe as group separator for the shares as well.
        // @formatter:on
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht26_synthetic.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000HY30YW6"), hasWkn(null), hasTicker(null), //
                        hasName("XTRACKERS S&P 500 SWAP II UCITS 1C ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-17T09:41"), hasShares(1450.00), //
                        hasSource("Transaktionsuebersicht26_synthetic.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 50419.32), hasGrossValue("CHF", 50416.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.82), //
                        hasSecurity(hasIsin("IE000HY30YW6")))));
    }

    @Test
    public void testTransacties01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacties01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5801351017"), hasWkn(null), hasTicker(null), //
                        hasName("MCDONALD'S CORPORATION"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE AW"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-09T15:30"), hasShares(4), //
                        hasSource("Transacties01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 757.83), hasGrossValue("EUR", 757.32), //
                        hasForexGrossValue("USD", 847.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-09T14:08"), hasShares(3), //
                        hasSource("Transacties01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 231.30), hasGrossValue("EUR", 231.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-05T20:52"), hasShares(20), //
                        hasSource("Transacties01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 564.36), hasGrossValue("EUR", 563.79), //
                        hasForexGrossValue("USD", 632), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.57))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46284V1017"), hasWkn(null), hasTicker(null), //
                        hasName("IRON MOUNTAIN INCORPOR"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransacciones01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US55616P1049"), hasWkn(null), hasTicker(null), //
                        hasName("MACYS INC COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        // check first transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-27T16:27"), hasShares(30), //
                        hasSource("Transacciones01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 269.10), hasGrossValue("EUR", 268.50), //
                        hasForexGrossValue("USD", 320.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.60))));

        // check 2nd transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27T15:49"), hasShares(55), //
                        hasSource("Transacciones01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 606.04), hasGrossValue("EUR", 606.72), //
                        hasForexGrossValue("USD", 726.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.68))));

        // check 3rd transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-09T13:07"), hasShares(247), //
                        hasSource("Transacciones01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 486.18), hasGrossValue("EUR", 488.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.24))));

        // check 4th transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-17T17:15"), hasShares(3), //
                        hasSource("Transacciones01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 416.00), hasGrossValue("EUR", 416.51), //
                        hasForexGrossValue("USD", 458.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US90069K1043"), hasWkn(null), hasTicker(null), //
                        hasName("TUSCAN HOLDINGS CORP. - COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("ES0113900J37"), hasWkn(null), hasTicker(null), //
                        hasName("SANTANDER"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US30303M1027"), hasWkn(null), hasTicker(null), //
                        hasName("FACEBOOK INC. - CLASS"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransacciones02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-27T20:55"), hasShares(9), //
                        hasSource("Transacciones02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1054.32), hasGrossValue("EUR", 1053.79), //
                        hasForexGrossValue("USD", 1275.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.53))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-27T20:54"), hasShares(48), //
                        hasSource("Transacciones02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1107.68), hasGrossValue("EUR", 1108.34), //
                        hasForexGrossValue("USD", 1344.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.66))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541101050"), hasWkn(null), hasTicker(null), //
                        hasName("NIKOLA CORP"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransacciones03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5504241051"), hasWkn(null), hasTicker(null), //
                        hasName("LUMINAR TECHNOLOGIES INC. - CLASS A COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check 1st sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-03T21:57"), hasShares(19), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 508.51), hasGrossValue("EUR", 507.95), //
                        hasForexGrossValue("USD", 611.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.56))));

        // check 2nd buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-03T21:55"), hasShares(20), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 526.87), hasGrossValue("EUR", 526.30), //
                        hasForexGrossValue("USD", 633.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.57))));

        // check 3rd sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-03T21:28"), hasShares(14), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 416.66), hasGrossValue("EUR", 416.71), //
                        hasForexGrossValue("USD", 502.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 4th sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-03T21:28"), hasShares(2), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 59.54), hasGrossValue("EUR", 59.55), //
                        hasForexGrossValue("USD", 71.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));

        // check 5th sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-03T21:28"), hasShares(1), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.27), hasGrossValue("EUR", 29.77), //
                        hasForexGrossValue("USD", 35.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 6th sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-03T21:18"), hasShares(20), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 780.01), hasGrossValue("EUR", 780.08), //
                        hasForexGrossValue("USD", 940.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.07))));

        // check 7th sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-03T21:18"), hasShares(1), //
                        hasSource("Transacciones03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 39.00), hasGrossValue("EUR", 39.00), //
                        hasForexGrossValue("USD", 47.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US69608A1088"), hasWkn(null), hasTicker(null), //
                        hasName("PALANTIR TECHNOLOGIES INC-A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US98138J2069"), hasWkn(null), hasTicker(null), //
                        hasName("WORKHORSE GROUP INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US91688F1049"), hasWkn(null), hasTicker(null), //
                        hasName("UPWORK INC. CMN"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransacciones04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA38217M1005"), hasWkn(null), hasTicker(null), //
                        hasName("GOODFOOD MARKET CORP"), //
                        hasCurrencyCode("CAD"))));

        // check 1st sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-26T15:35"), hasShares(100), //
                        hasSource("Transacciones04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 549.34), hasGrossValue("EUR", 546.67), //
                        hasForexGrossValue("CAD", 820.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.67))));
    }

    @Test
    public void testTransactions_english01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_english01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(12L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6293775085"), hasWkn(null), hasTicker(null), //
                        hasName("NRG ENERGY INC. COMMO"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02T17:39"), hasShares(25), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 973.16), hasGrossValue("CHF", 972.53), //
                        hasForexGrossValue("USD", 1082.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.63))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02T16:14"), hasShares(10), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 721.99), hasGrossValue("CHF", 721.95), //
                        hasForexGrossValue("USD", 803.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.04))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02T15:47"), hasShares(4), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 978.49), hasGrossValue("CHF", 977.94), //
                        hasForexGrossValue("USD", 1087.76), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check 4th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02T14:11"), hasShares(11), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 944.02), hasGrossValue("CHF", 939.22), //
                        hasForexGrossValue("EUR", 869.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.80))));

        // check 5th sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-28T15:30"), hasShares(50), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 842.14), hasGrossValue("CHF", 842.86), //
                        hasForexGrossValue("USD", 950.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.72))));

        // check 6th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-30T10:09"), hasShares(15), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1005.96), hasGrossValue("CHF", 998.93), //
                        hasForexGrossValue("PLN", 4167.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 7.03))));

        // check 7th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-03T09:30"), hasShares(15), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1248.40), hasGrossValue("CHF", 1248.40), //
                        hasForexGrossValue("EUR", 1156.35), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 8th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-08T17:52"), hasShares(9), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 510.50), hasGrossValue("CHF", 509.94), //
                        hasForexGrossValue("USD", 525.78), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.56))));

        // check 9th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-08T17:34"), hasShares(15), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 202.85), hasGrossValue("CHF", 202.26), //
                        hasForexGrossValue("USD", 208.58), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.59))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-08T17:02"), hasShares(6), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 341.87), hasGrossValue("CHF", 341.32), //
                        hasForexGrossValue("USD", 352.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-08T16:59"), hasShares(4), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 574.33), hasGrossValue("CHF", 573.78), //
                        hasForexGrossValue("USD", 592.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.55))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-08T16:58"), hasShares(10), //
                        hasSource("Transactions_english01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 506.05), hasGrossValue("CHF", 505.48), //
                        hasForexGrossValue("USD", 522.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.57))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0298991011"), hasWkn(null), hasTicker(null), //
                        hasName("AMERICAN STATES WATER"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4370761029"), hasWkn(null), hasTicker(null), //
                        hasName("HOME DEPOT INC. (THE)"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000FTG1111"), hasWkn(null), hasTicker(null), //
                        hasName("FLATEXDEGIRO AG"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA09228F1036"), hasWkn(null), hasTicker(null), //
                        hasName("BLACKBERRY LTD"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("PLOPTTC00011"), hasWkn(null), hasTicker(null), //
                        hasName("CD PROJEKT RED SA"), //
                        hasCurrencyCode("PLN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE AW"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9182041080"), hasWkn(null), hasTicker(null), //
                        hasName("V.F. CORPORATION COMMO"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("PA1436583006"), hasWkn(null), hasTicker(null), //
                        hasName("CARNIVAL CORPORATION C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8288061091"), hasWkn(null), hasTicker(null), //
                        hasName("SIMON PROPERTY GROUP"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88579Y1010"), hasWkn(null), hasTicker(null), //
                        hasName("3M COMPANY COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8718291078"), hasWkn(null), hasTicker(null), //
                        hasName("SYSCO CORPORATION COMM"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testTransactions_english02()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_english02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(39L));
        assertThat(countBuySell(results), is(72L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(116));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("JE00B1VS3333"), hasWkn(null), hasTicker(null), //
                        hasName("WISDOMTREE PHYSICAL SILVER INDIVIDUAL SECURITIES ETC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US17888H1032"), hasWkn(null), hasTicker(null), //
                        hasName("CIVITAS RESOURCES INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US78454L1008"), hasWkn(null), hasTicker(null), //
                        hasName("SM ENERGY CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5322751042"), hasWkn(null), hasTicker(null), //
                        hasName("LIGHTWAVE LOGIC INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7731211089"), hasWkn(null), hasTicker(null), //
                        hasName("ROCKET LAB CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA29872L2066"), hasWkn(null), hasTicker(null), //
                        hasName("EURO SUN MINING INC"), //
                        hasCurrencyCode("CAD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88262P1021"), hasWkn(null), hasTicker(null), //
                        hasName("TEXAS PACIFIC LAND CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("AN8068571086"), hasWkn(null), hasTicker(null), //
                        hasName("SLB NV"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BMG9460G1015"), hasWkn(null), hasTicker(null), //
                        hasName("VALARIS LTD"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000009082"), hasWkn(null), hasTicker(null), //
                        hasName("KONINKLIJKE KPN NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US76655K1034"), hasWkn(null), hasTicker(null), //
                        hasName("RIGETTI COMPUTING INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US18539C2044"), hasWkn(null), hasTicker(null), //
                        hasName("CLEARWAY ENERGY INC CLASS C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRONNEMENT SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1270971039"), hasWkn(null), hasTicker(null), //
                        hasName("COTERRA ENERGY INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US20825C1045"), hasWkn(null), hasTicker(null), //
                        hasName("CONOCOPHILLIPS"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US03674X1063"), hasWkn(null), hasTicker(null), //
                        hasName("ANTERO RESOURCES CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000371243"), hasWkn(null), hasTicker(null), //
                        hasName("NEDAP NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BMG0112X1056"), hasWkn(null), hasTicker(null), //
                        hasName("AEGON LTD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US6516391066"), hasWkn(null), hasTicker(null), //
                        hasName("NEWMONT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BZ3CNK81"), hasWkn(null), hasTicker(null), //
                        hasName("TORM PLC CLASS A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0011540547"), hasWkn(null), hasTicker(null), //
                        hasName("ABN AMRO BANK NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000302636"), hasWkn(null), hasTicker(null), //
                        hasName("VAN LANSCHOT KEMPEN NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasWkn(null), hasTicker(null), //
                        hasName("ASML HOLDING NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NO0011082075"), hasWkn(null), hasTicker(null), //
                        hasName("HOEGH AUTOLINERS ASA"), //
                        hasCurrencyCode("NOK"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000289213"), hasWkn(null), hasTicker(null), //
                        hasName("WERELDHAVE NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BE0003816338"), hasWkn(null), hasTicker(null), //
                        hasName("CMB TECH NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000360618"), hasWkn(null), hasTicker(null), //
                        hasName("SBM OFFSHORE NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0010558797"), hasWkn(null), hasTicker(null), //
                        hasName("OCI NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000303709"), hasWkn(null), hasTicker(null), //
                        hasName("AEGON"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010478248"), hasWkn(null), hasTicker(null), //
                        hasName("ATARI - TD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0014008D33"), hasWkn(null), hasTicker(null), //
                        hasName("ATARI - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BP6MXD84"), hasWkn(null), hasTicker(null), //
                        hasName("SHELL PLC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELLA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0009739416"), hasWkn(null), hasTicker(null), //
                        hasName("POSTNL NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150003E1"), hasWkn(null), hasTicker(null), //
                        hasName("FUGRO NV CLASS C"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150004A7"), hasWkn(null), hasTicker(null), //
                        hasName("FUGRO"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150001Y3"), hasWkn(null), hasTicker(null), //
                        hasName("FUGRO RIGHTS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000352565"), hasWkn(null), hasTicker(null), //
                        hasName("FUGRO"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BE0003818359"), hasWkn(null), hasTicker(null), //
                        hasName("GALAPAGOS NV"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-02-04T10:32"), hasShares(30.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2054.28), hasGrossValue("EUR", 2051.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("JE00B1VS3333")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-30T00:00"), hasShares(40.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 928.84), hasGrossValue("EUR", 928.84), //
                        hasForexGrossValue("USD", 1095.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("US17888H1032")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-30T00:00"), hasShares(58.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 928.84), hasGrossValue("EUR", 928.84), //
                        hasForexGrossValue("USD", 1095.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("US78454L1008")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16T21:59"), hasShares(130.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 518.81), hasGrossValue("EUR", 515.52), //
                        hasForexGrossValue("USD", 598.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.29 + 2.00)), //
                        hasSecurity(hasIsin("US5322751042")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T21:28"), hasShares(6.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 432.52), hasGrossValue("EUR", 429.45), //
                        hasForexGrossValue("USD", 501.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.07 + 2.00)), //
                        hasSecurity(hasIsin("US7731211089")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T21:26"), hasShares(1500.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 357.91), hasGrossValue("EUR", 357.02), //
                        hasForexGrossValue("CAD", 577.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.89), //
                        hasSecurity(hasIsin("CA29872L2066")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T21:26"), hasShares(2000.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 477.21), hasGrossValue("EUR", 476.02), //
                        hasForexGrossValue("CAD", 770.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.19), //
                        hasSecurity(hasIsin("CA29872L2066")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T21:26"), hasShares(500.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 121.30), hasGrossValue("EUR", 119.00), //
                        hasForexGrossValue("CAD", 192.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.30 + 2.00)), //
                        hasSecurity(hasIsin("CA29872L2066")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-23T00:00"), hasShares(6.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1542.46), hasGrossValue("EUR", 1542.46), //
                        hasForexGrossValue("USD", 1816.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("US88262P1021")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-23T00:00"), hasShares(2.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1542.46), hasGrossValue("EUR", 1542.46), //
                        hasForexGrossValue("USD", 1816.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("US88262P1021")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-16T15:30"), hasShares(65.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2131.15), hasGrossValue("EUR", 2123.84), //
                        hasForexGrossValue("USD", 2502.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.31 + 2.00)), //
                        hasSecurity(hasIsin("AN8068571086")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-08T15:30"), hasShares(40.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2037.47), hasGrossValue("EUR", 2030.39), //
                        hasForexGrossValue("USD", 2365.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.08 + 2.00)), //
                        hasSecurity(hasIsin("BMG9460G1015")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-06T15:13"), hasShares(600.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2289.00), hasGrossValue("EUR", 2286.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000009082")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-06T15:12"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 384.10), hasGrossValue("EUR", 381.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000009082")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-16T18:33"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4131.98), hasGrossValue("EUR", 4144.34), //
                        hasForexGrossValue("USD", 4838.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (10.36 + 2.00)), //
                        hasSecurity(hasIsin("US76655K1034")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-29T16:23"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2420.05), hasGrossValue("EUR", 2412.02), //
                        hasForexGrossValue("USD", 2830.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (6.03 + 2.00)), //
                        hasSecurity(hasIsin("US18539C2044")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T20:30"), hasShares(2.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1827.58), hasGrossValue("EUR", 1821.03), //
                        hasForexGrossValue("USD", 2106.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.55 + 2.00)), //
                        hasSecurity(hasIsin("US88262P1021")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-13T15:30"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1008.34), hasGrossValue("EUR", 1003.83), //
                        hasForexGrossValue("USD", 1155.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (2.51 + 2.00)), //
                        hasSecurity(hasIsin("US76655K1034")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-28T13:45"), hasShares(75.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2383.90), hasGrossValue("EUR", 2379.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.90), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-31T15:30"), hasShares(75.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2053.31), hasGrossValue("EUR", 2046.19), //
                        hasForexGrossValue("USD", 2122.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.12 + 2.00)), //
                        hasSecurity(hasIsin("US1270971039")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-30T21:32"), hasShares(40.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1999.13), hasGrossValue("EUR", 1992.15), //
                        hasForexGrossValue("USD", 2078.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.98 + 2.00)), //
                        hasSecurity(hasIsin("US17888H1032")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-27T20:17"), hasShares(20.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1959.48), hasGrossValue("EUR", 1952.60), //
                        hasForexGrossValue("USD", 2046.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.88 + 2.00)), //
                        hasSecurity(hasIsin("US20825C1045")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-27T19:04"), hasShares(70.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2439.03), hasGrossValue("EUR", 2447.15), //
                        hasForexGrossValue("USD", 2566.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (6.12 + 2.00)), //
                        hasSecurity(hasIsin("US03674X1063")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-13T09:00"), hasShares(18.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 996.60), hasGrossValue("EUR", 993.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000371243")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-17T11:08"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1184.09), hasGrossValue("EUR", 1187.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("BMG0112X1056")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-22T17:56"), hasShares(11.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 461.24), hasGrossValue("EUR", 458.09), //
                        hasForexGrossValue("USD", 476.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.15 + 2.00)), //
                        hasSecurity(hasIsin("US6516391066")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-22T15:31"), hasShares(70.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2219.20), hasGrossValue("EUR", 2211.67), //
                        hasForexGrossValue("USD", 2303.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.53 + 2.00)), //
                        hasSecurity(hasIsin("US03674X1063")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-22T15:30"), hasShares(13.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 546.43), hasGrossValue("EUR", 543.07), //
                        hasForexGrossValue("USD", 565.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.36 + 2.00)), //
                        hasSecurity(hasIsin("US6516391066")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-15T21:32"), hasShares(75.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2073.86), hasGrossValue("EUR", 2066.69), //
                        hasForexGrossValue("USD", 2250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.17 + 2.00)), //
                        hasSecurity(hasIsin("GB00BZ3CNK81")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-10-10T09:32"), hasShares(155.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2446.00), hasGrossValue("EUR", 2449.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0011540547")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-10T09:14"), hasShares(60.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2643.00), hasGrossValue("EUR", 2640.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000302636")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-16T09:00"), hasShares(2.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1470.20), hasGrossValue("EUR", 1467.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-07T11:04"), hasShares(300.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2932.31), hasGrossValue("EUR", 2920.11), //
                        hasForexGrossValue("NOK", 33600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (7.30 + 4.90)), //
                        hasSecurity(hasIsin("NO0011082075")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-25T09:00"), hasShares(150.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2151.00), hasGrossValue("EUR", 2148.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000289213")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-03T10:10"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2379.45), hasGrossValue("EUR", 2379.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("BE0003816338")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-27T17:09"), hasShares(30.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2037.00), hasGrossValue("EUR", 2034.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000371243")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-11T12:49"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2354.80), hasGrossValue("EUR", 2354.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("BE0003816338")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-11T12:49"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2354.80), hasGrossValue("EUR", 2354.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("BE0003816338")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-28T09:07"), hasShares(91.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1178.45), hasGrossValue("EUR", 1178.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL0000360618")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-28T09:07"), hasShares(69.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 896.55), hasGrossValue("EUR", 893.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000360618")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-01T09:52"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2188.00), hasGrossValue("EUR", 2185.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0010558797")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-02T08:17"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 966.80), hasGrossValue("EUR", 966.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("BMG0112X1056")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-02T08:17"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 966.80), hasGrossValue("EUR", 966.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL0000303709")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-22T10:05"), hasShares(155.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2033.50), hasGrossValue("EUR", 2030.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0011540547")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-18T11:13"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 999.98), hasGrossValue("EUR", 996.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00), //
                        hasSecurity(hasIsin("NL0000303709")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-05T09:52"), hasShares(353.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.89), hasGrossValue("EUR", 57.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0014008D33")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-05T09:52"), hasShares(353.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.89), hasGrossValue("EUR", 57.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2022-03-25T00:00"), hasShares(1412.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0014008D33")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-25T00:00"), hasShares(353.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.89), hasGrossValue("EUR", 57.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR0014008D33")))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2022-03-11T00:00"), hasShares(1412.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        hasSecurity(hasIsin("FR0014008D33"))))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-31T06:45"), hasShares(213.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4855.34), hasGrossValue("EUR", 4855.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("GB00BP6MXD84")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-31T06:45"), hasShares(213.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4855.34), hasGrossValue("EUR", 4855.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("GB00B03MLX29")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-16T13:43"), hasShares(300.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1094.33), hasGrossValue("EUR", 1092.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.33), //
                        hasSecurity(hasIsin("NL0009739416")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-26T08:10"), hasShares(304.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2693.44), hasGrossValue("EUR", 2693.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL00150003E1")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-26T08:10"), hasShares(304.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2693.44), hasGrossValue("EUR", 2693.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL00150004A7")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-13T09:53"), hasShares(1412.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1004.20), hasGrossValue("EUR", 999.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.50), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-12-29T17:21"), hasShares(8.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL00150001Y3")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-21T00:00"), hasShares(304.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2333.50), hasGrossValue("EUR", 2333.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL00150004A7")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-21T00:00"), hasShares(608.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2333.50), hasGrossValue("EUR", 2333.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL0000352565")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-15T09:37"), hasShares(275.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 583.00), hasGrossValue("EUR", 583.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL0000352565")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-15T09:37"), hasShares(275.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 583.00), hasGrossValue("EUR", 583.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL0000352565")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-12-08T00:00"), hasShares(325.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL00150001Y3")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-08T00:00"), hasShares(275.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 583.00), hasGrossValue("EUR", 583.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("NL0000352565")))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2020-12-02T00:00"), hasShares(333.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        hasSecurity(hasIsin("NL00150001Y3"))))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-19T17:23"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 981.04), hasGrossValue("EUR", 978.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.29), //
                        hasSecurity(hasIsin("BE0003816338")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-24T09:05"), hasShares(92.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1034.55), hasGrossValue("EUR", 1032.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.31), //
                        hasSecurity(hasIsin("GB00B03MLX29")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-08T12:43"), hasShares(12.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1233.57), hasGrossValue("EUR", 1231.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.37), //
                        hasSecurity(hasIsin("BE0003818359")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-20T16:50"), hasShares(401.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 125.85), hasGrossValue("EUR", 125.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.06), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-20T16:50"), hasShares(3200.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1000.30), hasGrossValue("EUR", 1004.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.50), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-13T13:27"), hasShares(3600.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1003.50), hasGrossValue("EUR", 999.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.50), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-01T12:01"), hasShares(1.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.28), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.00), //
                        hasSecurity(hasIsin("FR0010478248")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-06T15:40"), hasShares(333.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1074.58), hasGrossValue("EUR", 1072.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.32), //
                        hasSecurity(hasIsin("NL0000352565")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T16:11"), hasShares(121.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1805.44), hasGrossValue("EUR", 1802.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.54), //
                        hasSecurity(hasIsin("GB00B03MLX29")))));
    }

    @Test
    public void testTransakcje01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(55L));
        assertThat(countBuySell(results), is(187L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(242));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0032077012"), hasWkn(null), hasTicker(null), //
                        hasName("INVESCO EQQQ NASDAQ-100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-23T15:56"), hasShares(8), //
                        hasSource("Transakcje01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2082.62), hasGrossValue("EUR", 2080.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.62))));

        // check 11th buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-23T15:30"), hasShares(224), //
                        hasSource("Transakcje01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1158.52), hasGrossValue("EUR", 1159.26), //
                        hasForexGrossValue("USD", 1411.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.74))));

        // check 37th buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-23T09:42"), hasShares(128), //
                        hasSource("Transakcje01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3108.15), hasGrossValue("EUR", 3113.71), //
                        hasForexGrossValue("GBX", 269440.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.56))));
    }

    @Test
    public void testTransakcje02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(95L));
        assertThat(countBuySell(results), is(715L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(4L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(818));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA03765K1049"), hasWkn(null), hasTicker(null), //
                        hasName("APHRIA INC. - COMMON SHARES"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-11T15:30"), hasShares(2), //
                        hasSource("Transakcje02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.81), hasGrossValue("EUR", 37.82), //
                        hasForexGrossValue("USD", 46.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));

        // check 7th buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-11T14:35"), hasShares(64), //
                        hasSource("Transakcje02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2614.14), hasGrossValue("EUR", 2624.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.86))));

        // check 280th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-14T15:36"), hasShares(2048), //
                        hasSource("Transakcje02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12245.87), hasGrossValue("EUR", 12245.87), //
                        hasForexGrossValue("USD", 14950.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 412th buy/sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-22T04:41"), hasShares(8000), //
                        hasSource("Transakcje02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4865.22), hasGrossValue("EUR", 4868.15), //
                        hasForexGrossValue("HKD", 46240.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.93))));

        // check 505th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-14"), hasShares(51), //
                        hasSource("Transakcje02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 791.47), hasGrossValue("EUR", 791.47), //
                        hasForexGrossValue("GBX", 72267.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTransakcje03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(108L));
        assertThat(countBuySell(results), is(1116L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(6L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1234));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA05156X8843"), hasWkn(null), hasTicker(null), //
                        hasName("AURORA CANNABIS"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-24T15:43"), hasShares(256), //
                        hasSource("Transakcje03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1853.22), hasGrossValue("EUR", 1862.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.18))));

        // check 233th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-06T12:47"), hasShares(128), //
                        hasSource("Transakcje03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1342.27), hasGrossValue("EUR", 1337.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.67))));

        // check 645th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-22"), hasShares(21), //
                        hasSource("Transakcje03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2407.86), hasGrossValue("EUR", 2407.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 850th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-28T09:00"), hasShares(1), //
                        hasSource("Transakcje03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.93), hasGrossValue("EUR", 8.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check inbound delivery (spin-off)
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2020-11-17T00:00"), hasShares(15.00), //
                                        hasSource("Transakcje03.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                                        hasSecurity(hasIsin("US92556V1061"))))));
    }

    @Test
    public void testEstrattoConto01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoConto01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-10-28T08:50"), hasAmount("EUR", 200.00), //
                        hasSource("EstrattoConto01.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-29T08:50"), hasAmount("EUR", 200.00), //
                        hasSource("EstrattoConto01.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-20T08:50"), hasAmount("EUR", 300.00), //
                        hasSource("EstrattoConto01.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-01T02:44"), hasAmount("EUR", 0.01), //
                        hasSource("EstrattoConto01.txt"), hasNote("Deposito"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-10-02T05:50"), hasAmount("EUR", 0.03), //
                        hasSource("EstrattoConto01.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-11-01T14:26"), hasAmount("EUR", 0.56), //
                        hasSource("EstrattoConto01.txt"), hasNote("DEGIRO Costi di connessione 2021 (Xetra - XET)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-10-02T10:46"), hasAmount("EUR", 1.24), //
                        hasSource("EstrattoConto01.txt"), hasNote("DEGIRO Costi di connessione 2021 (Xetra - XET)"))));
    }

    @Test
    public void testEstrattoConto02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoConto02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(49L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(51));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B74DQ490"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES GLOB HIG YLD CORP BOND UCITS"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-12T10:50"), hasAmount("EUR", 6000.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-11T10:50"), hasAmount("EUR", 3000.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-09T10:51"), hasAmount("EUR", 1000.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-05T11:00"), hasAmount("EUR", 1000.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-07-12T10:50"), hasAmount("EUR", 1750.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-13T08:50"), hasAmount("EUR", 2400.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-02-25T09:20"), hasAmount("EUR", 1700.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-11T09:00"), hasAmount("EUR", 1500.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-12T08:50"), hasAmount("EUR", 1300.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-10T08:50"), hasAmount("EUR", 2200.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-29T08:50"), hasAmount("EUR", 2500.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-12T08:50"), hasAmount("EUR", 4600.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-06T08:50"), hasAmount("EUR", 4032.40), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-06T08:50"), hasAmount("EUR", 4253.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-01T08:50"), hasAmount("EUR", 4253.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-23T08:50"), hasAmount("EUR", 3319.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-21T08:50"), hasAmount("EUR", 1197.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-20T08:50"), hasAmount("EUR", 7765.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-08T09:00"), hasAmount("EUR", 4.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito flatex"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-06T01:40"), hasAmount("EUR", 2500.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2020-09-24T01:24"), hasAmount("EUR", 1000.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Deposito"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-06-23T18:00"), hasAmount("EUR", 26600.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-06-14T13:00"), hasAmount("EUR", 1000.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-05-06T15:50"), hasAmount("EUR", 563.94), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-04-20T17:30"), hasAmount("EUR", 1046.92), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-04-06T18:30"), hasAmount("EUR", 2100.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-08T15:00"), hasAmount("EUR", 500.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-20T17:01"), hasAmount("EUR", 650.00), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-30T12:10"), hasAmount("EUR", 1586.30), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-10T17:20"), hasAmount("EUR", 25.23), //
                        hasSource("EstrattoConto02.txt"), hasNote("Prelievo flatex"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-29T07:55"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 68.23), hasGrossValue("EUR", 68.23), //
                        hasForexGrossValue("USD", 67.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-31T07:36"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 21.80), hasGrossValue("EUR", 21.80), //
                        hasForexGrossValue("USD", 24.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-12T07:31"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.14), hasGrossValue("EUR", 1.34), //
                        hasForexGrossValue("USD", 01.54), //
                        hasTaxes("EUR", 0.20), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-30T09:34"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 23.46), hasGrossValue("EUR", 23.46), //
                        hasForexGrossValue("USD", 27.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-13T07:49"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.42), hasGrossValue("EUR", 1.68), //
                        hasForexGrossValue("USD", 1.98), //
                        hasTaxes("EUR", 0.26), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-05-14T07:54"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.38), hasGrossValue("EUR", 1.64), //
                        hasForexGrossValue("USD", 1.98), //
                        hasTaxes("EUR", 0.26), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-25T07:40"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.01), hasGrossValue("EUR", 15.01), //
                        hasForexGrossValue("USD", 17.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-02-12T07:41"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.29), hasGrossValue("EUR", 1.52), //
                        hasForexGrossValue("USD", 1.85), //
                        hasTaxes("EUR", 0.23), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-10-01T16:02"), hasAmount("EUR", 0.63), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-07-01T10:20"), hasAmount("EUR", 10.35), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-04-02T02:40"), hasAmount("EUR", 10.53), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-12-30T22:20"), hasAmount("EUR", 1.67), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-10-01T18:30"), hasAmount("EUR", 1.39), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-07-02T01:10"), hasAmount("EUR", 0.55), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-04-01T20:30"), hasAmount("EUR", 0.82), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2020-12-31T07:10"), hasAmount("EUR", 1.11), //
                        hasSource("EstrattoConto02.txt"), hasNote("Flatex Interest"))));

        // check transaction
        assertThat(results, hasItem(interestCharge(hasDate("2020-11-02T16:06"), hasAmount("EUR", 0.01), //
                        hasSource("EstrattoConto02.txt"), hasNote("Interesse"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:15"), hasAmount("EUR", 2.50), //
                        hasSource("EstrattoConto02.txt"), //
                        hasNote("DEGIRO Costi di connessione 2021 (New York Stock Exchange - NSY)"))));

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:15"), hasAmount("EUR", 2.50), //
                        hasSource("EstrattoConto02.txt"), hasNote("DEGIRO Costi di connessione 2021 (NASDAQ - NDQ)"))));
    }

    @Test
    public void testEstrattoConto03()
    {
        Security security = new Security("ISHARES GLOB HIG YLD CORP BOND UCITS", CurrencyUnit.EUR);
        security.setIsin("IE00B74DQ490");

        Client client = new Client();
        client.addSecurity(security);

        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoConto03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-29T07:55"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstrattoConto03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 68.23), hasGrossValue("EUR", 68.23), //
                        hasForexGrossValue("USD", 67.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B74DQ490"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES GLOB HIG YLD CORP BOND UCITS"), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testOperazioni01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Operazioni01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL- WORLD UCITS ETF - (USD) ACCUMULATING"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-04T09:04"), hasShares(2), //
                        hasSource("Operazioni01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 191.22), hasGrossValue("EUR", 191.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2th buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-24T09:04"), hasShares(3), //
                        hasSource("Operazioni01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 291.75), hasGrossValue("EUR", 291.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELLA"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-22"), hasExDate("2020-05-14"), //
                        hasShares(20), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.41), hasGrossValue("EUR", 2.84), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTransactions_french01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_french01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(62L));
        assertThat(countBuySell(results), is(246L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(308));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("US88339J1051"), hasWkn(null), hasTicker(null), //
                        hasName("THE TRADE DESK CL A"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-09T16:44"), hasShares(1), //
                        hasSource("Transactions_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 68.87), hasGrossValue("EUR", 68.37), //
                        hasForexGrossValue("USD", 78.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));
    }

    @Test
    public void testTransactions_french02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_french02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check 1st security

        assertThat(results, hasItem(security( //
                        hasIsin("US02319V1035"), hasWkn(null), hasTicker(null), //
                        hasName("ADR ON AMBEV"), //
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B1FZSF77"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES PROP US"), //
                        hasCurrencyCode("EUR"))));
        assertThat(results, hasItem(security( //
                        hasIsin("US91324P1021"), hasWkn(null), hasTicker(null), //
                        hasName("UNITEDHEALTH GROUP INC"), //
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( //
                        hasIsin("US0367521038"), hasWkn(null), hasTicker(null), //
                        hasName("ELEVANCE HEALTH INC"), //
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( //
                        hasIsin("US5951121038"), hasWkn(null), hasTicker(null), //
                        hasName("MICRON TECHNOLOGY INC"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction

        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-16T21:56"), hasShares(150.00), //
                        hasSource("Transactions_french02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 317.95), hasGrossValue("EUR", 319.95), //
                        hasForexGrossValue("USD", 339.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00))));
        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-26T10:27"), hasShares(4.00), //
                        hasSource("Transactions_french02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 101.00), hasGrossValue("EUR", 101.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-26T10:13"), hasShares(12.00), //
                        hasSource("Transactions_french02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 303.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-22T16:51"), hasShares(1.00), //
                        hasSource("Transactions_french02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 468.94), hasGrossValue("EUR", 466.94), //
                        hasForexGrossValue("USD", 508.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00))));
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-22T16:31"), hasShares(1.00), //
                        hasSource("Transactions_french02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 428.59), hasGrossValue("EUR", 430.59), //
                        hasForexGrossValue("USD", 469.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00))));
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-22T16:30"), hasShares(4.00), //
                        hasSource("Transactions_french02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 322.22), hasGrossValue("EUR", 324.22), //
                        hasForexGrossValue("USD", 353.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00))));

    }

    @Test
    public void testTransactions_french03()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_french03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(64L));
        assertThat(countBuySell(results), is(213L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(279));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasWkn(null), hasTicker(null), //
                        hasName("ASML HOLDING NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US21873S1087"), hasWkn(null), hasTicker(null), //
                        hasName("COREWEAVE INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7655041058"), hasWkn(null), hasTicker(null), //
                        hasName("RICHTECH ROBOTICS INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US36317J2096"), hasWkn(null), hasTicker(null), //
                        hasName("GALAXY DIGITAL INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FI4000297767"), hasWkn(null), hasTicker(null), //
                        hasName("NORDEA BANK ABP"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US94106L1098"), hasWkn(null), hasTicker(null), //
                        hasName("WASTE MANAGEMENT INC."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0013341781"), hasWkn(null), hasTicker(null), //
                        hasName("2CRSI PROMESSES"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn(null), hasTicker(null), //
                        hasName("NOVO-NORDISK AS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE0002PG6CA6"), hasWkn(null), hasTicker(null), //
                        hasName("VANECK RARE EARTH AND STRATEGIC METALS UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000226223"), hasWkn(null), hasTicker(null), //
                        hasName("STMICROELECTRONICS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121014"), hasWkn(null), hasTicker(null), //
                        hasName("LVMH MOET HENNESSY LOUIS VUITTON SE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU1829221024"), hasWkn(null), hasTicker(null), //
                        hasName("AMUNDI NASDAQ-100 II UCITS ETF ACC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US6098391054"), hasWkn(null), hasTicker(null), //
                        hasName("MONOLITHIC POWER SYSTE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE000I8KRLL9"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES MSCI GLOBAL SEMICONDUCTORS UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US8716071076"), hasWkn(null), hasTicker(null), //
                        hasName("SYNOPSYS INC. - COMMO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120321"), hasWkn(null), hasTicker(null), //
                        hasName("L'OREAL"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M63516"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES MSCI BRAZIL UCITS ETF USD (DIST)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("SE0000202624"), hasWkn(null), hasTicker(null), //
                        hasName("GETINGE AB"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US2172041061"), hasWkn(null), hasTicker(null), //
                        hasName("COPART INC. - COMMON"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0404132054"), hasWkn(null), hasTicker(null), //
                        hasName("ARISTA NETWORKS  INC. COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BTJRMP35"), hasWkn(null), hasTicker(null), //
                        hasName("XTRACKERS MSCI EMERGING MARKETS UCITS ETF 1C"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU1841731745"), hasWkn(null), hasTicker(null), //
                        hasName("AMUNDI MSCI CHINA UCITS ETF ACC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0013269123"), hasWkn(null), hasTicker(null), //
                        hasName("RUBIS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US78573M1045"), hasWkn(null), hasTicker(null), //
                        hasName("SABRE CORPORATION - CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZCQB185"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES MSCI INDIA UCITS ETF USD ACC (EUR)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121485"), hasWkn(null), hasTicker(null), //
                        hasName("KERING"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0012032048"), hasWkn(null), hasTicker(null), //
                        hasName("ROCHE HOLDING AG"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin("SE0012673267"), hasWkn(null), hasTicker(null), //
                        hasName("EVOLUTION AB (PUBL)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("REALTY INCOME CORPORAT"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000031775"), hasWkn(null), hasTicker(null), //
                        hasName("VICAT SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA COMPANY (THE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker(null), //
                        hasName("ALPHABET INC. - CLASS A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000053381"), hasWkn(null), hasTicker(null), //
                        hasName("DERICHEBOURG SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3WJKG14"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES S&P 500 INF TECH SECTOR UCITS ETF USD(ACC)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("TESLA MOTORS INC. - C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US2546871060"), hasWkn(null), hasTicker(null), //
                        hasName("WALT DISNEY COMPANY (T"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("MICROSOFT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn(null), hasTicker(null), //
                        hasName("AMAZON.COM INC. - COM"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000ZAL1111"), hasWkn(null), hasTicker(null), //
                        hasName("ZALANDO SE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0079031078"), hasWkn(null), hasTicker(null), //
                        hasName("ADVANCED MICRO DEVICES"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0014008VX5"), hasWkn(null), hasTicker(null), //
                        hasName("EUROAPI SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US70450Y1038"), hasWkn(null), hasTicker(null), //
                        hasName("PAYPAL HOLDINGS INC."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120859"), hasWkn(null), hasTicker(null), //
                        hasName("IMERYS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5801351017"), hasWkn(null), hasTicker(null), //
                        hasName("MCDONALDS CORPORATION"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000051732"), hasWkn(null), hasTicker(null), //
                        hasName("ATOS SE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0011052257"), hasWkn(null), hasTicker(null), //
                        hasName("GLOBAL BIOENERGIES"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0011726835"), hasWkn(null), hasTicker(null), //
                        hasName("GTT"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US30303M1027"), hasWkn(null), hasTicker(null), //
                        hasName("META PLATFORMS INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US8740391003"), hasWkn(null), hasTicker(null), //
                        hasName("ADR ON TAIWAN SEMICONDUCTOR MANUFACTURING CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88579Y1010"), hasWkn(null), hasTicker(null), //
                        hasName("3M COMPANY COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121964"), hasWkn(null), hasTicker(null), //
                        hasName("KLEPIERRE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0210483332"), hasWkn(null), hasTicker(null), //
                        hasName("COMPAGNIE FINANCIERE RICHEMONT SA"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0244767585"), hasWkn(null), hasTicker(null), //
                        hasName("UBS GROUP AG REGISTERE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BTN1Y115"), hasWkn(null), hasTicker(null), //
                        hasName("MEDTRONIC PLC. ORDINAR"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150001Q9"), hasWkn(null), hasTicker(null), //
                        hasName("STELLANTIS"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US2254011081"), hasWkn(null), hasTicker(null), //
                        hasName("CREDIT SUISSE GROUP AM"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR001400GG91"), hasWkn(null), hasTicker(null), //
                        hasName("GLOBAL BIOENERGIES - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010112524"), hasWkn(null), hasTicker(null), //
                        hasName("NEXITY"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRON."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0004007813"), hasWkn(null), hasTicker(null), //
                        hasName("KAUFMAN ET BROAD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121972"), hasWkn(null), hasTicker(null), //
                        hasName("SCHNEIDER ELECTRIC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000045072"), hasWkn(null), hasTicker(null), //
                        hasName("CREDIT AGRICOLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010220475"), hasWkn(null), hasTicker(null), //
                        hasName("ALSTOM"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-05T15:22"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 894.15), hasGrossValue("CHF", 898.75), //
                        hasForexGrossValue("EUR", 960.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.60), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-03T19:06"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 621.51), hasGrossValue("CHF", 623.38), //
                        hasForexGrossValue("USD", 780.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US21873S1087")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-02T17:11"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 644.23), hasGrossValue("CHF", 642.36), //
                        hasForexGrossValue("USD", 798.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US21873S1087")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-02T17:00"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 283.39), hasGrossValue("CHF", 281.52), //
                        hasForexGrossValue("USD", 350.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US7655041058")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-02T16:57"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 412.15), hasGrossValue("CHF", 410.28), //
                        hasForexGrossValue("USD", 510.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US36317J2096")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-02T15:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 872.91), hasGrossValue("CHF", 877.50), //
                        hasForexGrossValue("EUR", 939.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.59), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-01T19:04"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1431.30), hasGrossValue("CHF", 1434.95), //
                        hasForexGrossValue("EUR", 1536.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.65), //
                        hasSecurity(hasIsin("FI4000297767")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-17T15:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 500.62), hasGrossValue("CHF", 498.77), //
                        hasForexGrossValue("USD", 627.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.85), //
                        hasSecurity(hasIsin("US94106L1098")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-11-13T17:29"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 625.40), hasGrossValue("CHF", 629.93), //
                        hasForexGrossValue("EUR", 683.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.53), //
                        hasSecurity(hasIsin("FR0013341781")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-04T09:33"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 786.61), hasGrossValue("CHF", 782.04), //
                        hasForexGrossValue("EUR", 840.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.57), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-04T09:04"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 510.14), hasGrossValue("CHF", 507.34), //
                        hasForexGrossValue("EUR", 545.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.80), //
                        hasSecurity(hasIsin("IE0002PG6CA6")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-03T17:17"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 470.02), hasGrossValue("CHF", 465.45), //
                        hasForexGrossValue("EUR", 500.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.57), //
                        hasSecurity(hasIsin("FR0013341781")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-20T19:41"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 433.69), hasGrossValue("CHF", 431.84), //
                        hasForexGrossValue("USD", 545.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.85), //
                        hasSecurity(hasIsin("US7655041058")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-20T16:45"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 954.88), hasGrossValue("CHF", 959.41), //
                        hasForexGrossValue("EUR", 1040.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.53), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-17T09:15"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 544.10), hasGrossValue("CHF", 541.32), //
                        hasForexGrossValue("EUR", 586.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.78), //
                        hasSecurity(hasIsin("IE0002PG6CA6")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-15T09:13"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 555.53), hasGrossValue("CHF", 560.10), //
                        hasForexGrossValue("EUR", 602.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.57), //
                        hasSecurity(hasIsin("FR0000121014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-14T15:38"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 479.61), hasGrossValue("CHF", 477.75), //
                        hasForexGrossValue("USD", 595.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.86), //
                        hasSecurity(hasIsin("US7655041058")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-07T13:52"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 483.24), hasGrossValue("CHF", 486.04), //
                        hasForexGrossValue("EUR", 522.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.80), //
                        hasSecurity(hasIsin("LU1829221024")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-03T19:50"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 574.66), hasGrossValue("CHF", 572.79), //
                        hasForexGrossValue("USD", 720.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US36317J2096")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-03T15:57"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 754.83), hasGrossValue("CHF", 756.70), //
                        hasForexGrossValue("USD", 950.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US6098391054")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-03T15:52"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 649.61), hasGrossValue("CHF", 645.02), //
                        hasForexGrossValue("EUR", 690.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.59), //
                        hasSecurity(hasIsin("FR0013341781")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-01T15:55"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 772.37), hasGrossValue("CHF", 776.97), //
                        hasForexGrossValue("EUR", 830.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.60), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-01T15:44"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 304.39), hasGrossValue("CHF", 307.21), //
                        hasForexGrossValue("EUR", 328.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.82), //
                        hasSecurity(hasIsin("IE000I8KRLL9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-24T20:54"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 373.28), hasGrossValue("CHF", 371.41), //
                        hasForexGrossValue("USD", 466.90), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US8716071076")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-24T10:34"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 696.05), hasGrossValue("CHF", 691.46), //
                        hasForexGrossValue("EUR", 740.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.59), //
                        hasSecurity(hasIsin("FR0000120321")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-24T10:34"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 696.05), hasGrossValue("CHF", 691.46), //
                        hasForexGrossValue("EUR", 740.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.59), //
                        hasSecurity(hasIsin("FR0000120321")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-23T18:57"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 393.81), hasGrossValue("CHF", 391.94), //
                        hasForexGrossValue("USD", 495.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US8716071076")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-23T15:07"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 385.09), hasGrossValue("CHF", 387.90), //
                        hasForexGrossValue("EUR", 415.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.81), //
                        hasSecurity(hasIsin("IE00B0M63516")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-22T09:03"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 264.14), hasGrossValue("CHF", 267.79), //
                        hasForexGrossValue("EUR", 286.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.65), //
                        hasSecurity(hasIsin("SE0000202624")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-13T14:00"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 409.30), hasGrossValue("CHF", 404.68), //
                        hasForexGrossValue("EUR", 430.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-07-30T14:30"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1249.05), hasGrossValue("CHF", 1244.48), //
                        hasForexGrossValue("EUR", 1338.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.57), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-07-17T18:16"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 928.90), hasGrossValue("CHF", 927.03), //
                        hasForexGrossValue("USD", 1152.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US2172041061")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-07-17T15:30"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 875.37), hasGrossValue("CHF", 877.24), //
                        hasForexGrossValue("USD", 1090.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US0404132054")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-27T15:30"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 553.87), hasGrossValue("CHF", 549.27), //
                        hasForexGrossValue("EUR", 586.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.60), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-26T17:38"), hasShares(46.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1107.25), hasGrossValue("CHF", 1111.85), //
                        hasForexGrossValue("EUR", 1187.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.60), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-13T20:03"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 995.76), hasGrossValue("CHF", 993.88), //
                        hasForexGrossValue("USD", 1225.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US2172041061")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-06T13:57"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 889.81), hasGrossValue("CHF", 885.20), //
                        hasForexGrossValue("EUR", 943.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("FR0000121014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-13T17:19"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 554.69), hasGrossValue("CHF", 550.07), //
                        hasForexGrossValue("EUR", 585.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-30T15:14"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1081.58), hasGrossValue("CHF", 1076.98), //
                        hasForexGrossValue("EUR", 1150.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.60), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-22T16:30"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 235.25), hasGrossValue("CHF", 232.44), //
                        hasForexGrossValue("EUR", 248.92), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.81), //
                        hasSecurity(hasIsin("IE00BTJRMP35")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-22T14:39"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 304.28), hasGrossValue("CHF", 304.28), //
                        hasForexGrossValue("EUR", 326.48), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("LU1841731745")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-22T14:39"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 301.67), hasGrossValue("CHF", 304.47), //
                        hasForexGrossValue("EUR", 326.68), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.80), //
                        hasSecurity(hasIsin("LU1841731745")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T11:47"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 790.58), hasGrossValue("CHF", 786.02), //
                        hasForexGrossValue("EUR", 846.45), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.56), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-07T16:30"), hasShares(36.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 580.64), hasGrossValue("CHF", 576.02), //
                        hasForexGrossValue("EUR", 612.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-01T16:21"), hasShares(9.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 552.81), hasGrossValue("CHF", 548.13), //
                        hasForexGrossValue("EUR", 575.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.68), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T16:36"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 417.79), hasGrossValue("CHF", 414.90), //
                        hasForexGrossValue("EUR", 432.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.89), //
                        hasSecurity(hasIsin("LU1829221024")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T11:38"), hasShares(8.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 553.49), hasGrossValue("CHF", 548.78), //
                        hasForexGrossValue("EUR", 572.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.71), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-28T15:16"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 642.54), hasGrossValue("CHF", 637.92), //
                        hasForexGrossValue("EUR", 679.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-26T20:16"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 858.83), hasGrossValue("CHF", 856.95), //
                        hasForexGrossValue("USD", 958.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US0404132054")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-02-13T09:01"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 977.16), hasGrossValue("CHF", 981.82), //
                        hasForexGrossValue("EUR", 1036.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.66), //
                        hasSecurity(hasIsin("FR0013269123")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-06T18:11"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 590.30), hasGrossValue("CHF", 588.42), //
                        hasForexGrossValue("USD", 650.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US6098391054")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-31T15:30"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 152.84), hasGrossValue("CHF", 154.73), //
                        hasForexGrossValue("USD", 170.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.89), //
                        hasSecurity(hasIsin("US78573M1045")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-17T16:31"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 150.64), hasGrossValue("CHF", 146.02), //
                        hasForexGrossValue("EUR", 155.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-17T10:32"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 411.66), hasGrossValue("CHF", 410.72), //
                        hasForexGrossValue("EUR", 438.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.94), //
                        hasSecurity(hasIsin("IE00BZCQB185")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-17T09:01"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 450.07), hasGrossValue("CHF", 445.46), //
                        hasForexGrossValue("EUR", 475.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("FR0000121485")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-14T09:27"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 262.00), hasGrossValue("CHF", 268.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00), //
                        hasSecurity(hasIsin("CH0012032048")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-23T14:22"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 134.81), hasGrossValue("CHF", 131.16), //
                        hasForexGrossValue("EUR", 140.52), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.65), //
                        hasSecurity(hasIsin("SE0012673267")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-09T15:31"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 395.71), hasGrossValue("CHF", 392.08), //
                        hasForexGrossValue("EUR", 422.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.63), //
                        hasSecurity(hasIsin("SE0012673267")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-09T15:30"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 493.73), hasGrossValue("CHF", 491.87), //
                        hasForexGrossValue("USD", 560.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.86), //
                        hasSecurity(hasIsin("US7561091049")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-27T09:02"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 400.99), hasGrossValue("CHF", 397.36), //
                        hasForexGrossValue("EUR", 428.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.63), //
                        hasSecurity(hasIsin("SE0012673267")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-26T11:30"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 570.92), hasGrossValue("CHF", 575.49), //
                        hasForexGrossValue("EUR", 618.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.57), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-25T13:02"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 928.45), hasGrossValue("CHF", 923.87), //
                        hasForexGrossValue("EUR", 990.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.58), //
                        hasSecurity(hasIsin("FR0000120321")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-07T15:12"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 683.84), hasGrossValue("CHF", 688.47), //
                        hasForexGrossValue("EUR", 730.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.63), //
                        hasSecurity(hasIsin("FR0000031775")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-07T14:30"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 381.00), hasGrossValue("CHF", 376.37), //
                        hasForexGrossValue("EUR", 399.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.63), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-01T14:56"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 220.73), hasGrossValue("CHF", 216.09), //
                        hasForexGrossValue("EUR", 229.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.64), //
                        hasSecurity(hasIsin("FR0000121485")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-30T09:32"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 660.32), hasGrossValue("CHF", 655.71), //
                        hasForexGrossValue("EUR", 698.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("FR0000120321")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-29T13:45"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 420.63), hasGrossValue("CHF", 416.97), //
                        hasForexGrossValue("EUR", 445.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.66), //
                        hasSecurity(hasIsin("SE0012673267")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-21T16:23"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 414.76), hasGrossValue("CHF", 411.09), //
                        hasForexGrossValue("EUR", 438.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.67), //
                        hasSecurity(hasIsin("FI4000297767")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-16T10:19"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 600.56), hasGrossValue("CHF", 595.95), //
                        hasForexGrossValue("EUR", 635.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-16T09:04"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 654.22), hasGrossValue("CHF", 649.61), //
                        hasForexGrossValue("EUR", 692.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("FR0000120321")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-16T09:00"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1108.11), hasGrossValue("CHF", 1103.50), //
                        hasForexGrossValue("EUR", 1175.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("FR0000121014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-15T17:13"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 652.88), hasGrossValue("CHF", 648.26), //
                        hasForexGrossValue("EUR", 690.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("NL0010273215")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-15T09:14"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 624.22), hasGrossValue("CHF", 621.40), //
                        hasForexGrossValue("EUR", 661.76), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.82), //
                        hasSecurity(hasIsin("LU1841731745")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-10-01T16:16"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 609.78), hasGrossValue("CHF", 611.66), //
                        hasForexGrossValue("USD", 725.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US1912161007")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-10-01T15:32"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 284.44), hasGrossValue("CHF", 286.32), //
                        hasForexGrossValue("USD", 338.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US02079K3059")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-27T17:12"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 301.52), hasGrossValue("CHF", 305.20), //
                        hasForexGrossValue("EUR", 324.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.68), //
                        hasSecurity(hasIsin("FR0000053381")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-26T09:04"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 413.72), hasGrossValue("CHF", 414.67), //
                        hasForexGrossValue("EUR", 436.95), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.95), //
                        hasSecurity(hasIsin("IE00B3WJKG14")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-24T15:38"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 430.54), hasGrossValue("CHF", 432.43), //
                        hasForexGrossValue("USD", 510.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.89), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-20T17:01"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 710.24), hasGrossValue("CHF", 705.59), //
                        hasForexGrossValue("EUR", 744.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.65), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-18T15:47"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 567.44), hasGrossValue("CHF", 562.82), //
                        hasForexGrossValue("EUR", 599.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.62), //
                        hasSecurity(hasIsin("FR0000121014")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-16T15:32"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 386.44), hasGrossValue("CHF", 388.32), //
                        hasForexGrossValue("USD", 460.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US2546871060")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-11T21:54"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 717.41), hasGrossValue("CHF", 719.29), //
                        hasForexGrossValue("USD", 844.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US5949181045")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-10T19:45"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 302.92), hasGrossValue("CHF", 304.79), //
                        hasForexGrossValue("USD", 360.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.87), //
                        hasSecurity(hasIsin("US0231351067")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-05T15:33"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 569.23), hasGrossValue("CHF", 571.11), //
                        hasForexGrossValue("USD", 675.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.88), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-08-21T10:46"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 339.54), hasGrossValue("CHF", 344.21), //
                        hasForexGrossValue("EUR", 362.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.67), //
                        hasSecurity(hasIsin("DE000ZAL1111")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-08-20T16:03"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 689.27), hasGrossValue("CHF", 691.18), //
                        hasForexGrossValue("USD", 805.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.91), //
                        hasSecurity(hasIsin("US0079031078")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-08-13T10:52"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 512.57), hasGrossValue("CHF", 507.92), //
                        hasForexGrossValue("EUR", 536.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.65), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-08-13T10:47"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 7.56), hasGrossValue("CHF", 7.56), //
                        hasForexGrossValue("EUR", 7.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR0014008VX5")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-08-13T10:47"), hasShares(23.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 82.31), hasGrossValue("CHF", 86.96), //
                        hasForexGrossValue("EUR", 91.77), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.65), //
                        hasSecurity(hasIsin("FR0014008VX5")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-30T15:40"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 170.74), hasGrossValue("CHF", 172.66), //
                        hasForexGrossValue("USD", 195.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.92), //
                        hasSecurity(hasIsin("US70450Y1038")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-29T16:33"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 456.12), hasGrossValue("CHF", 451.41), //
                        hasForexGrossValue("EUR", 471.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.71), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T15:49"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 358.87), hasGrossValue("CHF", 354.11), //
                        hasForexGrossValue("EUR", 365.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.76), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T10:51"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 155.29), hasGrossValue("CHF", 160.04), //
                        hasForexGrossValue("EUR", 165.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.75), //
                        hasSecurity(hasIsin("FR0000120859")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T21:55"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 708.75), hasGrossValue("CHF", 706.81), //
                        hasForexGrossValue("USD", 800.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.94), //
                        hasSecurity(hasIsin("US0079031078")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T16:12"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 425.64), hasGrossValue("CHF", 424.67), //
                        hasForexGrossValue("EUR", 438.75), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.97), //
                        hasSecurity(hasIsin("IE00B3WJKG14")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-02T13:56"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 649.35), hasGrossValue("CHF", 644.59), //
                        hasForexGrossValue("EUR", 665.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.76), //
                        hasSecurity(hasIsin("DK0062498333")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-20T15:36"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 308.96), hasGrossValue("CHF", 306.08), //
                        hasForexGrossValue("EUR", 320.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.88), //
                        hasSecurity(hasIsin("IE000I8KRLL9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-20T15:24"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 200.86), hasGrossValue("CHF", 197.98), //
                        hasForexGrossValue("EUR", 207.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.88), //
                        hasSecurity(hasIsin("IE00B0M63516")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-17T09:00"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 389.46), hasGrossValue("CHF", 384.78), //
                        hasForexGrossValue("EUR", 403.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.68), //
                        hasSecurity(hasIsin("FR0013269123")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-11T15:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 229.10), hasGrossValue("CHF", 227.17), //
                        hasForexGrossValue("USD", 253.01), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.93), //
                        hasSecurity(hasIsin("US5801351017")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-06T22:00"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 492.00), hasGrossValue("CHF", 493.94), //
                        hasForexGrossValue("USD", 555.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.94), //
                        hasSecurity(hasIsin("US0231351067")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-05T15:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 466.84), hasGrossValue("CHF", 468.79), //
                        hasForexGrossValue("USD", 525.45), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.95), //
                        hasSecurity(hasIsin("US02079K3059")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-05T10:42"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 334.28), hasGrossValue("CHF", 333.31), //
                        hasForexGrossValue("EUR", 343.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.97), //
                        hasSecurity(hasIsin("IE00BZCQB185")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-05T10:29"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 56.93), hasGrossValue("CHF", 61.70), //
                        hasForexGrossValue("EUR", 63.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.77), //
                        hasSecurity(hasIsin("FR0000051732")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-30T15:46"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 731.79), hasGrossValue("CHF", 729.83), //
                        hasForexGrossValue("USD", 798.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.96), //
                        hasSecurity(hasIsin("US5949181045")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-24T15:33"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 325.84), hasGrossValue("CHF", 322.02), //
                        hasForexGrossValue("EUR", 330.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.82), //
                        hasSecurity(hasIsin("FI4000297767")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-24T09:29"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 579.04), hasGrossValue("CHF", 574.24), //
                        hasForexGrossValue("EUR", 588.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.80), //
                        hasSecurity(hasIsin("NL0000226223")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-23T11:46"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 16.57), hasGrossValue("CHF", 21.34), //
                        hasForexGrossValue("EUR", 21.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.77), //
                        hasSecurity(hasIsin("FR0011052257")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-12T17:32"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 535.26), hasGrossValue("CHF", 533.32), //
                        hasForexGrossValue("USD", 585.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.94), //
                        hasSecurity(hasIsin("US1912161007")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-05T14:17"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 407.80), hasGrossValue("CHF", 412.61), //
                        hasForexGrossValue("EUR", 421.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.81), //
                        hasSecurity(hasIsin("FR0011726835")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T21:59"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 245.30), hasGrossValue("CHF", 243.34), //
                        hasForexGrossValue("USD", 270.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.96), //
                        hasSecurity(hasIsin("US5801351017")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-04T09:00"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 146.82), hasGrossValue("CHF", 151.65), //
                        hasForexGrossValue("EUR", 154.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.83), //
                        hasSecurity(hasIsin("FR0000120859")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-21T10:26"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 365.46), hasGrossValue("CHF", 360.67), //
                        hasForexGrossValue("EUR", 369.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.79), //
                        hasSecurity(hasIsin("FR0000121485")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-20T15:23"), hasShares(19.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 248.20), hasGrossValue("CHF", 245.30), //
                        hasForexGrossValue("EUR", 253.95), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.90), //
                        hasSecurity(hasIsin("LU1841731745")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-20T10:46"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 15.76), hasGrossValue("CHF", 12.85), //
                        hasForexGrossValue("EUR", 13.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.91), //
                        hasSecurity(hasIsin("LU1841731745")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-14T14:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 439.29), hasGrossValue("CHF", 441.22), //
                        hasForexGrossValue("USD", 501.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.93), //
                        hasSecurity(hasIsin("US30303M1027")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-13T15:48"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 466.77), hasGrossValue("CHF", 464.85), //
                        hasForexGrossValue("USD", 530.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.92), //
                        hasSecurity(hasIsin("US7561091049")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-13T13:48"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 233.22), hasGrossValue("CHF", 230.33), //
                        hasForexGrossValue("EUR", 240.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.89), //
                        hasSecurity(hasIsin("IE00B0M63516")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-12T16:34"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 384.26), hasGrossValue("CHF", 386.18), //
                        hasForexGrossValue("USD", 439.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.92), //
                        hasSecurity(hasIsin("US8740391003")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-12T14:31"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 351.09), hasGrossValue("CHF", 353.01), //
                        hasForexGrossValue("USD", 402.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.92), //
                        hasSecurity(hasIsin("US88579Y1010")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-12T10:49"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 227.02), hasGrossValue("CHF", 231.72), //
                        hasForexGrossValue("EUR", 242.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.70), //
                        hasSecurity(hasIsin("FR0000121964")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-12T09:00"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 430.80), hasGrossValue("CHF", 436.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00), //
                        hasSecurity(hasIsin("CH0210483332")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-11T20:42"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 161.32), hasGrossValue("CHF", 163.24), //
                        hasForexGrossValue("USD", 186.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.92), //
                        hasSecurity(hasIsin("CH0244767585")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-07T09:14"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 95.62), hasGrossValue("CHF", 100.34), //
                        hasForexGrossValue("EUR", 104.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.72), //
                        hasSecurity(hasIsin("FR0011052257")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-27T16:00"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 441.17), hasGrossValue("CHF", 443.08), //
                        hasForexGrossValue("USD", 504.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.91), //
                        hasSecurity(hasIsin("IE00BTN1Y115")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-20T15:33"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 415.50), hasGrossValue("CHF", 417.41), //
                        hasForexGrossValue("USD", 475.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.91), //
                        hasSecurity(hasIsin("US30303M1027")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-10T17:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 326.10), hasGrossValue("CHF", 320.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00), //
                        hasSecurity(hasIsin("CH0210483332")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-18T11:59"), hasShares(19.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 86.39), hasGrossValue("CHF", 86.39), //
                        hasForexGrossValue("EUR", 91.01), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR0014008VX5")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-18T11:58"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 31.95), hasGrossValue("CHF", 27.29), //
                        hasForexGrossValue("EUR", 28.74), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.66), //
                        hasSecurity(hasIsin("FR0014008VX5")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-16T21:34"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 226.40), hasGrossValue("CHF", 224.50), //
                        hasForexGrossValue("USD", 249.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.90), //
                        hasSecurity(hasIsin("US5801351017")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-16T20:37"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 354.37), hasGrossValue("CHF", 356.27), //
                        hasForexGrossValue("USD", 396.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.90), //
                        hasSecurity(hasIsin("NL00150001Q9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-12T15:32"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 319.84), hasGrossValue("CHF", 315.15), //
                        hasForexGrossValue("EUR", 330.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.69), //
                        hasSecurity(hasIsin("DE000ZAL1111")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-27T16:56"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 679.93), hasGrossValue("CHF", 675.18), //
                        hasForexGrossValue("EUR", 698.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.75), //
                        hasSecurity(hasIsin("FR0000121014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-07T14:10"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 300.00), hasGrossValue("CHF", 295.27), //
                        hasForexGrossValue("EUR", 306.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.73), //
                        hasSecurity(hasIsin("FR0000051732")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-14T14:54"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 199.85), hasGrossValue("CHF", 198.88), //
                        hasForexGrossValue("EUR", 205.86), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.97), //
                        hasSecurity(hasIsin("IE00BZCQB185")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-13T15:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 268.50), hasGrossValue("CHF", 270.43), //
                        hasForexGrossValue("USD", 313.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.93), //
                        hasSecurity(hasIsin("US30303M1027")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-27T14:52"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 228.56), hasGrossValue("CHF", 224.73), //
                        hasForexGrossValue("EUR", 229.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.83), //
                        hasSecurity(hasIsin("SE0000202624")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-23T09:00"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 269.54), hasGrossValue("CHF", 264.72), //
                        hasForexGrossValue("EUR", 269.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.82), //
                        hasSecurity(hasIsin("FR0011726835")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-22T09:05"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 292.19), hasGrossValue("CHF", 288.36), //
                        hasForexGrossValue("EUR", 294.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.83), //
                        hasSecurity(hasIsin("FR0000053381")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-13T15:56"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 248.27), hasGrossValue("CHF", 243.47), //
                        hasForexGrossValue("EUR", 249.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.80), //
                        hasSecurity(hasIsin("FR0013269123")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-13T00:00"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 108.16), hasGrossValue("CHF", 108.16), //
                        hasForexGrossValue("USD", 119.48), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("CH0244767585")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-13T00:00"), hasShares(150.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 120.28), hasGrossValue("CHF", 120.28), //
                        hasForexGrossValue("USD", 132.87), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("US2254011081")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-08T18:31"), hasShares(16.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 48.09), hasGrossValue("CHF", 48.09), //
                        hasForexGrossValue("USD", 53.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("US78573M1045")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-08T18:31"), hasShares(23.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 69.13), hasGrossValue("CHF", 69.13), //
                        hasForexGrossValue("USD", 76.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("US78573M1045")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-08T18:31"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 30.06), hasGrossValue("CHF", 30.06), //
                        hasForexGrossValue("USD", 33.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("US78573M1045")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-08T18:31"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 4.96), hasGrossValue("CHF", 3.01), //
                        hasForexGrossValue("USD", 3.34), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.95), //
                        hasSecurity(hasIsin("US78573M1045")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-05-30T15:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 539.98), hasGrossValue("CHF", 541.92), //
                        hasForexGrossValue("USD", 600.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.94), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-22T16:37"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 170.82), hasGrossValue("CHF", 168.87), //
                        hasForexGrossValue("USD", 188.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.95), //
                        hasSecurity(hasIsin("US70450Y1038")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-05-22T15:42"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 222.33), hasGrossValue("CHF", 224.28), //
                        hasForexGrossValue("USD", 250.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.95), //
                        hasSecurity(hasIsin("US30303M1027")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-02T10:34"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 299.32), hasGrossValue("CHF", 295.47), //
                        hasForexGrossValue("EUR", 300.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.85), //
                        hasSecurity(hasIsin("FI4000297767")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-04-03T08:39"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 24.18), hasGrossValue("CHF", 24.18), //
                        hasForexGrossValue("EUR", 24.84), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR001400GG91")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-03T08:39"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 24.18), hasGrossValue("CHF", 24.18), //
                        hasForexGrossValue("EUR", 24.84), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR0011052257")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-23T00:00"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 24.18), hasGrossValue("CHF", 24.18), //
                        hasForexGrossValue("EUR", 24.84), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR001400GG91")))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2023-03-23T00:00"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasSecurity(hasIsin("FR001400GG91")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-15T17:23"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 176.69), hasGrossValue("CHF", 175.71), //
                        hasForexGrossValue("USD", 190.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.98), //
                        hasSecurity(hasIsin("US2254011081")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-03-15T17:19"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 179.01), hasGrossValue("CHF", 179.99), //
                        hasForexGrossValue("USD", 194.62), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.98), //
                        hasSecurity(hasIsin("US30303M1027")))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionInboundDeliveryWithoutValue, //
                        inboundDelivery( //
                                        hasDate("2023-03-08T00:00"), hasShares(50.00), //
                                        hasSource("Transactions_french03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        hasSecurity(hasIsin("FR001400GG91"))))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-02T15:30"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 414.43), hasGrossValue("CHF", 413.43), //
                        hasForexGrossValue("USD", 438.68), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.00), //
                        hasSecurity(hasIsin("US88579Y1010")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-17T15:54"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 232.67), hasGrossValue("CHF", 232.67), //
                        hasForexGrossValue("EUR", 235.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("IE00BTJRMP35")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-17T15:54"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 391.00), hasGrossValue("CHF", 386.14), //
                        hasForexGrossValue("EUR", 390.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.86), //
                        hasSecurity(hasIsin("FR0013269123")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-17T14:41"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 419.24), hasGrossValue("CHF", 414.38), //
                        hasForexGrossValue("EUR", 419.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.86), //
                        hasSecurity(hasIsin("FR0000120859")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-26T19:45"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 450.24), hasGrossValue("CHF", 449.24), //
                        hasForexGrossValue("USD", 488.16), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.00), //
                        hasSecurity(hasIsin("IE00BTN1Y115")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-24T10:16"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 301.00), hasGrossValue("CHF", 295.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00), //
                        hasSecurity(hasIsin("CH0012032048")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-16T10:31"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 556.43), hasGrossValue("CHF", 561.35), //
                        hasForexGrossValue("EUR", 560.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.92), //
                        hasSecurity(hasIsin("FR0010112524")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-13T14:55"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 496.24), hasGrossValue("CHF", 491.30), //
                        hasForexGrossValue("EUR", 489.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.94), //
                        hasSecurity(hasIsin("FR0000031775")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-12T16:19"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 445.20), hasGrossValue("CHF", 446.21), //
                        hasForexGrossValue("USD", 480.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.01), //
                        hasSecurity(hasIsin("NL00150001Q9")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-12T10:19"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 386.28), hasGrossValue("CHF", 391.21), //
                        hasForexGrossValue("EUR", 390.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.93), //
                        hasSecurity(hasIsin("FR0000051732")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-06T15:38"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 288.03), hasGrossValue("CHF", 287.04), //
                        hasForexGrossValue("USD", 306.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-04T16:36"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 199.80), hasGrossValue("CHF", 199.80), //
                        hasForexGrossValue("EUR", 202.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("IE00BZCQB185")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-04T16:31"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 209.64), hasGrossValue("CHF", 208.65), //
                        hasForexGrossValue("USD", 224.25), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US8740391003")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-16T16:45"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 417.47), hasGrossValue("CHF", 416.48), //
                        hasForexGrossValue("USD", 447.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US2546871060")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-14T15:30"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 150.76), hasGrossValue("CHF", 149.77), //
                        hasForexGrossValue("USD", 161.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US2254011081")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-13T16:29"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 304.77), hasGrossValue("CHF", 303.78), //
                        hasForexGrossValue("USD", 327.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-06T15:35"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 507.69), hasGrossValue("CHF", 506.70), //
                        hasForexGrossValue("USD", 540.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US88160R1014")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-02T12:39"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 298.74), hasGrossValue("CHF", 295.78), //
                        hasForexGrossValue("EUR", 300.68), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 2.96), //
                        hasSecurity(hasIsin("LU1841731745")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-22T09:08"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 486.42), hasGrossValue("CHF", 491.25), //
                        hasForexGrossValue("EUR", 500.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.83), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-18T16:16"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 204.30), hasGrossValue("CHF", 199.46), //
                        hasForexGrossValue("EUR", 202.25), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.84), //
                        hasSecurity(hasIsin("FR0011052257")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-10T15:30"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 344.39), hasGrossValue("CHF", 345.38), //
                        hasForexGrossValue("USD", 355.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("NL00150001Q9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-09T18:12"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 437.44), hasGrossValue("CHF", 436.45), //
                        hasForexGrossValue("USD", 443.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US02079K3059")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-04T17:00"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 492.02), hasGrossValue("CHF", 496.87), //
                        hasForexGrossValue("EUR", 503.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.85), //
                        hasSecurity(hasIsin("FR0004007813")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-04T16:58"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 446.34), hasGrossValue("CHF", 445.35), //
                        hasForexGrossValue("USD", 446.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US0231351067")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-04T15:22"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 335.35), hasGrossValue("CHF", 336.34), //
                        hasForexGrossValue("USD", 337.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("NL00150001Q9")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-04T14:54"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 646.06), hasGrossValue("CHF", 650.90), //
                        hasForexGrossValue("EUR", 660.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.84), //
                        hasSecurity(hasIsin("FR0000121972")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-04T14:53"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 452.76), hasGrossValue("CHF", 457.60), //
                        hasForexGrossValue("EUR", 464.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.84), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-02T09:03"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 273.51), hasGrossValue("CHF", 278.36), //
                        hasForexGrossValue("EUR", 282.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.85), //
                        hasSecurity(hasIsin("FR0000045072")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-28T16:35"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 498.33), hasGrossValue("CHF", 497.34), //
                        hasForexGrossValue("USD", 499.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.99), //
                        hasSecurity(hasIsin("US30303M1027")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-10-28T16:13"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 396.57), hasGrossValue("CHF", 401.44), //
                        hasForexGrossValue("EUR", 405.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.87), //
                        hasSecurity(hasIsin("FR0000121964")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-28T16:11"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 484.56), hasGrossValue("CHF", 479.69), //
                        hasForexGrossValue("EUR", 483.90), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.87), //
                        hasSecurity(hasIsin("FR0000051732")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-10-26T10:52"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 242.73), hasGrossValue("CHF", 247.60), //
                        hasForexGrossValue("EUR", 250.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.87), //
                        hasSecurity(hasIsin("FR0004007813")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-10-26T09:28"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 651.61), hasGrossValue("CHF", 656.48), //
                        hasForexGrossValue("EUR", 662.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.87), //
                        hasSecurity(hasIsin("FR0000121972")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-20T12:41"), hasShares(14.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 296.83), hasGrossValue("CHF", 292.09), //
                        hasForexGrossValue("EUR", 302.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.74), //
                        hasSecurity(hasIsin("FR0004007813")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-19T15:29"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 413.11), hasGrossValue("CHF", 408.37), //
                        hasForexGrossValue("EUR", 423.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.74), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-09-19T09:22"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 454.07), hasGrossValue("CHF", 458.81), //
                        hasForexGrossValue("EUR", 475.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.74), //
                        hasSecurity(hasIsin("FR0010220475")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-08-08T13:50"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 285.48), hasGrossValue("CHF", 289.78), //
                        hasForexGrossValue("EUR", 297.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.30), //
                        hasSecurity(hasIsin("FR0000045072")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-12T16:55"), hasShares(16.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 412.91), hasGrossValue("CHF", 408.56), //
                        hasForexGrossValue("EUR", 414.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.35), //
                        hasSecurity(hasIsin("FR0004007813")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-05T10:06"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 566.75), hasGrossValue("CHF", 562.36), //
                        hasForexGrossValue("EUR", 564.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.39), //
                        hasSecurity(hasIsin("FR0000121972")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-29T15:40"), hasShares(7.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 176.37), hasGrossValue("CHF", 176.37), //
                        hasForexGrossValue("EUR", 176.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR0010112524")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-29T15:40"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 302.34), hasGrossValue("CHF", 302.34), //
                        hasForexGrossValue("EUR", 302.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("FR0010112524")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-29T15:40"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 29.61), hasGrossValue("CHF", 25.20), //
                        hasForexGrossValue("EUR", 25.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.41), //
                        hasSecurity(hasIsin("FR0010112524")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-29T15:15"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 299.28), hasGrossValue("CHF", 294.86), //
                        hasForexGrossValue("EUR", 294.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.42), //
                        hasSecurity(hasIsin("FR0000121964")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-06T09:55"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 626.71), hasGrossValue("CHF", 622.13), //
                        hasForexGrossValue("EUR", 599.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.58), //
                        hasSecurity(hasIsin("FR0000045072")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-25T10:22"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 354.98), hasGrossValue("CHF", 350.48), //
                        hasForexGrossValue("EUR", 343.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.50), //
                        hasSecurity(hasIsin("FR0000121964")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-25T09:04"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 576.83), hasGrossValue("CHF", 572.32), //
                        hasForexGrossValue("EUR", 560.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.51), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-03-24T20:57"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 970.89), hasGrossValue("CHF", 971.40), //
                        hasForexGrossValue("USD", 1044.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.51), //
                        hasSecurity(hasIsin("US0378331005")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-11T11:41"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 511.43), hasGrossValue("CHF", 506.92), //
                        hasForexGrossValue("EUR", 496.25), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.51), //
                        hasSecurity(hasIsin("FR0010220475")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-08T16:38"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 646.07), hasGrossValue("CHF", 645.56), //
                        hasForexGrossValue("USD", 695.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.51), //
                        hasSecurity(hasIsin("NL00150001Q9")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-02-22T09:01"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 494.50), hasGrossValue("CHF", 500.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 5.50), //
                        hasSecurity(hasIsin("CH0210483332")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-01T10:57"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 792.37), hasGrossValue("CHF", 787.79), //
                        hasForexGrossValue("EUR", 758.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.58), //
                        hasSecurity(hasIsin("FR0000121972")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-28T13:29"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 976.71), hasGrossValue("CHF", 981.29), //
                        hasForexGrossValue("EUR", 945.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.58), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-27T11:19"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 530.70), hasGrossValue("CHF", 525.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 5.50), //
                        hasSecurity(hasIsin("CH0210483332")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-19T20:46"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 922.91), hasGrossValue("CHF", 922.39), //
                        hasForexGrossValue("USD", 1007.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.52), //
                        hasSecurity(hasIsin("US0378331005")))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-19T17:21"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 769.15), hasGrossValue("CHF", 769.67), //
                        hasForexGrossValue("USD", 840.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.52), //
                        hasSecurity(hasIsin("US0378331005")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-13T14:25"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1014.91), hasGrossValue("CHF", 1010.30), //
                        hasForexGrossValue("EUR", 966.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 4.61), //
                        hasSecurity(hasIsin("FR0000124141")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-05T18:24"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 960.46), hasGrossValue("CHF", 959.94), //
                        hasForexGrossValue("USD", 1048.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.52), //
                        hasSecurity(hasIsin("NL00150001Q9")))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-16T15:54"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 817.42), hasGrossValue("CHF", 817.42), //
                        hasForexGrossValue("USD", 887.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasSecurity(hasIsin("US0378331005")))));
    }

    @Test
    public void testTransakce01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakce01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(55L));
        assertThat(countBuySell(results), is(85L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(4L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(145));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US71654V4086"), hasWkn(null), hasTicker(null), //
                        hasName("PETROLEO BRASILEIRO S."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US83417Q2049"), hasWkn(null), hasTicker(null), //
                        hasName("SOLARWINDS CORPORATION COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-08-08T16:42"), hasShares(16), //
                        hasSource("Transakce01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 232.79), hasGrossValue("EUR", 232.29), //
                        hasForexGrossValue("USD", 237.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(8), //
                        hasSource("Transakce01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 152.74), hasGrossValue("EUR", 152.74), //
                        hasForexGrossValue("USD", 179.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(47L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(54));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("ES0148396007"), hasWkn(null), hasTicker(null), //
                        hasName("INDITEX"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5486611073"), hasWkn(null), hasTicker(null), //
                        hasName("LOWES COMPANIES INC."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46625H1005"), hasWkn(null), hasTicker(null), //
                        hasName("JP MORGAN CHASE & CO."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US74144T1088"), hasWkn(null), hasTicker(null), //
                        hasName("T. ROWE PRICE GROUP I"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88579Y1010"), hasWkn(null), hasTicker(null), //
                        hasName("3M COMPANY COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4781601046"), hasWkn(null), hasTicker(null), //
                        hasName("JOHNSON & JOHNSON COMM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B1TXK627"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES GLOBAL WATER UCITS ETF USD"), //
                        hasCurrencyCode("USD"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-09-03T02:25"), hasAmount("EUR", 2000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-03T02:24"), hasAmount("EUR", 2000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-07-26T07:54"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-07-03T02:19"), hasAmount("EUR", 2000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-28T10:27"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-24T07:51"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-21T06:39"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-19T12:12"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-16T16:10"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-15T19:00"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-03T02:40"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-25T08:01"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-17T07:38"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-05-03T03:49"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-04-27T11:43"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-04-03T02:18"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-29T07:59"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-17T10:04"), hasAmount("EUR", 5000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-15T08:02"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-10T16:10"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-03T02:39"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-31T22:40"), hasAmount("EUR", 6000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-31T22:30"), hasAmount("EUR", 4000.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-31T22:10"), hasAmount("EUR", 1.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-21T16:30"), hasAmount("EUR", 1.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("flatex Deposit"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-20T02:53"), hasAmount("EUR", 1.00), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Ingreso"))));

        // @formatter:off
        // 07-11-2022 09:59 02-11-2022 INDITEX ES0148396007 Dividendo EUR -317,70 EUR 534,43
        // 07-11-2022 09:59 02-11-2022 INDITEX ES0148396007 Retención del dividendo EUR 60,36 EUR 852,13
        // @formatter:on
        // check 1st cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2022-11-07T09:59"), hasExDate(null), //
                                        hasShares(0), //
                                        hasSource("EstadoDeCuenta01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 317.70), hasGrossValue("EUR", 317.70), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 1st dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-07T07:28"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 257.34), hasGrossValue("EUR", 317.70), //
                        hasTaxes("EUR", 60.36), hasFees("EUR", 0.00))));

        // check 2nd dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-04T10:35"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 257.34), hasGrossValue("EUR", 317.70), //
                        hasTaxes("EUR", 60.36), hasFees("EUR", 0.00))));

        // check 3rd dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-04T09:47"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 141.54), hasGrossValue("EUR", 174.74), //
                        hasTaxes("EUR", 33.20), hasFees("EUR", 0.00))));

        // check 4th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-03T07:06"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.13), hasGrossValue("EUR", 10.74), //
                        hasForexGrossValue("USD", 10.50), //
                        hasTaxes("EUR", 1.61), hasFees("EUR", 0.00))));

        // check 5th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-01T07:04"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.58), hasGrossValue("EUR", 10.10), //
                        hasForexGrossValue("USD", 10.00), //
                        hasTaxes("EUR", 1.52), hasFees("EUR", 0.00))));

        // check 6th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-30T07:25"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.99), hasGrossValue("EUR", 31.75), //
                        hasForexGrossValue("USD", 31.20), //
                        hasTaxes("EUR", 4.76), hasFees("EUR", 0.00))));

        // check 7th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-13T07:20"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.33), hasGrossValue("EUR", 7.45), //
                        hasForexGrossValue("USD", 7.45), //
                        hasTaxes("EUR", 1.12), hasFees("EUR", 0.00))));

        // check 8th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-07T07:44"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.45), hasGrossValue("EUR", 67.59), //
                        hasForexGrossValue("USD", 67.80), //
                        hasTaxes("EUR", 10.14), hasFees("EUR", 0.00))));

        // check 9th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-08-04T07:23"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.68), hasGrossValue("EUR", 10.22), //
                        hasForexGrossValue("USD", 10.50), //
                        hasTaxes("EUR", 1.54), hasFees("EUR", 0.00))));

        // check 10th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-08-01T07:11"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.26), hasGrossValue("EUR", 9.72), //
                        hasForexGrossValue("USD", 10.00), //
                        hasTaxes("EUR", 1.46), hasFees("EUR", 0.00))));

        // check 11th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-26T08:06"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 21.21), hasGrossValue("EUR", 21.21), //
                        hasForexGrossValue("USD", 22.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-02T07:40"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 267.05), hasGrossValue("EUR", 329.69), //
                        hasTaxes("EUR", 62.64), hasFees("EUR", 0.00))));

        // check 2nd cancellation (Storno) transaction - counter booking of the
        // cancelled dividend above
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2022-11-07T09:59"), //
                                        hasShares(0), //
                                        hasSource("EstadoDeCuenta01.txt"), //
                                        hasNote("Retención del dividendo: ES0148396007"), //
                                        hasAmount("EUR", 60.36), hasGrossValue("EUR", 60.36), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 1st interest charge transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-10-01T09:31"), hasAmount("EUR", 2.15), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Flatex Interest"))));

        // check 2nd interest charge transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-07-02T06:20"), hasAmount("EUR", 5.31), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Flatex Interest"))));

        // check 3rd interest charge transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-04-02T02:30"), hasAmount("EUR", 4.45), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Flatex Interest"))));

        // check 1st fee transaction
        assertThat(results, hasItem(fee(hasDate("2022-07-05T08:32"), hasAmount("EUR", 2.50), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Giro Exchange Connection Fee 2022"))));

        // check 2nd fee transaction
        assertThat(results, hasItem(fee(hasDate("2022-07-05T08:32"), hasAmount("EUR", 2.50), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Giro Exchange Connection Fee 2022"))));

        // check 3rd fee transaction
        assertThat(results, hasItem(fee(hasDate("2022-04-01T23:24"), hasAmount("EUR", 2.50), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Giro Exchange Connection Fee 2022"))));

        // check 4th fee transaction
        assertThat(results, hasItem(fee(hasDate("2022-03-02T08:42"), hasAmount("EUR", 2.50), //
                        hasSource("EstadoDeCuenta01.txt"), hasNote("Giro Exchange Connection Fee 2022"))));
    }

    @Test
    public void testPrehleductu01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Prehleductu01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(43L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(44));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3XXRP09"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD S&P500"), //
                        hasCurrencyCode("USD"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-21T16:59"), hasAmount("CZK", 50000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-23T15:10"), hasAmount("CZK", 50000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-10-18T11:40"), hasAmount("CZK", 50000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-21T14:39"), hasAmount("CZK", 50000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-20T14:49"), hasAmount("CZK", 90000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-07T11:29"), hasAmount("CZK", 270000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-31T12:43"), hasAmount("CZK", 400000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-21T12:08"), hasAmount("CZK", 28000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-11T11:43"), hasAmount("CZK", 30000.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-01T13:08"), hasAmount("CZK", 1.00), //
                        hasSource("Prehleductu01.txt"), hasNote("Vklad"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-29T08:50"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.27), hasGrossValue("EUR", 33.27), //
                        hasForexGrossValue("USD", 35.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-29T08:50"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.61), hasGrossValue("EUR", 4.61), //
                        hasForexGrossValue("USD", 4.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-29T08:03"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 36.78), hasGrossValue("EUR", 36.78), //
                        hasForexGrossValue("USD", 36.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-29T08:03"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.10), hasGrossValue("EUR", 5.10), //
                        hasForexGrossValue("USD", 5.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-30T10:12"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.33), hasGrossValue("EUR", 33.33), //
                        hasForexGrossValue("USD", 35.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-30T10:12"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.62), hasGrossValue("EUR", 4.62), //
                        hasForexGrossValue("USD", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-01T09:37"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 31.85), hasGrossValue("EUR", 31.85), //
                        hasForexGrossValue("USD", 35.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-01T09:37"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.41), hasGrossValue("EUR", 4.41), //
                        hasForexGrossValue("USD", 4.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-30T09:04"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 28.09), hasGrossValue("EUR", 28.09), //
                        hasForexGrossValue("USD", 31.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-30T09:04"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.98), hasGrossValue("EUR", 3.98), //
                        hasForexGrossValue("USD", 4.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-30T14:50"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.79), hasGrossValue("EUR", 25.79), //
                        hasForexGrossValue("USD", 29.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-30T14:50"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.19), hasGrossValue("EUR", 4.19), //
                        hasForexGrossValue("USD", 4.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-01T10:56"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.74), hasGrossValue("EUR", 9.74), //
                        hasForexGrossValue("USD", 11.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-01T10:56"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.63), hasGrossValue("EUR", 3.63), //
                        hasForexGrossValue("USD", 4.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-04-01T13:56"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.86), hasGrossValue("EUR", 3.86), //
                        hasForexGrossValue("USD", 4.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-31T09:20"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.32), hasGrossValue("EUR", 3.32), //
                        hasForexGrossValue("USD", 4.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-07T08:05"), hasExDate(null), //
                        hasShares(0), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.04), hasGrossValue("EUR", 4.04), //
                        hasForexGrossValue("USD", 4.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-10-01T20:01"), hasAmount("EUR", 0.06), //
                        hasSource("Prehleductu01.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-07-03T00:11"), hasAmount("EUR", 0.12), //
                        hasSource("Prehleductu01.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2022-04-02T16:50"), hasAmount("EUR", 0.08), //
                        hasSource("Prehleductu01.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-12-31T00:40"), hasAmount("EUR", 0.33), //
                        hasSource("Prehleductu01.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2021-10-02T00:20"), hasAmount("EUR", 0.25), //
                        hasSource("Prehleductu01.txt"), hasNote("Flatex Interest"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-03T14:01"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2023 (Borsa Italiana S.p.A. - MIL)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-03T14:01"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2023 (Euronext Amsterdam - EAM)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-03T14:01"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), hasNote("DEGIRO poplatek za Obchodování 2023 (Xetra - XET)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-02-03T10:03"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2022 (Borsa Italiana S.p.A. - MIL)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-02-03T10:03"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2022 (Euronext Amsterdam - EAM)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-02-03T10:03"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), hasNote("DEGIRO poplatek za Obchodování 2022 (Xetra - XET)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-07-02T08:23"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), hasNote("DEGIRO poplatek za Obchodování 2021 (Xetra - XET)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:20"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2021 (Borsa Italiana S.p.A. - MIL)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-31T13:20"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2021 (Euronext Amsterdam - EAM)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-01-04T11:09"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2020 (Euronext Amsterdam - EAM)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-10-01T11:58"), hasAmount("EUR", 2.50), //
                        hasSource("Prehleductu01.txt"), //
                        hasNote("DEGIRO poplatek za Obchodování 2020 (Borsa Italiana S.p.A. - MIL)"))));
    }

    @Test
    public void testTransacoes01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacoes01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA2926717083"), hasWkn(null), hasTicker(null), //
                        hasName("ENERGY FUELS INC"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL- WORLD UCITS ETF - (USD) ACCUMULATING"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-13T14:31"), hasShares(410), //
                        hasSource("Transacoes01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2069.01), hasGrossValue("EUR", 2068.01), //
                        hasForexGrossValue("USD", 2214.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // check 2nd buy/sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-15T17:01"), hasShares(52), //
                        hasSource("Transacoes01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4992.00), hasGrossValue("EUR", 4992.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

}
