package name.abuchen.portfolio.datatransfer.pdf.c24bankgmbh;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.C24BankGmbHPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class C24BankGmbHPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-17"), hasAmount("EUR", 1508.42), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung | Tagesgeld | Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 1115.22), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung | adsf sdfwWr | xHzutg g52 hgT"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 1460.11), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisung | Girokonto | Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-05-31"), hasShares(0.00), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Zinsen | 16.05.2024-30.05.2024"), //
                        hasAmount("EUR", 1.93), hasGrossValue("EUR", 4.22), //
                        hasTaxes("EUR", 2.29), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-06-30"), hasShares(0.00), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Zinsen | 31.05.2024-29.06.2024"), //
                        hasAmount("EUR", 15.32), hasGrossValue("EUR", 19.36), //
                        hasTaxes("EUR", 4.04), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-31"), hasShares(0.00), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Zinsen | 30.06.2024-30.07.2024"), //
                        hasAmount("EUR", 15.86), hasGrossValue("EUR", 20.04), //
                        hasTaxes("EUR", 4.18), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-05"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Echtzeitüberweisung | eNfGuN dsBkLn"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-05"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung | Tagesgeld"))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-31"), hasAmount("EUR", 3800.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Lastschrift | YsoCMl fMTfnR | suhdrfgu"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-30"), hasAmount("EUR", 1055.70), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | HbTVQjcyAjxqI exK üaoidfiohuo-utfut"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | tupqsV cUwvwG"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-29"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | wesrtr"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-27"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"),
                        hasNote("Überweisung | NNCgYS nTwSst | gtea mzDXqrNqgAAt MBkwXmzb SvpTXbE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-27"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug06.txt"),
                        hasNote("Überweisung | SlWanw RxrgVj | EZGQ dgEBCkOghaJf NTgUOQB RYeRO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-20"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | gsiPlfIwT"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-20"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | bRbIsa iYxVlb | eTbFNHDOO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-20"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug06.txt"), hasNote("Lastschrift | bvKnrezgr | Bogr-CykoY-xdPF"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-19"), hasAmount("EUR", 1.20), //
                        hasSource("Kontoauszug06.txt"),
                        hasNote("Echtzeitüberweisung | UPtbw JlCrjXlEqv | VvfdbHnDb"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-16"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | HMNLhgjkj | YiXR-nEOaZ-WVDD"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-14"), hasAmount("EUR", 6000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung | pwWxHXRqd"))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-31"), hasAmount("EUR", 3753.00), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Lastschrift | vuDXwHsb rfXBAYA gKOx | EYrKCnMa RPjyvBt PUeknu 7h TJXysAGQ"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-30"), hasAmount("EUR", 2253.11), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Überweisung | ptf jFWjLYlU dgPU | GufY - JItPfQ HjpMNytYHm 06/2849"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-27"), hasAmount("EUR", 728.00), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Überweisung | qKzxJC YiraYB | QmLt FUYANsXVMByi itujkIIX FWmNrRK"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-27"), hasAmount("EUR", 649.00), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Überweisung | RqZpyu tYmnuo | sChx yprPPvDuHANL cJBOXOv xBSNP"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-26"), hasAmount("EUR", 494.51), //
                        hasSource("Kontoauszug07.txt"), hasNote("QVOeWA-KMJdstQmSSRbX | wb nWy.QJw"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-15"), hasAmount("EUR", 32.48), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Echtzeitüberweisung | ZxwvKtmD GrtYjZy GdSz epyU | etPMbkry"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-15"), hasAmount("EUR", 68.97), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung | wNFfnt SaYbFB | UCIwnacA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-14"), hasAmount("EUR", 6.35), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Lastschrift | vPKvNsPSKc poFglmA RpBs + bJ. Nmr | YW-hA.: 0290700587, sq-Ko.: 0332544005/8, uIYJ FyiYnOucAzBjF"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-11"), hasAmount("EUR", 0.30), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung | fPkwHh nGtlnt | eoHeSMxX"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-11"), hasAmount("EUR", 6.93), //
                        hasSource("Kontoauszug07.txt"), hasNote("MoneySend Zahlung | 27-97 DEKMDGhLg wQQzQ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-06"), hasAmount("EUR", 407.44), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung | yucljg FDhNLS | xTlLECPt"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-06"), hasAmount("EUR", 670.67), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Überweisung | tgZLFe VSwKtA | DpkFRDDI GsjsZXl lRcnIy clhTbdfEiI"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-04"), hasAmount("EUR", 32.51), //
                        hasSource("Kontoauszug07.txt"), hasNote("Online-Kartenzahlung | OjrVVmDfrQm"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-03"), hasAmount("EUR", 4286.49), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung | SyhjPw kGqAel | TyRNKrlM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-03"), hasAmount("EUR", 575.42), //
                        hasSource("Kontoauszug07.txt"), hasNote("Online-Kartenzahlung | Ii RJu.KWu"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-03"), hasAmount("EUR", 9444.60), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Überweisung | vDqfsJ dKjzhC | PXvMPrOU Lwivcmp enuKLZ WsVUiBwXHW"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-02"), hasAmount("EUR", 19.89), //
                        hasSource("Kontoauszug07.txt"),
                        hasNote("Überweisung | OGsXcQL LJThhd pvX TBpfeO lJddUX | JaDWlJeB GEgniz slqaI JcXqgq uvf"))));
    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(30L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-29"), hasAmount("EUR", 2469.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Lastschrift | pRNbLaYZ ZEjPysH MLVE | PnfvIREr IlbnmOC wcqMbV 5q YhdTRDQV"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-29"), hasAmount("EUR", 91.27), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | Utzv Z*13-40585-21163"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-28"), hasAmount("EUR", 7522.77), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Überweisung | HSS pDoZtnjv MGFv | aIFH - tBncOq yUgelmgQUK 33/9487"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-28"), hasAmount("EUR", 86.74), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | BnfmLm *JcnbYLQi pLnOD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-27"), hasAmount("EUR", 60.09), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | lc isFgpkey NuLQ"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-27"), hasAmount("EUR", 9.06), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisung | NDJfKo ittOZdw qcyU | BFXax9628909"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-26"), hasAmount("EUR", 3.64), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Lastschrift | hfjOZpMkt MkSynd pHVF | H9943503 o993358773 J624340099 xoKrlDzyt"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-25"), hasAmount("EUR", 65.95), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisung | QwlwJn RnjFCHm SiEH | dbXvs6723504"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-25"), hasAmount("EUR", 406.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Echtzeitüberweisung | gYZGQrgV nGwDaCf nuyx jXbj | VxjbaTDMb YKwuJJYE EbTshWbAOtpXgOosdNm"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-24"), hasAmount("EUR", 81.46), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Echtzeitüberweisung | tJFS XmEZVcqaD | LcJLmHVOMgWPEy"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-24"), hasAmount("EUR", 54.15), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Echtzeitüberweisung | LhbYOLhebvQuJv vQfSGz ApAn | LARFwFiIJOHrFaq 18974128 LjOqQdfJiHsq 554581373"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-21"), hasAmount("EUR", 7.72), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisung | pojcef AhGsuMz IUAk | fhNCs6460619"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-20"), hasAmount("EUR", 20.99), //
                        hasSource("Kontoauszug08.txt"), hasNote("Rückerstattung | KUaIfw* ze5888Pd4"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-18"), hasAmount("EUR", 986.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Überweisung | WjDRAGMK ljQLMxI ItLB | jnhRnoWz sERPYMH EHQToMfxP CuWpFMqLaV"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-17"), hasAmount("EUR", 877.87), //
                        hasSource("Kontoauszug08.txt"), hasNote("Kartenzahlung | eTFf sdPr JCTyC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-16"), hasAmount("EUR", 962.03), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | hsI olqGh.Uxe"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-16"), hasAmount("EUR", 253.66), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | nnW TywXy.zFb"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-16"), hasAmount("EUR", 197.18), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | dQExNIhpB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-16"), hasAmount("EUR", 563.54), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | yVUJMS* aY36R2CG8"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-16"), hasAmount("EUR", 33.34), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | NPxHCCjIflzJN.qJ DRlp"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-13"), hasAmount("EUR", 7.49), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | knEZCa* gL08v9jE6"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-13"), hasAmount("EUR", 30.99), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Lastschrift | wyiguDCOtm tqHoKOw DcLL + sZ. qBh | xA-bm.: 5177084828, SH-hL.: 3953523948/0, zvRo HjqvBAEfdFWso"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-11"), hasAmount("EUR", 14.98), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Lastschrift | JxxZP-zcd oykCURg FJIo | 79-36-8876 / 06-71-6330"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-09"), hasAmount("EUR", 54.97), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | aPMx Y*80-79463-43430"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-06"), hasAmount("EUR", 93.57), //
                        hasSource("Kontoauszug08.txt"), hasNote("Online-Kartenzahlung | EWGxIEWUBZmAD.mU mbWr"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-05"), hasAmount("EUR", 39.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Überweisung | QD ciqneEzK eYiG | 53o33382334 Saq sxwqrMIdjLIAmbUCdI / qHBrysEZW jP 24.91.3008/ KxmDQAIJN: UkZVtc, cZUhJc"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-05"), hasAmount("EUR", 431.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Überweisung | zkxHFbF biTmxF uDb StiLJv wTxeNc | atGRvONy QjAmEg wAvNc LCkUYl Dhc"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-03"), hasAmount("EUR", 63.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Echtzeitüberweisung | pTjPZy wtuPXa | WfFhn + VtwHgqWYrEJQJSPMnJzxYJoDxom SSn LEhSjv oI. 8 Pk GBaBNvWdjJc, tPshASMNivEhcy 59, 27576 YSORJI"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-01"), hasAmount("EUR", 320.00), //
                        hasSource("Kontoauszug08.txt"),
                        hasNote("Echtzeitüberweisung | mQkscabB SiqHqWc MtFv bmAz | Epbj oBCibRKQRpJO RrTDaqVg oEBvotf"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-01"), hasAmount("EUR", 91.94), //
                        hasSource("Kontoauszug08.txt"), hasNote("Rückerstattung | oQVEOH* Cv9Y66fz8"))));
    }
}
