package name.abuchen.portfolio.datatransfer.pdf.commsec;

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
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.CommSecPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CommSecPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        CommSecPDFExtractor extractor = new CommSecPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("QAN"));
        assertThat(security.getName(), is("QANTAS AIRWAYS LIMITED"));
        assertThat(security.getCurrencyCode(), is("AUD"));

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(277)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("AUD", Values.Amount.factorize(1029.92))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("AUD", Values.Amount.factorize(997.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("AUD", Values.Amount.factorize(2.72))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("AUD", Values.Amount.factorize(29.95))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        CommSecPDFExtractor extractor = new CommSecPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("MSB"));
        assertThat(security.getName(), is("MESOBLAST LIMITED"));
        assertThat(security.getCurrencyCode(), is("AUD"));

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4500)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("AUD", Values.Amount.factorize(20495.47))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("AUD", Values.Amount.factorize(20473.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("AUD", Values.Amount.factorize(1.86))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("AUD", Values.Amount.factorize(20.47))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        CommSecPDFExtractor extractor = new CommSecPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("WTC"));
        assertThat(security.getName(), is("WISETECH GLOBAL LIMITED"));
        assertThat(security.getCurrencyCode(), is("AUD"));

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("AUD", Values.Amount.factorize(28031.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("AUD", Values.Amount.factorize(28062.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("AUD", Values.Amount.factorize(2.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("AUD", Values.Amount.factorize(28.06))));
    }
}
