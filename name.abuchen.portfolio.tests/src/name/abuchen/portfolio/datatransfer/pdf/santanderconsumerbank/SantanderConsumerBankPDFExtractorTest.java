package name.abuchen.portfolio.datatransfer.pdf.santanderconsumerbank;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
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
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SantanderConsumerBankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SantanderConsumerBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88579Y1010"));
        assertThat(security.getWkn(), is("851745"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("3M CO. REGISTERED SHARES DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-17T16:53:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getPortfolioTransaction().getSource(), is("Kauf01.txt"));
        assertThat(entry.getPortfolioTransaction().getNote(), is("Auftragsnummer 000000/00.00 | Limit billigst"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(325.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(317.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.90))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0008404005"));
        assertThat(security.getWkn(), is("840400"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-17T16:38:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getPortfolioTransaction().getSource(), is("Kauf02.txt"));
        assertThat(entry.getPortfolioTransaction().getNote(), is("Auftragsnummer 000000/00.00 | Limit billigst"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(430.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(422.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.90 + 0.60))));
    }

    @Test
    public void testDividende01()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88579Y1010"));
        assertThat(security.getWkn(), is("851745"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("3M CO. REGISTERED SHARES DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(2)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 00000000000 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.07))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.96))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        Security security = new Security("3M CO. REGISTERED SHARES DL -,01", CurrencyUnit.EUR);
        security.setIsin("US88579Y1010");
        security.setWkn("851745");

        Client client = new Client();
        client.addSecurity(security);

        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(2)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 00000000000 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.07))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende02()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0008404005"));
        assertThat(security.getWkn(), is("840400"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-10T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(2)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 0000000000"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-30T00:00"), hasShares(495.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 327.96), hasGrossValue("EUR", 327.96), //
                        hasForexGrossValue("USD", 360.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON", CurrencyUnit.EUR);
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        Client client = new Client();
        client.addSecurity(security);

        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-30T00:00"), hasShares(495.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 327.96), hasGrossValue("EUR", 327.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-27T00:00"), hasShares(495.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 291.95), hasGrossValue("EUR", 363.10), //
                        hasForexGrossValue("USD", 390.48), //
                        hasTaxes("EUR", 62.15 + 3.41 + 5.59), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        Security security = new Security("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON", CurrencyUnit.EUR);
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        Client client = new Client();
        client.addSecurity(security);

        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-27T00:00"), hasShares(495.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 291.95), hasGrossValue("EUR", 363.10), //
                        hasTaxes("EUR", 62.15 + 3.41 + 5.59), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testKontoauszug01()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-05-31"), hasAmount("EUR", 1.66), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-05-31"), hasAmount("EUR", 6.63), //
                        hasSource("Kontoauszug01.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-20"), hasAmount("EUR", 1), //
                        hasSource("Kontoauszug01.txt"),
                        hasNote("auf Konto IBAN AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-19"), hasAmount("EUR", 34), //
                        hasSource("Kontoauszug01.txt"),
                        hasNote("von Konto AT123456789012345678 lautend auf Max Muster"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-15"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug01.txt"),
                        hasNote("von Konto AT123456789012345678 lautend auf Max Muster"))));
    }

    @Test
    public void testKontoauszug02()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-12-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug02.txt"), hasNote("Habenzinsen"))));
    }

    @Test
    public void testKontoauszug03()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-01-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-02-28"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-03-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-04-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-05-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-06-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-07-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-08-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-09-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-10-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-11-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));
    }

    @Test
    public void testKontoauszug04()
    {
        SantanderConsumerBankPDFExtractor extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-01-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-03-31"), hasAmount("EUR", 5.87), //
                        hasSource("Kontoauszug04.txt"), hasNote("ÜBERWEISUNG"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-04-28"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("ÜBERWEISUNG"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-06-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-07-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-08-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

    }

}
