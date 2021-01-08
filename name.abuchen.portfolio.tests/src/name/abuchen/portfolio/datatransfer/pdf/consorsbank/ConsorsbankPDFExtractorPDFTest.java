package name.abuchen.portfolio.datatransfer.pdf.consorsbank;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * PDF extrator tests for Consorsbank using PDF documents.
 * <p>
 * Tests are in a separate class because the Infinitest plugin cannot run them.
 * <p>
 * To launch from within Eclipse, make sure to uncheck the option "Use the
 * -XstartOnFirstThread when launching with SWT" in the launch configuration of
 * the tests.
 */
@SuppressWarnings("nls")
public class ConsorsbankPDFExtractorPDFTest
{
    @Test
    public void testErtragsgutschrift6_USD_Freibetrag_ausgeschoepft() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();
        URL url = FileLocator.resolve(
                        getClass().getResource("ConsorsbankErtragsgutschrift6_USD_Freibetrag_ausgeschoepft.pdf"));

        PDFInputFile inputFile = new PDFInputFile(new File(url.getPath()));
        inputFile.convertPDFtoText();

        List<Item> results = extractor.extract(Arrays.asList(inputFile), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("850866"));
        assertThat(security.getName(), is("DEERE & CO."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2015-11-02T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(300)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 121_36)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 180_00)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 16_30 + 89 + 24_45)));
    }

    @Test
    public void testErtragsgutschrift7_USD_Freibetrag_nicht_ausgeschoepft() throws IOException
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();
        URL url = FileLocator.resolve(
                        getClass().getResource("ConsorsbankErtragsgutschrift7_USD_Freibetrag_nicht_ausgeschoepft.pdf"));

        PDFInputFile inputFile = new PDFInputFile(new File(url.getPath()));
        inputFile.convertPDFtoText();

        List<Item> results = extractor.extract(Arrays.asList(inputFile), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("200417"));
        assertThat(security.getName(), is("ALTRIA GROUP INC."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2016-01-11T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(650)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 285_60)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 367_25)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 50_40))); // QUEST
    }
}
