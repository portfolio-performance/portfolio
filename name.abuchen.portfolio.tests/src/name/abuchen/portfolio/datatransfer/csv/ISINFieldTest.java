package name.abuchen.portfolio.datatransfer.csv;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.text.Format;
import java.text.ParseException;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.ISINField;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;

@SuppressWarnings("nls")
public class ISINFieldTest
{
    @Test
    public void testValidAndExistingISIN() throws ParseException
    {
        Client client = new Client();
        client.addSecurity(new Security("BASF", "DE000BASF111", "BAS.DE", QuoteFeed.MANUAL));

        ISINField field = new ISINField("isin", Messages.CSVColumn_ISIN);

        Format format = field.guessFormat(client, null).getFormat();

        assertThat(format.parseObject("DE000BASF111"), is("DE000BASF111"));
    }

    @Test(expected = ParseException.class)
    public void testValidAndNotExistingISIN() throws ParseException
    {
        ISINField field = new ISINField("isin", Messages.CSVColumn_ISIN);

        Format format = field.guessFormat(new Client(), null).getFormat();
        
        format.parseObject("DE0007164600");
    }

    @Test(expected = ParseException.class)
    public void testNotValidISIN() throws ParseException
    {
        ISINField field = new ISINField("isin", Messages.CSVColumn_ISIN);

        Format format = field.guessFormat(new Client(), null).getFormat();

        format.parseObject("not valid");
    }
    
    @Test
    public void testPartialMatch() throws ParseException
    {
        Client client = new Client();
        client.addSecurity(new Security("SAP", "DE0007164600", "SAP.DE", QuoteFeed.MANUAL));

        ISINField field = new ISINField("isin", Messages.CSVColumn_ISIN);

        Format format = field.guessFormat(client, null).getFormat();

        assertThat(format.parseObject("Zins/Dividende ISIN DE0007164600 SAP SE O."), is("DE0007164600"));
        assertThat(format.parseObject("ISIN DE0007164600"), is("DE0007164600"));
    }

}
