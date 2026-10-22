package name.abuchen.portfolio.datatransfer.pdf.baaderbank;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BaaderBankPDFExtractorLabelTest
{
    private String labelOf(String testCase)
    {
        var extractor = new BaaderBankPDFExtractor(new Client());

        return extractor.getLabel(PDFInputFile.loadSingleTestCase(getClass(), testCase));
    }

    @Test
    public void testDefaultLabel()
    {
        var extractor = new BaaderBankPDFExtractor(new Client());

        assertThat(extractor.getLabel(), is("Baader Bank AG"));
    }

    @Test
    public void testFinanzenNetZero()
    {
        assertThat(labelOf("Dividende01.txt"), is("Baader Bank AG / finanzen.net zero"));
        assertThat(labelOf("Kauf12.txt"), is("Baader Bank AG / finanzen.net zero"));
        assertThat(labelOf("CryptoKauf01.txt"), is("Baader Bank AG / finanzen.net zero"));
    }

    @Test
    public void testGratisbrokerIsGroupedWithFinanzenNetZero()
    {
        // GRATISBROKER was renamed to finanzen.net zero; both documents belong
        // to the same depot and are intentionally grouped under one label
        assertThat(labelOf("Dividende04.txt"), is("Baader Bank AG / finanzen.net zero"));
        assertThat(labelOf("Kauf07.txt"), is("Baader Bank AG / finanzen.net zero"));
        assertThat(labelOf("Steuerausgleichsrechnung02.txt"), is("Baader Bank AG / finanzen.net zero"));
    }

    @Test
    public void testSmartbrokerPlus()
    {
        assertThat(labelOf("Dividende17.txt"), is("Baader Bank AG / Smartbroker+"));
        assertThat(labelOf("Kauf28.txt"), is("Baader Bank AG / Smartbroker+"));
        assertThat(labelOf("Periodenauszug15.txt"), is("Baader Bank AG / Smartbroker+"));
    }

    @Test
    public void testOskar()
    {
        // Oskar shares the address of Scalable Capital, but is a separate
        // broker
        assertThat(labelOf("Kauf25.txt"), is("Baader Bank AG / Oskar"));
    }

    @Test
    public void testScalableCapital()
    {
        assertThat(labelOf("Kauf01.txt"), is("Baader Bank AG / Scalable Capital"));
        assertThat(labelOf("Dividende08.txt"), is("Baader Bank AG / Scalable Capital"));
        assertThat(labelOf("AdvanceTax01.txt"), is("Baader Bank AG / Scalable Capital"));
    }

    @Test
    public void testScalableCapitalLetterheadWithTradersPlaceFooter()
    {
        assertThat(labelOf("Dividende25.txt"), is("Baader Bank AG / Scalable Capital"));
    }

    @Test
    public void testTradersPlace()
    {
        assertThat(labelOf("Kauf30.txt"), is("Baader Bank AG / Traders Place"));
        assertThat(labelOf("Dividende22.txt"), is("Baader Bank AG / Traders Place"));
        assertThat(labelOf("Rechnungsabschluss03.txt"), is("Baader Bank AG / Traders Place"));
    }

    @Test
    public void testDocumentsWithoutBrokerKeepDefaultLabel()
    {
        assertThat(labelOf("Kauf05.txt"), is("Baader Bank AG"));
        assertThat(labelOf("Dividende27.txt"), is("Baader Bank AG"));
        assertThat(labelOf("Rechnungsabschluss01.txt"), is("Baader Bank AG"));
    }
}
