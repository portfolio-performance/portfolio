package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.model.proto.v1.PTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CorporateActionPersistenceTest
{
    /**
     * Builds a client with a parent (source) and spinco (target) security in
     * one EUR portfolio, plus a two-leg SPIN_OFF CorporateActionEntry registered
     * on the client. Returns the client; the two legs and the entry are wired
     * exactly as production would (entry.insert() adds the legs to the
     * portfolio; addCorporateAction registers the event).
     */
    private Client buildSpinOffClient()
    {
        Client client = new Client();

        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2015-01-02", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        // PortfolioBuilder.buy(..) auto-creates a reference Account via the
        // (String) constructor, which bypasses setName() and leaves
        // updatedAt unset; ProtobufWriter#saveAccounts requires a non-null
        // updatedAt, so set it explicitly here (unrelated to this task's
        // DISTRIBUTION_* leg mapping).
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
        entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 3));
        entry.setReferencePrice(Values.Quote.factorize(20));
        entry.addLeg(portfolio, source, CorporateActionEntry.LegRole.SOURCE);
        entry.addLeg(portfolio, target, CorporateActionEntry.LegRole.TARGET);
        entry.insert();

        client.addCorporateAction(entry);

        return client;
    }

    private Client protobufRoundtrip(Client client) throws IOException
    {
        var writer = new ProtobufWriter();
        var out = new ByteArrayOutputStream();
        writer.save(client, out);
        out.close();
        return writer.load(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void testDistributionLegsRoundtripWithoutOtherUuid() throws IOException
    {
        Client loaded = protobufRoundtrip(buildSpinOffClient());

        Portfolio portfolio = loaded.getPortfolios().get(0);

        var source = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND).findFirst()
                        .orElse(null);
        var target = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_INBOUND).findFirst()
                        .orElse(null);

        assertThat(source, is(notNullValue()));
        assertThat(target, is(notNullValue()));

        // source leg: shares untouched (0), market value preserved
        assertThat(source.getShares(), is(0L));
        assertThat(source.getAmount(), is(Values.Amount.factorize(2000)));
        assertThat(source.getSecurity().getName(), is(notNullValue()));

        // target leg: full entitlement + market value preserved
        assertThat(target.getShares(), is(Values.Share.factorize(100)));
        assertThat(target.getAmount(), is(Values.Amount.factorize(2000)));

        // exactly the two distribution legs (the BUY is the parent's own history)
        List<PortfolioTransaction> distributionLegs = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND
                                        || t.getType() == PortfolioTransaction.Type.DISTRIBUTION_INBOUND)
                        .toList();
        assertThat(distributionLegs, hasSize(2));
    }

    @Test
    public void testCorporateActionRegistryRoundtrip() throws IOException
    {
        Client loaded = protobufRoundtrip(buildSpinOffClient());

        assertThat(loaded.getCorporateActions(), hasSize(1));
        CorporateActionEntry entry = loaded.getCorporateActions().get(0);

        assertThat(entry.getType(), is(CorporateActionEntry.Type.SPIN_OFF));
        assertThat(entry.getExDate(), is(LocalDate.parse("2015-01-09")));
        assertThat(entry.getBasisRatio(), is(new BigDecimal("0.25")));
        assertThat(entry.getDistributionRatio(), is(new CorporateActionEntry.DistributionRatio(1, 3)));
        assertThat(entry.getReferencePrice(), is(Values.Quote.factorize(20)));

        // both legs present with the right roles ...
        var sourceLeg = (PortfolioTransaction) entry.getLeg(CorporateActionEntry.LegRole.SOURCE).orElse(null);
        var targetLeg = (PortfolioTransaction) entry.getLeg(CorporateActionEntry.LegRole.TARGET).orElse(null);
        assertThat(sourceLeg, is(notNullValue()));
        assertThat(targetLeg, is(notNullValue()));
        assertThat(sourceLeg.getType(), is(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND));
        assertThat(targetLeg.getType(), is(PortfolioTransaction.Type.DISTRIBUTION_INBOUND));

        // ... the legs are the SAME instances that live in the portfolio ...
        Portfolio portfolio = loaded.getPortfolios().get(0);
        assertThat(portfolio.getTransactions().contains(sourceLeg), is(true));
        assertThat(portfolio.getTransactions().contains(targetLeg), is(true));

        // ... and the cross-entry back-pointer is re-bound to this entry
        assertThat(sourceLeg.getCrossEntry(), is(sameInstance((CrossEntry) entry)));
        assertThat(targetLeg.getCrossEntry(), is(sameInstance((CrossEntry) entry)));
    }

    private Client xstreamRoundtrip(Client client, boolean idReferences) throws IOException
    {
        var serialization = new ClientFactory.XmlSerialization(idReferences);
        var out = new ByteArrayOutputStream();
        serialization.save(client, out);
        out.close();
        return serialization.load(new java.io.InputStreamReader(
                        new ByteArrayInputStream(out.toByteArray()), java.nio.charset.StandardCharsets.UTF_8));
    }

    private Client xstreamRoundtrip(Client client) throws IOException
    {
        return xstreamRoundtrip(client, false);
    }

    /**
     * Shared assertions for the XStream round-trip tests (XPATH references
     * and ID_REFERENCES mode alike): entry fields survive, both legs are
     * present with the right roles, the legs are the SAME instances that
     * live in the loaded portfolio, and each leg's cross-entry back-pointer
     * is re-bound to the loaded entry.
     */
    private void assertSpinOffSurvived(Client loaded)
    {
        assertThat(loaded.getCorporateActions(), hasSize(1));
        CorporateActionEntry entry = loaded.getCorporateActions().get(0);

        assertThat(entry.getType(), is(CorporateActionEntry.Type.SPIN_OFF));
        assertThat(entry.getExDate(), is(LocalDate.parse("2015-01-09")));
        assertThat(entry.getBasisRatio(), is(new BigDecimal("0.25")));
        assertThat(entry.getDistributionRatio(), is(new CorporateActionEntry.DistributionRatio(1, 3)));
        assertThat(entry.getReferencePrice(), is(Values.Quote.factorize(20)));

        var sourceLeg = entry.getLeg(CorporateActionEntry.LegRole.SOURCE).orElse(null);
        var targetLeg = entry.getLeg(CorporateActionEntry.LegRole.TARGET).orElse(null);
        assertThat(sourceLeg, is(notNullValue()));
        assertThat(targetLeg, is(notNullValue()));

        // the legs are the SAME instances that live in the portfolio (XStream
        // reference resolution), and the cross-entry is re-linked
        Portfolio portfolio = loaded.getPortfolios().get(0);
        assertThat(portfolio.getTransactions().contains(sourceLeg), is(true));
        assertThat(portfolio.getTransactions().contains(targetLeg), is(true));
        assertThat(sourceLeg.getCrossEntry(), is(sameInstance((CrossEntry) entry)));
        assertThat(targetLeg.getCrossEntry(), is(sameInstance((CrossEntry) entry)));
    }

    @Test
    public void testCorporateActionXStreamRoundtrip() throws IOException
    {
        assertSpinOffSurvived(xstreamRoundtrip(buildSpinOffClient(), false));
    }

    /**
     * Same as {@link #testCorporateActionXStreamRoundtrip()}, but through
     * the ID_REFERENCES save path (production's real save mode via
     * SaveFlag.ID_REFERENCES), which exercises RecordConverter's handling of
     * record-typed CorporateActionEntry fields under XStream's id-based
     * reference resolution rather than XPATH references.
     */
    @Test
    public void testCorporateActionXStreamRoundtripWithIdReferences() throws IOException
    {
        assertSpinOffSurvived(xstreamRoundtrip(buildSpinOffClient(), true));
    }

    @Test
    public void testCurrentVersionIs71()
    {
        // bumped 70 -> 71 for corporate-action support (§9.4): a version-70
        // app must refuse a spin-off file rather than mis-read the new types
        assertThat(Client.CURRENT_VERSION, is(71));
    }

    @Test
    public void testOldVersionUpgradesCleanly() throws IOException
    {
        Client client = buildSpinOffClient();
        client.setVersion(69); // a file written by the pre-spin-off release

        Client loaded = xstreamRoundtrip(client);

        assertThat(loaded.getVersion(), is(Client.CURRENT_VERSION));
        // the spin-off written at v69 still round-trips
        assertThat(loaded.getCorporateActions(), hasSize(1));
    }

    @Test(expected = IOException.class)
    public void testNewerVersionIsRefused() throws IOException
    {
        Client client = buildSpinOffClient();
        client.setVersion(Client.CURRENT_VERSION + 1);

        xstreamRoundtrip(client); // must throw: "file from a newer version"
    }

    @Test
    public void testFourLegSpinOffRoundtripSerialisesFractionalLegsBare() throws IOException
    {
        Client client = new Client();

        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        Account reference = new Account("reference");
        reference.setCurrencyCode(CurrencyUnit.EUR);
        reference.setUpdatedAt(Instant.now()); // pre-existing saveAccounts NPE workaround
        portfolio.setReferenceAccount(reference);
        client.addAccount(reference);
        client.addPortfolio(portfolio);

        var exDate = LocalDateTime.parse("2015-01-09T00:00");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setCurrencyCode(CurrencyUnit.EUR);

        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setShares(Values.Share.factorize(33));
        target.setAmount(Values.Amount.factorize(330));

        var fractionSale = new PortfolioTransaction();
        fractionSale.setType(PortfolioTransaction.Type.SELL);
        fractionSale.setDateTime(exDate);
        fractionSale.setSecurity(spinco);
        fractionSale.setCurrencyCode(CurrencyUnit.EUR);
        fractionSale.setShares(Values.Share.factorize(0.333));
        fractionSale.setAmount(Values.Amount.factorize(3));

        var cashInLieu = new AccountTransaction();
        cashInLieu.setType(AccountTransaction.Type.SELL);
        cashInLieu.setDateTime(exDate);
        cashInLieu.setSecurity(spinco);
        cashInLieu.setCurrencyCode(CurrencyUnit.EUR);
        cashInLieu.setAmount(Values.Amount.factorize(3));

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setBasisRatio(new BigDecimal("0.25"));
        entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 3));
        entry.setReferencePrice(Values.Quote.factorize(9));
        entry.setExDate(exDate.toLocalDate());
        entry.addLeg(portfolio, source, CorporateActionEntry.LegRole.SOURCE);
        entry.addLeg(portfolio, target, CorporateActionEntry.LegRole.TARGET);
        entry.addLeg(portfolio, fractionSale, CorporateActionEntry.LegRole.FRACTION_SALE);
        entry.addLeg(reference, cashInLieu, CorporateActionEntry.LegRole.CASH_IN_LIEU);
        entry.insert();
        client.addCorporateAction(entry);

        // wire-level assertions (spec §9.1): the fractional legs must
        // serialise bare -- no otherUuid on the portfolio leg, and the
        // account leg must appear as its own independent PTransaction
        // (not merely embedded as the portfolio leg's otherUuid)
        var wireOut = new ByteArrayOutputStream();
        new ProtobufWriter().save(client, wireOut);
        wireOut.close();
        byte[] wireBytes = wireOut.toByteArray();
        // skip the 6-byte "PPPBV1" signature ProtobufWriter#save prepends
        // before the raw PClient protobuf payload
        PClient wire = PClient.parseFrom(Arrays.copyOfRange(wireBytes, 6, wireBytes.length));

        PTransaction wireFractionSale = wire.getTransactionsList().stream()
                        .filter(pt -> pt.getUuid().equals(fractionSale.getUUID())).findFirst().orElseThrow();
        assertThat(wireFractionSale.hasOtherUuid(), is(false));

        boolean cashInLieuSerialisedBare = wire.getTransactionsList().stream()
                        .anyMatch(pt -> pt.getUuid().equals(cashInLieu.getUUID()));
        assertThat(cashInLieuSerialisedBare, is(true));

        Client reloaded = protobufRoundtrip(client);

        // all four legs are back in their owners
        Portfolio p = reloaded.getPortfolios().get(0);
        Account a = reloaded.getAccounts().get(0);
        assertThat(p.getTransactions(), hasSize(3));
        assertThat(a.getTransactions(), hasSize(1));

        // regrouped into exactly one entry, all four roles present
        assertThat(reloaded.getCorporateActions(), hasSize(1));
        CorporateActionEntry e = reloaded.getCorporateActions().get(0);
        assertThat(e.getLegs(), hasSize(4));
        assertThat(e.getLeg(CorporateActionEntry.LegRole.SOURCE).isPresent(), is(true));
        assertThat(e.getLeg(CorporateActionEntry.LegRole.TARGET).isPresent(), is(true));
        assertThat(e.getLeg(CorporateActionEntry.LegRole.FRACTION_SALE).isPresent(), is(true));
        assertThat(e.getLeg(CorporateActionEntry.LegRole.CASH_IN_LIEU).isPresent(), is(true));

        // the fractional legs point back at the CorporateActionEntry (bare
        // serialisation regrouped, NOT reconstructed as a BuySellEntry)
        var reFraction = e.getLeg(CorporateActionEntry.LegRole.FRACTION_SALE).orElseThrow();
        assertThat(reFraction.getCrossEntry(), is(sameInstance((CrossEntry) e)));
    }
}
