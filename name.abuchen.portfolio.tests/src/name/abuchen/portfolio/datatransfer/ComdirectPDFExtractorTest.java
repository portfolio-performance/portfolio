package name.abuchen.portfolio.datatransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

import org.junit.Test;

@SuppressWarnings("nls")
public class ComdirectPDFExtractorTest
{

    private String gutschriftText;
    private String kaufText;

    public ComdirectPDFExtractorTest()
    {
        gutschriftText = new Scanner(getClass().getResourceAsStream("Gutschrift.txt"), "UTF-8").useDelimiter("\\A")
                        .next();

        kaufText = new Scanner(getClass().getResourceAsStream("Wertpapierabrechnung_Kauf.txt"), "UTF-8").useDelimiter(
                        "\\A").next();
    }

    @Test
    public void testGutschrift()
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(gutschriftText, "Gutschrift", errors);
        SecurityItem secItem = null;
        AccountTransaction accItem = null;
        for (Item it : results)
        {
            if (it instanceof SecurityItem)
            {
                secItem = (SecurityItem) it;
            }
            if (it instanceof AccountTransaction)
            {
                accItem = (AccountTransaction) it;
            }
        }
        assert (secItem != null);
        assert (accItem != null);
        Security security = secItem.getSecurity();
        assert (security.getName().equals("Name des Wertpapiers"));
        assert (accItem.getAmount() == 1);
        assert (accItem.getSecurity().equals(security));
        assert (results.size() == 2);
        // Should complete without error
        assert (errors.size() == 0);
    }

    @Test
    public void testKauf()
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(kaufText, "Wertpapierabrechnung_Kauf", errors);
        SecurityItem secItem = null;
        BuySellEntryItem buyItem = null;
        for (Item it : results)
        {
            if (it instanceof SecurityItem)
            {
                secItem = (SecurityItem) it;
            }
            if (it instanceof BuySellEntryItem)
            {
                buyItem = (BuySellEntryItem) it;
            }
        }
        assert (secItem != null);
        assert (buyItem != null);
        Security security = secItem.getSecurity();
        assert (security.getName().equals("Name der Security"));
        assert (buyItem.getSecurity().equals(security));
        assert (buyItem.getAmount() == 1);
        assert (results.size() == 2);
        // Should complete without error
        assert (errors.size() == 0);
    }

}
