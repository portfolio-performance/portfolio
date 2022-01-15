package name.abuchen.portfolio.datatransfer.pdf.simpel;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SelfWealthPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class SimpelPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        SelfWealthPDFExtractor extractor = new SelfWealthPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SimpelBuy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("AT0000A1QA38"));
        assertThat(security.getName(), is("Standortfonds Österreich"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-10T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.071)));
        assertThat(entry.getSource(), is("SimpelBuy01.txt"));
        assertThat(entry.getNote(), is(""));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9999)))); // TODO ??
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.0))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.0))));
    }

   /* @Test
    public void testSecuritySell01()
    {
        SelfWealthPDFExtractor extractor = new SelfWealthPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SelfWealthSell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("WPL"));
        assertThat(security.getName(), is("WOODSIDE PETROLEUM"));
        assertThat(security.getCurrencyCode(), is("AUD"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-24T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(397)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("AUD", Values.Amount.factorize(8676.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("AUD", Values.Amount.factorize(8686.36))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("AUD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("AUD", Values.Amount.factorize(9.50))));
    }*/
}
