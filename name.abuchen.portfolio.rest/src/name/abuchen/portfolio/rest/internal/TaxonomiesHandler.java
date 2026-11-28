package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Taxonomies, read. Two shapes in one answer: the taxonomies themselves as
 * {@code items}, always all of them and always small, and their classes as one
 * flat {@code classes} list that can be paged - because a file of any age has
 * more classes than fit in one answer. A real one here held a single class with
 * several hundred vehicles: nested in its taxonomy, that class is one object
 * nobody can receive whole, so the trees are flattened and the classes are what
 * the cursor walks.
 *
 * <p/>
 * Every class row names the {@code taxonomy} it belongs to, so a page of classes
 * can still be read on its own, and arrives in the order the application shows
 * it: a parent before its children, children by rank and then by name.
 */
public final class TaxonomiesHandler
{
    /** one class, with what a flat list needs to know about its place */
    private record Node(Taxonomy taxonomy, int taxIndex, Classification node, String parent, List<String> path,
                    String rankPath)
    {
        /**
         * The order a page is cut by: the taxonomies in the order the file lists
         * them, then the rank path inside one, then the id. Sorting the pages by
         * it is therefore the order of the trees on screen, and not the order of
         * their ids - which a uuid of a template taxonomy would not be.
         */
        String key()
        {
            var tail = "|" + rankPath + "|" + node.getId(); //$NON-NLS-1$ //$NON-NLS-2$
            return pad(taxIndex) + ":" + taxonomy.getId() + tail; //$NON-NLS-1$
        }
    }

    private TaxonomiesHandler()
    {
    }

    /**
     * @param taxonomyParam
     *            the id of one taxonomy, to read one tree instead of all of them
     * @param classParam
     *            the id of one class, to read one class instead of a whole tree -
     *            the way to ask for the vehicles of a single class without paging
     *            through every other class to reach it
     * @param withAssignmentsParam
     *            whether every class carries the vehicles assigned to it; by
     *            default it only says how many it has, because a single class can
     *            hold hundreds of them
     * @param withPathParam
     *            whether every class also carries the ids of the way down from
     *            the root; off by default, because the parent above already links
     *            the tree and the way costs a uuid per step
     */
    public static JsonObject list(Client client, String taxonomyParam, String classParam,
                    String withAssignmentsParam, String withPathParam, String cursorParam, String limitParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var wanted = taxonomyParam == null ? null : resolve(taxonomyParam, client, errors);
        var only = classParam == null ? null : resolveClass(classParam, wanted, client, errors);
        var withAssignments = flag("withAssignments", withAssignmentsParam, errors); //$NON-NLS-1$
        var withPath = flag("withPath", withPathParam, errors); //$NON-NLS-1$

        // which classes the list holds depends on the taxonomy and on one class,
        // but never on the assignments - so they stay out of the cursor's scope
        // which list the page walks through: the classes, or - narrowed to one
        // class and asked for its vehicles - those vehicles, because they are the
        // many. The scope names which, so a cursor of the one is not taken for
        // the other
        var byVehicle = only != null && withAssignments;
        var tail = taxonomyParam + "|" + classParam; //$NON-NLS-1$
        var scope = (byVehicle ? "vehicles|" : "classes|") + tail; //$NON-NLS-1$ //$NON-NLS-2$
        var paging = Paging.parse(byVehicle ? Paging.Kind.VEHICLES : Paging.Kind.CLASSES, scope, cursorParam,
                        limitParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var json = new JsonObject();

        var headers = new JsonArray();
        var classes = new ArrayList<Node>();
        var taxonomies = client.getTaxonomies();
        for (var index = 0; index < taxonomies.size(); index++)
        {
            var taxonomy = taxonomies.get(index);
            if (wanted != null && !taxonomy.getId().equals(wanted.getId()))
                continue;

            headers.add(header(taxonomy));
            walk(taxonomy, index, taxonomy.getRoot(), null, List.of(), "", classes);
        }
        json.add("items", headers); //$NON-NLS-1$

        // one class asked for by name: the walk stays as it is, because the
        // display order of what remains has to stay the order it would have had
        if (only != null)
            classes.removeIf(entry -> !entry.node().getId().equals(only.getId()));

        // the classes are the array this report pages, and it is not the one
        // called "items": the headers above are those, and they are never paged
        if (byVehicle)
        {
            // one class whole in `classes` - it says how many vehicles there are
            // in all - and its vehicles as the paged list, because a single class
            // of a working file holds more of them than one answer carries
            var rows = new JsonArray();
            var vehicles = new ArrayList<Classification.Assignment>();
            if (!classes.isEmpty())
            {
                var entry = classes.get(0);
                rows.add(classJson(entry, false, withPath));
                vehicles.addAll(entry.node().getAssignments());
            }
            json.add("classes", rows); //$NON-NLS-1$

            paging.apply(Paging.Kind.VEHICLES, json, "assignments", vehicles, //$NON-NLS-1$
                            TaxonomiesHandler::vehicleKey, TaxonomiesHandler::assignmentJson);
        }
        else
        {
            paging.apply(Paging.Kind.CLASSES, json, "classes", classes, Node::key, //$NON-NLS-1$
                            node -> classJson(node, withAssignments, withPath));
        }

        return json;
    }

    private static JsonObject header(Taxonomy taxonomy)
    {
        var json = new JsonObject();
        json.addProperty("id", taxonomy.getId()); //$NON-NLS-1$
        json.addProperty("name", taxonomy.getName()); //$NON-NLS-1$

        // a taxonomy imported from a template says where it came from; a local
        // one says nothing, and silence is the answer
        var source = taxonomy.getSource();
        if (source != null && !source.isBlank())
            json.addProperty("source", source); //$NON-NLS-1$

        // the field is not initialized: a taxonomy that defines no dimensions
        // answers with null, not with an empty list
        var dimensions = taxonomy.getDimensions();
        if (dimensions != null && !dimensions.isEmpty())
        {
            var array = new JsonArray();
            dimensions.forEach(array::add);
            json.add("dimensions", array); //$NON-NLS-1$
        }

        json.addProperty("classCount", count(taxonomy.getRoot())); //$NON-NLS-1$
        return json;
    }

    /** pre-order, so the flat list reads as the tree is displayed */
    private static void walk(Taxonomy taxonomy, int taxIndex, Classification node, String parent, List<String> path,
                    String rankPath, List<Node> into)
    {
        if (node == null)
            return;

        // the separator has to sort *after* the '|' that closes the rank path, or
        // a child's deeper path would sort in front of its own parent
        var here = rankPath.isEmpty() ? pad(node.getRank()) : rankPath + "}" + pad(node.getRank()); //$NON-NLS-1$
        var down = path.isEmpty() ? List.of(node.getId()) : concat(path, node.getId());
        into.add(new Node(taxonomy, taxIndex, node, parent, down, here));

        ordered(node).forEach(child -> walk(taxonomy, taxIndex, child, node.getId(), down, here, into));
    }

    private static JsonObject classJson(Node entry, boolean withAssignments, boolean withPath)
    {
        var taxonomy = entry.taxonomy();
        var node = entry.node();

        var json = new JsonObject();
        json.addProperty("taxonomy", taxonomy.getId()); //$NON-NLS-1$
        json.addProperty("id", node.getId()); //$NON-NLS-1$
        json.addProperty("name", node.getName()); //$NON-NLS-1$
        if (entry.parent() != null)
            json.addProperty("parent", entry.parent()); //$NON-NLS-1$

        // opt-in: a reader holding a page of classes can rebuild the tree from
        // `parent` alone, while this column carries one uuid for every step down
        // from the root - about as much as everything else in the row together
        if (withPath)
        {
            var path = new JsonArray();
            entry.path().forEach(path::add);
            json.add("path", path); //$NON-NLS-1$
        }

        json.addProperty("rank", node.getRank()); //$NON-NLS-1$
        json.add("targetWeight", ratio(node.getWeight())); //$NON-NLS-1$

        var note = node.getNote();
        if (note != null && !note.isBlank())
            json.addProperty("note", note); //$NON-NLS-1$

        var assignments = node.getAssignments();
        json.addProperty("assignedCount", assignments.size()); //$NON-NLS-1$

        // off by default: one class of a working file holds several hundred
        // vehicles, which is more than one answer, let alone one page of classes
        if (withAssignments && !assignments.isEmpty())
        {
            var array = new JsonArray();
            for (var assignment : assignments)
                array.add(assignmentJson(assignment));
            json.add("assignments", array); //$NON-NLS-1$
        }

        return json;
    }

    /**
     * One vehicle of a class: what it is, of what kind, where it sits in the
     * class and the factor the class applies to its value.
     */
    private static JsonObject assignmentJson(Classification.Assignment assignment)
    {
        var vehicle = assignment.getInvestmentVehicle();

        var json = new JsonObject();
        json.addProperty("uuid", vehicle.getUUID()); //$NON-NLS-1$
        json.addProperty("name", vehicle.getName()); //$NON-NLS-1$
        json.addProperty("type", kindOf(vehicle)); //$NON-NLS-1$
        // the page is ordered by this and continues a cursor by it, so a caller
        // that sees a row should be able to see its place too
        json.addProperty("rank", assignment.getRank()); //$NON-NLS-1$
        json.add("weight", ratio(assignment.getWeight())); //$NON-NLS-1$

        return json;
    }

    /**
     * The order of one class's vehicles as the application shows them: rank
     * first, so a page reads in the order of the table and not by identifier,
     * and the uuid behind it because a rank alone is not the key of one row.
     * Formatted with no locale in mind: these keys travel inside a cursor, where
     * digits must stay the ten ASCII digits the cursor is checked against.
     */
    private static String vehicleKey(Classification.Assignment assignment)
    {
        return String.format(Locale.ROOT, "%03d|%s", assignment.getRank(), //$NON-NLS-1$
                        assignment.getInvestmentVehicle().getUUID());
    }

    private static List<Classification> ordered(Classification node)
    {
        return node.getChildren().stream() //
                        .sorted(Comparator.comparingInt(Classification::getRank) //
                                        .thenComparing(Classification::getName)) //
                        .toList();
    }

    private static int count(Classification node)
    {
        if (node == null)
            return 0;

        var total = 1;
        for (var child : node.getChildren())
            total += count(child);

        return total;
    }

    private static List<String> concat(List<String> path, String id)
    {
        var longer = new ArrayList<String>(path.size() + 1);
        longer.addAll(path);
        longer.add(id);
        return List.copyOf(longer);
    }

    /**
     * The rank as three digits, so that sorting the rank paths textually is the
     * order the classes are shown in. A branch with more than a thousand children
     * would sort wrong; none exists, and it would not be readable either.
     */
    private static String pad(int rank)
    {
        var digits = Integer.toString(Math.max(rank, 0));
        return switch (digits.length())
        {
            case 1 -> "00" + digits; //$NON-NLS-1$
            case 2 -> "0" + digits; //$NON-NLS-1$
            default -> digits;
        };
    }

    /**
     * A weight of the model as a fraction. The model holds a percent multiplied
     * by its own factor - {@link Classification#ONE_HUNDRED_PERCENT} is a whole
     * - while every ratio of this API is a fraction, as a holding's
     * {@code weight} already is: the 60 % the application shows travels as
     * {@code 0.6}. Not private: the assignment writes report the share the same
     * way the read side does.
     */
    static JsonElement ratio(int weight)
    {
        return EntityJson.decimal((double) weight / Classification.ONE_HUNDRED_PERCENT);
    }

    /**
     * which kind of vehicle the class holds, in the words the rest of this API
     * uses for them. Not private: an assignment write names the vehicle it placed
     * in the same words.
     */
    static String kindOf(InvestmentVehicle vehicle)
    {
        if (vehicle instanceof Security)
            return "instrument"; //$NON-NLS-1$
        if (vehicle instanceof Account)
            return "cash-account"; //$NON-NLS-1$
        // the model assigns instruments and cash accounts; the fallback keeps an
        // kind this method never heard of from being named after one it did
        return "vehicle"; //$NON-NLS-1$
    }

    /** one taxonomy, by id; shared with the class report, which needs the tree a class belongs to */
    static Taxonomy resolve(String raw, Client client, List<ApiException.FieldError> errors)
    {
        var trimmed = raw.trim();
        for (var taxonomy : client.getTaxonomies())
        {
            if (taxonomy.getId().equals(trimmed))
                return taxonomy;
        }

        errors.add(new ApiException.FieldError("taxonomy", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "no taxonomy of this file has this id - see the items of pp_list_taxonomies")); //$NON-NLS-1$
        return null;
    }

    /**
     * The one class the caller is after. A class id is unique inside a taxonomy
     * and nothing else, so without a taxonomy it is looked for in all of them -
     * and an id that several of them define is refused rather than picked by a
     * guess.
     */
    static Classification resolveClass(String raw, Taxonomy taxonomy, Client client,
                    List<ApiException.FieldError> errors)
    {
        var trimmed = raw.trim();

        if (taxonomy != null)
        {
            var inside = taxonomy.getClassificationById(trimmed);
            if (inside == null)
            {
                errors.add(new ApiException.FieldError("class", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "no class of this taxonomy has this id - see pp_list_taxonomies")); //$NON-NLS-1$
            }
            return inside;
        }

        Classification found = null;
        for (var candidate : client.getTaxonomies())
        {
            var match = candidate.getClassificationById(trimmed);
            if (match == null)
                continue;

            if (found != null)
            {
                errors.add(new ApiException.FieldError("class", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "this id belongs to more than one taxonomy - name the taxonomy as well"));
                return null;
            }
            found = match;
        }

        if (found == null)
        {
            errors.add(new ApiException.FieldError("class", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "no class of any taxonomy has this id - see pp_list_taxonomies")); //$NON-NLS-1$
        }
        return found;
    }

    private static boolean flag(String field, String raw, List<ApiException.FieldError> errors)
    {
        if (raw == null || raw.isBlank())
            return false;
        if (raw.equalsIgnoreCase("true")) //$NON-NLS-1$
            return true;
        if (raw.equalsIgnoreCase("false")) //$NON-NLS-1$
            return false;

        errors.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        field + " takes true or false")); //$NON-NLS-1$
        return false;
    }
}
