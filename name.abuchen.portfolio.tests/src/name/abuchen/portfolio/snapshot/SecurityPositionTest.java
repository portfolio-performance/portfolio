package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
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
        SecurityPosition position = new SecurityPosition(new Security(), new SecurityPrice(), tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactions()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 25 * Values.Share.factor(), 25000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 75 * Values.Share.factor(), 150000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 100000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new SecurityPrice(), tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 20_00)));
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactionsMiddlePrice()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 75 * Values.Share.factor(), 75000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.BUY, 25 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 100000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new SecurityPrice(), tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 15_00)));
    }

    @Test
    public void testPurchasePriceNaNIfOnlySellTransactions()
    {
        SecurityPosition position = new SecurityPosition(new Security(), new SecurityPrice(), Arrays.asList( //
                        new PortfolioTransaction(Dates.today(), null, Type.SELL, 50 * Values.Share.factor(), 50000, 0,
                                        0)));

        assertThat(position.getShares(), is(-50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testThatTransferInCountsIfTransferOutIsMissing()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 50000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 500_00)));
    }

    @Test
    public void testThatTransferInCountsIfTransferOutIsMissingPlusBuyTransaction()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_IN, 50 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertThat(position.getShares(), is(100L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_50)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 2000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 950_00)));
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

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 500_00)));
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

        assertThat(position.getShares(), is(100L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_50)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 2000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 950_00)));
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

        assertThat(position.getShares(), is(25L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 250_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 250_00)));
    }

    @Test
    public void testPurchasePriceIfSharesArePartiallyTransferredOut()
    {
        SecurityPrice price = new SecurityPrice(Dates.date(2012, Calendar.DECEMBER, 2), 2000);
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction("2012-01-01", null, Type.BUY, 50 * Values.Share.factor(), 50000, 0, 0));
        tx.add(new PortfolioTransaction("2012-02-01", null, Type.TRANSFER_OUT, 25 * Values.Share.factor(), 55000, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), price, tx);

        assertThat(position.getShares(), is(25L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 250_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 250_00)));
    }

}
