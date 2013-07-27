package name.abuchen.portfolio.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;

public final class TaxonomyTemplate
{
    private static final List<TaxonomyTemplate> TEMPLATES = Arrays.asList( //
                    new TaxonomyTemplate("assetclasses"), //$NON-NLS-1$
                    new TaxonomyTemplate("industry-gics"), //$NON-NLS-1$
                    new TaxonomyTemplate("industry-simple2level")); //$NON-NLS-1$

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

    public Taxonomy build()
    {
        ResourceBundle bundle = ResourceBundle.getBundle("/META-INF/taxonomy/" + id); //$NON-NLS-1$

        Taxonomy taxonomy = new Taxonomy(UUID.randomUUID().toString(), name);

        Classification root = new Classification(id, name);
        taxonomy.setRootNode(root);
        taxonomy.setDimensions(Arrays.asList(getString(bundle, "labels").split(","))); //$NON-NLS-1$ //$NON-NLS-2$);

        readClassification(bundle, root);

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

            Classification child = new Classification(parent, childId, label);

            String description = getString(bundle, childId + ".description"); //$NON-NLS-1$
            if (description != null)
                child.setDescription(description);

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

}
