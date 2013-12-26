package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Dates;

import org.junit.Before;
import org.junit.Test;

public class PortfolioMergeTest
{
    private final Date referenceDate = Dates.date(2010, Calendar.JANUARY, 31);

    private Client client;

    private Security securityA;
    private Security securityB;
    private Security securityX;

    @Before
    public void setUpClient()
    {
        // Portfolio A : Security A + Security X
        // Portfolio B : Security B + Security X

        client = new Client();

        securityA = new Security();
        securityA.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1000));
        client.addSecurity(securityA);

        securityB = new Security();
        securityB.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1100));
        client.addSecurity(securityB);

        securityX = new Security();
        securityX.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityX);

        Portfolio portfolioA = new Portfolio();
        portfolioA.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityA,
                        PortfolioTransaction.Type.BUY, 1000000, 10000, 0));
        portfolioA.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityX,
                        PortfolioTransaction.Type.BUY, 1000000, 12100, 100));
        client.addPortfolio(portfolioA);

        Portfolio portfolioB = new Portfolio();
        portfolioB.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityB,
                        PortfolioTransaction.Type.BUY, 1000000, 11000, 0));
        portfolioB.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityX,
                        PortfolioTransaction.Type.BUY, 1000000, 10000, 0));
        client.addPortfolio(portfolioB);
    }

    @Test
    public void testMergingPortfolioSnapshots()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, referenceDate);
        assertNotNull(snapshot);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionA = jointPortfolio.getPositionsBySecurity().get(securityA);
        assertEquals(1000000, positionA.getShares());
        assertEquals(10000, positionA.calculateValue());

        SecurityPosition positionB = jointPortfolio.getPositionsBySecurity().get(securityB);
        assertEquals(1000000, positionB.getShares());
        assertEquals(11000, positionB.calculateValue());

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);
        assertEquals(2000000, positionX.getShares());
        assertEquals(24000, positionX.calculateValue());
    }

    @Test
    public void testThatTransactionsAreMergedOnSecurityPosition()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, referenceDate);
        assertNotNull(snapshot);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);
        assertEquals(2000000, positionX.getShares());
        assertEquals(24000, positionX.calculateValue());
        // calculate purchase price w/o costs
        assertEquals(1100, positionX.getFIFOPurchasePrice());
        // calculate purchase value w/ costs
        assertEquals(22100, positionX.getFIFOPurchaseValue());
        assertEquals(1900, positionX.getProfitLoss());
    }

}
