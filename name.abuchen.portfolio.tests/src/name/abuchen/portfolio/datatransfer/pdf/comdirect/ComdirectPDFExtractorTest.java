package name.abuchen.portfolio.datatransfer.pdf.comdirect;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
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
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ComdirectPDFExtractorTest
{

    @Test
    public void testWertpapierKauf()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Name der Security Inhaber-Anteile"));
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertThat(security.getWkn(), is("BASF11"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1.0)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2000-01-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
    }

    @Test
    public void testWertpapierKauf2()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("ComSta foobar .ETF Inhaber-Anteile I o.N."));
        assertThat(security.getIsin(), is("LU1234444444"));
        assertThat(security.getWkn(), is("ETF999"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1413.46)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2011-01-01T09:04")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(13.6))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(42)));
    }

    @Test
    public void testWertpapierKauf3()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("NXP Semiconductors NV Aandelen aan toonder EO -,20"));
        assertThat(security.getIsin(), is("NL0009538784"));
        assertThat(security.getWkn(), is("A1C5WJ"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(822.66)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-06-27T17:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(9.9))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(12)));
    }

    @Test
    public void testWertpapierKauf4()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Medtronic PLC Registered Shares DL -,0001"));
        assertThat(security.getIsin(), is("IE00BTN1Y115"));
        assertThat(security.getWkn(), is("A14M2J"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1431.40)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-11-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(11.40))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
    }

    @Test
    public void testWertpapierKauf5()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Boeing Co. Registered Shares DL 5"));
        assertThat(security.getIsin(), is("US0970231058"));
        assertThat(security.getWkn(), is("850471"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(19359.18)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-07-18T17:02")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(55.66))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(160)));
    }

    @Test
    public void testWertpapierKauf6()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("BayWa AG vink. Namens-Aktien o.N."));
        assertThat(security.getIsin(), is("DE0005194062"));
        assertThat(security.getWkn(), is("519406"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(16312.80)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-03-14T12:09")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(47.66))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1437)));
    }

    @Test
    public void testWertpapierKauf7()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf7.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Allianz SE vink.Namens-Aktien o.N."));
        assertThat(security.getIsin(), is("DE0008404005"));
        assertThat(security.getWkn(), is("840400"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(7586.80)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2008-10-16T09:54")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(23.80 + 1.5 + 0.6))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testWertpapierKauf8()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf8.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("T. Rowe Price Group Inc. Registered Shares DL -,20"));
        assertThat(security.getIsin(), is("US74144T1088"));
        assertThat(security.getWkn(), is("870967"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1469.55)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-11-08T11:51")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(2.9 + 9.9))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testWertpapierKauf9()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf9.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Fresenius SE & Co. KGaA Inhaber-Aktien o.N."));
        assertThat(security.getIsin(), is("DE0005785604"));
        assertThat(security.getWkn(), is("578560"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(49.96)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-04-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.74))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.805)));
    }

    @Test
    public void testWertpapierKauf10()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Toromont Industries Ltd. Registered Shares o.N."));
        assertThat(security.getIsin(), is("CA8911021050"));
        assertThat(security.getWkn(), is("914305"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1686.80)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-02-21T15:43")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(20.80))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(34)));
    }

    @Test
    public void testWertpapierKauf11()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Amazon.com Inc. Registered Shares DL -,01"));
        assertThat(security.getIsin(), is("US0231351067"));
        assertThat(security.getWkn(), is("906866"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(4444.15)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-16T18:33")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(31.79))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
    }

    @Test
    public void testWertpapierKauf11WithSecurityInUSD()
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        Security existingSecurity = new Security("Amazon.com Inc. Registered Shares DL -,01", CurrencyUnit.USD);
        existingSecurity.setIsin("US0231351067");
        existingSecurity.setWkn("906866");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item;

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(entry.getAccountTransaction(), account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(4444.15)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-16T18:33")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(31.79))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        Unit grossValue = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4412.36))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4768.00))));
    }

    @Test
    public void testWertpapierVerkauf()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Verkauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("FooBar. ETF Inhaber-Anteile I o.N."));
        assertThat(security.getIsin(), is("DE1234567890"));
        assertThat(security.getWkn(), is("ABC123"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        // expected total is total amount minux taxes
        long expectedTotal = Values.Amount.factorize(10111.11 - 11.11);
        assertThat(entry.getPortfolioTransaction().getAmount(), is(expectedTotal));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-01-01T10:57")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(11.51))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(11.11))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testWertpapierVerkauf2()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Verkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("Boeing Co. Registered Shares DL 5"));
        assertThat(security.getIsin(), is("US0970231058"));
        assertThat(security.getWkn(), is("850471"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(20413.33)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-12-08T17:03")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(56.07))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1140)));
    }

    @Test
    public void testWertpapierVerkauf3()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Verkauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("ITC Holdings Corp. Registered Shares o. N."));
        assertThat(security.getIsin(), is("US4656851056"));
        assertThat(security.getWkn(), is("A0F401"));

        // purchase
        PortfolioTransaction txP = ((BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject()).getPortfolioTransaction();

        assertThat(txP.getType(), is(PortfolioTransaction.Type.SELL));

        assertThat(txP.getAmount(), is(Values.Amount.factorize(21239.83)));
        assertThat(txP.getDateTime(), is(LocalDateTime.parse("2016-02-25T00:00")));
        assertThat(txP.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(66.47))));
        assertThat(txP.getShares(), is(Values.Share.factorize(570)));

        // tax refund
        AccountTransaction txA = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(txA.getSecurity(), is(security));
        assertThat(txA.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(71.73))));
    }

    @Test
    public void testWertpapierVerkauf4()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Verkauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("Wirecard AG Inhaber-Aktien o.N."));
        assertThat(security.getIsin(), is("DE0007472060"));
        assertThat(security.getWkn(), is("747206"));

        // purchase
        PortfolioTransaction txP = ((BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject()).getPortfolioTransaction();

        assertThat(txP.getType(), is(PortfolioTransaction.Type.SELL));

        assertThat(txP.getAmount(), is(Values.Amount.factorize(-8.86)));
        assertThat(txP.getDateTime(), is(LocalDateTime.parse("2020-08-25T14:34")));
        assertThat(txP.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(12.40))));
        assertThat(txP.getShares(), is(Values.Share.factorize(3)));
    }

    @Test
    public void testWertpapierVerkauf5()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectWertpapierabrechnung_Verkauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("Sunrun Inc. Registered Shares DL -,0001"));
        assertThat(security.getIsin(), is("US86771W1053"));
        assertThat(security.getWkn(), is("A14V1T"));

        // purchase
        PortfolioTransaction txP = ((BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject()).getPortfolioTransaction();

        assertThat(txP.getType(), is(PortfolioTransaction.Type.SELL));

        assertThat(txP.getAmount(), is(Values.Amount.factorize(1263.05)));
        assertThat(txP.getDateTime(), is(LocalDateTime.parse("2020-12-15T20:37")));
        assertThat(txP.getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(12.90 + 13.90 / 1.222500))));
        assertThat(txP.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testGutschrift1()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectGutschrift1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000A9AXXX6"));
        assertThat(security.getName(), is("i S h a r e s I I I x x x x x x x x x x x x x x x E T F"));
        assertThat(security.getWkn(), is("A1XXXX"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2011-01-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(21.99)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(14)));
    }

    @Test
    public void testGutschrift2()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectGutschrift2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US0991991039"));
        assertThat(security.getName(), is("F oo B a r I n c ."));
        assertThat(security.getWkn(), is("123456"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2011-01-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(13.78)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(40)));
    }

    @Test
    public void testGutschrift3()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectGutschrift3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US74348T1025"));
        assertThat(security.getName(), is("P  r os p e c t  C  ap i t a l   C o r p."));
        assertThat(security.getWkn(), is("A0B746"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(7.52)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(175)));
    }

    @Test
    public void testGutschrift4()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectGutschrift4.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00B9M6RS56"));
        assertThat(security.getName(), is("i S hs  VI -  JP  M  D L  EM  B d   E OH  U .  ET F D"));
        assertThat(security.getWkn(), is("A1W0MQ"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-31T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(8.54)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(42)));
    }

    @Test
    public void testGutschrift5()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectGutschrift5.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getName(), is("A p  pl e  I  nc  ."));
        assertThat(security.getWkn(), is("865985"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-11-20T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.5)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(9.914)));
    }

    @Test
    public void testDividende1()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("NL0000009355"));
        assertThat(security.getName(), is("U n il  e ve r  N . V  ."));
        assertThat(security.getWkn(), is("A0JMZB"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-12-15T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(335.92)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1900)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(59.28)));
    }

    @Test
    public void testDividende2()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0008232125"));
        assertThat(security.getName(), is("De u t s  c he   L uf  t h a n s a A G"));
        assertThat(security.getWkn(), is("823212"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2009-04-27T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1546.13)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3000)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(525 + 28.87)));
    }

    @Test
    public void testDividende3()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US1266501006"));
        assertThat(security.getName(), is("C V  S  He a  lt h  C  or  p ."));
        assertThat(security.getWkn(), is("859034"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-11-07T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(11.65)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
    }

    @Test
    public void testDividende3withTax()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende3.txt",
                        "comdirectSteuermitteilung_Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US1266501006"));
        assertThat(security.getName(), is("C V  S  He a  lt h  C  or  p ."));
        assertThat(security.getWkn(), is("859034"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-11-07T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.21)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.49)));
    }

    @Test
    public void testDividende3withTaxFirst()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(),
                        "comdirectSteuermitteilung_Dividende03.txt", "comdirectDividende3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US1266501006"));
        // assertThat(security.getName(), is("C V S He a lt h C or p ."));
        assertThat(security.getWkn(), is("859034"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-11-07T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.21)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.49)));
    }

    @Test
    public void testDividende3withSecurity()
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        Security existingSecurity = new Security("C V  S  He a  lt h  C  or  p .", CurrencyUnit.USD);
        existingSecurity.setIsin("US1266501006");
        existingSecurity.setWkn("859034");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende3.txt",
                        "comdirectSteuermitteilung_Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-11-07T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.21)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.49)));

        Unit grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.70))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(16.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

    }

    @Test
    public void testDividende4()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende4.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US6516391066"));
        assertThat(security.getWkn(), is("853823"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-23T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(9.71)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(88)));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende4WithSecurity()
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        Security existingSecurity = new Security("N e w m  o n t C o r p .", CurrencyUnit.USD);
        existingSecurity.setIsin("US6516391066");
        existingSecurity.setWkn("853823");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectDividende4.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(9.71)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(88)));

        Unit grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.42))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(12.32))));

        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.72))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testMergeDividendeWithTaxFileWithoutTax()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectMerge1_Dividende.txt",
                        "comdirectMerge1_Steuer.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0008404005"));
        assertThat(security.getName(), is("Al l  i an z   S E"));
        assertThat(security.getWkn(), is("840400"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-14T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(128.00)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getNote(), is("comdirectMerge1_Dividende.txt"));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.0)));
    }

    @Test
    public void testMergeDividendeWithNewFooter()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectMerge2_Dividende.txt",
                        "comdirectMerge2_Steuer.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US1266501006"));
        assertThat(security.getName(), is("C VS  H e  a lt  h  Co  r p."));
        assertThat(security.getWkn(), is("859034"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-04T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.19)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
        assertThat(transaction.getNote(), is("comdirectMerge2_Steuer.txt"));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.51)));
    }
    
    @Test
    public void testMergeDividende3SameTaxAmount()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectMerge3_Dividende.txt", 
                        "comdirectMerge3_Steuer.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("CA8911021050"));
        assertThat(security.getName(), is("To r o  m on  t  I n du  st r i e  s L  t d ."));
        assertThat(security.getWkn(), is("914305"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-07T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5.03)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(34)));
        assertThat(transaction.getNote(), is("comdirectMerge3_Steuer.txt"));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(1.68)));
    }

    @Test
    public void testInvestmentAusschuettung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectInvestmentAusschuettung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M63284"));
        assertThat(security.getName(), is("IS EUR.PROP.YI.U.ETF EOD"));
        assertThat(security.getWkn(), is("A0HGV5"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findAny().orElseThrow(IllegalArgumentException::new).getSubject();

        // dividend
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-27T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.98)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(15.558)));
    }

    @Test
    public void testVorabsteuerpauschale()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "comdirectVorabsteuerpauschale.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BP3QZJ36"));
        assertThat(security.getName(), is("ISIV-MSCI FRAN. U.ETF EOA"));
        assertThat(security.getWkn(), is("A12ATD"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // tax
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.TAXES.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getShares(), is(Values.Share.factorize(11.486)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-14T00:00")));
        assertThat(transaction.getUnitSum(Type.TAX), is(Money.of("EUR", 0_07 + 2L)));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.09)));
    }

    @Test
    public void testDividendeAusSteuermitteilung1()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectSteuermitteilung_Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US7427181091"));
        assertThat(security.getName(), is("PROCTER GAMBLE"));
        assertThat(security.getWkn(), is("852062"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(302.55)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(53.40)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(518)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-20T00:00")));
    }

    @Test
    public void testDividendeAusSteuermitteilung2()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectSteuermitteilung_Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("AN8068571086"));
        assertThat(security.getName(), is("SCHLUMBERGER   DL-,01"));
        assertThat(security.getWkn(), is("853390"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(58.44)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.00)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(130)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-14T00:00")));
    }

    @Test
    public void testInvestmentAusschuettung02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectInvestmentAusschuettung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000ETF7011"));
        assertThat(security.getName(), is("CS VERMOEG.STRATE.U.ETF I"));
        assertThat(security.getWkn(), is("ETF701"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.05)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0.088)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-10-31T00:00")));
    }

    @Test
    public void testDividendeAusSteuermitteilung3()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectSteuermitteilung_Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US1266501006"));
        assertThat(security.getName(), is("CVS HEALTH CORP.   DL-,01"));
        assertThat(security.getWkn(), is("859034"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.21)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.49)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(32)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-11-07T00:00")));
    }

    @Test
    public void testDividendeAusSteuermitteilung4()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectSteuermitteilung_Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0006766504"));
        assertThat(security.getName(), is("AURUBIS AG"));
        assertThat(security.getWkn(), is("676650"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(45.12)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(17.38)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-03T00:00")));
    }

    @Test
    public void testSteuermitteilungNumberShares1()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectSteuermitteilung_Number_Shares_1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US88579Y1010"));
        assertThat(security.getName(), is("3M CO.             DL-,01"));
        assertThat(security.getWkn(), is("851745"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getShares(), is(Values.Share.factorize(13)));
    }

    @Test
    public void testSteuermitteilungNumberShares2()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "comdirectSteuermitteilung_Number_Shares_2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("LU0496786574"));
        assertThat(security.getName(), is("MUL-LYX.S+P500UC.ETF DEO"));
        assertThat(security.getWkn(), is("LYX0FS"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getShares(), is(Values.Share.factorize(78.416)));
    }

    @Test
    public void testGebuehrenAusVerwahrentgelt()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "comdirectVerwahrentgeld.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getWkn(), is("A0S9GB"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.0)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-06T00:00")));
    }
}
