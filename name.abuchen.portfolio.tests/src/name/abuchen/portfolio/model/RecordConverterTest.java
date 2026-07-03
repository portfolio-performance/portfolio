package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class RecordConverterTest
{
    /**
     * Mirrors {@code CorporateActionPersistenceTest#buildSpinOffClient}
     * (SOURCE+TARGET legs in one portfolio, entry registered), but takes the
     * distribution ratio's numerator/denominator as parameters so the caller
     * can control the record components under test.
     */
    private Client buildSpinOffClient(int numerator, int denominator)
    {
        Client client = new Client();

        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2015-01-02", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        portfolio.getReferenceAccount().setUpdatedAt(Instant.now());

        var exDate = LocalDateTime.parse("2015-01-09T00:00");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(2000));

        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setShares(Values.Share.factorize(100));
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setAmount(Values.Amount.factorize(2000));

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setExDate(LocalDate.parse("2015-01-09"));
        entry.setBasisRatio(new BigDecimal("0.25"));
        entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(numerator, denominator));
        entry.setReferencePrice(Values.Quote.factorize(20));
        entry.addLeg(portfolio, source, CorporateActionEntry.LegRole.SOURCE);
        entry.addLeg(portfolio, target, CorporateActionEntry.LegRole.TARGET);
        entry.insert();

        client.addCorporateAction(entry);

        return client;
    }

    @Test
    public void testMissingPrimitiveComponentDefaultsInsteadOfThrowing() throws Exception
    {
        Client client = buildSpinOffClient(1, 3);

        var serialization = new ClientFactory.XmlSerialization(false);
        var out = new ByteArrayOutputStream();
        serialization.save(client, out);
        String xml = out.toString(StandardCharsets.UTF_8);

        // simulate a partial/externally-edited file: remove the denominator element
        String trimmed = xml.replaceAll("<denominator>.*?</denominator>", "");

        Client loaded = serialization.load(new InputStreamReader(
                        new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

        var ratio = loaded.getCorporateActions().get(0).getDistributionRatio();
        assertThat(ratio.numerator(), is(1));
        assertThat(ratio.denominator(), is(0)); // defaulted, not thrown
    }
}
