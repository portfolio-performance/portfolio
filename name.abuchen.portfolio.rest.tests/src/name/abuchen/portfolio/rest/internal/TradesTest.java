package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradesTest
{
    private static JsonObject trades(Client client, String status, String grouping, String costMethod,
                    String taxesAndFees, String currency)
    {
        var json = TradesHandler.list(client, new ExchangeRateProviderFactory(client), status, grouping,
                        costMethod, taxesAndFees, currency);
        return json.getAsJsonObject();
    }

    private static JsonObject trades(Client client)
    {
        return trades(client, null, null, null, null, null);
    }

    private static JsonObject forInstrument(Client client, String uuid)
    {
        return TradesHandler
                        .forInstrument(client, new ExchangeRateProviderFactory(client), uuid, null, null, null, null,
                                        null)
                        .getAsJsonObject();
    }

    /** a client with one security bought for 1000 and sold for 1300 */
    private static Client closedTrade()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(130)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);
        account.setName("Cash");

        var portfolio = new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .sell(security, "2026-03-16", Values.Share.factorize(10), Values.Amount.factorize(1300)) //
                        .addTo(client);
        portfolio.setName("Depot");

        return client;
    }

    @Test
    public void testClosedTrade()
    {
        var client = closedTrade();
        var security = client.getSecurities().get(0);

        var response = trades(client);

        // the parameters the response was computed with, echoed back
        assertThat(response.get("reportingCurrency").getAsString(), is("EUR"));
        assertThat(response.get("costMethod").getAsString(), is("fifo"));
        assertThat(response.get("taxesAndFees").getAsString(), is("included"));
        assertThat(response.get("grouping").getAsString(), is("combined"));
        assertThat(response.get("valuationDate").getAsString(), is(LocalDate.now().toString()));

        var status = response.get("status").getAsJsonArray();
        assertThat(status.size(), is(2));
        assertThat(status.get(0).getAsString(), is("open"));
        assertThat(status.get(1).getAsString(), is("closed"));

        // always present, even when nothing failed
        assertThat(response.get("warnings").getAsJsonArray().size(), is(0));

        var items = response.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));

        var trade = items.get(0).getAsJsonObject();
        assertThat(trade.get("status").getAsString(), is("closed"));
        assertThat(trade.get("direction").getAsString(), is("long"));
        // a local date-time, always with seconds and without a UTC offset
        assertThat(trade.get("start").getAsString(), is("2026-01-15T00:00:00"));
        assertThat(trade.get("end").getAsString(), is("2026-03-16T00:00:00"));
        assertThat(trade.get("shares").getAsDouble(), is(10d));
        assertThat(trade.get("transactionCount").getAsInt(), is(2));

        var instrument = trade.get("instrument").getAsJsonObject();
        assertThat(instrument.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(instrument.get("name").getAsString(), is("ACME"));
        assertThat(instrument.get("currencyCode").getAsString(), is("EUR"));

        var portfolio = trade.get("portfolio").getAsJsonObject();
        assertThat(portfolio.get("uuid").getAsString(), is(client.getPortfolios().get(0).getUUID()));
        assertThat(portfolio.get("name").getAsString(), is("Depot"));

        assertThat(trade.get("entryValue").getAsJsonObject().get("value").getAsDouble(), is(1000d));
        assertThat(trade.get("exitValue").getAsJsonObject().get("value").getAsDouble(), is(1300d));
        assertThat(trade.get("profitLoss").getAsJsonObject().get("value").getAsDouble(), is(300d));
        assertThat(trade.get("profitLoss").getAsJsonObject().get("currency").getAsString(), is("EUR"));

        // a fraction, not a percentage
        assertThat(trade.get("return").getAsDouble(), closeTo(0.3, 1e-10));
        assertThat(trade.get("holdingPeriodDays").getAsInt(), is(60));

        // no synthetic identity: a trade is recomputed on every request
        assertThat(trade.has("id"), is(false));
        assertThat(trade.has("uuid"), is(false));
    }

    @Test
    public void testProfitLossReconcilesWithEntryAndExitValue()
    {
        var trade = trades(closedTrade()).get("items").getAsJsonArray().get(0).getAsJsonObject();

        var entry = trade.get("entryValue").getAsJsonObject().get("value").getAsDouble();
        var exit = trade.get("exitValue").getAsJsonObject().get("value").getAsDouble();
        var profitLoss = trade.get("profitLoss").getAsJsonObject().get("value").getAsDouble();

        // the identity a client will compute for itself
        assertThat(profitLoss, closeTo(exit - entry, 1e-10));
    }

    @Test
    public void testTaxesAndFeesMovesEntryExitAndProfitLossTogether()
    {
        var client = new Client();

        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1010),
                                        Values.Amount.factorize(10), 0) //
                        .sell(security, "2026-03-16", Values.Share.factorize(10), Values.Amount.factorize(1280),
                                        Values.Amount.factorize(20)) //
                        .addTo(client);

        var included = trades(client, null, null, null, "included", null).get("items").getAsJsonArray().get(0)
                        .getAsJsonObject();
        var excluded = trades(client, null, null, null, "excluded", null).get("items").getAsJsonArray().get(0)
                        .getAsJsonObject();

        assertThat(included.get("entryValue").getAsJsonObject().get("value").getAsDouble(), is(1010d));
        assertThat(included.get("exitValue").getAsJsonObject().get("value").getAsDouble(), is(1280d));
        assertThat(included.get("profitLoss").getAsJsonObject().get("value").getAsDouble(), is(270d));

        // gross: the purchase fee comes off the entry, the sale fee off the exit
        assertThat(excluded.get("entryValue").getAsJsonObject().get("value").getAsDouble(), is(1000d));
        assertThat(excluded.get("exitValue").getAsJsonObject().get("value").getAsDouble(), is(1300d));
        assertThat(excluded.get("profitLoss").getAsJsonObject().get("value").getAsDouble(), is(300d));

        // and the identity still holds in both
        for (var trade : new JsonObject[] { included, excluded })
        {
            var entry = trade.get("entryValue").getAsJsonObject().get("value").getAsDouble();
            var exit = trade.get("exitValue").getAsJsonObject().get("value").getAsDouble();
            assertThat(trade.get("profitLoss").getAsJsonObject().get("value").getAsDouble(),
                            closeTo(exit - entry, 1e-10));
        }

        // the ratios deliberately do not follow taxesAndFees
        assertThat(excluded.get("return").getAsDouble(), is(included.get("return").getAsDouble()));
        assertThat(excluded.get("irr").getAsDouble(), is(included.get("irr").getAsDouble()));
    }

    @Test
    public void testOpenTradeHasNoEndAndIsValuedAtTheValuationDate()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice(LocalDate.now().toString(), Values.Quote.factorize(150)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var response = trades(client);
        var trade = response.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(trade.get("status").getAsString(), is("open"));
        // null rather than absent: the field is part of the schema
        assertThat(trade.get("end").isJsonNull(), is(true));

        // valued at the current market price, not at a realized amount
        assertThat(trade.get("exitValue").getAsJsonObject().get("value").getAsDouble(), is(1500d));
        assertThat(trade.get("profitLoss").getAsJsonObject().get("value").getAsDouble(), is(500d));
        assertThat(response.get("valuationDate").getAsString(), is(LocalDate.now().toString()));
    }

    @Test
    public void testMovingAverageIsUndefinedForShortTrades()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice(LocalDate.now().toString(), Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .sell(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var fifo = trades(client, null, null, "fifo", null, null).get("items").getAsJsonArray().get(0)
                        .getAsJsonObject();
        var mvavg = trades(client, null, null, "moving-average", null, null).get("items").getAsJsonArray().get(0)
                        .getAsJsonObject();

        assertThat(fifo.get("direction").getAsString(), is("short"));
        assertThat(mvavg.get("direction").getAsString(), is("short"));

        // FIFO defines all three, with the short-side sign
        assertThat(fifo.get("entryValue").getAsJsonObject().get("value").getAsDouble(), is(1000d));
        assertThat(fifo.get("profitLoss").getAsJsonObject().get("value").getAsDouble(), is(200d));
        assertThat(fifo.get("return").getAsDouble(), closeTo(0.2, 1e-10));

        // short trades have no moving-average acquisition cost
        assertThat(mvavg.get("entryValue").isJsonNull(), is(true));
        assertThat(mvavg.get("profitLoss").isJsonNull(), is(true));
        assertThat(mvavg.get("return").isJsonNull(), is(true));

        // the three are null as a set, never a mixture
        for (var field : new String[] { "entryValue", "profitLoss", "return" })
            assertThat(field, mvavg.get(field).isJsonNull(), is(true));

        // what does not depend on the cost basis stays defined
        assertThat(mvavg.get("exitValue").getAsJsonObject().get("value").getAsDouble(), is(800d));
        assertThat(mvavg.get("irr").getAsDouble(), is(fifo.get("irr").getAsDouble()));
        assertThat(mvavg.get("holdingPeriodDays").getAsInt(), is(fifo.get("holdingPeriodDays").getAsInt()));
    }

    @Test
    public void testMovingAverageIsUndefinedForShortTradesWithTaxesAndFeesExcluded()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice(LocalDate.now().toString(), Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .sell(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000),
                                        Values.Amount.factorize(5)) //
                        .addTo(client);

        // the gross variant runs through different getters and must agree
        var mvavg = trades(client, null, null, "moving-average", "excluded", null).get("items").getAsJsonArray()
                        .get(0).getAsJsonObject();

        assertThat(mvavg.get("entryValue").isJsonNull(), is(true));
        assertThat(mvavg.get("profitLoss").isJsonNull(), is(true));
        assertThat(mvavg.get("return").isJsonNull(), is(true));
    }

    @Test
    public void testMovingAverageStaysDefinedForLongTrades()
    {
        var client = closedTrade();

        var trade = trades(client, null, null, "moving-average", null, null).get("items").getAsJsonArray().get(0)
                        .getAsJsonObject();

        // the null is specific to shorts, not to the cost method
        assertThat(trade.get("direction").getAsString(), is("long"));
        assertThat(trade.get("entryValue").isJsonNull(), is(false));
        assertThat(trade.get("profitLoss").isJsonNull(), is(false));
        assertThat(trade.get("return").isJsonNull(), is(false));
    }

    @Test
    public void testStatusFilter()
    {
        var client = closedTrade();

        // a second, still open position in another security
        var other = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(50)) //
                        .addTo(client);
        other.setName("BETA");

        new PortfolioBuilder(client.getAccounts().get(0)) //
                        .buy(other, "2026-02-01", Values.Share.factorize(10), Values.Amount.factorize(400)) //
                        .addTo(client);

        assertThat(trades(client).get("items").getAsJsonArray().size(), is(2));

        var closed = trades(client, "closed", null, null, null, null).get("items").getAsJsonArray();
        assertThat(closed.size(), is(1));
        assertThat(closed.get(0).getAsJsonObject().get("instrument").getAsJsonObject().get("name").getAsString(),
                        is("ACME"));

        var open = trades(client, "open", null, null, null, null).get("items").getAsJsonArray();
        assertThat(open.size(), is(1));
        assertThat(open.get(0).getAsJsonObject().get("instrument").getAsJsonObject().get("name").getAsString(),
                        is("BETA"));

        // spelling both is the same as omitting the parameter
        var both = trades(client, "open,closed", null, null, null, null);
        assertThat(both.get("items").getAsJsonArray().size(), is(2));
        assertThat(both.get("status").getAsJsonArray().size(), is(2));

        // echoed in declaration order, not the order sent
        var reversed = trades(client, "closed,open", null, null, null, null).get("status").getAsJsonArray();
        assertThat(reversed.get(0).getAsString(), is("open"));
        assertThat(reversed.get(1).getAsString(), is("closed"));
    }

    @Test
    public void testSortedByInstrumentName()
    {
        var client = closedTrade();

        var zulu = new SecurityBuilder().addTo(client);
        zulu.setName("ZULU");
        var alpha = new SecurityBuilder().addTo(client);
        alpha.setName("ALPHA");

        var account = client.getAccounts().get(0);
        new PortfolioBuilder(account) //
                        .buy(zulu, "2026-02-01", Values.Share.factorize(1), Values.Amount.factorize(100)) //
                        .sell(zulu, "2026-02-10", Values.Share.factorize(1), Values.Amount.factorize(110)) //
                        .addTo(client);
        new PortfolioBuilder(account) //
                        .buy(alpha, "2026-02-01", Values.Share.factorize(1), Values.Amount.factorize(100)) //
                        .sell(alpha, "2026-02-10", Values.Share.factorize(1), Values.Amount.factorize(120)) //
                        .addTo(client);

        var items = trades(client).get("items").getAsJsonArray();
        assertThat(items.size(), is(3));
        assertThat(items.get(0).getAsJsonObject().get("instrument").getAsJsonObject().get("name").getAsString(),
                        is("ACME"));
        assertThat(items.get(1).getAsJsonObject().get("instrument").getAsJsonObject().get("name").getAsString(),
                        is("ALPHA"));
        assertThat(items.get(2).getAsJsonObject().get("instrument").getAsJsonObject().get("name").getAsString(),
                        is("ZULU"));
    }

    @Test
    public void testGroupingSplitsLots()
    {
        var client = new Client();

        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        // two acquisitions closed by one sale
        new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .buy(security, "2026-02-15", Values.Share.factorize(10), Values.Amount.factorize(1200)) //
                        .sell(security, "2026-03-16", Values.Share.factorize(20), Values.Amount.factorize(2600)) //
                        .addTo(client);

        var combined = trades(client, null, "combined", null, null, null).get("items").getAsJsonArray();
        assertThat(combined.size(), is(1));
        assertThat(combined.get(0).getAsJsonObject().get("shares").getAsDouble(), is(20d));

        var perLot = trades(client, null, "per-lot", null, null, null).get("items").getAsJsonArray();
        assertThat(perLot.size(), is(2));
        assertThat(perLot.get(0).getAsJsonObject().get("shares").getAsDouble(), is(10d));
        assertThat(perLot.get(1).getAsJsonObject().get("shares").getAsDouble(), is(10d));

        // the split is exact: the lots still add up to the combined trade
        var combinedProfitLoss = combined.get(0).getAsJsonObject().get("profitLoss").getAsJsonObject()
                        .get("value").getAsDouble();
        var lotProfitLoss = perLot.get(0).getAsJsonObject().get("profitLoss").getAsJsonObject().get("value")
                        .getAsDouble()
                        + perLot.get(1).getAsJsonObject().get("profitLoss").getAsJsonObject().get("value")
                                        .getAsDouble();
        assertThat(lotProfitLoss, closeTo(combinedProfitLoss, 1e-10));
    }

    @Test
    public void testUnreconcilableSecurityBecomesAWarningNotAnError()
    {
        var client = closedTrade();

        // sells more than was ever bought: the collector gives up on this
        // security. (A sale into an account holding *nothing* would not do -
        // that opens a short position, see testSellWithoutHoldingsOpensAShort)
        var broken = new SecurityBuilder().addTo(client);
        broken.setName("BROKEN");

        new PortfolioBuilder(client.getAccounts().get(0)) //
                        .buy(broken, "2026-02-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .sell(broken, "2026-02-10", Values.Share.factorize(15), Values.Amount.factorize(1500)) //
                        .addTo(client);

        var response = trades(client);

        // the healthy security still reports its trade
        assertThat(response.get("items").getAsJsonArray().size(), is(1));

        var warnings = response.get("warnings").getAsJsonArray();
        assertThat(warnings.size(), is(1));

        var warning = warnings.get(0).getAsJsonObject();
        assertThat(warning.get("instrument").getAsJsonObject().get("uuid").getAsString(), is(broken.getUUID()));
        assertThat(warning.get("instrument").getAsJsonObject().get("name").getAsString(), is("BROKEN"));
        assertThat(warning.get("code").getAsString(), is("missing-holdings-for-sell"));
        // English, whatever language the application runs in
        assertThat(warning.get("message").getAsString(), containsString("more shares than"));
    }

    @Test
    public void testSellWithoutHoldingsOpensAShort()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice(LocalDate.now().toString(), Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        // a sale into an account holding nothing: the collector reads this as
        // opening a short position, not as an unreconcilable security
        new PortfolioBuilder(account) //
                        .sell(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var response = trades(client);

        assertThat(response.get("warnings").getAsJsonArray().size(), is(0));

        var items = response.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));

        var trade = items.get(0).getAsJsonObject();
        assertThat(trade.get("status").getAsString(), is("open"));
        assertThat(trade.get("direction").getAsString(), is("short"));

        // profit is entry minus exit for a short, the reverse of a long - and
        // shares is positive either way, which is why direction is a field
        assertThat(trade.get("shares").getAsDouble(), is(10d));

        var entry = trade.get("entryValue").getAsJsonObject().get("value").getAsDouble();
        var exit = trade.get("exitValue").getAsJsonObject().get("value").getAsDouble();
        var profitLoss = trade.get("profitLoss").getAsJsonObject().get("value").getAsDouble();

        assertThat(entry, is(1000d));
        assertThat(exit, is(800d));
        assertThat(profitLoss, closeTo(entry - exit, 1e-10));
    }

    @Test
    public void testNoteIsTakenFromTheLastTransaction()
    {
        var client = closedTrade();

        // omitted, not null, when the transaction carries none
        var without = trades(client).get("items").getAsJsonArray().get(0).getAsJsonObject();
        assertThat(without.has("note"), is(false));

        // the last transaction of a closed trade is the sale that closed it
        client.getPortfolios().get(0).getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.SELL)
                        .forEach(t -> t.setNote("closed the position"));

        var with = trades(client).get("items").getAsJsonArray().get(0).getAsJsonObject();
        assertThat(with.get("note").getAsString(), is("closed the position"));
    }

    @Test
    public void testReportingCurrencyConvertsEveryAmount()
    {
        var client = closedTrade();

        var response = trades(client, null, null, null, null, "USD");

        assertThat(response.get("reportingCurrency").getAsString(), is("USD"));

        var trade = response.get("items").getAsJsonArray().get(0).getAsJsonObject();

        // the amounts follow the reporting currency; the rate they were
        // converted at is the provider's business, not this test's
        for (var field : new String[] { "entryValue", "exitValue", "profitLoss" })
            assertThat(field, trade.get(field).getAsJsonObject().get("currency").getAsString(), is("USD"));

        // ... while the instrument keeps its own declared currency
        assertThat(trade.get("instrument").getAsJsonObject().get("currencyCode").getAsString(), is("EUR"));
    }

    @Test
    public void testInstrumentSubCollection()
    {
        var client = closedTrade();
        var security = client.getSecurities().get(0);

        var other = new SecurityBuilder().addTo(client);
        other.setName("BETA");
        new PortfolioBuilder(client.getAccounts().get(0)) //
                        .buy(other, "2026-02-01", Values.Share.factorize(10), Values.Amount.factorize(400)) //
                        .addTo(client);

        var response = forInstrument(client, security.getUUID());
        var items = response.get("items").getAsJsonArray();

        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("instrument").getAsJsonObject().get("uuid").getAsString(),
                        is(security.getUUID()));

        // the same envelope as the collection route
        assertThat(response.has("reportingCurrency"), is(true));
        assertThat(response.has("warnings"), is(true));
    }

    @Test
    public void testInstrumentWithoutTradesIsEmptyNotAbsent()
    {
        var client = closedTrade();

        var untraded = new SecurityBuilder().addTo(client);
        untraded.setName("UNTRADED");

        var response = forInstrument(client, untraded.getUUID());

        // a sub-collection, not a member lookup: no 404 here
        assertThat(response.get("items").getAsJsonArray().size(), is(0));
        assertThat(response.get("warnings").getAsJsonArray().size(), is(0));
    }

    @Test
    public void testUnknownInstrumentIsNotFound()
    {
        var client = closedTrade();

        var e = Assert.assertThrows(ApiException.class,
                        () -> forInstrument(client, "00000000-0000-0000-0000-000000000000"));
        assertThat(e.getStatus(), is(404));
    }

    @Test
    public void testInvalidParametersAreReportedTogether()
    {
        var client = closedTrade();

        var e = Assert.assertThrows(ApiException.class,
                        () -> trades(client, "half-open", "lifo", "wifo", "sometimes", "XYZ"));

        assertThat(e.getStatus(), is(400));

        var fields = e.getErrors().stream().map(ApiException.FieldError::field).toList();
        assertThat(fields, is(java.util.List.of("status", "grouping", "costMethod", "taxesAndFees",
                        "reportingCurrency")));
    }

    @Test
    public void testGroupingDoesNotSilentlyFallBackToCombined()
    {
        var client = closedTrade();

        // TradeGrouping.fromString answers an unknown name with COMBINED; the
        // API must reject it instead of returning plausible but unasked data
        var e = Assert.assertThrows(ApiException.class, () -> trades(client, null, "fifo", null, null, null));

        assertThat(e.getStatus(), is(400));
        assertThat(e.getErrors().get(0).field(), is("grouping"));
        assertThat(e.getErrors().get(0).code(), is("invalid-value"));
    }
}
