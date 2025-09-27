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
import name.abuchen.portfolio.util.Pair;

public class TaxonomyJSONImporter
{
    public enum Operation
    {
        CREATE, UPDATE, DELETE, SKIPPED, WARNING, ERROR
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

    /**
     * Result of the import operation.
     */
    public static class ImportResult
    {
        private final List<ChangeEntry> changes = new ArrayList<>();
        private final Set<Object> createdObjects = new HashSet<>();
        private final Set<Object> modifiedObjects = new HashSet<>();

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

    /**
     * Tracks newly created keys and processed items.
     */
    private static class ImportProcessingState
    {
        /**
         * Keeps track of newly created classifications by their key.
         */
        private final Map<String, Classification> newKeys = new HashMap<>();

        private final Set<Classification> processedCategories = new HashSet<>();
        private final Set<InvestmentVehicle> processedInstruments = new HashSet<>();

        /**
         * Keeps track of processed categories in the order they were processed.
         */
        private final List<Classification> processedCategoriesInOrder = new ArrayList<>();
    }

    private final Client client;
    private final Taxonomy taxonomy;
    private final boolean preserveNameAndDescription;
    private final boolean pruneAbsentClassifications;

    private final Map<String, Classification> key2classification;

    public TaxonomyJSONImporter(Client client, Taxonomy taxonomy)
    {
        this(client, taxonomy, false, false);
    }

    public TaxonomyJSONImporter(Client client, Taxonomy taxonomy, boolean preserveNameAndDescription,
                    boolean pruneAbsentClassifications)
    {
        this.client = client;
        this.taxonomy = taxonomy;
        this.preserveNameAndDescription = preserveNameAndDescription;
        this.pruneAbsentClassifications = pruneAbsentClassifications;

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

    public ImportResult importTaxonomy(Reader reader) throws IOException
    {
        try
        {
            Gson gson = new Gson();
            Map<String, Object> jsonData = gson.fromJson(reader, new TypeToken<Map<String, Object>>()
            {
            }.getType());

            return importTaxonomy(jsonData);
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
    public ImportResult importTaxonomy(Map<String, Object> jsonData) throws IOException
    {
        if (jsonData == null)
            throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, "null")); //$NON-NLS-1$

        try
        {
            var state = new ImportProcessingState();
            var result = new ImportResult();

            var name = (String) jsonData.get("name"); //$NON-NLS-1$
            updateNameIfNeeded(taxonomy.getRoot(), name, result);

            var color = (String) jsonData.get("color"); //$NON-NLS-1$
            updateColorIfNeeded(taxonomy.getRoot(), color, result);

            var categories = (List<Map<String, Object>>) jsonData.get("categories"); //$NON-NLS-1$
            var instruments = (List<Map<String, Object>>) jsonData.get("instruments"); //$NON-NLS-1$

            if (categories != null)
            {
                importCategories(categories, taxonomy.getRoot(), state, result);
            }

            if (instruments != null)
            {
                importInstruments(instruments, state, result);
            }

            if (pruneAbsentClassifications)
            {
                removeUnprocessedCategories(taxonomy.getRoot(), state, result);
                removeUnprocessedAssignments(state, result);
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
    private void importCategories(List<Map<String, Object>> categories, Classification parent,
                    ImportProcessingState state, ImportResult result)
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
            var classification = findClassificationByKey(state, key);
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
                var newClassification = new Classification(parent, id, name);

                // assign a weight of 0 to the new category in order to not
                // destroy the existing sum of weights
                newClassification.setWeight(0);

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
                    state.newKeys.put(newClassification.getKey(), newClassification);
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

            state.processedCategories.add(classification);
            state.processedCategoriesInOrder.add(classification);

            // process children
            var children = (List<Map<String, Object>>) category.get("children"); //$NON-NLS-1$
            if (children != null)
            {
                importCategories(children, classification, state, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void importInstruments(List<Map<String, Object>> instruments, ImportProcessingState state,
                    ImportResult result) throws IOException
    {
        var processedVehicles = new HashSet<InvestmentVehicle>();

        for (var instrument : instruments)
        {
            var identifiers = (Map<String, Object>) instrument.get("identifiers"); //$NON-NLS-1$
            if (identifiers == null)
                continue;

            // find the investment vehicle by identifiers
            var investmentVehicles = findInvestmentVehicle(identifiers);
            if (investmentVehicles.isEmpty())
            {
                var name = (String) identifiers.get("name"); //$NON-NLS-1$
                result.addChange(new ChangeEntry(InvestmentVehicle.class, Operation.SKIPPED,
                                MessageFormat.format("Instrument not found: {0}", name != null ? name : "unknown")));
                continue;
            }

            if (investmentVehicles.size() > 1)
            {
                var name = (String) identifiers.get("name"); //$NON-NLS-1$
                result.addChange(new ChangeEntry(InvestmentVehicle.class, Operation.WARNING,
                                MessageFormat.format("{0} instruments found with identifiers from JSON: {1}",
                                                investmentVehicles.size(), name != null ? name : "unknown")));
            }

            try
            {
                for (var vehicle : investmentVehicles)
                {
                    var hasNotBeenMatchedBefore = processedVehicles.add(vehicle);
                    if (hasNotBeenMatchedBefore)
                    {
                        state.processedInstruments.add(vehicle);

                        importInstrument(instrument, vehicle, state, result);
                    }
                    else
                    {
                        result.addChange(new ChangeEntry(InvestmentVehicle.class, Operation.WARNING, MessageFormat
                                        .format("Ignoring assignment {0} because instrument was matched by another entry from the JSON already.",
                                                        vehicle.getName())));
                    }
                }
            }
            catch (ClassCastException e)
            {
                // happens if attributes cannot be cast to the expected type
                var name = (String) identifiers.get("name"); //$NON-NLS-1$
                throw new IOException(MessageFormat.format("Invalid data format for instrument {0}: {1}",
                                name != null ? name : "unknown", e.getMessage()), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void importInstrument(Map<String, Object> instrument, InvestmentVehicle investmentVehicle,
                    ImportProcessingState state, ImportResult result)
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
                classification = findClassificationByKey(state, key);
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
     * Remove categories not processed and reorder to match JSON
     */
    private void removeUnprocessedCategories(Classification parent, ImportProcessingState state, ImportResult result)
    {
        // First, recursively process all children
        for (var child : new ArrayList<>(parent.getChildren()))
        {
            removeUnprocessedCategories(child, state, result);
        }

        // Remove unprocessed children
        var childrenToRemove = new ArrayList<Classification>();
        for (var child : parent.getChildren())
        {
            if (!state.processedCategories.contains(child))
            {
                childrenToRemove.add(child);
            }
        }

        for (var child : childrenToRemove)
        {
            parent.getChildren().remove(child);
            result.addChange(new ChangeEntry(Classification.class, Operation.DELETE,
                            MessageFormat.format("Remove category ''{0}'' not present in JSON", child.getName())));
        }

        // retrieve the children in the order they were processed
        var childrenOfThisParent = new ArrayList<Classification>();
        for (var processedCategory : state.processedCategoriesInOrder)
        {
            if (processedCategory.getParent() == parent)
            {
                childrenOfThisParent.add(processedCategory);
            }
        }

        // rebuild children list in JSON order
        parent.getChildren().clear();
        for (int ii = 0; ii < childrenOfThisParent.size(); ii++)
        {
            var child = childrenOfThisParent.get(ii);
            child.setParent(parent);
            child.setRank(ii);
            parent.addChild(child);
        }
    }

    /**
     * Remove assignments for instruments not processed
     */
    private void removeUnprocessedAssignments(ImportProcessingState state, ImportResult result)
    {
        var allAssignmentsToRemove = new ArrayList<Pair<Classification, Classification.Assignment>>();
        this.taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification, Classification.Assignment assignment)
            {
                if (!state.processedInstruments.contains(assignment.getInvestmentVehicle()))
                {
                    allAssignmentsToRemove.add(new Pair<>(classification, assignment));
                }
            }
        });

        for (var pair : allAssignmentsToRemove)
        {
            var classification = pair.getLeft();
            var assignment = pair.getRight();

            result.addChange(new ChangeEntry(Assignment.class, Operation.DELETE,
                            MessageFormat.format("Deleted assignment with weight {0} for {1} in {2} (not in JSON)",
                                            Values.WeightPercent.format(assignment.getWeight()),
                                            assignment.getInvestmentVehicle().getName(),
                                            getPathString(classification))));
            classification.removeAssignment(assignment);
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
    private Classification findClassificationByKey(ImportProcessingState state, String key)
    {
        if (key == null || key.isEmpty())
            return null;

        var classification = key2classification.get(key);
        if (classification != null)
            return classification;

        return state.newKeys.get(key);
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

    private List<InvestmentVehicle> findInvestmentVehicle(Map<String, Object> identifiers)
    {
        var name = (String) identifiers.get("name"); //$NON-NLS-1$
        var isin = (String) identifiers.get("isin"); //$NON-NLS-1$
        var wkn = (String) identifiers.get("wkn"); //$NON-NLS-1$
        var ticker = (String) identifiers.get("ticker"); //$NON-NLS-1$

        if (isin != null && !isin.trim().isEmpty())
        {
            var securities = client.getSecurities().stream().filter(s -> isin.equals(s.getIsin()))
                            .map(s -> (InvestmentVehicle) s).toList();
            if (!securities.isEmpty())
                return securities;
        }

        if (ticker != null && !ticker.trim().isEmpty())
        {
            var securities = client.getSecurities().stream().filter(s -> ticker.equals(s.getTickerSymbol()))
                            .map(s -> (InvestmentVehicle) s).toList();
            if (!securities.isEmpty())
                return securities;
        }

        if (wkn != null && !wkn.trim().isEmpty())
        {
            var securities = client.getSecurities().stream().filter(s -> wkn.equals(s.getWkn()))
                            .map(s -> (InvestmentVehicle) s).toList();
            if (!securities.isEmpty())
                return securities;
        }

        if (name != null && !name.trim().isEmpty())
        {
            var securities = client.getSecurities().stream().filter(s -> name.equals(s.getName()))
                            .map(s -> (InvestmentVehicle) s).toList();
            if (!securities.isEmpty())
                return securities;

            // also check accounts
            var accounts = client.getAccounts().stream().filter(a -> name.equals(a.getName()))
                            .map(s -> (InvestmentVehicle) s).toList();
            if (!accounts.isEmpty())
                return accounts;
        }

        return Collections.emptyList();
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
