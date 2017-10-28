package name.abuchen.portfolio.datatransfer.pdf.hellobank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    public void testErtrag() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("NO0003054108"));
        assertThat(security.getName(), is("M a r i n e H a r v est ASA"));
        assertThat(security.getCurrencyCode(), is("NOK"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2017-09-06")));
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
    public void testErtragWithExistingSecurity() throws IOException
    {
        Security security = new Security("Marine Harvest ASA", CurrencyUnit.EUR);
        security.setIsin("NO0003054108");

        Client client = new Client();
        client.addSecurity(security);

        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ertrag.txt"), errors);

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
        assertThat(transaction.getDate(), is(LocalDate.parse("2017-09-06")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(48.71))));

        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95 + 0.19 + (176.01 / 9.308)))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
    }

    @Test
    public void testKauf() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("NO0003054108"));
        assertThat(security.getName(), is("M a r i n e H a r v est ASA"));
        assertThat(security.getCurrencyCode(), is("NOK"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));

        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1867.77))));
        assertThat(tx.getDate(), is(LocalDate.parse("2017-07-03")));
        assertThat(tx.getShares(), is(Values.Share.factorize(126)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.49))));

        Unit grossValueUnit = tx.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValueUnit.getAmount(), is(Money.of("EUR", Values.Amount.factorize(1865.28))));
        assertThat(grossValueUnit.getForex(), is(Money.of("NOK", Values.Amount.factorize(17640))));
        assertThat(grossValueUnit.getExchangeRate(),
                        is(BigDecimal.ONE.divide(BigDecimal.valueOf(9.457), 10, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    public void testKaufWithExistingSecurity() throws IOException
    {
        Security security = new Security("Marine Harvest ASA", CurrencyUnit.EUR);
        security.setIsin("NO0003054108");

        Client client = new Client();
        client.addSecurity(security);

        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf.txt"), errors);

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

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1867.77))));
        assertThat(tx.getDate(), is(LocalDate.parse("2017-07-03")));
        assertThat(tx.getShares(), is(Values.Share.factorize(126)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.49))));
    }

    @Test
    public void testVerkauf() throws IOException
    {
        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("AU000000SHV6"));
        assertThat(security.getName(), is("S E L E C T H A R V EST LTD."));
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
        assertThat(tx.getDate(), is(LocalDate.parse("2017-10-12")));
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
    public void testVerkaufWithExistingSecurity() throws IOException
    {
        Security security = new Security("SELECT HARVEST LTD.", CurrencyUnit.EUR);
        security.setIsin("AU000000SHV6");

        Client client = new Client();
        client.addSecurity(security);

        HelloBankPDFExtractor extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf.txt"), errors);

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
        assertThat(tx.getDate(), is(LocalDate.parse("2017-10-12")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1000)));
        assertThat(tx.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.99 + 6.42 + 7.95))));
        assertThat(tx.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(254.1 / 1.5181))));
    }

}
