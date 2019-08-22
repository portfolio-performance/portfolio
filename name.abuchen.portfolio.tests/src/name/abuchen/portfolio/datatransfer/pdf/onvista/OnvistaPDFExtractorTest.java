package name.abuchen.portfolio.datatransfer.pdf.onvista;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
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
public class OnvistaPDFExtractorTest
{
    private Security assertSecurityBuyAktien(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CBK1001"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Aktien o.N."));

        return security;
    }

    private Security assertSecurityBuySparplan(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0006289473"));
        assertThat(security.getName(), is("iS.eb.r.Go.G.1.5-2.5y U.ETF DE Inhaber-Anteile"));

        return security;
    }

    private Security assertSecurityBuyBezugsrechte(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRCZ2"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Bezugsrechte"));

        return security;
    }

    private Security assertSecuritySell(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRRB1"));
        assertThat(security.getName(), is("Porsche Automobil Holding SE Inhaber-Bezugsrechte auf VZO"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftKupon(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000TUAG117"));
        assertThat(security.getName(), is("5,5% TUI AG Wandelanl.v.2009(2014)"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftDividende(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CBK1001"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Aktien o.N."));

        return security;
    }

    private Security assertSecurityEinloesung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000TUAG117"));
        assertThat(security.getName(), is("TUI AG Wandelanl.v.2009(2014)"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftErtraegnisgutschrift(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0140355917"));
        assertThat(security.getName(), is("Allianz Euro Bond Fund Inhaber-Anteile A (EUR) o.N."));

        return security;
    }

    private Security assertSecurityWertpapieruebertrag(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0140355917"));
        assertThat(security.getName(), is("Allianz PIMCO Euro Bd Tot.Ret. Inhaber-Anteile A (EUR) o.N."));

        return security;
    }

    private Security assertSecurityErtragsgutschriftDividendeReinvestition(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getName(), is("Deutsche Telekom AG Namens-Aktien o.N."));

        return security;
    }

    private Security assertSecurityErtragsgutschriftDividendeReinvestitionTarget(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000A1TNRX5"));
        assertThat(security.getName(), is("Deutsche Telekom AG Dividend in Kind-Cash Line"));

        return security;
    }

    private Security assertSecurityKapitalherabsetzungOriginal(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0008032004"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Aktien o.N."));

        return security;
    }

    private Security assertSecurityKapitalherabsetzungTransfer(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CBKTLR7"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Teilrechte"));

        return security;
    }

    private Security assertSecurityKapitalherabsetzungZiel(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CBK1001"));
        assertThat(security.getName(), is("Commerzbank AG konv.Inhaber-Aktien o.N."));

        return security;
    }

    private Security assertSecurityKapitalherhöhung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRJ01"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Erwerbsrechte"));

        return security;
    }

    private Security assertSecurityEinAusbuchungDividendenRechte(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A2AA2C3"));
        assertThat(security.getName(), is("Deutsche Telekom AG Dividend in Kind-Cash Line"));

        return security;
    }

    private Security assertSecurityUmtauschZiel(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0165915215"));
        assertThat(security.getName(), is("AGIF-Allianz Euro Bond Inhaber Anteile A (EUR) o.N."));

        return security;
    }

    private Security assertSecurityUmtauschOriginal(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("LU0140355917"));
        assertThat(security.getName(), is("Allianz Euro Bond Fund Inhaber-Anteile A (EUR) o.N."));

        return security;
    }
    
    private Security assertSecurityUmtauschZiel2(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU1900068328"));
        assertThat(security.getName(), is("MUL-Lyx.MSCI AC As.Paci.e.Jap. Act. au Port. EUR Acc. oN"));

        return security;
    }

    private Security assertSecurityUmtauschOriginal2(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("FR0010312124"));
        assertThat(security.getName(), is("Lyxor MSCI AC As.Pa.x Ja.U.ETF Act. au Port. Acc o.N."));

        return security;
    }

    private Security assertSecurityZwangsabfindung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000SKYD000"));
        assertThat(security.getName(), is("Sky Deutschland AG Namens-Aktien o.N."));

        return security;
    }

    private Security assertSecurityDividendeAbfindung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1TNRX5"));
        assertThat(security.getName(), is("Deutsche Telekom AG Dividend in Kind-Cash Line"));

        return security;
    }
    
    private Security assertSecurityFusionNeu(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000A2GS4R1"));
        assertThat(security.getName(), is("Vonovia SE Inhaber-Teilrechte (Gagfah)"));

        return security;
    }
    
    private Security assertSecurityFusionAusbuchung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0269583422"));
        assertThat(security.getName(), is("Gagfah S.A. Actions nom. EO 1,25"));

        return security;
    }
    
    private Security assertSecurityRegistrierungsgebuehrAndDepotauszug2018(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1ML7J1"));
        assertThat(security.getName(), is("Vonovia SE Namens-Aktien o.N."));

        return security;
    }

    private Security assertFirstSecurityDepotauszug(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000PAH0038"));
        assertThat(security.getName(), is("Porsche Automobil Holding SE Inhaber-Vorzugsaktien o.St.o.N"));

        return security;
    }
    
    private Security assertSecondSecurityDepotauszug2018(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000A1YCMM2"));
        assertThat(security.getName(), is("SolarWorld AG Inhaber-Aktien o.N."));

        return security;
    }
    
    private Security assertSecondToLastSecurityDepotauszug2018(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("SG1DD2000002"));
        assertThat(security.getName(), is("Ocean Sky International Ltd Registered"));

        return security;
    }

    @Test
    public void testErtragsgutschriftDividende() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftDividende.txt"), errors);

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
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-04-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testErtragsgutschriftDividende2() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftDividende2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));

        // transaction #1
        Security security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .filter(s -> "FR0010296061".equals(s.getIsin())) //
                        .findFirst().get();
        assertThat(security.getName(), is("Lyxor ETF MSCI USA Actions au Porteur D-EUR o.N."));

        AccountTransaction transaction = results.stream() //
                        .filter(i -> i instanceof Extractor.TransactionItem) //
                        .filter(i -> "FR0010296061".equals(i.getSecurity().getIsin())) //
                        .map(i -> (AccountTransaction) ((Extractor.TransactionItem) i).getSubject()).findFirst().get();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.8))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.0545)));

        // transaction #2
        security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .filter(s -> "FR0010315770".equals(s.getIsin())) //
                        .findFirst().get();
        assertThat(security.getName(), is("Lyxor ETF MSCI WORLD FCP Actions au Port.D-EUR o.N."));

        transaction = results.stream() //
                        .filter(i -> i instanceof Extractor.TransactionItem) //
                        .filter(i -> "FR0010315770".equals(i.getSecurity().getIsin())) //
                        .map(i -> (AccountTransaction) ((Extractor.TransactionItem) i).getSubject()).findFirst().get();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.8))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.2879)));
    }

    @Test
    public void testErtragsgutschriftDividende3()
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftDividende3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // transaction #1
        Security security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(Item::getSecurity) //
                        .filter(s -> "US56035L1044".equals(s.getIsin())) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);
        assertThat(security.getName(), is("Main Street Capital Corp. Registered Shares DL -,01"));

        AccountTransaction transaction = results.stream() //
                        .filter(i -> i instanceof Extractor.TransactionItem) //
                        .filter(i -> i.getSecurity() == security) //
                        .map(i -> (AccountTransaction) ((Extractor.TransactionItem) i).getSubject()) //
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.47))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));

        Money taxes = transaction.getUnitSum(Unit.Type.TAX);
        assertThat(taxes, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.81 + 0.1 + 2.72))));
    }

    @Test
    public void testErtragsgutschriftKupon() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftKupon.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = assertSecurityErtragsgutschriftKupon((SecurityItem) item.get());

        // check transaction
        Optional<Item> transactionItem = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(transactionItem.isPresent(), is(true));
        assertThat(transactionItem.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) transactionItem.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-11-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.14))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41))));
    }

    @Test
    public void testErtragsgutschriftErtraegnisgutschrift() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftErtraegnisgutschrift.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftErtraegnisgutschrift(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.69))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(28)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.96))));
    }

    @Test
    public void testErtragsgutschriftErtraegnisgutschrift2() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftErtraegnisgutschrift2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .findFirst().get();
        assertThat(security.getIsin(), is("DE0002635307"));
        assertThat(security.getName(), is("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"));

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.16))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(5.8192)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testErtragsgutschriftErtraegnisgutschrift3()
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftErtraegnisgutschrift3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .findFirst().get();
        assertThat(security.getIsin(), is("LU0340285161"));
        assertThat(security.getName(), is("UBS-ETF-UBS-ETF MSCI Wld U.ETF Inhaber-Anteile (USD) A-dis"));

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.6))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKaufAktien() throws IOException // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaKaufAktien.txt"), errors);

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
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.55))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.of(2015, 1, 12, 10, 11)));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.05))));
    }

    @Test
    public void testWertpapierKaufAktien2() // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaKaufAktien2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Security security = results.stream().filter(i -> i instanceof SecurityItem).map(Item::getSecurity).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(security.getName(), is("DWS Deutschland Inhaber-Anteile LC"));
        assertThat(security.getIsin(), is("DE0008490962"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(150.01))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-15T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.7445)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testWertpapierKaufAktien3() // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaKaufAktien3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Security security = results.stream().filter(i -> i instanceof SecurityItem).map(Item::getSecurity).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(security.getName(), is("Cisco Systems Inc. Registered Shares DL-,001"));
        assertThat(security.getIsin(), is("US17275R1023"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1536.13))));

        Unit grossValue = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1677.20))));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-19T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(35)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((11.03 + 5.51) / 1.1026))));
    }

    @Test
    public void testWertpapierKaufSparplanMitSteuerausgleich() throws IOException // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaKaufSparplanMitSteuerausgleich.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        assertSecurityBuySparplan(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-07-17T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.5638)));

        // check Steuererstattung
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction entryTaxReturn = (AccountTransaction) item.get().getSubject();
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(entryTaxReturn.getDateTime(), is(is(LocalDateTime.parse("2017-07-18T00:00"))));
    }

    @Test
    public void testWertpapierKaufBezugsrechte() throws IOException // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaKaufBezugsrechte.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyBezugsrechte(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.40))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2011-05-30T12:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));
    }

    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaVerkauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        assertSecuritySell(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.45))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(is(LocalDateTime.parse("2011-04-08T12:30"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.75))));

        // check Steuererstattung
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction entryTaxReturn = (AccountTransaction) item.get().getSubject();
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.28))));
        assertThat(entryTaxReturn.getDateTime(), is(is(LocalDateTime.parse("2011-04-12T00:00"))));
    }

    @Test
    public void testWertpapierEinloesung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaEinloesung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityEinloesung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(51.85))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(is(LocalDateTime.parse("2014-11-17T00:00"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.45))));
    }

    @Test
    public void testWertpapieruebertrag() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaWertpapieruebertragEingang.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityWertpapieruebertrag(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_IN));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(is(LocalDateTime.parse("2011-12-02T00:00"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(28)));
    }

    @Test
    public void testErtragsgutschriftDividendeReinvestition() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaErtragsgutschriftDividendeReinvestition.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check security
        Security security = assertSecurityErtragsgutschriftDividendeReinvestition(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-05-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.50))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));

        assertSecurityErtragsgutschriftDividendeReinvestitionTarget(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));
        Item reinvestItem = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(0);

        // check transaction
        assertThat(reinvestItem.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry2 = (BuySellEntry) reinvestItem.getSubject();
        assertThat(entry2.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry2.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-05-17T00:00")));
        assertThat(entry2.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertThat(entry2.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.50))));
    }

    @Test
    public void testKapitalherabsetzung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaKapitalherabsetzung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        // check security
        Security security = assertSecurityKapitalherabsetzungOriginal(results);

        // check transaction (original security)
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-04-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(55)));

        assertSecurityKapitalherabsetzungTransfer(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));
        Item transferItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1);

        // check transaction (transfer security, in)
        assertThat(transferItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry2 = (PortfolioTransaction) transferItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDateTime(), is(LocalDateTime.parse("2013-04-24T00:00")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(5.5)));

        assertSecurityKapitalherabsetzungZiel(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(2));
        Item transferItem2 = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(2);

        // check transaction (transfer security, out)
        assertThat(transferItem2.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry3 = (PortfolioTransaction) transferItem2.getSubject();
        assertThat(entry3.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry3.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry3.getDateTime(), is(LocalDateTime.parse("2013-04-24T00:00")));
        assertThat(entry3.getShares(), is(Values.Share.factorize(5)));

        assertSecurityKapitalherabsetzungZiel(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(2));
        Item targetItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(3);

        // check transaction (target security)
        assertThat(targetItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry4 = (PortfolioTransaction) targetItem.getSubject();
        assertThat(entry4.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry4.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry4.getDateTime(), is(LocalDateTime.parse("2013-04-24T00:00")));
        assertThat(entry4.getShares(), is(Values.Share.factorize(5)));
    }

    @Test
    public void testKapitalerhoehung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaKapitalerhoehung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityKapitalherhöhung(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDateTime(), is(is(LocalDateTime.parse("2011-04-06T00:00"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testEinbuchungDividendenRechte() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaEinbuchungDividendenRechte.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityEinAusbuchungDividendenRechte(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDateTime(), is(is(LocalDateTime.parse("2016-05-25T00:00"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testAusbuchungDividendenRechte() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaAusbuchungDividendenRechte.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityEinAusbuchungDividendenRechte(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(entry.getDateTime(), is(is(LocalDateTime.parse("2016-06-21T00:00"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testUmtausch() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaUmtausch.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyBezugsrechte(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(entry.getDateTime(), is(is(LocalDateTime.parse("2011-06-06T00:00"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(33)));
    }

    @Test
    public void testUmtauschFonds() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaUmtauschFonds.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));

        // check security
        Security security = assertSecurityUmtauschZiel(results);

        // check transaction (target security, in)
        Optional<Item> item = results.stream() //
                        .filter(i -> i.getSubject() instanceof PortfolioTransaction)
                        .filter(i -> ((PortfolioTransaction) i.getSubject())
                                        .getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(156.729)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check Steuererstattung
        Item itemTaxReturn = results.stream() //
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.TAX_REFUND)
                        .findFirst().get();
        AccountTransaction entryTaxReturn = (AccountTransaction) itemTaxReturn.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.90))));
        assertThat(entryTaxReturn.getDateTime(), is(is(LocalDateTime.parse("2015-11-26T00:00"))));

        // check security (original)
        assertSecurityUmtauschOriginal(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));

        Item targetItem = results.stream() //
                        .filter(i -> i.getSubject() instanceof PortfolioTransaction)
                        .filter(i -> ((PortfolioTransaction) i.getSubject())
                                        .getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        .findFirst().get();

        // check transaction (original security, out)
        PortfolioTransaction entry2 = (PortfolioTransaction) targetItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDateTime(), is(LocalDateTime.parse("2015-11-23T00:00")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(28)));

        // check Steuerbuchung
        Item itemTax = results.stream() //
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.TAXES)
                        .findFirst().get();

        AccountTransaction entryTax = (AccountTransaction) itemTax.getSubject();
        assertThat(entryTax.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(entryTax.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.86))));
        assertThat(entryTax.getDateTime(), is(is(LocalDateTime.parse("2015-11-23T00:00"))));
    }
    
    @Test
    public void testUmtauschFonds2() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaUmtauschFonds2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));

        // check security
        Security security = assertSecurityUmtauschZiel2(results);

        // check transaction (target security, in)
        Optional<Item> item = results.stream() //
                        .filter(i -> i.getSubject() instanceof PortfolioTransaction)
                        .filter(i -> ((PortfolioTransaction) i.getSubject())
                                        .getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.9315)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check security (original)
        assertSecurityUmtauschOriginal2(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));

        Item targetItem = results.stream() //
                        .filter(i -> i.getSubject() instanceof PortfolioTransaction)
                        .filter(i -> ((PortfolioTransaction) i.getSubject())
                                        .getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        .findFirst().get();

        // check transaction (original security, out)
        PortfolioTransaction entry2 = (PortfolioTransaction) targetItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDateTime(), is(LocalDateTime.parse("2019-02-22T00:00")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(1.9315)));

        // check Steuerbuchung
        Item itemTax = results.stream() //
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.TAXES)
                        .findFirst().get();

        AccountTransaction entryTax = (AccountTransaction) itemTax.getSubject();
        assertThat(entryTax.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(entryTax.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27))));
        assertThat(entryTax.getDateTime(), is(is(LocalDateTime.parse("2019-02-22T00:00"))));
        assertThat(entryTax.getCurrencyCode(), is(CurrencyUnit.EUR));
        //assertThat(entryTax.getUnit(Type.TAX).get().getForex().getCurrencyCode(), is(CurrencyUnit.USD));
        //assertThat(entryTax.getUnit(Type.TAX).get().getForex().getAmount(), is(Values.Amount.factorize(0.30)));
    }

    @Test
    public void testWertpapierVerkaufSpitzeMitSteuerrückerstattung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaVerkaufSpitzeMitSteuerErstattung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        assertSecurityKapitalherabsetzungTransfer(
                        results.stream().filter(i -> i instanceof SecurityItem).findFirst().get());

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.41))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(is(LocalDateTime.parse("2013-05-06T12:00"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.5)));

        // check Steuererstattung
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction entryTaxReturn = (AccountTransaction) item.get().getSubject();
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.05))));
        assertThat(entryTaxReturn.getDateTime(), is(is(LocalDateTime.parse("2013-05-06T00:00"))));

    }

    @Test
    public void testZwangsabfindung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaZwangsabfindung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityZwangsabfindung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(167.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(is(LocalDateTime.parse("2015-09-22T00:00"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testDividendeAbfindung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaDividendeAbfindung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityDividendeAbfindung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.50))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(is(LocalDateTime.parse("2013-06-11T00:00"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
    }
    
    @Test
    public void testFusion() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaFusion.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check security
        Security security = assertSecurityFusionAusbuchung(results);

        // check transaction (original security, out)
        Optional<Item> item = results.stream() //
                        .filter(i -> i.getSubject() instanceof PortfolioTransaction)
                        .filter(i -> ((PortfolioTransaction) i.getSubject())
                                        .getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        .findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));

        // check security (new)
        assertSecurityFusionNeu(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));

        Item targetItem = results.stream() //
                        .filter(i -> i.getSubject() instanceof PortfolioTransaction)
                        .filter(i -> ((PortfolioTransaction) i.getSubject())
                                        .getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .findFirst().get();

        // check transaction (new security, in)
        PortfolioTransaction entry2 = (PortfolioTransaction) targetItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDateTime(), is(LocalDateTime.parse("2017-07-04T00:00")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(6.840)));
    }
    
    @Test
    public void testRegistrierungsgebuehr()
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaRegistrierungsgebuehr.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityRegistrierungsgebuehrAndDepotauszug2018(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-24T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.89))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
    }

    @Test
    public void testDepotauszug() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaDepotauszug.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(26));

        // check first security
        Security security = assertFirstSecurityDepotauszug(results);

        // check transaction (first security, in)
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4)));

        // check second security
        assertSecurityErtragsgutschriftKupon(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(3));
        Item secondItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(3);

        // check second transaction (second security, in)
        assertThat(secondItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry2 = (PortfolioTransaction) secondItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDateTime(), is(LocalDateTime.parse("2010-12-31T00:00")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(1)));

    }
    
    @Test
    public void testDepotauszug2018() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaDepotauszug2018.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(22));

        // check first security
        Security security = assertSecurityRegistrierungsgebuehrAndDepotauszug2018(results);

        // check transaction (first security, in)
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6)));

        // check second security
        assertSecondSecurityDepotauszug2018(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));
        Item secondItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1);

        // check second transaction (second security, in)
        assertThat(secondItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry2 = (PortfolioTransaction) secondItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDateTime(), is(LocalDateTime.parse("2018-12-31T00:00")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(600)));
        
        //check second to last security
        assertSecondToLastSecurityDepotauszug2018(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(9));
        Item secondToLastItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(9);

        // check second transaction (second security, in)
        assertThat(secondToLastItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry10 = (PortfolioTransaction) secondToLastItem.getSubject();
        assertThat(entry10.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry10.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry10.getDateTime(), is(LocalDateTime.parse("2018-12-31T00:00")));
        assertThat(entry10.getShares(), is(Values.Share.factorize(1000)));

    }

    @Test
    public void testKontoauszugEinzelneBuchung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaKontoauszugEinzelneBuchung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-10-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.66))));

    }

    @Test
    public void testKontoauszugMehrereBuchungen() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaKontoauszugMehrereBuchungen.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.62))));

    }

    @Test
    public void testKontoauszugMehrereBuchungen2017() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "OnvistaKontoauszugMehrereBuchungen2017.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertTransaction(results, 0, "2017-04-04T00:00", AccountTransaction.Type.DEPOSIT, CurrencyUnit.EUR, 200.00);
        assertTransaction(results, 1, "2017-04-10T00:00", AccountTransaction.Type.REMOVAL, CurrencyUnit.EUR, 0.89);
        assertTransaction(results, 2, "2017-05-03T00:00", AccountTransaction.Type.DEPOSIT, CurrencyUnit.EUR, 200.00);
        assertTransaction(results, 3, "2017-06-01T00:00", AccountTransaction.Type.DEPOSIT, CurrencyUnit.EUR, 100.00);
        assertTransaction(results, 4, "2017-06-02T00:00", AccountTransaction.Type.DEPOSIT, CurrencyUnit.EUR, 200.00);
        assertTransaction(results, 5, "2017-06-26T00:00", AccountTransaction.Type.DEPOSIT, CurrencyUnit.EUR, 300.00);
        assertTransaction(results, 6, "2017-06-26T00:00", AccountTransaction.Type.DEPOSIT, CurrencyUnit.EUR, 200.00);

    }

    private void assertTransaction(List<Item> results, int j, String date, AccountTransaction.Type type, String unit,
                    double amount)
    {
        Item item = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(j);
        assertThat(item.getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.getSubject();

        assertThat(transaction.getType(), is(type));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse(date)));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(unit, Values.Amount.factorize(amount))));
    }

    @Test
    public void testMehrereTransaktionenInEinerDatei() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaMultipartKaufVerkauf.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        List<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList());
        assertThat(item.isEmpty(), is(false));

        Item firstItem = item.get(0);
        assertNotNull(firstItem);
        assertThat(firstItem.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry firstEntry = (BuySellEntry) firstItem.getSubject();

        assertThat(firstEntry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(firstEntry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(firstEntry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(firstEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(firstEntry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(623.49))));
        assertThat(firstEntry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.of(2016, 9, 2, 9, 10)));
        assertThat(firstEntry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5))));

        Item secondItem = item.get(1);
        assertNotNull(secondItem);
        assertThat(secondItem.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry secondEntry = (BuySellEntry) secondItem.getSubject();

        assertThat(secondEntry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(secondEntry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(secondEntry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(secondEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80)));
        assertThat(secondEntry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2508.47))));
        assertThat(secondEntry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-09-02T09:10")));
        assertThat(secondEntry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.5))));
    }

    @Test
    public void testMultiTypeDocument() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OnvistaMultiTypePDFDokument.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        List<Item> securities = results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList());
        assertThat(securities.isEmpty(), is(false));

        Item mphSecurity = securities.get(0);
        assertThat(mphSecurity.getSubject(), instanceOf(Security.class));
        assertThat(((Security) mphSecurity.getSubject()).getIsin(), is("DE000A0L1H32"));

        Item paragonSecurity = securities.get(1);
        assertThat(paragonSecurity.getSubject(), instanceOf(Security.class));
        assertThat(((Security) paragonSecurity.getSubject()).getIsin(), is("DE0005558696"));

        List<Item> buySellItems = results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList());
        assertThat(buySellItems.isEmpty(), is(false));

        Optional<Item> taxReturnOption = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertTrue(taxReturnOption.isPresent());

        Item mph = buySellItems.get(0);
        assertNotNull(mph);
        assertThat(mph.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry mphEntry = (BuySellEntry) mph.getSubject();

        assertThat(mphEntry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(mphEntry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(mphEntry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(mphEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));
        assertThat(mphEntry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.70))));
        assertThat(mphEntry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-09-14T09:02")));
        assertThat(mphEntry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.5))));

        Item paragon = buySellItems.get(1);
        assertNotNull(paragon);
        assertThat(paragon.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry paragonEntry = (BuySellEntry) paragon.getSubject();

        assertThat(paragonEntry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(paragonEntry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(paragonEntry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(paragonEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(55)));
        assertThat(paragonEntry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1665.41))));
        assertThat(paragonEntry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-09-14T12:54")));
        assertThat(paragonEntry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.5))));
        assertThat(paragonEntry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.04))));

        // check tax return
        Item taxReturnItem = taxReturnOption.get();
        assertNotNull(taxReturnItem);
        assertThat(taxReturnItem.getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction taxReturnEntry = (AccountTransaction) taxReturnItem.getSubject();
        assertThat(taxReturnEntry.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.18))));
        assertThat(taxReturnEntry.getDateTime(), is(is(LocalDateTime.parse("2016-09-14T00:00"))));
        Security taxReturnSecurity = taxReturnEntry.getSecurity();
        assertThat(((Security) mphSecurity.getSubject()).getIsin(), is(taxReturnSecurity.getIsin()));
    }
}
