package name.abuchen.portfolio.datatransfer.pdf.merkurprivatbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.MerkurPrivatBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class MerkurPrivatBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security identification:
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BJ0KDQ92"));
        assertThat(security.getWkn(), is("A1XB5U"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1C O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-05-02T09:34:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(125.3258)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 284722/61.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(10002.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(10000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(2.50))));

    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security identification:
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BJ0KDQ92"));
        assertThat(security.getWkn(), is("A1XB5U"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1C O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-05-09T09:26:37")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(600.0)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 166647/10.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(47811.46))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(47786.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(25.06))));

    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA1363751027"), hasWkn("897879"), hasTicker(null), //
                        hasName("CANADIAN NATIONAL RAILWAY CO. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-01T09:36:10"), hasShares(0.625), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer 292030/42.00"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security identification:
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005933931"));
        assertThat(security.getWkn(), is("593393"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ISHARES CORE DAX UCITS ETF DE INHABER-ANTEILE EUR ACC."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-05-09T09:44:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(500.0)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 306589/61.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(66844.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(25.06))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(66870.0))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security identification:
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BZ02LR44"));
        assertThat(security.getWkn(), is("A2AQST"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("XTR.(IE)-MSCI WORLD ESG REGISTERED SHARES 1C O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2024-03-22T20:15:51")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3260.0)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 749922/41.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(116003.09))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(25.00 + 0.06))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(119446.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(3240.05 + 178.20))));

    }

    @Test
    public void testDividende01()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005557508"), hasWkn("555750"), hasTicker(null), //
                        hasName("DEUTSCHE TELEKOM AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-15T00:00"), hasShares(55), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 60338188850"), //
                        hasAmount("EUR", 42.35), hasGrossValue("EUR", 42.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierDividende02()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011683594"), hasWkn("A2JAHJ"), hasTicker(null), //
                        hasName("VANECK MSTR.DM DIVIDEND.UC.ETF AANDELEN OOP TOONDER O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-11T00:00"), hasShares(2950), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 13354957156"), //
                        hasAmount("EUR", 753.20), hasGrossValue("EUR", 914.50), //
                        hasTaxes("EUR", 137.18 + 22.86 + 1.26), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-10-01T00:00"), hasShares(3.8506), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 77550511480"), //
                        hasAmount("EUR", 1.13), hasGrossValue("EUR", 1.38), //
                        hasForexGrossValue("USD", 1.63), //
                        hasTaxes("EUR", 0.24 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON", "EUR");
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new MerkurPrivatBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-10-01T00:00"), hasShares(3.8506), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 77550511480"), //
                        hasAmount("EUR", 1.13), hasGrossValue("EUR", 1.38), //
                        hasTaxes("EUR", 0.24 + 0.01), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");
        // new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA1363751027"), hasWkn("897879"), hasTicker(null), //
                        hasName("CANADIAN NATIONAL RAILWAY CO. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-29T00:00"), hasShares(0.9854), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 77350300260"), //
                        hasAmount("EUR", 0.34), hasGrossValue("EUR", 0.53), //
                        hasForexGrossValue("CAD", 0.87), //
                        hasTaxes("EUR", 0.13 + 0.05 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("CANADIAN NATIONAL RAILWAY CO. REGISTERED SHARES O.N.", "EUR");
        security.setIsin("CA1363751027");
        security.setWkn("897879");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new MerkurPrivatBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-29T00:00"), hasShares(0.9854), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 77350300260"), //
                        hasAmount("EUR", 0.34), hasGrossValue("EUR", 0.53), //
                        hasTaxes("EUR", 0.13 + 0.05 + 0.01), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(deposit(hasDate("2024-01-19"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Dauerauftrag"))));

        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 25000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 25000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Festgeldanlage"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 25000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Neuanlage"))));

    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new MerkurPrivatBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ02LR44"), hasWkn("A2AQST"), hasTicker(null), //
                        hasName("XTR.(IE)-MSCI WORLD ESG REGISTERED SHARES 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-03T00:00"), hasShares(8300.2084), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungsnr. 82937342032"), //
                        hasAmount("EUR", 437.75), hasGrossValue("EUR", 437.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

}
