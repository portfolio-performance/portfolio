package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.Messages;

public final class TaxonomiesHandler
{
    private static final String KIND_TAXONOMY = "taxonomy"; //$NON-NLS-1$
    private static final String KIND_CLASSIFICATION = "classification"; //$NON-NLS-1$

    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}"); //$NON-NLS-1$
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private TaxonomiesHandler()
    {
    }

    /**
     * The taxonomies the file defines, with their category trees.
     * <p>
     * This is the key to the {@code classifications} object on a holding, which is
     * keyed by taxonomy id and names its categories by path: without this list a
     * client has an id it cannot label and no way to know which categories exist but
     * hold nothing. The tree is what lets it show an empty category as empty rather
     * than not at all.
     */
    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getTaxonomies(), EntityJson::toJson);
    }

    public static JsonElement get(Client client, String id)
    {
        return EntityJson.toJson(find(client, id));
    }

    public static JsonElement getClassification(Client client, String id, String classificationId)
    {
        var taxonomy = find(client, id);
        return EntityJson.toJson(taxonomy, findClassification(taxonomy, classificationId));
    }

    // taxonomies

    /**
     * Creates an empty taxonomy {@code {name}} with its root category, as the
     * application's "new taxonomy" does without a template. Must be called on
     * the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    JsonObject body)
    {
        var client = context.client();

        var existing = idempotency.findEntity(context.file().getPath(), KIND_TAXONOMY, context.clientRef())
                        .map(client::getTaxonomy);
        if (existing.isPresent())
            return MasterDataWrites.replayed(EntityJson.toJson(existing.get()), context.dryRun());

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);
        var name = json.requireString("name"); //$NON-NLS-1$
        json.rejectUnknownFields();
        json.throwIfErrors();

        var taxonomy = new Taxonomy(name);
        taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(taxonomy));

        client.addTaxonomy(taxonomy);
        client.markDirty();
        idempotency.rememberEntity(context.file().getPath(), KIND_TAXONOMY, context.clientRef(), taxonomy.getId());

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND_TAXONOMY, name, context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(taxonomy), true);
    }

    /** renames the taxonomy ({@code {name}}), as the application's "rename" does */
    public static MasterDataWrites.WriteResult rename(WriteContext context, String id, JsonObject body)
    {
        var client = context.client();
        var taxonomy = find(client, id);

        var json = new Json(body);
        var name = json.has("name") ? json.requireString("name") : null; //$NON-NLS-1$ //$NON-NLS-2$
        json.rejectUnknownFields();
        json.throwIfErrors();

        var before = EntityJson.toJson(taxonomy);
        if (name == null || name.equals(taxonomy.getName()))
            return context.dryRun() ? MasterDataWrites.dryRun(before) : new MasterDataWrites.WriteResult(before, false);

        var after = EntityJson.toJson(taxonomy);
        after.addProperty("name", name); //$NON-NLS-1$
        if (context.dryRun())
            return MasterDataWrites.dryRun(after);

        var oldName = taxonomy.getName();
        taxonomy.setName(name);
        client.markDirty();

        ChangeLog.recordChanges(List.of(new ChangeLog.Change("name", oldName, name)), Messages.MsgApiEntityChanged, //$NON-NLS-1$
                        KIND_TAXONOMY, oldName, context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(taxonomy), true);
    }

    /**
     * Deletes the taxonomy with all its categories and assignments, as the
     * application does; the instruments and accounts are not affected. A dry
     * run answers the taxonomy that would be removed; a real delete answers
     * null. Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String id)
    {
        var client = context.client();
        var taxonomy = find(client, id);

        if (context.dryRun())
            return MasterDataWrites.deletePreview(EntityJson.toJson(taxonomy));

        client.removeTaxonomy(taxonomy);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiEntityDeleted, KIND_TAXONOMY, taxonomy.getName(),
                        context.file().getLabel());
        return null;
    }

    // classifications

    /**
     * Creates a category {@code {parent, name, color, weight, note}} below
     * {@code parent} (a category id; absent or null: a top-level category).
     * As in the application, the category is created with the parent
     * constructor and added to the parent's children; its weight defaults to
     * what is left of 100 % among its siblings. Must be called on the UI
     * thread.
     */
    public static MasterDataWrites.WriteResult createClassification(WriteContext context,
                    IdempotencyIndex idempotency, String id, JsonObject body)
    {
        var client = context.client();
        var taxonomy = find(client, id);

        var existing = idempotency.findEntity(context.file().getPath(), KIND_CLASSIFICATION, context.clientRef())
                        .map(taxonomy::getClassificationById);
        if (existing.isPresent())
            return MasterDataWrites.replayed(EntityJson.toJson(taxonomy, existing.get()), context.dryRun());

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);

        var parent = taxonomy.getRoot();
        if (json.has("parent") && !json.isNull("parent")) //$NON-NLS-1$ //$NON-NLS-2$
            parent = parseParent(json, taxonomy, null);

        var name = json.requireString("name"); //$NON-NLS-1$
        var color = parseColor(json);
        var weight = json.has("weight") ? parseWeight(json, "weight") : null; //$NON-NLS-1$ //$NON-NLS-2$
        var note = json.optString("note"); //$NON-NLS-1$
        json.rejectUnknownFields();
        json.throwIfErrors();

        var classification = new Classification(parent, UUID.randomUUID().toString(), name, color);
        classification.setWeight(weight != null ? weight
                        : Math.max(0, Classification.ONE_HUNDRED_PERCENT - parent.getChildrenWeight()));
        classification.setNote(note);
        classification.setRank(nextRank(parent));

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(taxonomy, classification));

        parent.addChild(classification);
        client.markDirty();
        idempotency.rememberEntity(context.file().getPath(), KIND_CLASSIFICATION, context.clientRef(),
                        classification.getId());

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND_CLASSIFICATION, name, context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(taxonomy, classification), true);
    }

    /**
     * Applies a JSON Merge Patch to a category: {@code name}, {@code color},
     * {@code weight}, {@code note}, and {@code parent} to move it (null: to
     * the top level). A category cannot be moved below itself. A patch that
     * changes nothing does not mark the file dirty. Must be called on the UI
     * thread.
     */
    public static MasterDataWrites.WriteResult patchClassification(WriteContext context, String id,
                    String classificationId, JsonObject body)
    {
        var client = context.client();
        var taxonomy = find(client, id);
        var classification = findClassification(taxonomy, classificationId);

        var json = new Json(body);
        var setters = new ArrayList<Consumer<Classification>>();
        Classification newParent = null;

        if (json.has("parent")) //$NON-NLS-1$
            newParent = json.isNull("parent") ? taxonomy.getRoot() : parseParent(json, taxonomy, classification); //$NON-NLS-1$
        if (json.has("name")) //$NON-NLS-1$
        {
            var name = json.requireString("name"); //$NON-NLS-1$
            setters.add(c -> c.setName(name));
        }
        if (json.has("color")) //$NON-NLS-1$
        {
            var color = parseColor(json);
            if (json.isNull("color")) //$NON-NLS-1$
                json.add(new ApiException.FieldError("color", "required", "color cannot be cleared")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            setters.add(c -> c.setColor(color));
        }
        if (json.has("weight")) //$NON-NLS-1$
        {
            var weight = parseWeight(json, "weight"); //$NON-NLS-1$
            if (json.isNull("weight")) //$NON-NLS-1$
                json.add(new ApiException.FieldError("weight", "required", "weight cannot be cleared")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            setters.add(c -> c.setWeight(weight != null ? weight : 0));
        }
        if (json.has("note")) //$NON-NLS-1$
        {
            var note = json.optString("note"); //$NON-NLS-1$
            setters.add(c -> c.setNote(note));
        }
        json.rejectUnknownFields();
        json.throwIfErrors();

        var parent = newParent != null ? newParent : classification.getParent();
        var moves = parent != classification.getParent();

        var preview = new Classification(parent, classification.getId(), classification.getName(),
                        classification.getColor());
        preview.setColor(classification.getColor()); // the constructor picks a random color for null
        preview.setWeight(classification.getWeight());
        preview.setNote(classification.getNote());
        preview.getChildren().addAll(classification.getChildren());
        classification.getAssignments().forEach(preview::addAssignment);
        setters.forEach(setter -> setter.accept(preview));

        var before = EntityJson.toJson(taxonomy, classification);
        var after = EntityJson.toJson(taxonomy, preview);

        if (context.dryRun())
            return MasterDataWrites.dryRun(after);

        var changes = MasterDataWrites.diff(before, after);
        if (changes.isEmpty())
            return new MasterDataWrites.WriteResult(before, false);

        var oldName = classification.getName();
        setters.forEach(setter -> setter.accept(classification));
        if (moves)
        {
            classification.getParent().getChildren().remove(classification);
            classification.setParent(parent);
            classification.setRank(nextRank(parent));
            parent.addChild(classification);
        }
        client.markDirty();

        ChangeLog.recordChanges(changes, Messages.MsgApiEntityChanged, KIND_CLASSIFICATION, oldName,
                        context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(taxonomy, classification), true);
    }

    /**
     * Deletes a category. A category with subcategories or assignments is
     * only deleted with {@code cascade}: its whole subtree goes, and its
     * instruments and accounts become unassigned - as a delete in the
     * application does. A dry run answers what would be removed; a real
     * delete answers null. Must be called on the UI thread.
     */
    public static JsonObject deleteClassification(WriteContext context, String id, String classificationId,
                    String cascadeParam)
    {
        var client = context.client();
        var taxonomy = find(client, id);
        var classification = findClassification(taxonomy, classificationId);

        var cascade = parseCascade(cascadeParam);
        if (!cascade)
        {
            var errors = new ArrayList<ApiException.FieldError>();
            if (!classification.getChildren().isEmpty())
                errors.add(new ApiException.FieldError("children", "referenced", "the category has subcategories")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (!classification.getAssignments().isEmpty())
                errors.add(new ApiException.FieldError("assignments", "referenced", "the category has assignments")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (!errors.isEmpty())
                throw ApiException.conflict("delete-blocked", //$NON-NLS-1$
                                "Category is not empty; delete it with cascade=true", null, errors); //$NON-NLS-1$
        }

        if (context.dryRun())
            return MasterDataWrites.deletePreview(EntityJson.toJson(taxonomy, classification));

        classification.getParent().getChildren().remove(classification);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiEntityDeleted, KIND_CLASSIFICATION, classification.getName(),
                        context.file().getLabel());
        return null;
    }

    // assignments

    /**
     * Assigns an instrument or a cash account to a category with a weight in
     * percent ({@code {weight}}, default: what is not yet assigned elsewhere in
     * the taxonomy), or changes the weight of an existing assignment. As in
     * the application, the weights of one vehicle across a taxonomy must not
     * exceed 100 %. Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult putAssignment(WriteContext context, String id,
                    String classificationId, String vehicleUuid, JsonObject body)
    {
        var client = context.client();
        var taxonomy = find(client, id);
        var classification = findClassification(taxonomy, classificationId);
        var vehicle = findVehicle(client, vehicleUuid);

        var json = new Json(body);
        var weight = json.has("weight") && !json.isNull("weight") ? parseWeight(json, "weight") : null; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        json.rejectUnknownFields();
        json.throwIfErrors();

        var existing = classification.getAssignments().stream().filter(a -> a.getInvestmentVehicle() == vehicle)
                        .findFirst().orElse(null);

        var assignedElsewhere = assignments(taxonomy.getRoot()) //
                        .filter(a -> a.getInvestmentVehicle() == vehicle && a != existing) //
                        .mapToInt(Assignment::getWeight).sum();
        var available = Classification.ONE_HUNDRED_PERCENT - assignedElsewhere;

        var newWeight = weight != null ? weight.intValue() : available;
        if (weight != null && newWeight <= 0)
            json.add(new ApiException.FieldError("weight", "must-be-positive", "weight must be positive")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        else if (newWeight > available || newWeight <= 0)
            json.add(new ApiException.FieldError("weight", "weight-exceeds-100", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("the weights of {0} in this taxonomy would exceed 100 % ({1} % are assigned elsewhere)", //$NON-NLS-1$
                                            vehicle.getName(), Values.Weight.format(assignedElsewhere))));
        json.throwIfErrors();

        var preview = new Assignment(vehicle, newWeight);

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(preview));

        if (existing != null && existing.getWeight() == newWeight)
            return new MasterDataWrites.WriteResult(EntityJson.toJson(existing), false);

        if (existing != null)
        {
            var before = existing.getWeight();
            existing.setWeight(newWeight);
            client.markDirty();
            ChangeLog.recordChanges(List.of(new ChangeLog.Change(vehicle.getName(), Values.Weight.format(before),
                            Values.Weight.format(newWeight))), Messages.MsgApiEntityChanged, KIND_CLASSIFICATION,
                            classification.getName(), context.file().getLabel());
            return new MasterDataWrites.WriteResult(EntityJson.toJson(existing), true);
        }

        preview.setRank(nextRank(classification));
        classification.addAssignment(preview);
        client.markDirty();

        ChangeLog.recordChanges(List.of(new ChangeLog.Change(vehicle.getName(), null, Values.Weight.format(newWeight))),
                        Messages.MsgApiEntityChanged, KIND_CLASSIFICATION, classification.getName(),
                        context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(preview), true);
    }

    /**
     * Removes the assignment of the vehicle from the category; 404 if there
     * is none. A dry run answers the assignment that would be removed; a real
     * delete answers null. Must be called on the UI thread.
     */
    public static JsonObject deleteAssignment(WriteContext context, String id, String classificationId,
                    String vehicleUuid)
    {
        var client = context.client();
        var taxonomy = find(client, id);
        var classification = findClassification(taxonomy, classificationId);

        var assignment = classification.getAssignments().stream()
                        .filter(a -> a.getInvestmentVehicle().getUUID().equals(vehicleUuid)).findFirst()
                        .orElseThrow(ApiException::notFound);

        if (context.dryRun())
            return MasterDataWrites.deletePreview(EntityJson.toJson(assignment));

        classification.removeAssignment(assignment);
        client.markDirty();

        var vehicle = assignment.getInvestmentVehicle();
        ChangeLog.recordChanges(List.of(new ChangeLog.Change(vehicle.getName(),
                        Values.Weight.format(assignment.getWeight()), null)), Messages.MsgApiEntityChanged,
                        KIND_CLASSIFICATION, classification.getName(), context.file().getLabel());
        return null;
    }

    // helpers

    /* package */ static Taxonomy find(Client client, String id)
    {
        var taxonomy = client.getTaxonomy(id);
        if (taxonomy == null)
            throw ApiException.notFound();
        return taxonomy;
    }

    /** a category of the taxonomy; the root, which names the taxonomy itself, is not addressable */
    private static Classification findClassification(Taxonomy taxonomy, String classificationId)
    {
        var classification = taxonomy.getClassificationById(classificationId);
        if (classification == null)
            throw ApiException.notFound();
        return classification;
    }

    private static InvestmentVehicle findVehicle(Client client, String uuid)
    {
        return Stream.<InvestmentVehicle>concat(client.getSecurities().stream(), client.getAccounts().stream())
                        .filter(v -> v.getUUID().equals(uuid)).findFirst().orElseThrow(ApiException::notFound);
    }

    /**
     * The new parent category of the {@code parent} field; for a move of
     * {@code moving}, it must not be the category itself or below it.
     */
    private static Classification parseParent(Json json, Taxonomy taxonomy, Classification moving)
    {
        var parentId = json.optString("parent"); //$NON-NLS-1$
        if (parentId == null)
            return taxonomy.getRoot();

        var parent = taxonomy.getClassificationById(parentId);
        if (parent == null)
        {
            json.add(new ApiException.FieldError("parent", "unknown-reference", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("no category with id {0} in this taxonomy", parentId))); //$NON-NLS-1$
            return taxonomy.getRoot();
        }

        if (moving != null && parent.getPathToRoot().contains(moving))
        {
            json.add(new ApiException.FieldError("parent", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "a category cannot be moved below itself")); //$NON-NLS-1$
            return moving.getParent();
        }

        return parent;
    }

    private static String parseColor(Json json)
    {
        var color = json.optString("color"); //$NON-NLS-1$
        if (color != null && !COLOR.matcher(color).matches())
        {
            json.add(new ApiException.FieldError("color", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("color must be #RRGGBB, got {0}", color))); //$NON-NLS-1$
            return null;
        }
        return color;
    }

    /** a weight in percent, 0 to 100 with at most 2 decimals, in the model's unit (100 % = 10000) */
    private static Integer parseWeight(Json json, String field)
    {
        var value = json.decimal(field, Values.Weight.precision());
        if (value == null)
            return null;

        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0)
        {
            json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} must be a percentage between 0 and 100", field))); //$NON-NLS-1$
            return null;
        }

        return value.movePointRight(Values.Weight.precision()).intValueExact();
    }

    private static boolean parseCascade(String cascade)
    {
        if (cascade == null || "false".equals(cascade)) //$NON-NLS-1$
            return false;
        if ("true".equals(cascade)) //$NON-NLS-1$
            return true;
        throw ApiException.badRequest(List.of(new ApiException.FieldError("cascade", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "cascade must be true or false"))); //$NON-NLS-1$
    }

    /** the rank after the last child or assignment, as the application appends */
    private static int nextRank(Classification parent)
    {
        return Stream.concat(parent.getChildren().stream().map(Classification::getRank),
                        parent.getAssignments().stream().map(Assignment::getRank))
                        .mapToInt(Integer::intValue).max().orElse(-1) + 1;
    }

    /** every assignment in the subtree */
    private static Stream<Assignment> assignments(Classification classification)
    {
        return Stream.concat(classification.getAssignments().stream(),
                        classification.getChildren().stream().flatMap(TaxonomiesHandler::assignments));
    }
}
