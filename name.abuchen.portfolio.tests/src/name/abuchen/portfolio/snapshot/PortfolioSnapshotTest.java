package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class PortfolioSnapshotTest
{
    @Test
    public void testMergingPortfolioSnapshots()
    {
        // Portfolio A : Security A + Security X
        // Portfolio B : Security B + Security X

        Date referenceDate = Dates.date(2010, Calendar.JANUARY, 31);

        Client client = new Client();

        Security securityA = new Security();
        securityA.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1000));
        client.addSecurity(securityA);

        Security securityB = new Security();
        securityB.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1100));
        client.addSecurity(securityB);

        Security securityX = new Security();
        securityX.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityX);

        Portfolio portfolioA = new Portfolio();
        portfolioA.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityA,
                        PortfolioTransaction.Type.BUY, 10, 10000, 0));
        portfolioA.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityX,
                        PortfolioTransaction.Type.BUY, 10, 12000, 0));
        client.addPortfolio(portfolioA);

        Portfolio portfolioB = new Portfolio();
        portfolioB.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityB,
                        PortfolioTransaction.Type.BUY, 10, 11000, 0));
        portfolioB.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityX,
                        PortfolioTransaction.Type.BUY, 10, 12000, 0));
        client.addPortfolio(portfolioB);

        ClientSnapshot snapshot = ClientSnapshot.create(client, referenceDate);
        assertNotNull(snapshot);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionA = jointPortfolio.getPositionsBySecurity().get(securityA);
        assertEquals(10, positionA.getShares());
        assertEquals(10000, positionA.calculateValue());

        SecurityPosition positionB = jointPortfolio.getPositionsBySecurity().get(securityB);
        assertEquals(10, positionB.getShares());
        assertEquals(11000, positionB.calculateValue());

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);
        assertEquals(20, positionX.getShares());
        assertEquals(24000, positionX.calculateValue());
    }

    @Test
    public void testGroupByCategories()
    {
        Date referenceDate = Dates.date(2010, Calendar.JANUARY, 31);

        Client client = new Client();

        Security securityA = new Security();
        securityA.setType(AssetClass.BOND);
        securityA.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1000));
        client.addSecurity(securityA);

        Security securityB = new Security();
        securityB.setType(AssetClass.COMMODITY);
        securityB.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1100));
        client.addSecurity(securityB);

        Security securityC = new Security();
        securityC.setName("Security C");
        securityC.setType(AssetClass.STOCK);
        securityC.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityC);

        Security securityD = new Security();
        securityD.setName("Security D");
        securityD.setType(AssetClass.STOCK);
        securityD.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityD);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityA,
                        PortfolioTransaction.Type.BUY, 10, 10000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityB,
                        PortfolioTransaction.Type.BUY, 10, 11000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityC,
                        PortfolioTransaction.Type.BUY, 10, 12000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityD,
                        PortfolioTransaction.Type.BUY, 10, 12000, 0));
        client.addPortfolio(portfolio);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, referenceDate);
        assertNotNull(snapshot);

        Map<AssetClass, AssetCategory> mapped = new HashMap<Security.AssetClass, AssetCategory>();
        for (AssetCategory category : snapshot.groupByCategory())
            mapped.put(category.getAssetClass(), category);

        AssetCategory bonds = mapped.get(AssetClass.BOND);
        assertNotNull(bonds);
        assertEquals(10000, bonds.getValuation());
        assertEquals(1, bonds.getPositions().size());

        AssetCategory commodities = mapped.get(AssetClass.COMMODITY);
        assertNotNull(commodities);
        assertEquals(11000, commodities.getValuation());
        assertEquals(1, commodities.getPositions().size());

        AssetCategory stocks = mapped.get(AssetClass.STOCK);
        assertNotNull(stocks);
        assertEquals(24000, stocks.getValuation());
        assertEquals(2, stocks.getPositions().size());
    }

    @Test
    public void testBuyAndSellLeavesNoEntryInSnapshot()
    {
        Date referenceDate = Dates.date(2010, Calendar.JANUARY, 31);

        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 10, 10000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 2), security,
                        PortfolioTransaction.Type.SELL, 7, 11000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 3), security,
                        PortfolioTransaction.Type.SELL, 3, 12000, 0));
        client.addPortfolio(portfolio);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, referenceDate);
        assertNotNull(snapshot);

        assertTrue(snapshot.getPositions().isEmpty());
    }
}
