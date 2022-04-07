package name.abuchen.portfolio.datatransfer.pdf.ubsag;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.UBSAGBankingAGPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class UBSAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {

        UBSAGBankingAGPDFExtractor extractor = new UBSAGBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.size(), is(2));

        new AssertImportActions().check(results, "USD");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0950674175"));
        assertThat(security.getName(), is("UBS (Lux) Fund Solutions - MSCI Emerging Markets UCITS ETF"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-08T15:02:13")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(450)));
        assertThat(entry.getSource(), is("Kauf01.txt"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("USD", Values.Amount.factorize(4890.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("USD", Values.Amount.factorize(4846.25))));

        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("USD", Values.Amount.factorize(7.34 + 37.01))));

    }

    @Test
    public void testWertpapierVerkauf01()
    {

        UBSAGBankingAGPDFExtractor extractor = new UBSAGBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.size(), is(2));

        new AssertImportActions().check(results, "USD");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BX7RRJ27"));
        assertThat(security.getName(), is("UBS (Irl) ETF Plc - Factor MSCI USA Quality UCITS ETF"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-29T14:19:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(265)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));

        // assertThat(entry.getNote(), is("Valorennummer 906020"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("USD", Values.Amount.factorize(11116.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("USD", Values.Amount.factorize(11203.46))));

        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("USD", Values.Amount.factorize(16.68 + 70.03))));

    }

}
