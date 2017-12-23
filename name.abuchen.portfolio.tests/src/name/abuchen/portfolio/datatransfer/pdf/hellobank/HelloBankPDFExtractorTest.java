package name.abuchen.portfolio.datatransfer.pdf.hellobank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.HelloBankPDFExtractor;
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
public class HelloBankPDFExtractorTest
{
    @Test
    public void testErtrag01() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("NO0003054108"));
        assertThat(security.getName(), is("M a r i n e  H a r v est ASA"));
        assertThat(security.getCurrencyCode(), is("NOK"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-09-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(48.71))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95 + 0.19 + (176.01 / 9.308)))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(640 / 9.308))));
        assertThat(grossValueUnit.getForex(), is(Money.of("NOK", Values.Amount.factorize(640))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(9.308), 10, BigDecimal.ROUND_HALF_UP)));

        assertThat(grossValueUnit.getAmount().getAmount() - transaction.getUnitSum(Unit.Type.TAX).getAmount(),
                        is(transaction.getMonetaryAmount().getAmount()));
    }

    @Test
    public void testErtrag01WithExistingSecurity() throws IOException
    {
        Security security = new Security("Marine Harvest ASA", CurrencyUnit.EUR);
        security.setIsin("NO0003054108");

        Client client = new Client();
        client.addSecurity(security);

        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-09-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(48.71))));

        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95 + 0.19 + (176.01 / 9.308)))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
    }

    @Test
    public void testErtrag02() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US56035L1044"));
        assertThat(security.getName(), is("M a i n  S t r e e t Capital Corp."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95 + 0.19 + ((3.05 + 2.55) / 1.0942)))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(110)));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(20.35 / 1.0942))));
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(20.35))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(1.0942), 10, BigDecimal.ROUND_HALF_UP)));

        assertThat(grossValueUnit.getAmount().getAmount() - transaction.getUnitSum(Unit.Type.TAX).getAmount(),
                        is(transaction.getMonetaryAmount().getAmount()));
    }

    @Test
    public void testErtrag03() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("NL0012325773"));
        assertThat(security.getName(), is("R o y a l  D u t c h Shell PLC"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(41.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95 + 0.19 + 8.81 + 7.34))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(140)));

        assertThat(Values.Amount.factorize(58.72) - transaction.getUnitSum(Unit.Type.TAX).getAmount(),
                        is(transaction.getMonetaryAmount().getAmount()));
    }

    @Test
    public void testErtrag04() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US3682872078"));
        assertThat(security.getName(), is("G a z p r o m  P J S C"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.91))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR,
                        Values.Amount.factorize(0.19 + 0.95 + ((32.14 + 26.79 + 16) / 1.1805)))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(800)));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(214.28 / 1.1805))));
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(214.28))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(1.1805), 10, BigDecimal.ROUND_HALF_UP)));

        assertThat(grossValueUnit.getAmount().getAmount() - transaction.getUnitSum(Unit.Type.TAX).getAmount(),
                        is(transaction.getMonetaryAmount().getAmount()));
    }

    @Test
    public void testKauf01() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("NO0003054108"));
        assertThat(security.getName(), is("M a r i n e  H a r v est ASA"));
        assertThat(security.getCurrencyCode(), is("NOK"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));

        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1118.8))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(74)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.20 + 1.46))));

        Unit grossValueUnit = tx.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(1092.14))));
        assertThat(grossValueUnit.getForex(), is(Money.of("NOK", Values.Amount.factorize(10360))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(9.486), 10, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    public void testKauf01WithExistingSecurity() throws IOException
    {
        Security security = new Security("Marine Harvest ASA", CurrencyUnit.EUR);
        security.setIsin("NO0003054108");

        Client client = new Client();
        client.addSecurity(security);

        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));

        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1118.8))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(74)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.20 + 1.46))));
    }

    @Test
    public void testKauf02() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("GB00B03MLX29"));
        assertThat(security.getName(), is("R o y a l  D u t c h Shell"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));

        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1314.03))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-04-27T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(55)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.53 + 1.55))));

        Unit grossValueUnit = tx.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(1305.95))));
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(1100))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(0.8423), 10, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    public void testVerkauf01() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("AU000000SHV6"));
        assertThat(security.getName(), is("S E L E C T  H A R V EST LTD."));
        assertThat(security.getCurrencyCode(), is("AUD"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));

        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3096.85))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-10-12T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1000)));
        assertThat(tx.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.99 + 6.42 + 7.95))));
        assertThat(tx.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(254.1 / 1.5181))));

        Unit grossValueUnit = tx.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(5000 / 1.5181))));
        assertThat(grossValueUnit.getForex(), is(Money.of("AUD", Values.Amount.factorize(5000))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(1.5181), 10, BigDecimal.ROUND_HALF_UP)));

        assertThat(grossValueUnit.getAmount().getAmount() - tx.getUnitSum(Unit.Type.TAX).getAmount()
                        - tx.getUnitSum(Unit.Type.FEE).getAmount(), is(tx.getMonetaryAmount().getAmount()));

    }

    @Test
    public void testVerkauf01WithExistingSecurity() throws IOException
    {
        Security security = new Security("SELECT HARVEST LTD.", CurrencyUnit.EUR);
        security.setIsin("AU000000SHV6");

        Client client = new Client();
        client.addSecurity(security);

        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));

        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3096.85))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-10-12T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1000)));
        assertThat(tx.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.99 + 6.42 + 7.95))));
        assertThat(tx.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(254.1 / 1.5181))));
    }

    @Test
    public void testInboundDelivery01() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InboundDelivery01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DK0060534915"));
        assertThat(security.getName(), is("N o v o - N o r d i s k AS"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));

        PortfolioTransaction tx = (PortfolioTransaction) item.get().getSubject();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3225.37))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-03-29T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(80)));
    }

    @Test
    public void testInboundDelivery02() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InboundDelivery02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US56035L1044"));
        assertThat(security.getName(), is("M a i n  S t r e e t Capital Corp."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));

        PortfolioTransaction tx = (PortfolioTransaction) item.get().getSubject();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3021.70))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2017-03-31T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(110)));
    }
}
