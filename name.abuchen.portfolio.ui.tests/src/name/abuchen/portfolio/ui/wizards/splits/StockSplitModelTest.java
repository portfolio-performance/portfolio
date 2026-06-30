package name.abuchen.portfolio.ui.wizards.splits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

public class StockSplitModelTest
{
    private static final LocalDate EX_DATE = LocalDate.of(2020, 1, 10);
    private static final LocalDateTime TRANSACTION_DATE = LocalDateTime.of(2020, 1, 2, 0, 0);

    @Test
    public void testCalculateNewQuoteAndStock()
    {
        StockSplitModel model = new StockSplitModel(null, null);
        // 2 shares for 1
        model.setNewShares(new BigDecimal(2));
        model.setOldShares(new BigDecimal(1));
        assertThat(model.calculateNewQuote(4), is(2L)); // quote is halved
        assertThat(model.calculateNewStock(4), is(8L)); // stock is doubled

        // 1 shares for 3
        model.setNewShares(new BigDecimal(1));
        model.setOldShares(new BigDecimal(3));
        assertThat(model.calculateNewQuote(5), is(15L)); // quote multiplied by 3
        assertThat(model.calculateNewStock(12), is(4L)); // stock is divided by 3
        
        // 3 shares for 1
        model.setNewShares(new BigDecimal(3));
        model.setOldShares(new BigDecimal(1));
        assertThat(model.calculateNewQuote(6), is(2L)); // quote is divided by 3
        assertThat(model.calculateNewStock(3), is(9L)); // stock is multiplied by 3
    }

    @Test
    public void testReverseSplitNoRoundingError()
    {
        // Reproduces issue #5317: 119 shares with 3:1 reverse split
        var model = new StockSplitModel(null, null);

        // 1:3 reverse split (1 new share for every 3 old shares)
        model.setNewShares(new BigDecimal(1));
        model.setOldShares(new BigDecimal(3));

        long shares357 = Values.Share.factorize(357);
        assertThat(model.calculateNewStock(shares357), is(Values.Share.factorize(119)));
    }

    @Test
    public void testRoundTripSplitAndReverseSplit()
    {
        var model = new StockSplitModel(null, null);

        long originalShares = Values.Share.factorize(119);

        // 1:3 forward split (multiply shares by 3)
        model.setNewShares(new BigDecimal(3));
        model.setOldShares(new BigDecimal(1));
        long afterSplit = model.calculateNewStock(originalShares);
        assertThat(afterSplit, is(Values.Share.factorize(357)));

        // 3:1 reverse split (divide shares by 3)
        model.setNewShares(new BigDecimal(1));
        model.setOldShares(new BigDecimal(3));
        long afterReverse = model.calculateNewStock(afterSplit);
        assertThat(afterReverse, is(originalShares));
    }

    @Test
    public void testNonTrivialRatios()
    {
        var model = new StockSplitModel(null, null);

        // 10:12 split
        model.setNewShares(new BigDecimal(10));
        model.setOldShares(new BigDecimal(12));
        long shares120 = Values.Share.factorize(120);
        assertThat(model.calculateNewStock(shares120), is(Values.Share.factorize(100)));
        assertThat(model.calculateNewQuote(100L), is(120L));

        // 7:3 split
        model.setNewShares(new BigDecimal(7));
        model.setOldShares(new BigDecimal(3));
        long shares30 = Values.Share.factorize(30);
        assertThat(model.calculateNewStock(shares30), is(Values.Share.factorize(70)));
        assertThat(model.calculateNewQuote(70L), is(30L));
    }

    @Test
    public void testApplyChangesStillUpdatesLegacyTransactionsAndPrices()
    {
        var client = new Client();
        var security = security();
        var portfolio = portfolio();

        client.addSecurity(security);
        client.addPortfolio(portfolio);

        var legacyBuy = legacyBuy(security);
        portfolio.addTransaction(legacyBuy);
        security.addPrice(new SecurityPrice(LocalDate.of(2020, 1, 1), 100L));

        splitModel(client, security).applyChanges();

        assertThat(security.getEvents().size(), is(1));
        assertThat(security.getEvents().get(0).getDate(), is(EX_DATE));
        assertThat(security.getEvents().get(0).getType(), is(SecurityEvent.Type.STOCK_SPLIT));
        assertThat(legacyBuy.getShares(), is(Values.Share.factorize(20)));
        assertThat(security.getPrices().get(0).getValue(), is(50L));
    }

    private StockSplitModel splitModel(Client client, Security security)
    {
        var model = new StockSplitModel(client, security);
        model.setExDate(EX_DATE);
        model.setNewShares(BigDecimal.valueOf(2));
        model.setOldShares(BigDecimal.ONE);
        return model;
    }

    private PortfolioTransaction legacyBuy(Security security)
    {
        var transaction = new PortfolioTransaction();
        transaction.setType(PortfolioTransaction.Type.BUY);
        transaction.setDateTime(TRANSACTION_DATE);
        transaction.setSecurity(security);
        transaction.setShares(Values.Share.factorize(10));
        transaction.setAmount(Values.Amount.factorize(100));
        transaction.setCurrencyCode(security.getCurrencyCode());
        return transaction;
    }

    private Portfolio portfolio()
    {
        return new Portfolio();
    }

    private Security security()
    {
        return new Security("Security", name.abuchen.portfolio.money.CurrencyUnit.EUR);
    }

}
