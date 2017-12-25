package name.abuchen.portfolio.datatransfer.pdf.dkb;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DkbPDFExtractorPDFTest
{

    @Test
    public void testErtragsgutschriftDividende() throws IOException
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();
        URL url = FileLocator
                        .resolve(getClass().getResource("DkBErtragsgutschrift2_GBP_Freibetrrag_ausgeschoepft.pdf"));
        
        PDFInputFile inputFile = new PDFInputFile(new File(url.getPath()));
        inputFile.parse();
        
        List<Item> results = extractor.extract(Arrays.asList(inputFile), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("A0DJ58"));
        assertThat(security.getIsin(), is("GB00B02J6398"));
        assertThat(security.getName(), is("ADMIRAL GROUP PLC"));

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2015-10-13")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", 227_63L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(450)));
    }

}
