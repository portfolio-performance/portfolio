package name.abuchen.portfolio.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;

import name.abuchen.portfolio.money.Values;

public final class TaxonomyTemplate
{
    /* package */static final String INDUSTRY_GICS = "industry-gics"; //$NON-NLS-1$
    /* package */static final String INDUSTRY_SIMPLE2LEVEL = "industry-simple"; //$NON-NLS-1$

    private static final List<TaxonomyTemplate> TEMPLATES = Arrays.asList( //
                    new TaxonomyTemplate("assetclasses"), //$NON-NLS-1$
                    new TaxonomyTemplate(INDUSTRY_GICS), //
                    new TaxonomyTemplate("industry-gics-1st-level"), //$NON-NLS-1$
                    new TaxonomyTemplate(INDUSTRY_SIMPLE2LEVEL), //
                    new TaxonomyTemplate("kommer"), //$NON-NLS-1$
                    new TaxonomyTemplate("regions"), //$NON-NLS-1$
                    new TaxonomyTemplate("regions-msci"), //$NON-NLS-1$
                    new TaxonomyTemplate("security-type")); //$NON-NLS-1$

    private String id;
    private String name;

    private TaxonomyTemplate(String id)
    {
        this.id = id;

        ResourceBundle bundle = ResourceBundle.getBundle("/META-INF/taxonomy/" + id); //$NON-NLS-1$
        this.name = getString(bundle, "name"); //$NON-NLS-1$
    }

    public static List<TaxonomyTemplate> list()
    {
        return Collections.unmodifiableList(TEMPLATES);
    }

    public static TaxonomyTemplate byId(String id)
    {
        for (TaxonomyTemplate template : TEMPLATES)
        {
            if (template.getId().equals(id))
                return template;
        }
        return null;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Builds a taxonomy based on the template.
     */
    public Taxonomy build()
    {
        Taxonomy taxonomy = buildFromTemplate();

        // classification identifier must be globally unique because they are
        // used when storing chart configurations, etc.
        taxonomy.setId(UUID.randomUUID().toString());
        taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification)
            {
                classification.setId(UUID.randomUUID().toString());
            }
        });

        return taxonomy;
    }

    /**
     * Builds a taxonomy based on the template including the original UUIDs. Use
     * only for translation purposes.
     */
    public Taxonomy buildOriginal()
    {
        return buildFromTemplate();
    }

    /**
     * Builds a taxonomy with exactly the identifier specified in the template.
     * Needed to convert legacy (hard-coded) taxonomies such as asset classes
     * and industry classifications to new style taxonomies.
     */
    /* package */Taxonomy buildFromTemplate()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("/META-INF/taxonomy/" + id); //$NON-NLS-1$

        Taxonomy taxonomy = new Taxonomy(id, name);

        taxonomy.setSource(getString(bundle, "source")); //$NON-NLS-1$

        Classification root = new Classification(id, name);
        taxonomy.setRootNode(root);
        String labels = getString(bundle, "labels"); //$NON-NLS-1$
        if (labels == null)
            throw new IllegalArgumentException();
        taxonomy.setDimensions(Arrays.asList(labels.split(","))); //$NON-NLS-1$

        readClassification(bundle, root);

        String colors = getString(bundle, "colors"); //$NON-NLS-1$
        if (colors != null)
        {
            String[] hsb = colors.split(","); //$NON-NLS-1$
            root.assignRandomColors(Float.parseFloat(hsb[0]), Float.parseFloat(hsb[1]), Float.parseFloat(hsb[2]));
        }

        return taxonomy;
    }

    private void readClassification(ResourceBundle bundle, Classification parent)
    {
        String children = getString(bundle, parent.getId() + ".children"); //$NON-NLS-1$
        if (children == null)
            return;

        for (String childId : children.split(",")) //$NON-NLS-1$
        {
            String label = getString(bundle, childId + ".label"); //$NON-NLS-1$
            if (label == null)
                continue;

            String color = getString(bundle, childId + ".color"); //$NON-NLS-1$

            Classification child = new Classification(parent, childId, label, color);

            int weight = getInt(bundle, childId + ".weight"); //$NON-NLS-1$
            if (weight >= 0)
                child.setWeight(weight * Values.Weight.factor());

            String description = getString(bundle, childId + ".description"); //$NON-NLS-1$
            if (description != null)
                child.setNote(description);

            String data = getString(bundle, childId + ".data"); //$NON-NLS-1$
            if (data != null)
            {
                String[] elements = data.split(";"); //$NON-NLS-1$
                for (String element : elements)
                {
                    int p = element.indexOf('=');
                    if (p > 0)
                        child.setData(element.substring(0, p), element.substring(p + 1));
                }
            }

            parent.addChild(child);

            readClassification(bundle, child);
        }
    }

    private String getString(ResourceBundle bundle, String key)
    {
        try
        {
            return bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return null;
        }
    }

    private int getInt(ResourceBundle bundle, String key)
    {
        try
        {
            return Integer.parseInt(bundle.getString(key));
        }
        catch (MissingResourceException e)
        {
            return -1;
        }
    }

}
