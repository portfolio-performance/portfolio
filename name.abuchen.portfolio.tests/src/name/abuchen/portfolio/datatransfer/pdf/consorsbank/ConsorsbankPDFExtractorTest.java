package name.abuchen.portfolio.datatransfer.pdf.consorsbank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.NonImportableItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ConsorsbankPDFExtractorTest
{

    private Security assertSecurity(List<Item> results, boolean mustHaveIsin)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        if (mustHaveIsin)
            assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMS.-MSCI WORL.T.U.ETF I"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = assertSecurity(results, false);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertDividendTransaction(security, item);

        // check tax
        AccountTransaction t = (AccountTransaction) item.get().getSubject();
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 111_00L + 6_10L)));
    }

    private void assertDividendTransaction(Security security, Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", 326_90L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1370)));
    }

    @Test
    public void testErtragsgutschrift2() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift2.txt"), errors);

        assertThat(errors, empty());

        // since taxes are zero, no tax transaction must be created
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1444))));
    }

    @Test
    public void testErtragsgutschrift3() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift3.txt"), errors);

        assertThat(errors, empty());

        // since taxes are zero, no tax transaction must be created
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getWkn(), is("850866"));
        assertThat(security.getName(), is("DEERE & CO."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findAny().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2015-11-02T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(300)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 121_36)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 180_00)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 16_30 + 89 + 24_45)));
    }

    @Test
    public void testErtragsgutschrift4() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift4.txt"), errors);

        assertThat(errors, empty());

        // since taxes are zero, no tax transaction must be created
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("854242"));
        assertThat(security.getName(), is("WESTPAC BANKING CORP."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2015-07-02T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(1.0002)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 46)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("AUD", 93)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 16)));
    }

    @Test
    public void testErtragsgutschrift5() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("885823"));
        assertThat(security.getName(), is("GILEAD SCIENCES INC."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2015-06-29T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(0.27072)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 8)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 12)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 1 + 2)));
    }

    @Test
    public void testErtragsgutschrift8() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift8.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("891106"));
        assertThat(security.getName(), is("ROCHE HOLDING AG"));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2014-04-22T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(80)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 33_51)));
        assertThat(t.getGrossValue(), is(Money.of("EUR", 64_08)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 30_57)));

        checkCurrency(CurrencyUnit.EUR, t);
    }

    @Test
    public void testErtragsgutschrift9() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift9.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("A1409D"));
        assertThat(security.getName(), is("Welltower Inc."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2016-05-20T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(50)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 32_51)));
        assertThat(t.getGrossValue(), is(Money.of("EUR", 38_25)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 5_74)));

        checkCurrency(CurrencyUnit.EUR, t);
    }

    @Test
    public void testErtragsgutschrift8WithExistingSecurity() throws IOException
    {
        Client client = new Client();
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(client);

        Security existingSecurity = new Security("ROCHE HOLDING AG", CurrencyUnit.EUR);
        existingSecurity.setWkn("891106");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift8.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getSecurity(), is(existingSecurity));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2014-04-22T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(80)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 33_51)));
        assertThat(t.getGrossValue(), is(Money.of("EUR", 64_08)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 30_57)));

        checkCurrency(CurrencyUnit.EUR, t);
    }

    @Test
    public void testErtragsgutschrift10() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream() //
                        .filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()) //
                        .findAny().get();

        assertThat(t.getSecurity().getName(), is("OMNICOM GROUP INC. Registered Shares DL -,15"));
        assertThat(t.getSecurity().getIsin(), is("US6819191064"));
        assertThat(t.getSecurity().getWkn(), is("871706"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.USD));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2018-01-09T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(25)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.34))));

        Unit grossValue = t.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.54))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(15.00))));

        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.54))));

        assertThat(t.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06 + 1.26 + 1.88))));
    }

    @Test
    public void testErtragsgutschrift10WithExistingSecurityInTransactionCurrency() throws IOException
    {
        Client client = new Client();

        Security security = new Security("Omnicom", CurrencyUnit.EUR);
        security.setIsin("US6819191064");
        client.addSecurity(security);

        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = results.stream() //
                        .filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()) //
                        .findAny().get();

        assertThat(t.getSecurity(), is(security));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2018-01-09T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(25)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.34))));

        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.54))));

        assertThat(t.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06 + 1.26 + 1.88))));
    }

    @Test
    public void testErtragsgutschrift11() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream() //
                        .filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()) //
                        .findAny().get();

        assertThat(t.getSecurity().getName(), is("ComStage - MDAX UCITS ETF Inhaber-Anteile I o.N."));
        assertThat(t.getSecurity().getIsin(), is("LU1033693638"));
        assertThat(t.getSecurity().getWkn(), is("ETF007"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.EUR));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2018-08-23T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(43)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.36))));

        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.51 + 0.19))));
    }

    @Test
    public void testErtragsgutschrift12() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream() //
                        .filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()) //
                        .findAny().get();

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(t, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(t.getSecurity().getName(), is("SAMSUNG ELECTRONICS CO. LTD. R.Shs(NV)Pf(GDR144A)/25 SW 100"));
        assertThat(t.getSecurity().getIsin(), is("US7960502018"));
        assertThat(t.getSecurity().getWkn(), is("881823"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.USD));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-11-27T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(3)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.80))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.52 + 2.06 + 0.10))));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 22_65)));
    }

    @Test
    public void testErtragsgutschrift12withSecurity() throws IOException
    {
        Client client = new Client();
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(client);
        
        Security existingSecurity = new Security("SAMSUNG ELECTRONICS CO. LTD. R.Shs(NV)Pf(GDR144A)/25 SW 100", CurrencyUnit.EUR);
        existingSecurity.setIsin("US7960502018");
        existingSecurity.setWkn("881823");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankErtragsgutschrift12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = results.stream() //
                        .filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()) //
                        .findAny().get();

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(t, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-11-27T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(3)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.80))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.52 + 2.06 + 0.10))));
    }

    @Test
    public void testBezug1() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankBezug1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0V9L94"));
        assertThat(security.getWkn(), is("A0V9L9"));
        assertThat(security.getName(), is("EYEMAXX R.EST.AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 399_96L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-05-10T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(66_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 3_96L)));
    }

    @Test
    public void testWertpapierVerkauf1() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("A0DPR2"));
        assertThat(security.getName(), is("VOLKSWAGEN AG VZ ADR1/5"));

        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        Item item = results.get(0);
        BuySellEntry entry = (BuySellEntry) item.getSubject();
        PortfolioTransaction t = entry.getPortfolioTransaction();
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5794_56L)));
        assertThat(t.getUnitSum(Type.FEE), is(Money.of(CurrencyUnit.EUR, 26_65L)));
        assertThat(t.getUnitSum(Type.TAX), is(Money.of(CurrencyUnit.EUR, 226_79L)));
        assertThat(t.getDateTime(), is(LocalDateTime.of(2015, 2, 18, 12, 10, 30)));
        assertThat(t.getShares(), is(Values.Share.factorize(140)));
        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(43.2))));
    }

    @Test
    public void testWertpapierVerkauf2() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("PX2LEH"));
        assertThat(security.getName(), is("BNP PAR.EHG MINIS XAU"));

        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        Item item = results.get(0);
        BuySellEntry entry = (BuySellEntry) item.getSubject();
        PortfolioTransaction t = entry.getPortfolioTransaction();
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1386_96L)));
        assertThat(t.getUnitSum(Type.TAX), is(Money.of(CurrencyUnit.EUR, 1_04L)));
        assertThat(t.getDateTime(), is(LocalDateTime.of(2020, 3, 2, 14, 51, 46)));
        assertThat(t.getShares(), is(Values.Share.factorize(100)));
        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(13.88))));
    }

    @Test
    public void testWertpapierKauf1() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertSecurity(results, true);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5000_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.of(2015, 1, 15, 8, 13, 0)));
        assertThat(entry.getPortfolioTransaction().getShares(), is(132_802120L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> secItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(secItem.isPresent(), is(true));
        Security security = ((SecurityItem) secItem.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A0L1NN5"));
        assertThat(security.getWkn(), is("A0L1NN"));
        assertThat(security.getName(), is("HELIAD EQ.PARTN.KGAA"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1387_85L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.of(2015, 9, 21, 12, 45, 0)));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 17_85L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(5.48))));
    }

    @Test
    public void testWertpapierKauf3() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> secItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(secItem.isPresent(), is(true));
        Security security = ((SecurityItem) secItem.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A0J3UF6"));
        assertThat(security.getWkn(), is("A0J3UF"));
        assertThat(security.getName(), is("EARTH EXPLORAT.FDS UI EOR"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 25_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.of(2017, 10, 16, 15, 24, 0)));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.95126)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 61L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(25.6397))));
    }

    @Test
    public void testWertpapierKauf4()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B9KNR336"));
        assertThat(security.getWkn(), is("A1T8GC"));
        assertThat(security.getName(), is("SPDR S+P P.AS.DIV.ARI.ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9745.25))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.of(2020, 1, 15, 12, 00, 0)));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(210)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.5 + 11.54 + 24.26 + 4.95))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(46.2))));
    }

    @Test
    public void testWertpapierKauf5()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0000120628"));
        assertThat(security.getWkn(), is("855705"));
        assertThat(security.getName(), is("AXA S.A. INH.     EO 2,29"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-02-03T08:02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.01514)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));
    }

    @Test
    public void testWertpapierKauf6()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US92243N1037"));
        assertThat(security.getWkn(), is("A2P1CV"));
        assertThat(security.getName(), is("VECTO.ACQ.CORP. DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(525.92))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-11T15:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.95))));
        assertThat(entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE).get().getForex(),
                        is(Money.of("USD", 540_00)));

    }

    @Test
    public void testWertpapierKaufSparplan() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKaufSparplan.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> secItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(secItem.isPresent(), is(true));
        Security security = ((SecurityItem) secItem.get()).getSecurity();
        assertThat(security.getIsin(), is("PO6527623674"));
        assertThat(security.getWkn(), is("SP110Y"));
        assertThat(security.getName(), is("Sparplanname"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-06-15T11:07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.43915)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(15.53))));
    }

    @Test
    public void testWertpapierKaufIfSecurityIsPresent() throws IOException
    {
        Client client = new Client();
        Security s = new Security();
        s.setName("COMS.-MSCI WORL.T.U.ETF I");
        s.setWkn("ETF110");
        client.addSecurity(s);

        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        Item item = results.get(0);
        BuySellEntry entry = (BuySellEntry) item.getSubject();
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5000_00L)));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung1() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "ConsorsbankNachtraeglicheVerlustverrechnung1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction t = (AccountTransaction) item.get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.61))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2017-07-10T00:00")));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung2() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "ConsorsbankNachtraeglicheVerlustverrechnung2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction t = (AccountTransaction) item.get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.1))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2017-07-10T00:00")));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung3() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "ConsorsbankNachtraeglicheVerlustverrechnung3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof NonImportableItem).findFirst();
        assertThat(item.isPresent(), is(true));
    }

    @Test
    public void testVorabpauschale01() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "ConsorsbankVorabpauschale01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction t = (AccountTransaction) item.get().getSubject();

        assertThat(t.getSecurity().getName(), is("L&G-L&G R.Gbl Robot.Autom.UETF Bearer Shares (Dt. Zert.) o.N."));
        assertThat(t.getSecurity().getIsin(), is("DE000A12GJD2"));
        assertThat(t.getSecurity().getWkn(), is("A12GJD"));

        assertThat(t.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.73))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
    }

    
    private void checkCurrency(final String accountCurrency, AccountTransaction transaction)
    {
        Account account = new Account();
        account.setCurrencyCode(accountCurrency);
        Status status = new CheckCurrenciesAction().process(transaction, account);
        assertThat(status.getCode(), is(Code.OK));
    }
}
