package name.abuchen.portfolio.datatransfer.pdf.quirion;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.QuirionPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class QuirionPDFExtractorTest
{

    @Test
    public void testQuirionKontoauszug01()
    {
        QuirionPDFExtractor extractor = new QuirionPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(results.size(), is(27));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Item item;
        Iterator<Extractor.Item> iter;

        // get securities
        iter = results.stream().filter(i -> i instanceof SecurityItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof SecurityItem).count(), is(9L));
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00BL25JM42"));
            assertThat(security.getName(), is("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("LU1681045370"));
            assertThat(security.getName(), is("AIS-Amundi MSCI EMERG.MARKETS Namens-Anteile C Cap.EUR"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00BCBJG560"));
            assertThat(security.getName(), is("SPDR MSCI Wrld Small Cap U.ETF Registered Shares o.N."));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("LU1931974692"));
            assertThat(security.getName(), is("Amundi Index Solu.-A.PRIME GL. Nam.-Ant.UCI.ETF DR USD Dis.oN"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("LU1931975079"));
            assertThat(security.getName(), is("Amundi I.S.-A.PRIME EURO CORP. Nam.-Ant.UC.ETF DR EUR"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00BYML9W36"));
            assertThat(security.getName(), is("I.M.-I.S&P 500 UETF Reg.Shares Dist o.N."));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B95PGT31"));
            assertThat(security.getName(), is("Vanguard FTSE Japan UCITS ETF Registered Shares USD Dis.oN"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B4L60045"));
            assertThat(security.getName(), is("iShsIII-EO Corp Bd 1-5yr U.ETF Registered Shares o.N."));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B42THM37"));
            assertThat(security.getName(), is("Dimensional Fds-Emerg.Mkts Va. Registered Shares EUR Dis.o.N."));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        // get transactions
        iter = results.stream().filter(i -> i instanceof BuySellEntryItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof BuySellEntryItem).count(), is(4L));
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(428.06)));
            assertThat(entry.getPortfolioTransaction().getGrossValue().getAmount(),
                            is(Values.Amount.factorize(428.06)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-14T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16.091)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(408.26)));
            assertThat(entry.getPortfolioTransaction().getGrossValue().getAmount(),
                            is(Values.Amount.factorize(408.26)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-05T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(102.054)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(235.86)));
            assertThat(entry.getPortfolioTransaction().getGrossValue().getAmount(),
                            is(Values.Amount.factorize(235.86)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-10T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.648)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(110.86)));
            assertThat(entry.getPortfolioTransaction().getGrossValue().getAmount(),
                            is(Values.Amount.factorize(110.86)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-07T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.763)));
        }

        // check deposit (Lastschrift) transaction

        iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(14L));

        // assert transaction
        if (iter.hasNext())
        {
            // get transaction1
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-28T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(3000.00)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction2
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-19T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(5000.00)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction3
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.84)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction4
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(2000.00)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction5
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-12T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(36.82)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction6
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-03T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.4)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction7
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-19T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(5002.84)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction8
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.4)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction9
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-03T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.49)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
        if (iter.hasNext())
        {
            // get transaction10
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(156.33)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getGrossValue().getAmount(), is(Values.Amount.factorize(158.23)));
            assertThat(transaction.getGrossValue().getCurrencyCode(), is("EUR"));
        }
        if (iter.hasNext())
        {
            // get transaction11
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-28T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(54.86)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getGrossValue().getAmount(), is(Values.Amount.factorize(54.87)));
            assertThat(transaction.getGrossValue().getCurrencyCode(), is("EUR"));
        }
        if (iter.hasNext())
        {
            // get transaction12
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(11.54)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getGrossValue().getAmount(), is(Values.Amount.factorize(11.54)));
            // TODO misses 2.83 and 0.15 USD taxes (exchange rate missing)
        }
        if (iter.hasNext())
        {
            // get transaction13
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-08T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.94)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getGrossValue().getAmount(), is(Values.Amount.factorize(0.94)));
            // TODO misses 0.22 and 0.01 USD taxes (exchange rate missing)
        }
        if (iter.hasNext())
        {
            // get transaction14
            item = iter.next();
            AccountTransaction transaction = (AccountTransaction) item.getSubject();

            assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(5.35)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getGrossValue().getAmount(), is(Values.Amount.factorize(7.25)));
            assertThat(transaction.getGrossValue().getCurrencyCode(), is("EUR"));
        }

    }
}
