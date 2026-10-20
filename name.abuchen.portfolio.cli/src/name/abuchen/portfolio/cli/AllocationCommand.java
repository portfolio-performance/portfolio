package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

/**
 * <code>allocation</code>: how the value of the client splits across the
 * classifications of one taxonomy at a date, as flat rows.
 * <p>
 * The values are core's. A classification's value is that of its whole
 * subtree, as core's grouping by taxonomy sees a top-level classification:
 * {@link Classification#flattenAssignments()} turns each subtree into the one
 * classification core groups, so the top level equals
 * {@link ClientSnapshot#groupByTaxonomy} exactly.
 * <p>
 * As in <code>holdings</code>, a figure that needs a missing exchange rate
 * can't be trusted, so no value, share or total is stated then.
 */
final class AllocationCommand
{
    private static final OptionSpec TAXONOMY = new OptionSpec("taxonomy", "ref",
                    "Taxonomy to allocate by: its UUID, or for convenience its exact name. Names are accepted on reads only. List the taxonomies with the taxonomies command.")
                                    .asRequired();

    private static final OptionSpec DATE = OptionSpec.date("date",
                    "Day to value the allocation at. Defaults to today. Valuation uses the prices on or before this day, and conversions the rates on or before it.");

    private static final OptionSpec CURRENCY = new OptionSpec("currency", "code",
                    "ISO currency of the values and the total. Defaults to the base currency of the file.");

    static final WarningSpec OVER_ASSIGNED = new WarningSpec("over-assigned",
                    "An instrument or cash account is assigned more than 100% in the taxonomy. It is counted as core counts it. Carries the instrument or cashAccount UUID and the totalWeight as a fraction.");

    private static final Schema ROW = Schema.object("One classification, or the unassigned rest.")
                    .optionalProperty("uuid", Schema.string("Canonical UUID. Absent on the unassigned row."))
                    .property("name", Schema.string("Classification name; \"Unassigned\" on the unassigned row."))
                    .optionalProperty("parent", Schema.string("UUID of the parent classification. Absent at the top level."))
                    .property("path", Schema.array("Names of the ancestors, top level first, excluding this classification.",
                                    Schema.string("A classification name.")))
                    .optionalProperty("targetWeight", Schema.number(
                                    "The rebalancing target as an unrounded fraction of the parent. 0 if not set. Absent on the unassigned row."))
                    .optionalProperty("unassigned", Schema.bool("True on the unassigned row, the last one. Absent on others."))
                    .optionalProperty("value", Schema.number(
                                    "Value of the whole subtree in the reporting currency at the date. Absent if an exchange rate is missing."))
                    .optionalProperty("share", Schema.number(
                                    "The value as an unrounded fraction of total. Absent if an exchange rate is missing or the total is zero."));

    private static final Schema DATA = Schema.object("The allocation at the date.")
                    .property("items", Schema.array(
                                    "One row per classification at every depth in tree pre-order, then the unassigned row. The top-level rows and the unassigned row add up to the total.",
                                    ROW))
                    .optionalProperty("total", Schema.number(
                                    "Total assets (cash and instruments) of the client in the reporting currency. Absent if an exchange rate is missing."));

    private static final Schema META = ResponseMeta.fileSchema()
                    .property("date", Schema.string("The day the allocation is valued at: as given, otherwise today."))
                    .property("currency", Schema.string("The reporting currency of the values: as given, otherwise the base currency."))
                    .property("taxonomy", Schema.object("The taxonomy the reference resolved to.")
                                    .property("uuid", Schema.string("Canonical UUID."))
                                    .property("name", Schema.string("Taxonomy name; empty if the file has none.")))
                    .optionalProperty("exchangeRates", ExchangeRates.metaSchema());

    private static final List<WarningSpec> WARNINGS = Stream
                    .concat(Stream.of(Prices.STALE_PRICE, OVER_ASSIGNED), ExchangeRates.WARNINGS.stream()).toList();

    static final CommandSpec SPEC = new CommandSpec("allocation",
                    "How the value of the client splits across the classifications of one taxonomy at a date: one flat row per classification at every depth plus an unassigned row, with value and share in the base currency or --currency.",
                    true, List.of(TAXONOMY, DATE, CURRENCY), DATA, META, WARNINGS, AllocationCommand::execute);

    /** Something owned that takes part in the allocation, and what it is worth in its own currency. */
    private record Holding(InvestmentVehicle vehicle, Money value)
    {
    }

    private AllocationCommand()
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var date = DATE.parseDate(context.option(DATE.name())).orElse(LocalDate.now(context.clock()));
        var requested = ExchangeRates.parseCurrency(CURRENCY, context.option(CURRENCY.name()));
        var client = context.loadClient().client();
        var taxonomy = References.taxonomy(client, TAXONOMY.name(), context.option(TAXONOMY.name()).orElseThrow());

        var currency = requested.orElse(client.getBaseCurrency());
        var rates = new ExchangeRates(context, client);
        var snapshot = ClientSnapshot.create(client, rates.converter(currency), date);

        var holdings = new ArrayList<Holding>();
        var stalePrices = new LinkedHashMap<Security, Warning>();
        for (var position : snapshot.getJointPortfolio().getPositions())
        {
            holdings.add(new Holding(position.getInvestmentVehicle(), position.calculateValue()));
            Prices.staleWarning(position.getSecurity(), position.getPrice(), date)
                            .ifPresent(warning -> stalePrices.putIfAbsent(position.getSecurity(), warning));
        }
        for (var account : snapshot.getAccounts())
        {
            // core leaves out accounts without funds
            if (!account.getFunds().isZero())
                holdings.add(new Holding(account.getAccount(), account.getUnconvertedFunds()));
        }

        // look every rate up, so that all missing ones are reported
        var ratesFound = true;
        for (var holding : holdings)
        {
            var value = holding.value();
            if (value.isZero() && !currency.equals(value.getCurrencyCode()))
                continue;

            if (rates.lookup(date, value.getCurrencyCode(), currency).isEmpty())
                ratesFound = false;
        }

        var nodes = TaxonomyTree.preOrder(taxonomy);
        var items = new ArrayList<Map<String, Object>>();

        // without every rate no value can be trusted, so rows keep only their identity
        var total = ratesFound ? OptionalLong.of(snapshot.getMonetaryAssets().getAmount()) : OptionalLong.empty();
        for (var node : nodes)
        {
            var value = ratesFound ? OptionalLong.of(subtreeValue(snapshot, node.classification()))
                            : OptionalLong.empty();
            items.add(row(node, value, total));
        }
        items.add(unassignedRow(ratesFound ? OptionalLong.of(unassignedValue(snapshot, taxonomy)) : OptionalLong.empty(),
                        total));

        var data = new LinkedHashMap<String, Object>();
        data.put("items", items);
        total.ifPresent(t -> data.put("total", Decimals.of(t, Values.Amount)));

        var meta = ResponseMeta.of(context, client);
        meta.put("date", date.toString());
        meta.put("currency", currency);
        var reference = new LinkedHashMap<String, Object>();
        MasterData.putIdentity(reference, taxonomy.getId(), taxonomy.getName());
        meta.put("taxonomy", reference);
        rates.putCacheInto(meta);

        var response = Response.of(data, meta);
        stalePrices.values().forEach(response::warn);
        overAssigned(taxonomy, holdings).forEach(response::warn);
        rates.warnings().forEach(response::warn);
        return response;
    }

    private static Map<String, Object> row(TaxonomyTree.Node node, OptionalLong value, OptionalLong total)
    {
        var json = node.identity();
        json.put("targetWeight", Decimals.of(TaxonomyTree.fraction(node.classification().getWeight())));
        putFigures(json, value, total);
        return json;
    }

    private static Map<String, Object> unassignedRow(OptionalLong value, OptionalLong total)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("name", "Unassigned");
        json.put("path", List.of());
        json.put("unassigned", true);
        putFigures(json, value, total);
        return json;
    }

    /** The share is left out when it can't be stated: without a total, or with a total of zero. */
    private static void putFigures(Map<String, Object> json, OptionalLong value, OptionalLong total)
    {
        value.ifPresent(v -> json.put("value", Decimals.of(v, Values.Amount)));
        if (value.isPresent() && total.isPresent() && total.getAsLong() != 0)
            json.put("share", Decimals.of(BigDecimal.valueOf((double) value.getAsLong() / (double) total.getAsLong())));
    }

    /** What core's grouping makes of the subtree as if it were a top-level classification. */
    private static long subtreeValue(ClientSnapshot snapshot, Classification classification)
    {
        var root = new Classification(Classification.VIRTUAL_ROOT, Classification.VIRTUAL_ROOT);
        root.addChild(classification.flattenAssignments());

        var single = new Taxonomy(Classification.VIRTUAL_ROOT);
        single.setRootNode(root);

        return snapshot.groupByTaxonomy(single).getCategories().filter(c -> !isUnassigned(c))
                        .mapToLong(c -> c.getValuation().getAmount()).sum();
    }

    private static long unassignedValue(ClientSnapshot snapshot, Taxonomy taxonomy)
    {
        return snapshot.groupByTaxonomy(taxonomy).getCategories().filter(AllocationCommand::isUnassigned)
                        .mapToLong(c -> c.getValuation().getAmount()).sum();
    }

    private static boolean isUnassigned(AssetCategory category)
    {
        return Classification.UNASSIGNED_ID.equals(category.getClassification().getId());
    }

    /** The owned instruments and accounts whose assignments in the taxonomy add up to more than 100%. */
    private static List<Warning> overAssigned(Taxonomy taxonomy, List<Holding> holdings)
    {
        var weights = new IdentityHashMap<InvestmentVehicle, Integer>();
        taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification, Assignment assignment)
            {
                weights.merge(assignment.getInvestmentVehicle(), assignment.getWeight(), Integer::sum);
            }
        });

        var warnings = new ArrayList<Warning>();
        holdings.stream().map(Holding::vehicle)
                        .sorted(MasterData.byNameThenUuid(InvestmentVehicle::getName, InvestmentVehicle::getUUID))
                        .forEach(vehicle -> {
                            var weight = weights.getOrDefault(vehicle, 0);
                            if (weight <= Classification.ONE_HUNDRED_PERCENT)
                                return;

                            var fraction = TaxonomyTree.fraction(weight);
                            warnings.add(new Warning(OVER_ASSIGNED.code(),
                                            "'" + MasterData.orEmpty(vehicle.getName()) + "' is assigned "
                                                            + fraction.movePointRight(2).toPlainString()
                                                            + "% in the taxonomy '"
                                                            + MasterData.orEmpty(taxonomy.getName())
                                                            + "'; it is counted as core counts it.")
                                                                            .with(vehicle instanceof Security
                                                                                            ? "instrument"
                                                                                            : "cashAccount",
                                                                                            vehicle.getUUID())
                                                                            .with("totalWeight", Decimals.of(fraction)));
                        });
        return warnings;
    }
}
