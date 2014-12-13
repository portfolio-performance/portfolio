package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityPositionTest
{

    @Test
    public void testFIFOPurchasePrice()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 100 * Values.Share.factor(), 100000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 50000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), null, tx);

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(1000L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactions()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 25 * Values.Share.factor(), 25000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 75 * Values.Share.factor(), 150000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 100000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), null, tx);

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(2000L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactionsMiddlePrice()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 75 * Values.Share.factor(), 75000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 25 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 100000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), null, tx);

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(1500L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testPurchasePriceNaNIfOnlySellTransactions()
    {
        SecurityPosition position = new SecurityPosition(new Security(), null, Arrays.asList( //
                        new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 50000, 0,
                                        0)));

        assertEquals(-50 * Values.Share.factor(), position.getShares());
        assertEquals(0L, position.getFIFOPurchasePrice());
    }

    @Test
    public void testThatTransferInCountsIfTransferOutIsMissing()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 50000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(1000L, position.getFIFOPurchasePrice());
        assertEquals(50000L, position.getFIFOPurchaseValue());
        assertEquals(100000L, position.calculateValue());
        assertEquals(50000L, position.getProfitLoss());
    }

    @Test
    public void testThatTransferInCountsIfTransferOutIsMissingPlusBuyTransaction()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertEquals(100 * Values.Share.factor(), position.getShares());
        assertEquals(1050L, position.getFIFOPurchasePrice());
        assertEquals(105000L, position.getFIFOPurchaseValue());
        assertEquals(200000L, position.calculateValue());
        assertEquals(95000L, position.getProfitLoss());
    }

    @Test
    public void testThatTransferInDoesNotCountIfMatchingTransferOutIsIncluded()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_OUT, 50 * Values.Share.factor(), 55000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertEquals(50 * Values.Share.factor(), position.getShares());
        assertEquals(1000L, position.getFIFOPurchasePrice());
        assertEquals(50000L, position.getFIFOPurchaseValue());
        assertEquals(100000L, position.calculateValue());
        assertEquals(50000L, position.getProfitLoss());
    }

    @Test
    public void testThatOnlyMatchingTransfersAreRemoved_InRemains()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_OUT, 50 * Values.Share.factor(), 55000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 55000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-02", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertEquals(100 * Values.Share.factor(), position.getShares());
        assertEquals(1050L, position.getFIFOPurchasePrice());
        assertEquals(105000L, position.getFIFOPurchaseValue());
        assertEquals(200000L, position.calculateValue());
        assertEquals(95000L, position.getProfitLoss());
    }

    @Test
    public void testThatOnlyMatchingTransfersAreRemoved_OutRemains()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_OUT, 50 * Values.Share.factor(), 55000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 55000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-02", null, Type.TRANSFER_OUT, 25 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertEquals(25 * Values.Share.factor(), position.getShares());
        assertEquals(1000L, position.getFIFOPurchasePrice());
        assertEquals(25000L, position.getFIFOPurchaseValue());
        assertEquals(50000L, position.calculateValue());
        assertEquals(25000L, position.getProfitLoss());
    }

    @Test
    public void testPurchasePriceIfSharesArePartiallyTransferredOut()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_OUT, 25 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertEquals(25 * Values.Share.factor(), position.getShares());
        assertEquals(1000L, position.getFIFOPurchasePrice());
        assertEquals(25000L, position.getFIFOPurchaseValue());
        assertEquals(50000L, position.calculateValue());
        assertEquals(25000L, position.getProfitLoss());
    }

}
