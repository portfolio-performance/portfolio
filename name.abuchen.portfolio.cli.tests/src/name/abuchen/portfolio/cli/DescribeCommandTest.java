package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class DescribeCommandTest
{
    @Test
    public void describeStatesKnownLimitsPinnedTagAndMaxSupportedVersion()
    {
        var data = data(CliRun.run("describe"));

        assertThat(data.get("pinnedUpstreamTag").getAsString(), is("0.88.0"));
        assertThat(data.get("maxSupportedVersion").getAsInt(), is(Client.CURRENT_VERSION));

        var limits = data.getAsJsonArray("limits").toString().toLowerCase();
        assertThat(limits, containsString("unsaved"));
        assertThat(limits, containsString("pdf"));
        assertThat(limits, containsString("price"));
    }

    @Test
    public void describeRendersWriteOperationsWithInputSchemas()
    {
        var operation = new OperationSpec("instrument.create", "Creates an instrument.",
                        Schema.object("Instrument to create.").property("name", Schema.string("Display name."))
                                        .optionalProperty("isin", Schema.string("ISIN, if known.")),
                        context -> {
                        });

        var data = new Gson().toJsonTree(DescribeCommand.render(List.of(), List.of(operation))).getAsJsonObject();

        var described = data.getAsJsonArray("operations").get(0).getAsJsonObject();
        assertThat(described.get("name").getAsString(), is("instrument.create"));
        assertThat(described.get("summary").getAsString(), is("Creates an instrument."));

        var input = described.getAsJsonObject("input");
        assertThat(input.getAsJsonObject("properties").keySet(), is(Set.of("name", "isin")));
        assertThat(input.getAsJsonArray("required").toString(), is("[\"name\"]"));
    }

    @Test
    public void describePublishesApplyAndItsOperationsWithTheirInputs()
    {
        var data = data(CliRun.run("describe"));

        var apply = command(data, "apply");
        var byName = new HashMap<String, JsonObject>();
        apply.getAsJsonArray("options")
                        .forEach(o -> byName.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));
        assertThat(byName.keySet(), containsInAnyOrder("file", "input", "dry-run", "backup-dir", "keep", "force"));
        assertThat(byName.get("keep").get("default").getAsInt(), is(20));
        assertThat(byName.get("keep").get("minimum").getAsInt(), is(0));
        assertThat(byName.get("force").get("type").getAsString(), is("flag"));

        var warnings = new ArrayList<String>();
        apply.getAsJsonArray("warnings").forEach(w -> warnings.add(w.getAsJsonObject().get("code").getAsString()));
        assertThat(warnings,
                        containsInAnyOrder("file-migrated", "consistency-issues-preexisting", "file-open-in-app"));

        var operations = new HashMap<String, JsonObject>();
        data.getAsJsonArray("operations").forEach(
                        o -> operations.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));
        assertThat(operations.keySet(), containsInAnyOrder("instrument.add", "instrument.update", "cash-account.add",
                        "cash-account.update", "investment-account.add", "investment-account.update",
                        "transaction.add"));

        var addInput = operations.get("instrument.add").getAsJsonObject("input");
        assertThat(addInput.getAsJsonObject("properties").keySet(), containsInAnyOrder("operation", "name",
                        "currencyCode", "isin", "tickerSymbol", "wkn", "note", "retired"));
        assertThat(addInput.getAsJsonArray("required").toString(), is("[\"operation\",\"name\"]"));

        var updateInput = operations.get("instrument.update").getAsJsonObject("input");
        assertThat(updateInput.getAsJsonArray("required").toString(), is("[\"operation\",\"uuid\"]"));

        var cashAdd = operations.get("cash-account.add").getAsJsonObject("input");
        assertThat(cashAdd.getAsJsonObject("properties").keySet(),
                        containsInAnyOrder("operation", "name", "currencyCode", "note", "retired"));
        assertThat(cashAdd.getAsJsonArray("required").toString(),
                        is("[\"operation\",\"name\",\"currencyCode\"]"));
        assertThat(operations.get("cash-account.update").getAsJsonObject("input").getAsJsonArray("required")
                        .toString(), is("[\"operation\",\"uuid\"]"));

        var investmentAdd = operations.get("investment-account.add").getAsJsonObject("input");
        assertThat(investmentAdd.getAsJsonObject("properties").keySet(),
                        containsInAnyOrder("operation", "name", "referenceCashAccount", "note", "retired"));
        assertThat(investmentAdd.getAsJsonArray("required").toString(),
                        is("[\"operation\",\"name\",\"referenceCashAccount\"]"));
        assertThat(operations.get("investment-account.update").getAsJsonObject("input").getAsJsonArray("required")
                        .toString(), is("[\"operation\",\"uuid\"]"));

        var transactionAdd = operations.get("transaction.add").getAsJsonObject("input");
        var types = new ArrayList<String>();
        transactionAdd.getAsJsonObject("properties").getAsJsonObject("type").getAsJsonArray("enum")
                        .forEach(t -> types.add(t.getAsString()));
        assertThat(types, containsInAnyOrder("DEPOSIT", "REMOVAL", "INTEREST", "INTEREST_CHARGE", "DIVIDENDS", "FEES",
                        "FEES_REFUND", "TAXES", "TAX_REFUND", "BUY", "SELL", "DELIVERY_INBOUND", "DELIVERY_OUTBOUND",
                        "TRANSFER"));
        assertThat(transactionAdd.getAsJsonObject("properties").keySet(),
                        hasItems("investmentAccount", "cashAccount", "kind", "from", "to", "targetAmount",
                                        "exchangeRate"));
        assertThat(transactionAdd.getAsJsonArray("required").toString(),
                        is("[\"operation\",\"type\",\"date\",\"amount\",\"currencyCode\"]"));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static JsonObject command(JsonObject data, String name)
    {
        for (var element : data.getAsJsonArray("commands"))
        {
            if (element.getAsJsonObject().get("name").getAsString().equals(name))
                return element.getAsJsonObject();
        }
        throw new AssertionError("command not described: " + name);
    }

    @Test
    public void describeListsCommandsWithOptionsAndResponseSchemas()
    {
        var data = data(CliRun.run("describe"));

        var info = command(data, "info");
        assertThat(info.get("summary").getAsString().isBlank(), is(false));
        assertThat(info.get("usesClientFile").getAsBoolean(), is(true));

        var file = info.getAsJsonArray("options").get(0).getAsJsonObject();
        assertThat(file.get("name").getAsString(), is("file"));
        assertThat(file.get("valueName").getAsString(), is("path"));
        assertThat(file.get("description").getAsString().isBlank(), is(false));

        var response = info.getAsJsonObject("response");
        var infoData = response.getAsJsonObject("data");
        assertThat(infoData.get("type").getAsString(), is("object"));
        assertThat(infoData.getAsJsonObject("properties").has("fileVersion"), is(true));
        assertThat(response.getAsJsonObject("meta").getAsJsonObject("properties").has("asOf"), is(true));

        var describe = command(data, "describe");
        assertThat(describe.get("usesClientFile").getAsBoolean(), is(false));
        assertThat(describe.getAsJsonArray("options").size(), is(0));
    }

    @Test
    public void describeDocumentsEnvironmentVariablesAndDeclaresNoPasswordOption()
    {
        var data = data(CliRun.run("describe"));

        var environment = data.getAsJsonArray("environment");
        var names = new ArrayList<String>();
        for (var element : environment)
        {
            var variable = element.getAsJsonObject();
            names.add(variable.get("name").getAsString());
            assertThat(variable.get("description").getAsString().isBlank(), is(false));
        }
        assertThat(names, is(List.of("PP_FILE", "PP_ECB_RATES", "PP_PASSWORD")));

        for (var command : data.getAsJsonArray("commands"))
            for (var option : command.getAsJsonObject().getAsJsonArray("options"))
                assertThat(option.getAsJsonObject().get("name").getAsString().toLowerCase(),
                                not(containsString("password")));
    }

    @Test
    public void describePublishesOptionTypesDefaultsAndRanges()
    {
        var data = data(CliRun.run("describe"));
        var options = command(data, "instruments").getAsJsonArray("options");

        var byName = new HashMap<String, JsonObject>();
        options.forEach(o -> byName.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));

        var limit = byName.get("limit");
        assertThat(limit.get("valueName").getAsString().isBlank(), is(false));
        assertThat(limit.get("type").getAsString(), is("integer"));
        assertThat(limit.get("default").getAsInt(), is(200));
        assertThat(limit.get("minimum").getAsInt(), is(1));
        assertThat(limit.get("maximum").getAsInt(), is(1000));

        var offset = byName.get("offset");
        assertThat(offset.get("type").getAsString(), is("integer"));
        assertThat(offset.get("default").getAsInt(), is(0));
        assertThat(offset.get("minimum").getAsInt(), is(0));
        assertThat(offset.has("maximum"), is(false));

        var file = byName.get("file");
        assertThat(file.keySet(), is(Set.of("name", "valueName", "description")));
    }

    @Test
    public void describePublishesTheListShapeAndTheAttributeEncoding()
    {
        var data = data(CliRun.run("describe"));
        var response = command(data, "instruments").getAsJsonObject("response");

        var dataSchema = response.getAsJsonObject("data").getAsJsonObject("properties");
        assertThat(dataSchema.keySet(), is(Set.of("items", "total", "nextOffset")));
        assertThat(response.getAsJsonObject("data").getAsJsonArray("required").toString(),
                        is("[\"items\",\"total\"]"));

        var meta = response.getAsJsonObject("meta").getAsJsonObject("properties");
        assertThat(meta.keySet(), is(Set.of("fileVersion", "asOf", "limit", "offset")));

        var attribute = dataSchema.getAsJsonObject("items").getAsJsonObject("items").getAsJsonObject("properties")
                        .getAsJsonObject("attributes").getAsJsonObject("items").getAsJsonObject("properties");
        assertThat(attribute.getAsJsonObject("type").getAsJsonArray("enum").toString(), is(
                        "[\"string\",\"boolean\",\"date\",\"amount\",\"quote\",\"shares\",\"percent\",\"number\"]"));
        assertThat(attribute.getAsJsonObject("value").getAsJsonArray("type").toString(),
                        is("[\"string\",\"number\",\"boolean\"]"));
    }

    @Test
    public void describePublishesRequiredFlagDateOptionsAndWarningCodesOfPrices()
    {
        var data = data(CliRun.run("describe"));
        var prices = command(data, "prices");

        var byName = new HashMap<String, JsonObject>();
        prices.getAsJsonArray("options")
                        .forEach(o -> byName.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));
        assertThat(byName.keySet(), is(Set.of("file", "instrument", "from", "to", "limit", "offset")));

        var instrument = byName.get("instrument");
        assertThat(instrument.get("required").getAsBoolean(), is(true));
        assertThat(instrument.get("description").getAsString().toLowerCase(), containsString("reads only"));
        assertThat(instrument.has("type"), is(false));

        for (var date : List.of("from", "to"))
        {
            assertThat(byName.get(date).get("type").getAsString(), is("date"));
            assertThat(byName.get(date).has("required"), is(false));
        }
        assertThat(byName.get("file").has("required"), is(false));

        var warnings = prices.getAsJsonArray("warnings");
        assertThat(warnings.size(), is(1));
        assertThat(warnings.get(0).getAsJsonObject().get("code").getAsString(), is("stale-price"));
        assertThat(warnings.get(0).getAsJsonObject().get("description").getAsString().isBlank(), is(false));

        var meta = prices.getAsJsonObject("response").getAsJsonObject("meta");
        assertThat(meta.getAsJsonArray("required").toString(),
                        is("[\"fileVersion\",\"asOf\",\"limit\",\"offset\",\"to\"]"));
        assertThat(meta.getAsJsonObject("properties").has("from"), is(true));

        var response = prices.getAsJsonObject("response").getAsJsonObject("data");
        assertThat(response.getAsJsonObject("properties").keySet(),
                        is(Set.of("items", "total", "nextOffset", "instrument", "latest")));
        assertThat(response.getAsJsonArray("required").toString(),
                        is("[\"items\",\"total\",\"instrument\"]"));
    }

    @Test
    public void describePublishesTheOpenTypeEnumTheFiltersAndTheExchangeRateWarningsOfTransactions()
    {
        var data = data(CliRun.run("describe"));
        var transactions = command(data, "transactions");

        var byName = new HashMap<String, JsonObject>();
        transactions.getAsJsonArray("options")
                        .forEach(o -> byName.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));
        assertThat(byName.keySet(), is(Set.of("file", "investment-account", "cash-account", "instrument", "type",
                        "from", "to", "currency", "limit", "offset")));
        assertThat(byName.get("from").get("type").getAsString(), is("date"));
        assertThat(byName.get("to").get("type").getAsString(), is("date"));
        assertThat(byName.get("to").get("description").getAsString().toLowerCase(), containsString("future"));
        assertThat(byName.get("limit").get("default").getAsInt(), is(200));

        var codes = new ArrayList<String>();
        transactions.getAsJsonArray("warnings").forEach(w -> codes.add(w.getAsJsonObject().get("code").getAsString()));
        assertThat(codes, is(List.of("exchange-rate-missing", "stale-exchange-rate",
                        "exchange-rate-cache-unavailable")));

        var item = transactions.getAsJsonObject("response").getAsJsonObject("data").getAsJsonObject("properties")
                        .getAsJsonObject("items").getAsJsonObject("items");
        var type = item.getAsJsonObject("properties").getAsJsonObject("type");
        assertThat(type.get("type").getAsString(), is("string"));
        assertThat(type.has("enum"), is(false));
        assertThat(strings(type.getAsJsonArray("examples")), hasItems(coreTypeNames().toArray(String[]::new)));
        assertThat(strings(type.getAsJsonArray("examples")), hasItem("TRANSFER"));
        assertThat(strings(item.getAsJsonArray("required")),
                        containsInAnyOrder("id", "type", "date", "sides", "amount", "currencyCode", "units"));
    }

    @Test
    public void describePublishesTheSwitchesAndWarningsOfHoldings()
    {
        var data = data(CliRun.run("describe"));
        var holdings = command(data, "holdings");

        var byName = new HashMap<String, JsonObject>();
        holdings.getAsJsonArray("options")
                        .forEach(o -> byName.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));
        assertThat(byName.keySet(), is(Set.of("file", "date", "investment-account", "by-account", "include-zero",
                        "currency")));
        assertThat(byName.get("date").get("type").getAsString(), is("date"));

        for (var flag : List.of("by-account", "include-zero"))
        {
            assertThat(byName.get(flag).get("type").getAsString(), is("flag"));
            assertThat(byName.get(flag).get("default").getAsBoolean(), is(false));
            assertThat(byName.get(flag).has("valueName"), is(false));
        }
        assertThat(byName.get("currency").get("valueName").getAsString().isBlank(), is(false));

        var codes = new ArrayList<String>();
        holdings.getAsJsonArray("warnings").forEach(w -> codes.add(w.getAsJsonObject().get("code").getAsString()));
        assertThat(codes, is(List.of("stale-price", "exchange-rate-missing", "stale-exchange-rate",
                        "exchange-rate-cache-unavailable")));
    }

    @Test
    public void describePublishesTheOptionsAndWarningsOfAllocation()
    {
        var allocation = command(data(CliRun.run("describe")), "allocation");

        var byName = new HashMap<String, JsonObject>();
        allocation.getAsJsonArray("options")
                        .forEach(o -> byName.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject()));
        assertThat(byName.keySet(), is(Set.of("file", "taxonomy", "date", "currency")));
        assertThat(byName.get("taxonomy").get("required").getAsBoolean(), is(true));
        assertThat(byName.get("date").get("type").getAsString(), is("date"));

        var codes = new ArrayList<String>();
        allocation.getAsJsonArray("warnings").forEach(w -> codes.add(w.getAsJsonObject().get("code").getAsString()));
        assertThat(codes, is(List.of("stale-price", "over-assigned", "exchange-rate-missing", "stale-exchange-rate",
                        "exchange-rate-cache-unavailable")));
    }

    /** What the filters must keep up with: every type core knows, so that a new one is noticed. */
    private static List<String> coreTypeNames()
    {
        return Stream.concat(Arrays.stream(AccountTransaction.Type.values()),
                        Arrays.stream(PortfolioTransaction.Type.values())).map(Enum::name).distinct().toList();
    }

    private static List<String> strings(JsonArray array)
    {
        var strings = new ArrayList<String>();
        array.forEach(element -> strings.add(element.getAsString()));
        return strings;
    }

    @Test
    public void commandsWithoutWarningsDescribeAnEmptyList()
    {
        var data = data(CliRun.run("describe"));

        for (var name : List.of("info", "describe", "instruments", "investment-accounts", "cash-accounts"))
            assertThat(name, command(data, name).getAsJsonArray("warnings").size(), is(0));
    }

    @Test
    public void describeWorksWithoutFileAndReturnsEnvelope()
    {
        var run = CliRun.run("describe");

        assertThat(run.exitCode, is(0));

        var envelope = run.json().getAsJsonObject();
        assertThat(envelope.keySet(), is(Set.of("data", "meta", "warnings")));
        assertThat(envelope.getAsJsonArray("warnings").size(), is(0));
        assertThat(envelope.getAsJsonObject("meta").get("asOf").getAsString(), is("2026-10-16"));
    }
}
