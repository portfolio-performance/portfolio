package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

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
    public void testGroupByCategories()
    {
        Date referenceDate = Dates.date(2010, Calendar.JANUARY, 31);

        Client client = new Client();

        Security securityA = new Security();
        securityA.setType(AssetClass.DEBT);
        securityA.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1000));
        client.addSecurity(securityA);

        Security securityB = new Security();
        securityB.setType(AssetClass.COMMODITY);
        securityB.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1100));
        client.addSecurity(securityB);

        Security securityC = new Security();
        securityC.setName("Security C");
        securityC.setType(AssetClass.EQUITY);
        securityC.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityC);

        Security securityD = new Security();
        securityD.setName("Security D");
        securityD.setType(AssetClass.EQUITY);
        securityD.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityD);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityA,
                        PortfolioTransaction.Type.BUY, 1000000, 10000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityB,
                        PortfolioTransaction.Type.BUY, 1000000, 11000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityC,
                        PortfolioTransaction.Type.BUY, 1000000, 12000, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityD,
                        PortfolioTransaction.Type.BUY, 1000000, 12000, 0));
        client.addPortfolio(portfolio);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, referenceDate);
        assertNotNull(snapshot);

        GroupByAssetClass byAssetClass = snapshot.groupByAssetClass();

        AssetCategory debt = byAssetClass.byClass(AssetClass.DEBT);
        assertNotNull(debt);
        assertEquals(10000, debt.getValuation());
        assertEquals(1, debt.getPositions().size());

        AssetCategory commodities = byAssetClass.byClass(AssetClass.COMMODITY);
        assertNotNull(commodities);
        assertEquals(11000, commodities.getValuation());
        assertEquals(1, commodities.getPositions().size());

        AssetCategory stocks = byAssetClass.byClass(AssetClass.EQUITY);
        assertNotNull(stocks);
        assertEquals(24000, stocks.getValuation());
        assertEquals(2, stocks.getPositions().size());

        AssetCategory realEstate = byAssetClass.byClass(AssetClass.REAL_ESTATE);
        assertNull(realEstate);
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
