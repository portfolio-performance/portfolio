package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.rest.WriteApprovals;

/**
 * The one number of a class that this file keeps and no caller could set: the
 * target weight its rebalancing aims at. It has been readable since
 * pp_list_taxonomies carried {@code targetWeight}; this is the other half.
 *
 * <p>
 * A PATCH although targetWeight is the only field it takes. A PUT would
 * promise that the request states the class whole - name, parent, the whole
 * tree of it - and answer a caller that meant one number by describing the
 * class as if from nothing. One field, so one field is what the body holds.
 * </p>
 */
public final class TaxonomyClassHandler
{
    private TaxonomyClassHandler()
    {
    }

    /**
     * What the prompt will be about, with every lookup refused before anybody is
     * asked: the tree, the class, and the number all settle here, so a user is
     * never offered a change that could not have been made.
     */
    public static WriteApprovals.Facts describe(Client client, String taxonomyParam, String classParam,
                    JsonElement targetWeight, String fileLabel)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var taxonomy = TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var classification = TaxonomiesHandler.resolveClass(classParam, taxonomy, client, errors);
        targetWeight(targetWeight, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // the class carries the number; the taxonomy says which of the user's
        // classifications that class belongs to - the same pairing the vehicle
        // writes show, with the vehicle replaced by the class itself
        return new WriteApprovals.Facts(null, fileLabel, "classification", classification.getName(), //$NON-NLS-1$
                        taxonomy.getName(), null);
    }

    /**
     * Sets the target weight of one class. The fraction is the one
     * pp_list_taxonomies reads back - the model's scaled integer is its own
     * affair - and setting the same weight again changes nothing, so a caller
     * may send this twice.
     */
    public static JsonObject patch(Client client, String taxonomyParam, String classParam, JsonElement targetWeight)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var taxonomy = TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var classification = TaxonomiesHandler.resolveClass(classParam, taxonomy, client, errors);
        var weight = targetWeight(targetWeight, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var before = classification.getWeight();
        var changed = before != weight;

        // a restatement changes nothing, so the user is not asked to save a file
        // the API did not touch
        if (changed)
        {
            classification.setWeight(weight);
            client.markDirty();
        }

        var json = new JsonObject();

        var tree = new JsonObject();
        tree.addProperty("id", taxonomy.getId()); //$NON-NLS-1$
        tree.addProperty("name", taxonomy.getName()); //$NON-NLS-1$
        json.add("taxonomy", tree); //$NON-NLS-1$

        var cls = new JsonObject();
        cls.addProperty("id", classification.getId()); //$NON-NLS-1$
        cls.addProperty("name", classification.getName()); //$NON-NLS-1$
        var parent = classification.getParent();
        if (parent != null)
            cls.addProperty("parent", parent.getId()); //$NON-NLS-1$
        json.add("class", cls); //$NON-NLS-1$

        json.add("previousTargetWeight", TaxonomiesHandler.ratio(before)); //$NON-NLS-1$
        json.add("targetWeight", TaxonomiesHandler.ratio(classification.getWeight())); //$NON-NLS-1$
        json.addProperty("changed", changed); //$NON-NLS-1$
        return json;
    }

    /**
     * What the prompt will be about for a class that does not exist yet, with
     * every refusal - tree, parent, name, number, sibling of the same name -
     * settled before anybody is asked.
     */
    public static WriteApprovals.Facts describeCreate(Client client, String taxonomyParam, JsonObject body,
                    String fileLabel)
    {
        var fresh = validateCreate(client, taxonomyParam, body);
        return new WriteApprovals.Facts(null, fileLabel, "classification", fresh.name(), //$NON-NLS-1$
                        fresh.taxonomy().getName(), null);
    }

    /**
     * Adds one class to one taxonomy. A POST, the one class-write that brings
     * something new: the class gets a uuid here, which pp_assign_vehicle and
     * pp_update_class accept at once - and a second sending of the same request
     * is refused rather than doubled, because two classes of one name under one
     * parent could only be told apart by a caller that went and looked.
     */
    public static JsonObject create(Client client, String taxonomyParam, JsonObject body)
    {
        var fresh = validateCreate(client, taxonomyParam, body);

        var classification = new Classification(null, UUID.randomUUID().toString(), fresh.name());
        classification.setWeight(fresh.targetWeight());
        classification.setParent(fresh.parent());
        fresh.parent().addChild(classification);
        client.markDirty();

        var json = new JsonObject();

        var tree = new JsonObject();
        tree.addProperty("id", fresh.taxonomy().getId()); //$NON-NLS-1$
        tree.addProperty("name", fresh.taxonomy().getName()); //$NON-NLS-1$
        json.add("taxonomy", tree); //$NON-NLS-1$

        var cls = new JsonObject();
        cls.addProperty("id", classification.getId()); //$NON-NLS-1$
        cls.addProperty("name", classification.getName()); //$NON-NLS-1$
        cls.addProperty("parent", fresh.parent().getId()); //$NON-NLS-1$
        json.add("class", cls); //$NON-NLS-1$

        json.add("targetWeight", TaxonomiesHandler.ratio(classification.getWeight())); //$NON-NLS-1$
        json.addProperty("created", true); //$NON-NLS-1$
        json.addProperty("changed", true); //$NON-NLS-1$
        return json;
    }

    /** everything one POST asks for, valid; the parent defaults to the tree's root */
    private record Create(Taxonomy taxonomy, String name, Classification parent, int targetWeight)
    {
    }

    private static Create validateCreate(Client client, String taxonomyParam, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var taxonomy = TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var name = name(body, errors);
        var parent = parent(taxonomy, body, errors);
        var targetWeight = targetWeightOrWhole(body == null ? null : body.get("targetWeight"), errors); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // the same name under the same parent is refused before the prompt: a
        // user is never asked to approve a class the file cannot take. Another
        // parent makes another class, and that pair is ordinary - "growth"
        // under "equity" and "growth" under the root are two classes.
        if (parent.getChildren().stream().anyMatch(existing -> name.equals(existing.getName().trim())))
        {
            var field = new ApiException.FieldError("name", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "class \"" + parent.getName() + "\" already has a class named \"" + name //$NON-NLS-1$ //$NON-NLS-2$
                                            + "\" - see pp_list_taxonomies"); //$NON-NLS-1$
            throw ApiException.conflict("name-taken", "name already in use", field.message(), List.of(field)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return new Create(taxonomy, name, parent, targetWeight);
    }

    private static String name(JsonObject body, List<ApiException.FieldError> errors)
    {
        var element = body == null ? null : body.get("name"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
        {
            errors.add(new ApiException.FieldError("name", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "name is the one thing a class that does not exist yet must be given")); //$NON-NLS-1$
            return null;
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            errors.add(new ApiException.FieldError("name", "invalid-type", "name must be a string")); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        var trimmed = element.getAsString().trim();
        if (trimmed.isEmpty())
        {
            errors.add(new ApiException.FieldError("name", "required", "name must not be blank")); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        return trimmed;
    }

    /** the class the new one hangs under; the tree's root when the caller names none */
    private static Classification parent(Taxonomy taxonomy, JsonObject body, List<ApiException.FieldError> errors)
    {
        if (taxonomy == null)
            return null;

        var element = body == null ? null : body.get("parent"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
            return taxonomy.getRoot();

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            errors.add(new ApiException.FieldError("parent", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "parent must be the id of one class of this taxonomy, as a string")); //$NON-NLS-1$
            return null;
        }

        var found = taxonomy.getClassificationById(element.getAsString().trim());
        if (found == null)
        {
            errors.add(new ApiException.FieldError("parent", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "no class of this taxonomy has this id - see pp_list_taxonomies")); //$NON-NLS-1$
        }
        return found;
    }

    /** the target weight, whole when the request does not name one - as a fresh class of the application starts */
    private static int targetWeightOrWhole(JsonElement element, List<ApiException.FieldError> errors)
    {
        if (element == null || element.isJsonNull())
            return Classification.ONE_HUNDRED_PERCENT;

        return targetWeight(element, errors);
    }

    /**
     * by its own factor - {@link Classification#ONE_HUNDRED_PERCENT} is a whole.
     * More than a whole is stored as given: one tree's classes need not add up
     * to a hundred percent, and the application stores what its dialog was
     * shown. Negative is refused: files carry those from a version whose dialog
     * still accepted a minus, while the current dialog does not.
     */
    private static int targetWeight(JsonElement element, List<ApiException.FieldError> errors)
    {
        if (element == null || element.isJsonNull())
        {
            errors.add(new ApiException.FieldError("targetWeight", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "targetWeight is the only field this patch takes, so it must be given")); //$NON-NLS-1$
            return 0;
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
        {
            errors.add(new ApiException.FieldError("targetWeight", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "targetWeight must be a number, as a fraction: 0.6 is the 60 % the application shows")); //$NON-NLS-1$
            return 0;
        }

        double fraction = element.getAsDouble();
        if (!Double.isFinite(fraction) || fraction < 0)
        {
            errors.add(new ApiException.FieldError("targetWeight", "out-of-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "targetWeight must be 0 or more, as a fraction; more than 1 is stored as given")); //$NON-NLS-1$
            return 0;
        }

        return (int) Math.round(fraction * Classification.ONE_HUNDRED_PERCENT);
    }
}
