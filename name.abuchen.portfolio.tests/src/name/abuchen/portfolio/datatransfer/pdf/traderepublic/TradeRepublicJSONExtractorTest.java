package name.abuchen.portfolio.datatransfer.pdf.traderepublic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.JSONPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeRepublicJSONExtractorTest
{
    private Security assertSecurity(List<Item> results, String isin, String name, String currency)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is(isin));
        assertThat(security.getName(), is(name));
        assertThat(security.getCurrencyCode(), is(currency));

        return security;
    }
    
    private void assertDividendTransaction(List<Item> results,Security security,String date, double amount,double tax, double shares)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse(date)));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(amount)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(tax))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(shares)));
    }

    @Test
    public void testDividende01()
    {
        JSONPDFExtractor extractor = new JSONPDFExtractor(new Client(), "trade-republic-dividends.json");

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeRepublicDividende01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = assertSecurity(results, "US5949181045", "Microsoft Corp.", CurrencyUnit.USD);

        // transaction
        assertDividendTransaction(results, security, "2019-09-12T00:00", 3.1, 1.06, 10);
    }

    @Test
    public void testDividende02()
    {
        JSONPDFExtractor extractor = new JSONPDFExtractor(new Client(), "trade-republic-dividends.json");

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeRepublicDividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = assertSecurity(results, "US09247X1019", "Blackrock Inc.", CurrencyUnit.USD);

        // transaction
        assertDividendTransaction(results, security, "2019-12-23T00:00", 2.2, 0.76, 1);
    }

}
