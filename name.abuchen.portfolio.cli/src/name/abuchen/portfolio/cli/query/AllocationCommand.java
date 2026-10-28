package name.abuchen.portfolio.cli.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.ReferenceOption;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.cli.command.WarningSpec;
import name.abuchen.portfolio.cli.currency.ReportingCurrency;
import name.abuchen.portfolio.cli.currency.StalePrices;
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
public final class AllocationCommand
{
    private static final ReferenceOption<Taxonomy> TAXONOMY_OPTION = ReferenceOption.taxonomy("taxonomy",
                    "Taxonomy to allocate by. List the taxonomies with the taxonomies command.").asRequired();

    /** The taxonomy to allocate by; <code>meta</code> gets its identity. */
    private static final Facet<Taxonomy> TAXONOMY = Facet.of(Declaration.empty().options(TAXONOMY_OPTION.spec())
                    .meta("taxonomy", Schema.object("The taxonomy the reference resolved to.")
                                    .property("uuid", Schema.string("Canonical UUID."))
                                    .property("name", Schema.string("Taxonomy name; empty if the file has none."))),
                    run -> run.reference(TAXONOMY_OPTION, run.client()).orElseThrow(), (taxonomy, out) -> {
                        var reference = new LinkedHashMap<String, Object>();
                        MasterData.putIdentity(reference, taxonomy.getId(), taxonomy.getName());
                        out.meta("taxonomy", reference);
                    });

    private static final Facet<LocalDate> DATE = ValuationDay.facet(
                    "Day to value the allocation at. Defaults to today. Valuation uses the prices on or before this day, and conversions the rates on or before it.",
                    "The day the allocation is valued at: as given, otherwise today.");

    private static final Facet<ReportingCurrency> CURRENCY = ReportingCurrency.facet(
                    "ISO currency of the values and the total. Defaults to the base currency of the file.",
                    ReportingCurrency.Counting.ITEMS);

    static final WarningSpec OVER_ASSIGNED = new WarningSpec("over-assigned",
                    "An instrument or cash account is assigned more than 100% in the taxonomy. It is counted as core counts it. Carries the instrument or cashAccount UUID and the totalWeight as a fraction.");

    /** The handler warns of over-assigned holdings. */
    private static final Facet<Void> OVER_ASSIGNMENT = Facet.of(Declaration.empty().warnings(OVER_ASSIGNED),
                    run -> null);

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

    public static final CommandSpec SPEC = CommandSpec.of("allocation",
                    "How the value of the client splits across the classifications of one taxonomy at a date: one flat row per classification at every depth plus an unassigned row, with value and share in the base currency or --currency.",
                    DATA, AllocationCommand::execute, ClientFile.READ, TAXONOMY, DATE, StalePrices.FACET,
                    OVER_ASSIGNMENT, CURRENCY);

    /** Something owned that takes part in the allocation, and what it is worth in its own currency. */
    private record Holding(InvestmentVehicle vehicle, Money value)
    {
    }

    /** What core values the rows at: the total, each classification in pre-order and the unassigned row. */
    private record Valuation(long total, List<Long> values, long unassigned)
    {
    }

    private AllocationCommand()
    {
    }

    private static Map<String, Object> execute(CommandContext run) throws CliException
    {
        var date = run.get(DATE);
        var client = run.client();
        var taxonomy = run.get(TAXONOMY);
        var reporting = run.get(CURRENCY);
        var core = reporting.coreFigures();
        // only the holdings in their own currencies are read from this one
        var snapshot = ClientSnapshot.create(client, core.unreported(), date);

        var holdings = new ArrayList<Holding>();
        var stalePrices = run.get(StalePrices.FACET);
        for (var position : snapshot.getJointPortfolio().getPositions())
        {
            holdings.add(new Holding(position.getInvestmentVehicle(), position.calculateValue()));
            stalePrices.note(position.getSecurity(), position.getPrice(), date);
        }
        for (var account : snapshot.getAccounts())
        {
            // core leaves out accounts without funds
            if (!account.getFunds().isZero())
                holdings.add(new Holding(account.getAccount(), account.getUnconvertedFunds()));
        }

        var nodes = TaxonomyTree.preOrder(taxonomy);
        var valuation = core.trusted(date, holdings.stream().map(Holding::value).toList(), () -> {
            var converted = ClientSnapshot.create(client, core.converter(), date);
            return new Valuation(converted.getMonetaryAssets().getAmount(),
                            nodes.stream().map(node -> subtreeValue(converted, node.classification())).toList(),
                            unassignedValue(converted, taxonomy));
        });

        // without every rate no value can be trusted, so rows keep only their identity
        var items = new ArrayList<Map<String, Object>>();
        var total = valuation.map(v -> OptionalLong.of(v.total())).orElse(OptionalLong.empty());
        for (var index = 0; index < nodes.size(); index++)
        {
            var at = index;
            var value = valuation.map(v -> OptionalLong.of(v.values().get(at))).orElse(OptionalLong.empty());
            items.add(row(nodes.get(index), value, total));
        }
        items.add(unassignedRow(valuation.map(v -> OptionalLong.of(v.unassigned())).orElse(OptionalLong.empty()),
                        total));

        var data = new LinkedHashMap<String, Object>();
        data.put("items", items);
        total.ifPresent(t -> data.put("total", Decimals.of(t, Values.Amount)));

        overAssigned(taxonomy, holdings).forEach(run::warn);
        return data;
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
            // mirrors core's AssetCategory.getShare(); ADR 0007-cli-arithmetic-mirrors-core-rounding-per-site.md
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
