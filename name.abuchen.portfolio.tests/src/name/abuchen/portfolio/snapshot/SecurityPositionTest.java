package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

public class SecurityPositionTest
{

    @Test
    public void testFIFOPurchasePrice()
    {
        SecurityPosition position = new SecurityPosition(null);

        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.BUY, 100 * Values.Share.factor(),
                        100000, 0));
        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(),
                        50000, 0));

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(1000L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactions()
    {
        SecurityPosition position = new SecurityPosition(null);

        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.BUY, 25 * Values.Share.factor(),
                        25000, 0));
        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.BUY, 75 * Values.Share.factor(),
                        150000, 0));
        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(),
                        100000, 0));

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(2000L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactionsMiddlePrice()
    {
        SecurityPosition position = new SecurityPosition(null);

        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.BUY, 75 * Values.Share.factor(),
                        75000, 0));
        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.BUY, 25 * Values.Share.factor(),
                        50000, 0));
        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(),
                        100000, 0));

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(1500L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testPurchasePriceNaNIfOnlySellTransactions()
    {
        SecurityPosition position = new SecurityPosition(null);

        position.addTransaction(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(),
                        50000, 0));

        assertEquals(-50 * Values.Share.factor(), position.getShares());
        assertEquals(0L, position.getFIFOPurchasePrice());
    }

}
