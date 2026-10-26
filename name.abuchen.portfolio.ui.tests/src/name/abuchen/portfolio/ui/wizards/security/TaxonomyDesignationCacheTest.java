package name.abuchen.portfolio.ui.wizards.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.TaxonomyDesignation;

public class TaxonomyDesignationCacheTest
{
    private Client client;
    private Security security;
    private Taxonomy taxonomy;
    private TaxonomyDesignation designation;

    @Before
    public void setUp()
    {
        client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        security = new Security("TEST", CurrencyUnit.EUR);
        client.addSecurity(security);

        taxonomy = new TaxonomyBuilder() //
                        .addClassification("A") //
                        .addClassification("A", "B") //
                        .addClassification("B", "C") //
                        .addTo(client);

        taxonomy.getClassificationById("B").addAssignment(new Classification.Assignment(security));

        designation = new TaxonomyDesignation(taxonomy, security);
    }

    @Test
    public void testIndexOfFollowsTaxonomyOrder()
    {
        assertEquals(0, designation.indexOf(taxonomy.getClassificationById("A")));
        assertEquals(1, designation.indexOf(taxonomy.getClassificationById("B")));
        assertEquals(-1, designation.indexOf(null));
    }

    @Test
    public void testPathNameCalculatedOnce()
    {
        Classification classification = taxonomy.getClassificationById("C");

        String first = designation.getPathName(classification);
        assertSame(first, designation.getPathName(classification));
        assertEquals(classification.getPathName(false, 80), first);
        assertNull(designation.getPathName(null));
    }

    @Test
    public void testDesignationCollectsAssignmentOfSecurity()
    {
        assertEquals(1, designation.getLinks().size());
        assertEquals(taxonomy.getClassificationById("B"), designation.getLinks().get(0).getClassification());
    }
}
