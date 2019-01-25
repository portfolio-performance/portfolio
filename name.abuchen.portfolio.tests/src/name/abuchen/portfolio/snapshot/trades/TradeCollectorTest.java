package name.abuchen.portfolio.snapshot.trades;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

public class TradeCollectorTest
{
    private static Client client;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(TradeCollectorTest.class.getResourceAsStream("trade_test_case.xml"));
    }

    @Test
    public void testTradeExtraction()
    {
        TradeCollector collector = new TradeCollector(client);

        for (Security security : client.getSecurities())
        {
            List<Trade> trades = collector.collect(security);

            System.out.println("----- " + security.getName());

            trades.forEach(t -> {
                System.out.println(Values.DateTime.format(t.getStart()) + " - "
                                + ((t.getEnd() != null) ? Values.DateTime.format(t.getEnd()) : "open"));
                System.out.println(Values.Share.format(t.getShares()));

                t.getTransactions().forEach(pair -> {
                    System.out.println("\t" + pair.getTransaction());
                });
            });
        }

    }

}
