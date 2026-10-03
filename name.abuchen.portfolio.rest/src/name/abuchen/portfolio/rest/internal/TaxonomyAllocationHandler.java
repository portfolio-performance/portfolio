package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

/**
 * The value of every category of a taxonomy at a date: the statement of
 * assets distributed over the category tree by the assignment weights,
 * computed as the application's taxonomy view does it ({@code TaxonomyModel}):
 * one snapshot, each assignment contributes its vehicle's valuation times its
 * weight, a category sums its subtree, and what no category holds is
 * {@code unassigned}. The target value of a category follows the rebalancing
 * view: the parent's target less the parent's direct assignments, times the
 * category's weight.
 */
public final class TaxonomyAllocationHandler
{
    private static final BigDecimal HUNDRED_PERCENT = BigDecimal.valueOf(Classification.ONE_HUNDRED_PERCENT);

    private TaxonomyAllocationHandler()
    {
    }

    /**
     * @param client
     *            the file, owner of the taxonomy
     * @param reported
     *            the file as reported on, possibly narrowed by
     *            {@link ReportFilter}
     */
    public static JsonElement allocation(Client client, Client reported, ExchangeRateProviderFactory factory,
                    String taxonomyId, String dateParam, String currencyParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var date = LocalDate.now();
        if (dateParam != null)
        {
            try
            {
                date = LocalDate.parse(dateParam);
            }
            catch (DateTimeParseException e)
            {
                errors.add(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "date must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            }
        }

        var currency = client.getBaseCurrency();
        if (currencyParam != null)
        {
            if (CurrencyUnit.getInstance(currencyParam) == null)
                errors.add(new ApiException.FieldError("currency", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                                currencyParam + " is not a known currency")); //$NON-NLS-1$
            else
                currency = currencyParam;
        }

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var taxonomy = TaxonomiesHandler.find(client, taxonomyId);

        var snapshot = ClientSnapshot.create(reported, new CurrencyConverterImpl(factory, currency), date);
        var positions = snapshot.getPositionsByVehicle();
        var total = snapshot.getMonetaryAssets();

        var calculation = new Calculation(currency, total.getAmount(), positions);

        // what the categories do not hold, per vehicle, as the taxonomy view
        // lists it under "without classification"
        var assignedWeight = new HashMap<InvestmentVehicle, Integer>();
        taxonomy.getRoot().accept(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification, Classification.Assignment assignment)
            {
                assignedWeight.merge(assignment.getInvestmentVehicle(), assignment.getWeight(), Integer::sum);
            }
        });

        var unassignedItems = new JsonArray();
        long unassigned = 0;
        var vehicles = new ArrayList<>(positions.keySet());
        vehicles.sort(Comparator.comparing(v -> String.valueOf(v.getName())));
        for (var vehicle : vehicles)
        {
            var weight = Classification.ONE_HUNDRED_PERCENT - assignedWeight.getOrDefault(vehicle, 0);
            if (weight <= 0)
                continue;
            var value = calculation.valueOf(vehicle, weight);
            unassigned += value;
            unassignedItems.add(calculation.vehicleJson(vehicle, weight, value));
        }

        // the root category's target: everything that is classified
        var root = taxonomy.getRoot();
        var rootTarget = percentOf(total.getAmount() - unassigned, root.getWeight());

        var categories = new JsonArray();
        var rootAssignments = calculation.directAssignmentsValue(root);
        for (var child : sorted(root.getChildren()))
            categories.add(calculation.categoryJson(child, rootTarget - rootAssignments));

        var json = new JsonObject();
        var taxonomyJson = new JsonObject();
        taxonomyJson.addProperty("id", taxonomy.getId()); //$NON-NLS-1$
        taxonomyJson.addProperty("name", taxonomy.getName()); //$NON-NLS-1$
        json.add("taxonomy", taxonomyJson); //$NON-NLS-1$
        json.addProperty("date", date.toString()); //$NON-NLS-1$
        json.addProperty("currency", currency); //$NON-NLS-1$
        json.add("totalAssets", EntityJson.toJson(total)); //$NON-NLS-1$
        json.add("categories", categories); //$NON-NLS-1$
        var assignmentsAtRoot = calculation.assignmentsJson(root);
        if (!assignmentsAtRoot.isEmpty())
            json.add("assignments", assignmentsAtRoot); //$NON-NLS-1$

        var unassignedJson = new JsonObject();
        unassignedJson.add("value", EntityJson.toJson(Money.of(currency, unassigned))); //$NON-NLS-1$
        unassignedJson.add("share", calculation.share(unassigned)); //$NON-NLS-1$
        unassignedJson.add("assignments", unassignedItems); //$NON-NLS-1$
        json.add("unassigned", unassignedJson); //$NON-NLS-1$
        return json;
    }

    /** {@code amount} times a weight in the {@link Classification#ONE_HUNDRED_PERCENT} scale, rounded */
    private static long percentOf(long amount, int weight)
    {
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(weight))
                        .divide(HUNDRED_PERCENT, 0, RoundingMode.HALF_UP).longValueExact();
    }

    private static List<Classification> sorted(List<Classification> classifications)
    {
        var list = new ArrayList<>(classifications);
        list.sort(Comparator.comparingInt(Classification::getRank));
        return list;
    }

    private record Calculation(String currency, long total, Map<InvestmentVehicle, AssetPosition> positions)
    {
        /** the part of the vehicle's valuation that a weight assigns */
        long valueOf(InvestmentVehicle vehicle, int weight)
        {
            var position = positions.get(vehicle);
            return position == null ? 0 : percentOf(position.getValuation().getAmount(), weight);
        }

        long directAssignmentsValue(Classification classification)
        {
            return classification.getAssignments().stream()
                            .mapToLong(a -> valueOf(a.getInvestmentVehicle(), a.getWeight())).sum();
        }

        /** the value of the category's subtree */
        long valueOf(Classification classification)
        {
            var value = directAssignmentsValue(classification);
            for (var child : classification.getChildren())
                value += valueOf(child);
            return value;
        }

        JsonObject categoryJson(Classification classification, long availableTarget)
        {
            var value = valueOf(classification);
            var target = percentOf(availableTarget, classification.getWeight());

            var json = new JsonObject();
            json.addProperty("id", classification.getId()); //$NON-NLS-1$
            json.addProperty("name", classification.getName()); //$NON-NLS-1$
            if (classification.getColor() != null)
                json.addProperty("color", classification.getColor()); //$NON-NLS-1$
            json.add("weight", EntityJson.decimal(classification.getWeight(), Values.Weight.precision())); //$NON-NLS-1$
            json.add("value", EntityJson.toJson(Money.of(currency, value))); //$NON-NLS-1$
            json.add("share", share(value)); //$NON-NLS-1$
            json.add("targetValue", EntityJson.toJson(Money.of(currency, target))); //$NON-NLS-1$
            json.add("deviation", EntityJson.toJson(Money.of(currency, value - target))); //$NON-NLS-1$

            var ownAssignments = directAssignmentsValue(classification);
            var children = new JsonArray();
            for (var child : sorted(classification.getChildren()))
                children.add(categoryJson(child, target - ownAssignments));
            if (!children.isEmpty())
                json.add("children", children); //$NON-NLS-1$

            var assignments = assignmentsJson(classification);
            if (!assignments.isEmpty())
                json.add("assignments", assignments); //$NON-NLS-1$
            return json;
        }

        JsonArray assignmentsJson(Classification classification)
        {
            var assignments = new JsonArray();
            for (var assignment : classification.getAssignments())
            {
                var vehicle = assignment.getInvestmentVehicle();
                assignments.add(vehicleJson(vehicle, assignment.getWeight(),
                                valueOf(vehicle, assignment.getWeight())));
            }
            return assignments;
        }

        JsonObject vehicleJson(InvestmentVehicle vehicle, int weight, long value)
        {
            var json = new JsonObject();
            json.addProperty("vehicle", vehicle.getUUID()); //$NON-NLS-1$
            json.addProperty("type", vehicle instanceof Account ? "cash-account" : "instrument"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            json.addProperty("name", vehicle.getName()); //$NON-NLS-1$
            json.add("weight", EntityJson.decimal(weight, Values.Weight.precision())); //$NON-NLS-1$
            json.add("value", EntityJson.toJson(Money.of(currency, value))); //$NON-NLS-1$
            return json;
        }

        /** the value's share of the total assets, as a fraction */
        JsonElement share(long value)
        {
            if (total == 0)
                return EntityJson.decimal(0, 0);
            return EntityJson.decimal(BigDecimal.valueOf(value).divide(BigDecimal.valueOf(total), 10,
                            RoundingMode.HALF_EVEN).doubleValue());
        }
    }
}
