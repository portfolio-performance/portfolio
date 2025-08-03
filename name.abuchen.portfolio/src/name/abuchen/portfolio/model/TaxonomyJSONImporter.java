package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.money.Values;

public class TaxonomyJSONImporter
{
    public enum Operation
    {
        CREATE, UPDATE, DELETE, SKIPPED, ERROR
    }

    public static class ChangeEntry
    {
        private final Class<?> target;
        private final Operation operation;
        private final String comment;

        public ChangeEntry(Class<?> target, Operation operation, String comment)
        {
            this.target = target;
            this.operation = operation;
            this.comment = comment;
        }

        public Class<?> getTarget()
        {
            return target;
        }

        public Operation getOperation()
        {
            return operation;
        }

        public String getComment()
        {
            return comment;
        }
    }

    public static class ImportResult
    {
        private final List<ChangeEntry> changes = new ArrayList<>();
        private final Set<Object> createdObjects = new HashSet<>();
        private final Set<Object> modifiedObjects = new HashSet<>();

        /**
         * Keeps track of newly created classifications by their key.
         */
        private final Map<String, Classification> newKeys = new HashMap<>();

        public List<ChangeEntry> getChanges()
        {
            return changes;
        }

        private void addChange(ChangeEntry entry)
        {
            changes.add(entry);
        }

        private void addCreatedObject(Object object)
        {
            createdObjects.add(object);
        }

        private void addModifiedObject(Object object)
        {
            modifiedObjects.add(object);
        }

        public int getCreatedObjects()
        {
            return createdObjects.size();
        }

        public int getModifiedObjects()
        {
            return modifiedObjects.size();
        }

        public boolean hasChanges()
        {
            return changes.stream().anyMatch(e -> e.operation == Operation.CREATE //
                            || e.operation == Operation.UPDATE //
                            || e.operation == Operation.DELETE);
        }

        public boolean isCreated(Object object)
        {
            return createdObjects.contains(object);
        }

        public boolean isModified(Object object)
        {
            return modifiedObjects.contains(object);
        }
    }

    private final Client client;
    private final Taxonomy taxonomy;
    private final boolean preserveNameAndDescription;

    private final Map<String, Classification> key2classification;

    public TaxonomyJSONImporter(Client client, Taxonomy taxonomy)
    {
        this(client, taxonomy, false);
    }

    public TaxonomyJSONImporter(Client client, Taxonomy taxonomy, boolean preserveNameAndDescription)
    {
        this.client = client;
        this.taxonomy = taxonomy;
        this.preserveNameAndDescription = preserveNameAndDescription;

        var keys = new HashMap<String, Classification>();
        this.taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification)
            {
                var key = classification.getKey();
                if (!key.isEmpty())
                    keys.put(key, classification);
            }
        });

        this.key2classification = Collections.unmodifiableMap(keys);
    }

    @SuppressWarnings("unchecked")
    public ImportResult importTaxonomy(Reader reader) throws IOException
    {
        try
        {
            var result = new ImportResult();

            Gson gson = new Gson();
            Map<String, Object> jsonData = gson.fromJson(reader, new TypeToken<Map<String, Object>>()
            {
            }.getType());

            if (jsonData == null)
                throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, "null")); //$NON-NLS-1$

            var name = (String) jsonData.get("name"); //$NON-NLS-1$
            updateNameIfNeeded(taxonomy.getRoot(), name, result);

            var color = (String) jsonData.get("color"); //$NON-NLS-1$
            updateColorIfNeeded(taxonomy.getRoot(), color, result);

            var categories = (List<Map<String, Object>>) jsonData.get("categories"); //$NON-NLS-1$
            var instruments = (List<Map<String, Object>>) jsonData.get("instruments"); //$NON-NLS-1$

            if (categories != null)
            {
                importCategories(categories, taxonomy.getRoot(), result);
            }

            if (instruments != null)
            {
                importInstruments(instruments, result);
            }

            return result;
        }
        catch (ClassCastException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, e.getMessage()), e);
        }
        catch (JsonParseException e)
        {
            if (e.getCause() instanceof MalformedJsonException mfe)
                throw mfe;
            else
                throw new IOException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void importCategories(List<Map<String, Object>> categories, Classification parent, ImportResult result)
    {
        for (var category : categories)
        {
            var name = (String) category.get("name"); //$NON-NLS-1$
            if (name == null || name.trim().isEmpty())
                continue;

            var key = (String) category.get("key"); //$NON-NLS-1$
            var description = (String) category.get("description"); //$NON-NLS-1$
            var color = (String) category.get("color"); //$NON-NLS-1$

            // first use the key to find the category
            var classification = findClassificationByKey(result, key);
            if (classification != null)
            {
                if (!Objects.equals(parent, classification.getParent()))
                {
                    // prevent that parent is moved under itself
                    var path = parent.getPathToRoot();
                    if (path.contains(classification))
                    {
                        result.addChange(new ChangeEntry(Classification.class, Operation.ERROR, MessageFormat.format(
                                        "Cannot move category ''{0}'' under itself", classification.getName())));
                        continue;
                    }

                    result.addModifiedObject(classification);
                    result.addChange(new ChangeEntry(Classification.class, Operation.UPDATE, MessageFormat.format(
                                    "Move category ''{0}'' to ''{1}''", classification.getName(), parent.getName())));

                    classification.getParent().getChildren().remove(classification);
                    parent.addChild(classification);
                    classification.setParent(parent);
                }
            }
            else
            {
                classification = findClassificationByName(parent, name);
            }

            if (classification != null)
            {
                // update properties of existing classification

                updateNameIfNeeded(classification, name, result);

                if (!preserveNameAndDescription && description != null && !description.equals(classification.getNote()))
                {
                    result.addModifiedObject(classification);
                    result.addChange(new ChangeEntry(Classification.class, Operation.UPDATE, MessageFormat
                                    .format("Update description for category ''{0}''", classification.getName())));

                    classification.setNote(description);
                }

                updateColorIfNeeded(classification, color, result);
            }
            else
            {
                // create new classification

                String id = UUID.randomUUID().toString();
                Classification newClassification = new Classification(parent, id, name);

                if (!parent.getChildren().isEmpty())
                {
                    // during dry-run, new classifications might not get a valid
                    // new rank. We ignore this here to keep the code simpler.
                    int topRank = parent.getChildren().stream().mapToInt(Classification::getRank).max().orElse(0);
                    newClassification.setRank(topRank + 1);
                }

                if (key != null && !key.trim().isEmpty())
                {
                    newClassification.setKey(key);

                    // track new classification key (cannot exist; otherwise
                    // findClassificationByKey would have found it)
                    result.newKeys.put(newClassification.getKey(), newClassification);
                }

                if (description != null && !description.trim().isEmpty())
                    newClassification.setNote(description);
                if (color != null && !color.trim().isEmpty())
                    newClassification.setColor(color);

                result.addCreatedObject(newClassification);
                result.addChange(new ChangeEntry(Classification.class, Operation.CREATE,
                                MessageFormat.format("Create new category ''{0}''", getPathString(newClassification))));

                parent.addChild(newClassification);

                classification = newClassification;
            }

            // process children
            var children = (List<Map<String, Object>>) category.get("children"); //$NON-NLS-1$
            if (children != null)
            {
                importCategories(children, classification, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void importInstruments(List<Map<String, Object>> instruments, ImportResult result) throws IOException
    {
        for (var instrument : instruments)
        {
            var identifiers = (Map<String, Object>) instrument.get("identifiers"); //$NON-NLS-1$
            if (identifiers == null)
                continue;

            // find the investment vehicle by identifiers
            var investmentVehicle = findInvestmentVehicle(identifiers);
            if (investmentVehicle == null)
            {
                var name = (String) identifiers.get("name"); //$NON-NLS-1$
                result.addChange(new ChangeEntry(InvestmentVehicle.class, Operation.SKIPPED,
                                MessageFormat.format("Instrument not found: {0}", name != null ? name : "unknown")));
                continue;
            }

            try
            {
                importInstrument(instrument, investmentVehicle, result);
            }
            catch (ClassCastException e)
            {
                // happens if attributes cannot be cast to the expected type
                throw new IOException(MessageFormat.format("Invalid data format for instrument {0}: {1}",
                                investmentVehicle.getName(), e.getMessage()), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void importInstrument(Map<String, Object> instrument, InvestmentVehicle investmentVehicle,
                    ImportResult result)
    {
        // find all existing assignments to track assignments to be removed
        var existingAssignments = new HashMap<Classification, Classification.Assignment>();
        this.taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification, Classification.Assignment assignment)
            {
                if (investmentVehicle.equals(assignment.getInvestmentVehicle()))
                {
                    existingAssignments.put(classification, assignment);
                }
            }
        });

        var categories = (List<Map<String, Object>>) instrument.get("categories"); //$NON-NLS-1$
        if (categories == null)
            return;

        // track total weight to prevent imports with a weight sum > 100%
        var totalWeight = 0;

        for (var category : categories)
        {
            var key = (String) category.get("key"); //$NON-NLS-1$
            var path = (List<String>) category.get("path"); //$NON-NLS-1$
            var weight = (Double) category.get("weight"); //$NON-NLS-1$

            Classification classification = null;

            if (key != null && !key.isEmpty())
            {
                classification = findClassificationByKey(result, key);
            }

            if (classification == null && path != null && !path.isEmpty())
            {
                classification = findClassificationByPath(path);
            }

            if (classification == null)
            {
                result.addChange(new ChangeEntry(Assignment.class, Operation.SKIPPED,
                                MessageFormat.format("Category not found for {0}", investmentVehicle.getName())));

                continue;
            }

            // convert weight from percentage to internal format
            int weightValue = weight == null ? Classification.ONE_HUNDRED_PERCENT
                            : Math.clamp((int) Math.round(weight * Values.Weight.factor()), 0,
                                            Classification.ONE_HUNDRED_PERCENT);

            if (weightValue == 0)
            {
                result.addChange(new ChangeEntry(Assignment.class, Operation.SKIPPED,
                                MessageFormat.format("Skipping assignment with weight {0} for {1} in {2}", weight,
                                                investmentVehicle.getName(), getPathString(classification))));
                continue;
            }

            if (totalWeight + weightValue > Classification.ONE_HUNDRED_PERCENT)
            {
                result.addChange(new ChangeEntry(Assignment.class, Operation.ERROR,
                                MessageFormat.format("Total weight exceeds 100% for {0} in {1}",
                                                investmentVehicle.getName(), getPathString(classification))));
                continue;
            }

            totalWeight += weightValue;

            // check if assignment already exists
            var existingAssignment = existingAssignments.remove(classification);

            if (existingAssignment != null)
            {
                if (existingAssignment.getWeight() != weightValue)
                {
                    result.addModifiedObject(existingAssignment);
                    result.addChange(new ChangeEntry(Assignment.class, Operation.UPDATE,
                                    MessageFormat.format("Updated weight {0} for {1} in {2}",
                                                    Values.WeightPercent.format(weightValue),
                                                    investmentVehicle.getName(), getPathString(classification))));

                    existingAssignment.setWeight(weightValue);
                }
            }
            else
            {
                // Create new assignment
                var newAssignment = new Classification.Assignment(investmentVehicle, weightValue);

                // during dry-run, new assignments might not get a valid
                // new rank because previous assignments have not been
                // added. We ignore this here to keep the code simpler.
                int topRank = classification.getAssignments().stream().mapToInt(Assignment::getRank).max().orElse(0);
                newAssignment.setRank(topRank + 1);

                result.addCreatedObject(newAssignment);
                result.addChange(new ChangeEntry(Assignment.class, Operation.CREATE,
                                MessageFormat.format("Created assignment with weight {0} for {1} in {2}",
                                                Values.WeightPercent.format(weightValue), investmentVehicle.getName(),
                                                getPathString(classification))));

                classification.addAssignment(newAssignment);
            }
        }

        for (var entry : existingAssignments.entrySet())
        {
            result.addChange(new ChangeEntry(Assignment.class, Operation.DELETE,
                            MessageFormat.format("Deleted assignment with weight {0} for {1} in {2}",
                                            Values.WeightPercent.format(entry.getValue().getWeight()),
                                            investmentVehicle.getName(), getPathString(entry.getKey()))));

            entry.getKey().removeAssignment(entry.getValue());
        }
    }

    /**
     * Finds the classification by the given name in the parent.
     */
    private Classification findClassificationByName(Classification parent, String name)
    {
        for (var child : parent.getChildren())
        {
            if (name.equals(child.getName()))
                return child;
        }

        return null;
    }

    /**
     * Finds the classification by the given key - including newly created
     * classifications.
     */
    private Classification findClassificationByKey(ImportResult result, String key)
    {
        if (key == null || key.isEmpty())
            return null;

        var classification = key2classification.get(key);
        if (classification != null)
            return classification;

        return result.newKeys.get(key);
    }

    /**
     * Attempts to find the classification by the given path - including newly
     * created classifications.
     */
    private Classification findClassificationByPath(List<String> path)
    {
        var current = taxonomy.getRoot();

        for (String segment : path)
        {
            var child = findClassificationByName(current, segment);
            if (child == null)
                return null;
            current = child;
        }

        return current;
    }

    private InvestmentVehicle findInvestmentVehicle(Map<String, Object> identifiers)
    {
        var name = (String) identifiers.get("name"); //$NON-NLS-1$
        var isin = (String) identifiers.get("isin"); //$NON-NLS-1$
        var wkn = (String) identifiers.get("wkn"); //$NON-NLS-1$
        var ticker = (String) identifiers.get("ticker"); //$NON-NLS-1$

        if (isin != null && !isin.trim().isEmpty())
        {
            var security = client.getSecurities().stream().filter(s -> isin.equals(s.getIsin())).findFirst();
            if (security.isPresent())
                return security.get();
        }

        if (ticker != null && !ticker.trim().isEmpty())
        {
            var security = client.getSecurities().stream().filter(s -> ticker.equals(s.getTickerSymbol())).findFirst();
            if (security.isPresent())
                return security.get();
        }

        if (wkn != null && !wkn.trim().isEmpty())
        {
            var security = client.getSecurities().stream().filter(s -> wkn.equals(s.getWkn())).findFirst();
            if (security.isPresent())
                return security.get();
        }

        if (name != null && !name.trim().isEmpty())
        {
            var security = client.getSecurities().stream().filter(s -> name.equals(s.getName())).findFirst();
            if (security.isPresent())
                return security.get();

            // also check accounts
            var account = client.getAccounts().stream().filter(a -> name.equals(a.getName())).findFirst();
            if (account.isPresent())
                return account.get();
        }

        return null;
    }

    private void updateNameIfNeeded(Classification classification, String newName, ImportResult result)
    {
        if (!preserveNameAndDescription && newName != null && !newName.equals(classification.getName()))
        {
            result.addModifiedObject(classification);
            result.addChange(new ChangeEntry(Classification.class, Operation.UPDATE, MessageFormat
                            .format("Update name for category ''{0}'' to ''{1}''", classification.getName(), newName)));
            classification.setName(newName);
        }
    }

    private void updateColorIfNeeded(Classification classification, String newColor, ImportResult result)
    {
        if (!preserveNameAndDescription && newColor != null && !newColor.equals(classification.getColor()))
        {
            result.addModifiedObject(classification);
            result.addChange(new ChangeEntry(Classification.class, Operation.UPDATE,
                            MessageFormat.format("Update color for category ''{0}''", classification.getName())));
            classification.setColor(newColor);
        }
    }

    private String getPathString(Classification classification)
    {
        return classification.getPathToRoot().stream().skip(1).map(Classification::getName)
                        .collect(Collectors.joining(" / ")); //$NON-NLS-1$
    }
}
