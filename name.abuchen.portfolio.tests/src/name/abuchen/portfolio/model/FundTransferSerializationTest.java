package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class FundTransferSerializationTest
{
    @Test
    public void testXmlRoundTripPreservesFundTransferEntry() throws IOException
    {
        Client client = buildClient();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new ClientFactory.XmlSerialization(false).save(client, stream);

        String xml = new String(stream.toByteArray(), StandardCharsets.UTF_8);
        assertThat(xml.contains("fund-transfer"), is(true));

        Client reloaded = ClientFactory.load(new ByteArrayInputStream(stream.toByteArray()));

        assertThat(reloaded.getFileVersionAfterRead(), is(Client.CURRENT_VERSION));
        assertFundTransfer(reloaded);
    }

    @Test
    public void testProtobufRoundTripPreservesFundTransferEntry() throws IOException
    {
        Client client = buildClient();

        ProtobufWriter protobufWriter = new ProtobufWriter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);

        Client reloaded = protobufWriter.load(new ByteArrayInputStream(stream.toByteArray()));

        assertThat(reloaded.getFileVersionAfterRead(), is(Client.CURRENT_VERSION));
        assertFundTransfer(reloaded);
    }

    @Test
    public void testXmlRoundTripPreservesUiCarriedLotList() throws IOException
    {
        Client client = buildClient();
        FundTransferEntry entry = (FundTransferEntry) client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.FUND_TRANSFER_OUT) //
                        .findAny().orElseThrow(IllegalArgumentException::new).getCrossEntry();

        entry.setCarriedLots(entry.getCarriedLots().stream().map(FundTransferEntry.CarriedLot::copy).toList());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new ClientFactory.XmlSerialization(false).save(client, stream);

        Client reloaded = ClientFactory.load(new ByteArrayInputStream(stream.toByteArray()));
        assertFundTransfer(reloaded);
    }

    private Client buildClient()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        sourceFund.setName("Source Fund");
        Security targetFund = new SecurityBuilder().addTo(client);
        targetFund.setName("Target Fund");
        targetFund.setCurrencyCode(CurrencyUnit.USD);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .buy(sourceFund, "2020-03-01", Values.Share.factorize(6),
                                        Values.Amount.factorize(600)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction firstBuy = sourcePortfolio.getTransactions().get(0);
        PortfolioTransaction secondBuy = sourcePortfolio.getTransactions().get(1);

        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(sourceFund);
        entry.setTargetSecurity(targetFund);
        entry.setSourceShares(Values.Share.factorize(7));
        entry.setTargetShares(Values.Share.factorize(11));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1050)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1125)));
        entry.addCarriedLot(new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                        Values.Share.factorize(5), Values.Share.factorize(8),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500)), firstBuy.getUUID()));
        entry.addCarriedLot(new FundTransferEntry.CarriedLot(LocalDate.parse("2020-03-01"),
                        Values.Share.factorize(2), Values.Share.factorize(3),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200)), secondBuy.getUUID()));
        entry.insert();

        setUpdatedAt(client);

        return client;
    }

    private void setUpdatedAt(Client client)
    {
        Instant updatedAt = Instant.parse("2020-06-01T00:00:00Z");

        client.getSecurities().forEach(s -> s.setUpdatedAt(updatedAt));
        client.getAccounts().forEach(a -> {
            a.setUpdatedAt(updatedAt);
            a.getTransactions().forEach(t -> t.setUpdatedAt(updatedAt));
        });
        client.getPortfolios().forEach(p -> {
            p.setUpdatedAt(updatedAt);
            p.getTransactions().forEach(t -> t.setUpdatedAt(updatedAt));
        });
    }

    private void assertFundTransfer(Client client)
    {
        PortfolioTransaction sourceTx = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.FUND_TRANSFER_OUT) //
                        .findAny().orElseThrow(IllegalArgumentException::new);
        PortfolioTransaction targetTx = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.FUND_TRANSFER_IN) //
                        .findAny().orElseThrow(IllegalArgumentException::new);

        assertThat(sourceTx.getCrossEntry(), instanceOf(FundTransferEntry.class));
        assertThat(targetTx.getCrossEntry(), instanceOf(FundTransferEntry.class));
        assertThat(sourceTx.getCrossEntry(), is(targetTx.getCrossEntry()));

        FundTransferEntry entry = (FundTransferEntry) sourceTx.getCrossEntry();
        assertThat(entry.getCarriedLots().size(), is(2));
        assertThat(entry.getTargetTransaction().getSecurity().getName(), is("Target Fund"));
        assertThat(entry.getTargetTransaction().getShares(), is(Values.Share.factorize(11)));
        assertThat(entry.getSourceTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1050))));
        assertThat(entry.getTargetTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1125))));
        assertThat(entry.getCarriedLots().get(0).getAcquisitionDate(), is(LocalDate.parse("2020-01-01")));
        assertThat(entry.getCarriedLots().get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(entry.getCarriedLots().get(1).getSourceTransactionUUID(), is(
                        client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY)
                                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getUUID()));
    }
}
