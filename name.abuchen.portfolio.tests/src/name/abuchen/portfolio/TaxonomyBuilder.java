package name.abuchen.portfolio;

import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;

public class TaxonomyBuilder
{
    private Taxonomy taxonomy;

    public TaxonomyBuilder()
    {
        String uuid = UUID.randomUUID().toString();
        this.taxonomy = new Taxonomy(uuid);

        Classification root = new Classification(uuid, uuid);
        taxonomy.setRootNode(root);
    }

    public TaxonomyBuilder addClassification(String id)
    {
        return addClassificaiton(taxonomy.getRoot(), id);
    }

    public TaxonomyBuilder addClassification(String parent, String id)
    {
        return addClassificaiton(taxonomy.getClassificationById(parent), id);
    }

    private TaxonomyBuilder addClassificaiton(Classification parent, String id)
    {
        Classification c = new Classification(parent, id, id);
        parent.addChild(c);
        return this;
    }

    public Taxonomy addTo(Client client)
    {
        client.addTaxonomy(taxonomy);
        return taxonomy;
    }

}
