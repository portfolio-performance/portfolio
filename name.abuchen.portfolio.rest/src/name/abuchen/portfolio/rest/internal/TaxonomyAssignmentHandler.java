package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.rest.WriteApprovals;

/**
 * Putting one vehicle into one class of a taxonomy, and taking it out again.
 *
 * The application does this in its taxonomy editor, and the calls here are the
 * ones it makes there itself: an assignment is added to the class and the client
 * is marked dirty. That last call is the whole of it - nothing in the model
 * reports an assignment, {@link Classification#addAssignment} fires no event and
 * the class is not a listened-to object - so an open classification report would
 * go on showing the state before the change without it. Marking the client dirty
 * is what the application's own menus do in the same situation.
 *
 * A vehicle is an instrument or a cash account, and nothing else: an investment
 * account is not an {@link InvestmentVehicle}, so a depot cannot be assigned even
 * though it has a uuid like everything else in this API.
 */
public final class TaxonomyAssignmentHandler
{
    private TaxonomyAssignmentHandler()
    {
    }

    /**
     * What a placement or a removal would do, as facts for the user to approve -
     * and as the first validation of the request. Names are resolved here, so a
     * request that could not be carried out is refused before it ever interrupts
     * the user with a prompt.
     *
     * @param fileLabel
     *            the file the change would land in, as the application names it
     */
    public static WriteApprovals.Facts describe(Client client, String taxonomyParam, String classParam,
                    String vehicleParam, boolean removal, String fileLabel)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var taxonomy = TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var classification = TaxonomiesHandler.resolveClass(classParam, taxonomy, client, errors);
        var vehicle = resolveVehicle(vehicleParam, client, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // a removal of something that was never there is refused here rather than
        // after the user was asked to approve it
        if (removal && assignmentOf(classification, vehicle) == null)
            throw ApiException.notAssigned("this class does not hold " + vehicle.getName() //$NON-NLS-1$
                            + " - see the assignments of pp_list_taxonomies"); //$NON-NLS-1$

        // the class is where the vehicle goes, the taxonomy says which of the
        // user's classifications that class belongs to
        return new WriteApprovals.Facts(null, fileLabel, TaxonomiesHandler.kindOf(vehicle), vehicle.getName(),
                        classification.getName(), taxonomy.getName());
    }

    /**
     * Makes sure one vehicle belongs to one class. Asked twice, it has done
     * exactly what it did the first time, which is why the route behind it is a
     * PUT and not a POST: an agent may send it again without doubling anything.
     *
     * @param weightElement
     *            the share of the class as a fraction, or null to leave the
     *            share alone - a new assignment then takes the whole of it, an
     *            existing one keeps what it has
     */
    public static JsonObject put(Client client, String taxonomyParam, String classParam, String vehicleParam,
                    JsonElement weightElement)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var taxonomy = TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var classification = TaxonomiesHandler.resolveClass(classParam, taxonomy, client, errors);
        var vehicle = resolveVehicle(vehicleParam, client, errors);
        var weight = resolveWeight(weightElement, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var existing = assignmentOf(classification, vehicle);
        var before = classification.getAssignments().size();

        boolean changed;
        int rank;

        if (existing == null)
        {
            var assignment = new Classification.Assignment(vehicle,
                            weight == null ? Classification.ONE_HUNDRED_PERCENT : weight);
            assignment.setRank(nextRank(classification));
            classification.addAssignment(assignment);
            changed = true;
            rank = assignment.getRank();
        }
        else
        {
            rank = existing.getRank();
            // a share left out of the request is a share to keep, not a share to
            // reset: the same request twice must not undo what it just did
            if (weight != null && existing.getWeight() != weight)
            {
                existing.setWeight(weight);
                changed = true;
            }
            else
                changed = false;
        }

        // a restatement changes nothing, so the user is not asked to save a file
        // the API did not touch
        if (changed)
            client.markDirty();

        var after = classification.getAssignments().size();
        var json = response(classification, taxonomy, vehicle, before, after);
        json.addProperty("rank", rank); //$NON-NLS-1$
        json.addProperty("created", existing == null); //$NON-NLS-1$
        json.addProperty("changed", changed); //$NON-NLS-1$
        return json;
    }

    /**
     * Takes one vehicle out of one class. The assignment is described in the
     * response rather than only in its absence, because a caller that asked to
     * remove something wants to hear what was removed.
     */
    public static JsonObject remove(Client client, String taxonomyParam, String classParam, String vehicleParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var taxonomy = TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var classification = TaxonomiesHandler.resolveClass(classParam, taxonomy, client, errors);
        var vehicle = resolveVehicle(vehicleParam, client, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var assignment = assignmentOf(classification, vehicle);
        if (assignment == null)
        {
            throw ApiException.notAssigned("this class does not hold " + vehicle.getName() //$NON-NLS-1$
                            + " - see the assignments of pp_list_taxonomies"); //$NON-NLS-1$
        }

        var before = classification.getAssignments().size();
        var rank = assignment.getRank();
        var weight = assignment.getWeight();

        classification.removeAssignment(assignment);
        client.markDirty();

        var json = response(classification, taxonomy, vehicle, before, classification.getAssignments().size());
        json.addProperty("rank", rank); //$NON-NLS-1$
        json.add("weight", TaxonomiesHandler.ratio(weight)); //$NON-NLS-1$
        return json;
    }

    /** what the class, the vehicle and the counts around the change were */
    private static JsonObject response(Classification classification, Taxonomy taxonomy, InvestmentVehicle vehicle,
                    int before, int after)
    {
        var json = new JsonObject();

        var tree = new JsonObject();
        tree.addProperty("id", taxonomy.getId()); //$NON-NLS-1$
        tree.addProperty("name", taxonomy.getName()); //$NON-NLS-1$
        json.add("taxonomy", tree); //$NON-NLS-1$

        json.addProperty("class", classification.getId()); //$NON-NLS-1$
        json.addProperty("className", classification.getName()); //$NON-NLS-1$

        var held = new JsonObject();
        held.addProperty("uuid", vehicle.getUUID()); //$NON-NLS-1$
        held.addProperty("name", vehicle.getName()); //$NON-NLS-1$
        held.addProperty("type", TaxonomiesHandler.kindOf(vehicle)); //$NON-NLS-1$
        json.add("vehicle", held); //$NON-NLS-1$

        json.addProperty("assignedBefore", before); //$NON-NLS-1$
        json.addProperty("assignedAfter", after); //$NON-NLS-1$

        var assignment = assignmentOf(classification, vehicle);
        if (assignment != null)
            json.add("weight", TaxonomiesHandler.ratio(assignment.getWeight())); //$NON-NLS-1$

        return json;
    }

    /** the assignment of this very vehicle, or null when the class does not hold it */
    private static Classification.Assignment assignmentOf(Classification classification, InvestmentVehicle vehicle)
    {
        return classification.getAssignments().stream() //
                        .filter(a -> a.getInvestmentVehicle() == vehicle) //
                        .findFirst().orElse(null);
    }

    /**
     * One past the last rank, so the new vehicle lands at the end of the class
     * as it does in the application's table - and as the paging of a class's
     * vehicles reads them, since that key leads with the rank.
     */
    private static int nextRank(Classification classification)
    {
        var last = -1;
        for (var assignment : classification.getAssignments())
            last = Math.max(last, assignment.getRank());
        return last + 1;
    }

    /**
     * The vehicle by uuid: an instrument or a cash account, which is everything
     * the model can put into a class. A depot is named here on purpose - it has a
     * uuid and it is the fourth thing a caller would think to try, and a caller
     * who gets "no such vehicle" for one goes looking for a vehicle rather than
     * reading why this one cannot be placed.
     */
    private static InvestmentVehicle resolveVehicle(String raw, Client client,
                    List<ApiException.FieldError> errors)
    {
        var trimmed = raw.trim();

        InvestmentVehicle found = client.getSecurities().stream() //
                        .filter(s -> s.getUUID().equals(trimmed)) //
                        .findFirst().orElse(null);
        if (found == null)
        {
            found = client.getAccounts().stream() //
                            .filter(a -> a.getUUID().equals(trimmed)) //
                            .findFirst().orElse(null);
        }

        if (found == null)
        {
            var depot = client.getPortfolios().stream() //
                            .filter(p -> p.getUUID().equals(trimmed)) //
                            .findFirst().orElse(null);
            if (depot != null)
            {
                errors.add(new ApiException.FieldError("vehicle", "vehicle-not-assignable", //$NON-NLS-1$ //$NON-NLS-2$
                                "an investment account cannot be assigned to a class; assign its reference cash account instead")); //$NON-NLS-1$
                return null;
            }

            errors.add(new ApiException.FieldError("vehicle", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "no instrument and no cash account of this file has this uuid - see pp_list_instruments and pp_list_cash_accounts"));
        }

        return found;
    }

    /**
     * The share of the class, as a fraction of a whole - the way every ratio of
     * this API travels. Left out, it is nothing the request means to say.
     */
    private static Integer resolveWeight(JsonElement element, List<ApiException.FieldError> errors)
    {
        if (element == null || element.isJsonNull())
            return null;

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
        {
            errors.add(new ApiException.FieldError("weight", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "weight must be a number between 0 and 1 - 0.6 is the 60 % the application shows"));
            return null;
        }

        var fraction = element.getAsDouble();
        if (fraction <= 0d || fraction > 1d)
        {
            errors.add(new ApiException.FieldError("weight", "out-of-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "weight must be more than 0 and at most 1 - a share of one class, where 1 is a whole"));
            return null;
        }

        return (int) Math.round(fraction * Classification.ONE_HUNDRED_PERCENT);
    }
}
