package name.abuchen.portfolio.datatransfer.pdf.swisscardaecs;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SwisscardAECSPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SwisscardAECSPDFExtractorTest
{
    @Test
    public void testKreditkartenabrechnung01()
    {
        var extractor = new SwisscardAECSPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kreditkartenabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-01"), hasAmount("CHF", 600.00), //
                        hasSource("Kreditkartenabrechnung01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-16"), hasAmount("CHF", 20.30), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("jgfhjfgjdfh, sjdzj"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-20"), hasAmount("CHF", 69.50), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("pSZgXQmP oPrZBxEWM AG, irsSh - EUR 74.51 (Kurs 0.9329398)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-09"), hasAmount("CHF", 27.60), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("oacOb wkifsK ZKF C2, FPvBFHbfu - RON 143.00 (Kurs 0.1930393)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-13"), hasAmount("CHF", 23.50), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("HMztX rehdf, vcnncvn, HxF - EUR 25.00 (Kurs 0.9408607)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-25"), hasAmount("CHF", 4.00), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("iMTqB *ntooz icR CREM, JLnLhBlwyq"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-02"), hasAmount("CHF", 20.50), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("LQogc fiaQk asfd, nUGGFx - EUR 22.00 (Kurs 0.9328383)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-05"), hasAmount("CHF", 28.00), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("yvczTl stjh, fZYSZzowba"))));
    }

    @Test
    public void testKreditkartenabrechnung02()
    {
        var extractor = new SwisscardAECSPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kreditkartenabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-01"), hasAmount("CHF", 450.00), //
                        hasSource("Kreditkartenabrechnung02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-12-17"), hasAmount("CHF", 13.00), //
                        hasSource("Kreditkartenabrechnung02.txt"), //
                        hasNote("ojOSQF RtDOdike shfh, uLylx - GBP 12.00 (Kurs 1.085317)"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-01-09"), //
                        hasSource("Kreditkartenabrechnung02.txt"), //
                        hasNote("GEBÜHR FÜR PAPIERRECHNUNG"), //
                        hasAmount("CHF", 1.50), hasGrossValue("CHF", 1.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKreditkartenabrechnung03()
    {
        var extractor = new SwisscardAECSPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kreditkartenabrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-01"), hasAmount("CHF", 435.00), //
                        hasSource("Kreditkartenabrechnung03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-12"), hasAmount("CHF", 35.00), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("lHj ncfrAqc 54678, iFFmDyKuar"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-12"), hasAmount("CHF", 6.65), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("AxXmoonxY fvTIn UeJjUL (4, GjFNGd"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-13"), hasAmount("CHF", 164.15), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("BGa gfhjfghj, YZbgXAftD - EUR 178.45 (Kurs 0.9198399)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-13"), hasAmount("CHF", 164.15), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("mch fgjfghj, sPssGtRfU - EUR 178.45 (Kurs 0.9198399)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-14"), hasAmount("CHF", 192.90), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("fghjfghj gfhjfghj f, ghjfghj - CLP 211'370.00 (Kurs 0.0009126)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-14"), hasAmount("CHF", 157.35), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("MCwfSAC, SiVHzRPgZ - BRL 987.30 (Kurs 0.1593752)"))));

        // assert transaction (refund)
        assertThat(results, hasItem(deposit(hasDate("2026-03-15"), hasAmount("CHF", 164.15), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("qyO AIRWAYS, OwTucdTYZ - EUR -178.45 (Kurs 0.9198399)"))));

        // assert transaction (refund)
        assertThat(results, hasItem(deposit(hasDate("2026-03-15"), hasAmount("CHF", 164.15), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("iUN gfhj fgh suFpsBLoh - EUR -178.45 (Kurs 0.9198399)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-02"), hasAmount("CHF", 18.55), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("SSP S.L. cbMKnNiVtG gL BA, ykuqHmEd vTy - EUR 19.76 (Kurs 0.9377127)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-10"), hasAmount("CHF", 18.35), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("D hBuaKn I dfghjdfgh, ocCve - EUR 20.00 (Kurs 0.9168949)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-03"), hasAmount("CHF", 5.15), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("69703180 gWT kzRZHoe S, GhxYgb"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-08"), hasAmount("CHF", 17.45), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("jBw CGC sjFpUu LS, ZfwrWBthR - SEK 201.00 (Kurs 0.0867506)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-09"), hasAmount("CHF", 6.50), //
                        hasSource("Kreditkartenabrechnung03.txt"), //
                        hasNote("gfjhfgdjh, 21050, DWHAPL"))));
    }
}
