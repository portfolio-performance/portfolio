package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;

@SuppressWarnings("nls")
public class SecurityPositionTest
{

    @Test
    public void testFIFOPurchasePrice()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100000, null, 100 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.SELL, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(),
                        new SecurityPrice(), tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactions()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 25000, null, 25 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 150000, null, 75 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100000, null, 50 * Values.Share.factor(),
                        Type.SELL, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(),
                        new SecurityPrice(), tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 20_00)));
    }

    @Test
    public void testPurchasePriceWithMultipleBuyTransactionsMiddlePrice()
    {
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 75000, null, 75 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 50000, null, 25 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100000, null, 50 * Values.Share.factor(),
                        Type.SELL, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(),
                        new SecurityPrice(), tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 15_00)));
    }

    @Test
    public void testPurchasePriceNaNIfOnlySellTransactions()
    {
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(),
                        new SecurityPrice(), Arrays.asList( //
                                        new PortfolioTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 500_00, null,
                                                        50 * Values.Share.factor(), Type.SELL, 0, 0)));

        assertThat(position.getShares(), is(-50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testThatTransferInCountsIfTransferOutIsMissing()
    {
        SecurityPrice price = new SecurityPrice(LocalDate.of(2012, Month.DECEMBER, 2), Values.Quote.factorize(20));
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_IN, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(), price, tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 500_00)));
    }

    @Test
    public void testThatTransferInCountsIfTransferOutIsMissingPlusBuyTransaction()
    {
        SecurityPrice price = new SecurityPrice(LocalDate.of(2012, Month.DECEMBER, 2), Values.Quote.factorize(20));
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_IN, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(), price, tx);

        assertThat(position.getShares(), is(100L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_50)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 2000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 950_00)));
    }

    @Test
    public void testThatTransferInDoesNotCountIfMatchingTransferOutIsIncluded()
    {
        SecurityPrice price = new SecurityPrice(LocalDate.of(2012, Month.DECEMBER, 2), Values.Quote.factorize(20));
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_OUT, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_IN, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(), price, tx);

        assertThat(position.getShares(), is(50L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 500_00)));
    }

    @Test
    public void testThatOnlyMatchingTransfersAreRemoved_InRemains()
    {
        SecurityPrice price = new SecurityPrice(LocalDate.of(2012, Month.DECEMBER, 2), Values.Quote.factorize(20));
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_OUT, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_IN, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 2, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_IN, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(), price, tx);

        assertThat(position.getShares(), is(100L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_50)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 2000_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 950_00)));
    }

    @Test
    public void testThatOnlyMatchingTransfersAreRemoved_OutRemains()
    {
        SecurityPrice price = new SecurityPrice(LocalDate.of(2012, Month.DECEMBER, 2), Values.Quote.factorize(20));
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_OUT, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 50 * Values.Share.factor(),
                        Type.TRANSFER_IN, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 2, 0, 0), CurrencyUnit.EUR, 55000, null, 25 * Values.Share.factor(),
                        Type.TRANSFER_OUT, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(), price, tx);

        assertThat(position.getShares(), is(25L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 250_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 250_00)));
    }

    @Test
    public void testPurchasePriceIfSharesArePartiallyTransferredOut()
    {
        SecurityPrice price = new SecurityPrice(LocalDate.of(2012, Month.DECEMBER, 2), Values.Quote.factorize(20));
        List<PortfolioTransaction> tx = new ArrayList<PortfolioTransaction>();
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 50000, null, 50 * Values.Share.factor(),
                        Type.BUY, 0, 0));
        tx.add(new PortfolioTransaction(LocalDateTime.of(2012, Month.FEBRUARY, 1, 0, 0), CurrencyUnit.EUR, 55000, null, 25 * Values.Share.factor(),
                        Type.TRANSFER_OUT, 0, 0));
        SecurityPosition position = new SecurityPosition(new Security(), new TestCurrencyConverter(), price, tx);

        assertThat(position.getShares(), is(25L * Values.Share.factor()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 250_00)));
        assertThat(position.calculateValue(), is(Money.of(CurrencyUnit.EUR, 500_00)));
        assertThat(position.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 250_00)));
    }

    @Test
    public void testFIFOPurchasePriceWithForex()
    {
        CurrencyConverter currencyConverter = new TestCurrencyConverter().with(CurrencyUnit.USD);
        Security security = new Security("", CurrencyUnit.USD);

        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        t.setDateTime(LocalDateTime.parse("2017-01-25T00:00"));
        t.setShares(Values.Share.factorize(13));
        t.setSecurity(security);
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9659.24)));
        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9644.24)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(10287.13)),
                        BigDecimal.valueOf(0.937470704040499)));
        t.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(16)),
                        BigDecimal.valueOf(0.937470704040499)));

        List<PortfolioTransaction> tx = new ArrayList<>();
        tx.add(t);

        SecurityPosition position = new SecurityPosition(security, currencyConverter, new SecurityPrice(), tx);

        assertThat(position.getShares(), is(13L * Values.Share.factor()));

        // 10287.13 / 13 = 791.32
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(791.32))));

        // 9659.24 EUR x ( 1 / 0.937470704040499) = 10303,51
        assertThat(position.getFIFOPurchaseValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9659.24 * (1 / 0.937470704040499)))));

        Client client = new Client();
        client.addSecurity(security);
        Account a = new Account();
        client.addAccount(a);
        Portfolio p = new Portfolio();
        p.setReferenceAccount(a);
        p.addTransaction(t);
        client.addPortfolio(p);

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, currencyConverter,
                        new ReportingPeriod.FromXtoY(LocalDate.parse("2016-12-31"), LocalDate.parse("2017-02-01")));

        assertThat(snapshot.getRecords().size(), is(1));

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getFifoCost(), is(position.getFIFOPurchaseValue()));
        assertThat(record.getFifoCostPerSharesHeld().toMoney(), is(position.getFIFOPurchasePrice()));
    }

    @Test
    public void testSplittingPositionsWithForexGrossValue()
    {
        Security security = new Security("", CurrencyUnit.EUR);

        SecurityPrice price = new SecurityPrice(LocalDate.of(2016, Month.DECEMBER, 2), Values.Quote.factorize(10.19));

        PortfolioTransaction inbound_delivery = new PortfolioTransaction();
        inbound_delivery.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        inbound_delivery.setDateTime(LocalDateTime.parse("2016-01-01T00:00"));
        inbound_delivery.setSecurity(security);
        inbound_delivery.setMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(27409.55)));
        inbound_delivery.setShares(Values.Share.factorize(2415.794));

        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(27409.55)),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24616.95)),
                        BigDecimal.valueOf(1.1134421608));

        inbound_delivery.addUnit(grossValue);

        SecurityPosition position = new SecurityPosition(security, new TestCurrencyConverter(), price,
                        Arrays.asList(inbound_delivery));

        SecurityPosition third = SecurityPosition.split(position, 20 * Values.Weight.factor()); // 20%

        // 24616.95 EUR * 0.2 = 4923,39 EUR
        assertThat(third.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4923.39))));
        assertThat(third.getFIFOPurchaseValue(),
                        is(Money.of(CurrencyUnit.EUR, Math.round(position.getFIFOPurchaseValue().getAmount() * 0.2))));
    }

}
