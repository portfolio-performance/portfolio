package name.abuchen.portfolio.datatransfer.pdf.cetesdirecto;

import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.CetesDirectoPDFExtractor;
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
public class CetesDirectoPDFExtractorTest
{

    @Test
    public void testEdoCta01()
    {
        CetesDirectoPDFExtractor extractor = new CetesDirectoPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EdoCta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, CurrencyUnit.MXN);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("CETES"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.MXN));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-06T00:00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6080)));
        assertThat(entry.getSource(), is("EdoCta01.txt"));
        assertThat(entry.getNote(), is("ID:SVD147529623 Series:220203 Term:2 Rate:5.51"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.MXN, Values.Amount.factorize(60540.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.MXN, Values.Amount.factorize(60540.38))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.MXN, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.MXN, Values.Amount.factorize(0.00))));
    }

}
