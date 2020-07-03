package name.abuchen.portfolio.datatransfer.pdf.dkb;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
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
public class DkbPDFExtractorTest
{
    private Security assertSecurityBuy(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1HLTD2"));
        assertThat(security.getWkn(), is("A1HLTD"));
        assertThat(security.getName(), is("8,75 % METALCORP GROUP B.V."));

        return security;
    }

    private Security assertSecurityBuyAktien(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("BMG7945E1057"));
        assertThat(security.getWkn(), is("A0ERZ0"));
        assertThat(security.getName(), is("SEADRILL LTD."));

        return security;
    }

    private Security assertSecurityBuyFonds(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF"));

        return security;
    }

    private Security assertSecuritySell1(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("AT0000A0U9J2"));
        assertThat(security.getWkn(), is("A1MLSS"));
        assertThat(security.getName(), is("8,5 % SCHOLZ HOLDING"));

        return security;
    }

    private Security assertSecuritySell2(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005140008"));
        assertThat(security.getWkn(), is("514000"));
        assertThat(security.getName(), is("DEUTSCHE BANK AG"));

        return security;
    }

    private Security assertSecuritySell3(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A2YN900"));
        assertThat(security.getWkn(), is("A2YN90"));
        assertThat(security.getName(), is("TEAMVIEWER AG"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftZinsgutschrift(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertThat(security.getName(), is("PCC SE"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftDividende(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0007100000"));
        assertThat(security.getWkn(), is("710000"));
        assertThat(security.getName(), is("DAIMLER AG NAMENS-AKTIEN O.N."));

        return security;
    }
    
    private Security assertSecurityErtragsgutschriftDividende2019(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005552004"));
        assertThat(security.getWkn(), is("555200"));
        assertThat(security.getName(), is("DEUTSCHE POST AG NAMENS-AKTIEN O.N."));

        return security;
    }
    
    private Security assertSecurityErtragsgutschriftDividendeFremdwaehrung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A0LGQL5"));
        assertThat(security.getWkn(), is("A0LGQL"));
        assertThat(security.getName(), is("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N."));

        return security;
    }
    
    private Security assertSecurityErtragsgutschriftDividendeFremdwaehrung2(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("JP3414750004"));
        assertThat(security.getWkn(), is("471496"));
        assertThat(security.getName(), is("SEIKO EPSON CORP. REGISTERED SHARES O.N."));

        return security;
    }

    private Security assertSecurityErtragsgutschriftDividendeQuellensteuern(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getWkn(), is("865985"));
        assertThat(security.getName(), is("APPLE INC. REGISTERED SHARES O.N."));

        return security;
    }

    private Security assertSecurityRueckzahlung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("XS0149161217"));
        assertThat(security.getWkn(), is("858865"));
        assertThat(security.getName(), is("2,309 % RBS CAPITAL TRUST A"));

        return security;
    }

    private Security assertSecurityRueckzahlungHerabschreibung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH"));

        return security;
    }

    private Security assertSecurityDepotuebertragAusgehend(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getWkn(), is("US9RGR"));
        assertThat(security.getName(), is("24,75 % UBS AG (LONDON BRANCH)"));

        return security;
    }
    
    private Security assertSecurityGenussschein(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("CH0010570767"));
        assertThat(security.getWkn(), is("870503"));
        assertThat(security.getName(), is("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"));

        return security;
    }
    
    private Security assertSecurityBuyFondssparplan(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertThat(security.getName(), is("COMSTA.-MSCI EM.MKTS.TRN U.ETF"));

        return security;
    }

    @Test
    public void testErtragsgutschriftZinsen() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschrift.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftZinsgutschrift(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-04T00:00")));
        assertThat(transaction.getAmount(), is(14452L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testErtragsgutschriftZinsenAlt() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftAlt.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftZinsgutschrift(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-01-02T00:00")));
        assertThat(transaction.getAmount(), is(17302L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.23))));
    }

    @Test
    public void testErtragsgutschriftDividende() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftDividende.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftDividende(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-04-07T00:00")));
        assertThat(transaction.getAmount(), is(9750L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
    }
    
    @Test
    public void testErtragsgutschriftDividende2019() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftDividende2019.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftDividende2019(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-20T00:00")));
        assertThat(transaction.getAmount(), is(10580L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(92)));
    }
    
    @Test
    public void testErtragsgutschriftDividendeFremdwaehrung() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftDividendeFremdwaehrung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftDividendeFremdwaehrung(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-23T00:00")));
        assertThat(transaction.getAmount(), is(779L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(53)));
    }
    
    @Test
    public void testErtragsgutschriftDividendeFremdwaehrung2() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftDividendeFremdwaehrung2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftDividendeFremdwaehrung2(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T00:00")));
        assertThat(transaction.getAmount(), is(13358L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(715)));
        
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.59))));
    }

    @Test
    public void testErtragsgutschriftDividendeQuellensteuern() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftDividendeQuellensteuern.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftDividendeQuellensteuern(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-20T00:00")));
        assertThat(transaction.getAmount(), is(2995L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(66)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.29))));
    }

    @Test
    public void testErtragsgutschriftDividende_2017_12() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DkbErtragsgutschriftDividende_2017_12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B3XNN521"));
        assertThat(security.getWkn(), is("A1JJAG"));
        assertThat(security.getName(), is("DIM.FDS-GLOBAL SMALL COMPANIES REGISTERED SHARES EUR DIS.O.N."));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-07T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(32.93)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(216)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.68))));
    }

    @Test
    public void testInvestmentertraege() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbInvestmentertraege.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B6YX5D40"));
        assertThat(security.getWkn(), is("A1JKS0"));
        assertThat(security.getName(), is("SPDR S&P US DIVID.ARISTOCR.ETF"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-04T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.09)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10.6841)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.39))));
    }

    @Test
    public void testWertpapierKauf() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbKauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        assertSecurityBuy(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2030.66))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-11-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR,
                        Values.Amount.factorize(/* 80.66 */0.00))));

        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.50))));
    }

    @Test
    public void testWertpapierKaufAktien() throws IOException // Aktien
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbKaufAktien.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyAktien(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1760.91))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.91))));
    }

    @Test
    public void testWertpapierKaufFonds() throws IOException // Fonds
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbKaufFonds.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyFonds(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1410.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(29.2893)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierKaufFonds2() throws IOException
    {
        // Fonds, only 3 decimal places for amount of shares
        
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbKaufFonds2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyFonds(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(130.41))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.521)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.41))));
    }
    
    @Test
    public void testWertpapierKaufFonds3() throws IOException
    {
        // Fonds, only 3 decimal places for amount of shares
        
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbKaufFonds3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyFonds(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1201.50))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25.6)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.50))));
    }
    
    @Test
    public void testWertpapierKaufAktienOTC() throws IOException
    {        
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbKaufOTC.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyFonds(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1144.20))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.60))));
    }

    @Test
    public void testWertpapierVerkauf1() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbVerkauf1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecuritySell1(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4937.19))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-10-27T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(60)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(481.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierVerkauf2() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbVerkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecuritySell2(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3000))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-02-10T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierVerkauf3() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbVerkauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecuritySell3(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4123.12))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(109.37 + 6.01))));
    }

    @Test
    public void testWertpapierRueckzahlung() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbRueckzahlung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityRueckzahlung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_IN));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2974.39))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-07-31T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.61))));
    }

    @Test
    public void testWertpapierRueckzahlungNennwertHerabschreibung() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DkbRueckzahlungHerabschreibung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityRueckzahlungHerabschreibung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_IN));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1908.39))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(91.61))));
    }

    @Test
    public void testWertpapierTeilliquidation() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbTeilliquidation.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityRueckzahlungHerabschreibung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_IN));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
    }

    @Test
    public void testDepotuebertragAusgang() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbDepotuebertragAusgehend.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        assertSecurityDepotuebertragAusgehend(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-11-30T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testStornoWertpapierTeilliquidation() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbStornoTeilliquidation.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        assertSecurityRueckzahlungHerabschreibung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1908.39))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        // Steuergutschrift bei Storno:
        Item itemTaxReturn = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(0);
        AccountTransaction entryTaxReturn = (AccountTransaction) itemTaxReturn.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(91.61))));
        assertThat(entryTaxReturn.getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
    }
    
    @Test
    public void testAusschuettungGenussschein() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbAusschuettungGenussschein.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityGenussschein(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getAmount(), is(2772L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.93))));
    }
    
    @Test
    public void testRueckzahlungGenussschein() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbRueckzahlungGenussschein.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityGenussschein(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getAmount(), is(3512L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
    }
    
    @Test
    public void testAusschuettungInvestmentfonds() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbAusschuettungInvestmentfonds.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertThat(security.getWkn(), is("A0RPWH"));
        assertThat(security.getName(), is("iShares Core MSCI World UCITS ETF USD REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-06-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3.02)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.08))));
    }

    @Test
    public void testAusschuettungInvestmentfonds2() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DkbAusschuettungInvestmentfonds2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B3VVMM84"));
        assertThat(security.getWkn(), is("A1JX51"));
        assertThat(security.getName(), is("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.45)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16.3517)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.52 + 0.02))));
    }
    
    @Test
    public void testWertpapierKaufFondsSparplanSammelbeleg() throws IOException
    {
        // Fonds, only 3 decimal places for amount of shares
        
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DkbSammelbelegFondssparplan.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        assertSecurityBuyFondssparplan(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.2394)));
        
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(1).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.1692)));
        
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(2).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3253)));
        
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(3).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3496)));
        
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(4).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-11-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3678)));
        
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(5).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-12-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3160)));

    }    
    
    @Test
    public void testRuecknameInvestmentfonds() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DKBRuecknameInvestmentfonds.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("FR0010524777"));
        assertThat(security.getWkn(), is("LYX0CB")); 
        assertThat(security.getName(), is("LYXOR NEW ENERGY UCITS ETF"));

        // check buy sell transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.69))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-10-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.982)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.10))));
    }

}
