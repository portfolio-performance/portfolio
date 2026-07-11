package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class FundTransferLotBuilderTest
{
    @Test
    public void testBuildsFifoCarriedLotsAndAdjustsLastLotRounding()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(5),
                                        Values.Amount.factorize(500)) //
                        .buy(sourceFund, "2020-03-01", Values.Share.factorize(5),
                                        Values.Amount.factorize(601)) //
                        .addTo(client);

        PortfolioTransaction firstBuy = sourcePortfolio.getTransactions().get(0);
        PortfolioTransaction secondBuy = sourcePortfolio.getTransactions().get(1);

        List<FundTransferEntry.CarriedLot> lots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceFund,
                        LocalDateTime.parse("2020-06-01T00:00"), Values.Share.factorize(7),
                        Values.Share.factorize(10), CurrencyUnit.EUR);

        assertThat(lots.size(), is(2));

        assertThat(lots.get(0).getAcquisitionDate(), is(LocalDate.parse("2020-01-01")));
        assertThat(lots.get(0).getSourceShares(), is(Values.Share.factorize(5)));
        assertThat(lots.get(0).getTargetShares(), is(Math.round(Values.Share.factorize(5) / 7d * 10d)));
        assertThat(lots.get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(lots.get(0).getSourceTransactionUUID(), is(firstBuy.getUUID()));

        assertThat(lots.get(1).getAcquisitionDate(), is(LocalDate.parse("2020-03-01")));
        assertThat(lots.get(1).getSourceShares(), is(Values.Share.factorize(2)));
        assertThat(lots.get(1).getTargetShares(), is(Values.Share.factorize(10) - lots.get(0).getTargetShares()));
        assertThat(lots.get(1).getAcquisitionValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(240.4))));
        assertThat(lots.get(1).getSourceTransactionUUID(), is(secondBuy.getUUID()));

        assertThat(lots.stream().mapToLong(FundTransferEntry.CarriedLot::getTargetShares).sum(),
                        is(Values.Share.factorize(10)));
        assertThat(lots.stream().map(FundTransferEntry.CarriedLot::getAcquisitionValue)
                        .reduce(Money.of(CurrencyUnit.EUR, 0), Money::add),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(740.4))));
    }

    @Test
    public void testBuildConsumesPriorLiquidationsBeforeTransfer()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .sell(sourceFund, "2020-03-01", Values.Share.factorize(4),
                                        Values.Amount.factorize(600)) //
                        .addTo(client);

        PortfolioTransaction firstBuy = sourcePortfolio.getTransactions().get(0);

        List<FundTransferEntry.CarriedLot> lots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceFund,
                        LocalDateTime.parse("2020-06-01T00:00"), Values.Share.factorize(3),
                        Values.Share.factorize(6), CurrencyUnit.EUR);

        assertThat(lots.size(), is(1));
        assertThat(lots.get(0).getAcquisitionDate(), is(LocalDate.parse("2020-01-01")));
        assertThat(lots.get(0).getSourceShares(), is(Values.Share.factorize(3)));
        assertThat(lots.get(0).getTargetShares(), is(Values.Share.factorize(6)));
        assertThat(lots.get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300))));
        assertThat(lots.get(0).getSourceTransactionUUID(), is(firstBuy.getUUID()));
    }

    @Test
    public void testBuildIncludesLotsRecordedOnTransferDate()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-06-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        PortfolioTransaction firstBuy = sourcePortfolio.getTransactions().get(0);

        List<FundTransferEntry.CarriedLot> lots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceFund,
                        LocalDateTime.parse("2020-06-01T00:00"), Values.Share.factorize(7),
                        Values.Share.factorize(11), CurrencyUnit.EUR);

        assertThat(lots.size(), is(1));
        assertThat(lots.get(0).getAcquisitionDate(), is(LocalDate.parse("2020-06-01")));
        assertThat(lots.get(0).getSourceShares(), is(Values.Share.factorize(7)));
        assertThat(lots.get(0).getTargetShares(), is(Values.Share.factorize(11)));
        assertThat(lots.get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(700))));
        assertThat(lots.get(0).getSourceTransactionUUID(), is(firstBuy.getUUID()));
    }

    @Test
    public void testBuildIncludesTransferredLotsRecordedOnTransferDate()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);

        Portfolio originalPortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-06-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio sourcePortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction originalBuy = originalPortfolio.getTransactions().get(0);

        PortfolioTransferEntry transfer = new PortfolioTransferEntry(originalPortfolio, sourcePortfolio);
        transfer.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        transfer.setSecurity(sourceFund);
        transfer.setShares(Values.Share.factorize(10));
        transfer.setAmount(Values.Amount.factorize(1000));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();

        List<FundTransferEntry.CarriedLot> lots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceFund,
                        LocalDateTime.parse("2020-06-01T00:00"), Values.Share.factorize(7),
                        Values.Share.factorize(11), CurrencyUnit.EUR);

        assertThat(lots.size(), is(1));
        assertThat(lots.get(0).getAcquisitionDate(), is(LocalDate.parse("2020-06-01")));
        assertThat(lots.get(0).getSourceShares(), is(Values.Share.factorize(7)));
        assertThat(lots.get(0).getTargetShares(), is(Values.Share.factorize(11)));
        assertThat(lots.get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(700))));
        assertThat(lots.get(0).getSourceTransactionUUID(), is(originalBuy.getUUID()));
    }

    @Test
    public void testBuildPreservesOriginalLotsAfterPreviousFundTransfer()
    {
        Client client = new Client();
        Security originalFund = new SecurityBuilder().addTo(client);
        Security sourceFund = new SecurityBuilder().addTo(client);

        Portfolio originalPortfolio = new PortfolioBuilder() //
                        .buy(originalFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio sourcePortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction originalBuy = originalPortfolio.getTransactions().get(0);

        FundTransferEntry previousTransfer = new FundTransferEntry(originalPortfolio, sourcePortfolio);
        previousTransfer.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        previousTransfer.setSourceSecurity(originalFund);
        previousTransfer.setTargetSecurity(sourceFund);
        previousTransfer.setSourceShares(Values.Share.factorize(8));
        previousTransfer.setTargetShares(Values.Share.factorize(8));
        previousTransfer.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200)));
        previousTransfer.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200)));
        previousTransfer.addCarriedLot(new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                        Values.Share.factorize(8), Values.Share.factorize(8),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800)), originalBuy.getUUID()));
        previousTransfer.insert();

        List<FundTransferEntry.CarriedLot> lots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceFund,
                        LocalDateTime.parse("2021-01-01T00:00"), Values.Share.factorize(4),
                        Values.Share.factorize(6), CurrencyUnit.EUR);

        assertThat(lots.size(), is(1));
        assertThat(lots.get(0).getAcquisitionDate(), is(LocalDate.parse("2020-01-01")));
        assertThat(lots.get(0).getSourceShares(), is(Values.Share.factorize(4)));
        assertThat(lots.get(0).getTargetShares(), is(Values.Share.factorize(6)));
        assertThat(lots.get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(400))));
        assertThat(lots.get(0).getSourceTransactionUUID(), is(originalBuy.getUUID()));
    }

    @Test
    public void testBuildProcessesSameDayPurchasesBeforeLiquidations()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .sell(sourceFund, "2020-06-01", Values.Share.factorize(4), Values.Amount.factorize(100)) //
                        .addTo(client);

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);

        List<FundTransferEntry.CarriedLot> lots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceFund,
                        LocalDateTime.parse("2020-06-01T00:00"), Values.Share.factorize(3),
                        Values.Share.factorize(6), CurrencyUnit.EUR);

        assertThat(lots.size(), is(1));
        assertThat(lots.get(0).getSourceTransactionUUID(), is(sourceBuy.getUUID()));
        assertThat(lots.get(0).getSourceShares(), is(Values.Share.factorize(3)));
        assertThat(lots.get(0).getTargetShares(), is(Values.Share.factorize(6)));
        assertThat(lots.get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300))));
    }
}
