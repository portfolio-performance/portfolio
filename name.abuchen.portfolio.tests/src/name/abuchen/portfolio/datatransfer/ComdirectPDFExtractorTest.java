package name.abuchen.portfolio.datatransfer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

import org.junit.Test;

@SuppressWarnings("nls")
public class ComdirectPDFExtractorTest
{

    private String gutschriftText;
    private String kaufText;

    public ComdirectPDFExtractorTest()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader((new Temp()).getClass()
                            .getResourceAsStream("/name/abuchen/portfolio/datatransfer/Gutschrift.txt")));
            StringBuilder out = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1)
            {
                out.append((char) c);
            }
            gutschriftText = out.toString();
            reader.close();
            reader = new BufferedReader(new InputStreamReader((new Temp()).getClass().getResourceAsStream(
                            "/name/abuchen/portfolio/datatransfer/Wertpapierabrechnung_Kauf.txt")));
            out = new StringBuilder();
            while ((c = reader.read()) != -1)
            {
                out.append((char) c);
            }
            kaufText = out.toString();
            reader.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
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
