package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.money.Values;

/**
 * Exports a taxonomy to JSON format.
 */
public class TaxonomyJSONExporter implements Exporter
{
    public static class AllTaxonomies implements Exporter
    {
        private final Client client;

        public AllTaxonomies(Client client)
        {
            this.client = client;
        }

        @Override
        public String getName()
        {
            return "AllTaxonomies";
        }

        @Override
        public void export(OutputStream out) throws IOException
        {
            List<Map<String, Object>> taxonomiesArray = new ArrayList<>();

            for (Taxonomy taxonomy : client.getTaxonomies())
            {
                var individualExporter = new TaxonomyJSONExporter(taxonomy);
                var taxonomyJson = individualExporter.createJSONStructure();
                taxonomiesArray.add(taxonomyJson);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
            {
                gson.toJson(taxonomiesArray, writer);
            }
        }
    }

    private static class CategoryAssignment
    {
        final Classification classification;
        final Classification.Assignment assignment;

        CategoryAssignment(Classification classification, Classification.Assignment assignment)
        {
            this.classification = classification;
            this.assignment = assignment;
        }
    }

    private final Taxonomy taxonomy;

    public TaxonomyJSONExporter(Taxonomy taxonomy)
    {
        this.taxonomy = taxonomy;
    }

    @Override
    public String getName()
    {
        return taxonomy.getName();
    }

    /**
     * Exports the taxonomy to JSON format and writes it to the provided output
     * stream.
     */
    @Override
    public void export(OutputStream out) throws IOException
    {
        Map<String, Object> result = createJSONStructure();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
        {
            gson.toJson(result, writer);
        }
    }

    private Map<String, Object> createJSONStructure()
    {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> categories = new ArrayList<>();
        List<Map<String, Object>> instruments = new ArrayList<>();

        taxonomy.getRoot().getChildren().forEach(c -> addClassificationToCategories(c, categories));
        addInstrumentsFromTaxonomy(instruments);

        result.put("name", taxonomy.getName()); //$NON-NLS-1$
        result.put("color", taxonomy.getRoot().getColor()); //$NON-NLS-1$
        result.put("categories", categories); //$NON-NLS-1$
        result.put("instruments", instruments); //$NON-NLS-1$

        return result;
    }

    private void addClassificationToCategories(Classification classification, List<Map<String, Object>> categories)
    {
        Map<String, Object> category = new LinkedHashMap<>();
        category.put("name", classification.getName()); //$NON-NLS-1$

        var key = classification.getKey();
        if (!key.isEmpty())
            category.put("key", key); //$NON-NLS-1$

        if (classification.getNote() != null && !classification.getNote().isEmpty())
            category.put("description", classification.getNote()); //$NON-NLS-1$

        if (classification.getColor() != null)
            category.put("color", classification.getColor()); //$NON-NLS-1$

        List<Map<String, Object>> children = new ArrayList<>();

        classification.getChildren().stream().sorted((a, b) -> Integer.compare(a.getRank(), b.getRank()))
                        .forEach(child -> addClassificationToCategories(child, children));

        if (!children.isEmpty())
            category.put("children", children); //$NON-NLS-1$

        categories.add(category);
    }

    private void addInstrumentsFromTaxonomy(List<Map<String, Object>> instruments)
    {
        var instrumentAssignments = new LinkedHashMap<InvestmentVehicle, List<CategoryAssignment>>();

        taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification, Classification.Assignment assignment)
            {
                var vehicle = assignment.getInvestmentVehicle();
                instrumentAssignments.computeIfAbsent(vehicle, k -> new ArrayList<>())
                                .add(new CategoryAssignment(classification, assignment));
            }
        });

        for (var entry : instrumentAssignments.entrySet())
        {
            var vehicle = entry.getKey();
            var assignments = entry.getValue();

            // export sorted by rank (thereby implicitly exporting the rank)
            Collections.sort(assignments,
                            (a, b) -> Integer.compare(a.classification.getRank(), b.classification.getRank()));

            var instrument = new LinkedHashMap<String, Object>();

            var identifiers = new LinkedHashMap<>();
            identifiers.put("name", vehicle.getName()); //$NON-NLS-1$
            if (vehicle instanceof Security security)
            {
                if (security.getIsin() != null && !security.getIsin().isEmpty())
                    identifiers.put("isin", security.getIsin()); //$NON-NLS-1$
                if (security.getWkn() != null && !security.getWkn().isEmpty())
                    identifiers.put("wkn", security.getWkn()); //$NON-NLS-1$
                if (security.getTickerSymbol() != null && !security.getTickerSymbol().isEmpty())
                    identifiers.put("ticker", security.getTickerSymbol()); //$NON-NLS-1$
            }
            instrument.put("identifiers", identifiers); //$NON-NLS-1$

            // Categories with weights
            var categories = new ArrayList<>();
            for (var categoryAssignment : assignments)
            {
                var category = new LinkedHashMap<>();

                var classification = categoryAssignment.classification;
                var assignment = categoryAssignment.assignment;

                var key = classification.getKey();
                if (!key.isEmpty())
                    category.put("key", key); //$NON-NLS-1$

                var path = classification.getPathToRoot().stream().skip(1).map(c -> c.getName()).toList();
                category.put("path", path); //$NON-NLS-1$

                // Weight as percentage
                double weightPercent = (double) assignment.getWeight() / Values.Weight.factor();
                category.put("weight", weightPercent); //$NON-NLS-1$

                categories.add(category);
            }

            instrument.put("categories", categories); //$NON-NLS-1$
            instruments.add(instrument);
        }
    }
}
