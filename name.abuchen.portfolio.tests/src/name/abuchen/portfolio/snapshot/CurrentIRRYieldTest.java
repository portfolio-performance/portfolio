package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

public class CurrentIRRYieldTest
{
    private Client createTestClient()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2009, Calendar.DECEMBER, 31), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.DECEMBER, 31), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2009, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 1000000, 100, 0));
        client.addPortfolio(portfolio);

        return client;
    }

    @Test
    public void testSimplisticTwoYieldPeriods()
    {
        Client client = createTestClient();

        ClientIRRYield yield = ClientIRRYield.create(client, Dates.date(2009, Calendar.DECEMBER, 31),
                        Dates.date(2010, Calendar.DECEMBER, 31));

        assertEquals(10, yield.getIrr(), 0.001);

        ClientIRRYield yield2a = ClientIRRYield.create(client, Dates.date(2009, Calendar.DECEMBER, 31),
                        Dates.date(2010, Calendar.JUNE, 30));
        ClientIRRYield yield2b = ClientIRRYield.create(client, Dates.date(2010, Calendar.JUNE, 30),
                        Dates.date(2010, Calendar.DECEMBER, 31));

        assertEquals(yield.getIrr(), yield2a.getIrr() + yield2b.getIrr(), 0.002);
    }

    @Test
    public void testSimplisticMonthlyYieldPeriods()
    {
        Client client = createTestClient();

        ClientIRRYield yield = ClientIRRYield.create(client, Dates.date(2009, Calendar.DECEMBER, 31),
                        Dates.date(2010, Calendar.DECEMBER, 31));

        double accumulated = 0;
        Calendar cal = Dates.cal(2009, Calendar.DECEMBER, 31);

        for (int ii = 0; ii < 12; ii++)
        {
            Date start = cal.getTime();

            cal.add(Calendar.DATE, 1);
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DATE, -1);

            ClientIRRYield y = ClientIRRYield.create(client, start, cal.getTime());

            accumulated += y.getIrr();
        }

        assertEquals(yield.getIrr(), accumulated, 0.002);
    }

}
