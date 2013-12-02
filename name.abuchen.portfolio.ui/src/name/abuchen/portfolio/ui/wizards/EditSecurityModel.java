package name.abuchen.portfolio.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.ui.util.BindingHelper;

/* package */final class EditSecurityModel extends BindingHelper.Model
{
    static class TaxonomyDesignation extends BindingHelper.Model
    {
        private Taxonomy taxonomy;
        private Security security;
        private List<Classification> elements;

        private List<ClassificationLink> links = new ArrayList<ClassificationLink>();

        public TaxonomyDesignation(Taxonomy taxonomy, final Security security)
        {
            this.taxonomy = taxonomy;
            this.security = security;
            this.elements = taxonomy.getRoot().getTreeElements();

            taxonomy.foreach(new Visitor()
            {
                @Override
                public void visit(Classification classification, Assignment assignment)
                {
                    if (assignment.getInvestmentVehicle().equals(security))
                        links.add(new ClassificationLink(classification, assignment));
                }
            });
        }

        public Taxonomy getTaxonomy()
        {
            return taxonomy;
        }

        public List<Classification> getElements()
        {
            return elements;
        }

        public List<ClassificationLink> getLinks()
        {
            return links;
        }

        @Override
        public void applyChanges()
        {
            final Map<Classification, ClassificationLink> classification2link = new HashMap<Classification, ClassificationLink>();
            for (ClassificationLink link : links)
                classification2link.put(link.getClassification(), link);

            taxonomy.foreach(new Visitor()
            {
                @Override
                public void visit(Classification classification, Assignment assignment)
                {
                    if (assignment.getInvestmentVehicle().equals(security))
                    {
                        ClassificationLink link = classification2link.remove(classification);

                        if (link == null)
                            classification.getAssignments().remove(assignment);
                        else
                            assignment.setWeight(link.getWeight());
                    }
                }
            });

            for (ClassificationLink link : classification2link.values())
            {
                Assignment assignment = new Assignment(security);
                assignment.setWeight(link.getWeight());
                assignment.setRank(Integer.MAX_VALUE);
                link.getClassification().addAssignment(assignment);
            }
        }
    }

    static class ClassificationLink extends BindingHelper.Model
    {
        private Classification classification;
        private int weight;

        public ClassificationLink(Classification classification, Assignment assignment)
        {
            this.classification = classification;
            this.weight = assignment.getWeight();
        }

        public ClassificationLink()
        {
            weight = Classification.ONE_HUNDRED_PERCENT;
        }

        public Classification getClassification()
        {
            return classification;
        }

        public void setClassification(Classification classification)
        {
            this.classification = classification;
        }

        public int getWeight()
        {
            return weight;
        }

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        @Override
        public void applyChanges()
        {}
    }

    private Security security;

    private String name;
    private String isin;
    private String tickerSymbol;
    private String wkn;
    private String feed;
    private boolean isRetired;

    private List<TaxonomyDesignation> designations = new ArrayList<TaxonomyDesignation>();

    public EditSecurityModel(Client client, Security security)
    {
        super(client);

        this.security = security;

        this.name = security.getName();
        this.isin = security.getIsin();
        this.tickerSymbol = security.getTickerSymbol();
        this.wkn = security.getWkn();
        this.feed = security.getFeed();
        this.isRetired = security.isRetired();

        for (Taxonomy taxonomy : client.getTaxonomies())
            this.designations.add(new TaxonomyDesignation(taxonomy, security));
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        firePropertyChange("name", this.name, this.name = name); //$NON-NLS-1$
    }

    public String getIsin()
    {
        return isin;
    }

    public void setIsin(String isin)
    {
        firePropertyChange("isin", this.isin, this.isin = isin); //$NON-NLS-1$
    }

    public String getTickerSymbol()
    {
        return tickerSymbol;
    }

    public void setTickerSymbol(String tickerSymbol)
    {
        firePropertyChange("tickerSymbol", this.tickerSymbol, this.tickerSymbol = tickerSymbol); //$NON-NLS-1$
    }

    public String getWkn()
    {
        return wkn;
    }

    public void setWkn(String wkn)
    {
        firePropertyChange("wkn", this.wkn, this.wkn = wkn); //$NON-NLS-1$
    }

    public String getFeed()
    {
        return feed;
    }

    public void setFeed(String feed)
    {
        firePropertyChange("feed", this.feed, this.feed = feed); //$NON-NLS-1$
    }

    public boolean isRetired()
    {
        return isRetired;
    }

    public void setRetired(boolean isRetired)
    {
        firePropertyChange("retired", this.isRetired, this.isRetired = isRetired); //$NON-NLS-1$
    }

    public List<TaxonomyDesignation> getDesignations()
    {
        return designations;
    }

    @Override
    public void applyChanges()
    {
        security.setName(name);
        security.setIsin(isin);
        security.setTickerSymbol(tickerSymbol);
        security.setWkn(wkn);
        security.setFeed(feed);
        security.setRetired(isRetired);

        for (TaxonomyDesignation designation : designations)
            designation.applyChanges();
    }
}
