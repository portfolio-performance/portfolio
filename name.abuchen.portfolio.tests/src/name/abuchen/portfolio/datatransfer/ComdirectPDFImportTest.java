package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

import org.junit.Test;

@SuppressWarnings("nls")
public class ComdirectPDFImportTest
{

    private String gutschriftText;
    private String kaufText;

    public ComdirectPDFImportTest()
    {
        try
        {
            gutschriftText = new Scanner(new File("Gutschrift.txt")).useDelimiter("\\A").next();
            kaufText = new Scanner(new File("Wertpapierabrechnung_Kauf.txt")).useDelimiter("\\A").next();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void testGutschrift()
    {
        Client client = buildClient();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(gutschriftText, "Gutschrift", errors);
        assert (results.size() == 2);
        assert (errors.size() == 0);
    }

    @Test
    public void testKauf()
    {
        Client client = buildClient();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(kaufText, "Wertpapierabrechnung_Kauf", errors);
        assert (results.size() == 2);
        assert (errors.size() == 0);
    }

    private Client buildClient()
    {
        Client client = new Client();

        Account account = new Account();
        account.setName("testAccount");
        client.addAccount(account);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("testPortfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        return client;
    }

}
