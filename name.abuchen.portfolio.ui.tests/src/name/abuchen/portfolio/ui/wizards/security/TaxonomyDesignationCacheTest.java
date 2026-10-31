package name.abuchen.portfolio.ui.wizards.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.ClassificationLink;
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

    @Test
    public void testEmptyAssignmentsAreValid()
    {
        designation.getLinks().clear();

        assertTrue(designation.validateAssignments().isOK());
    }

    @Test
    public void testFullAssignmentIsValid()
    {
        assertTrue(designation.validateAssignments().isOK());
    }

    @Test
    public void testPartialAssignmentsAreValid()
    {
        designation.getLinks().get(0).setWeight(Classification.ONE_HUNDRED_PERCENT / 4);
        addLink(designation, taxonomy.getClassificationById("C"), Classification.ONE_HUNDRED_PERCENT / 2);

        assertTrue(designation.validateAssignments().isOK());
    }

    @Test
    public void testMissingClassificationIsRejected()
    {
        designation.getLinks().get(0).setClassification(null);

        assertError(designation.validateAssignments(), Messages.EditWizardMasterDataMsgClassificationMissing);
    }

    @Test
    public void testDuplicateClassificationIsRejected()
    {
        designation.getLinks().get(0).setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        addLink(designation, taxonomy.getClassificationById("B"), Classification.ONE_HUNDRED_PERCENT / 2);

        assertError(designation.validateAssignments(),
                        MessageFormat.format(Messages.EditWizardMasterDataMsgDuplicateClassification, "B"));
    }

    @Test
    public void testZeroWeightIsRejected()
    {
        designation.getLinks().get(0).setWeight(0);

        assertError(designation.validateAssignments(), Messages.EditWizardMasterDataMsgWeightEqualsZero);
    }

    @Test
    public void testNegativeWeightIsRejected()
    {
        designation.getLinks().get(0).setWeight(-1);

        assertError(designation.validateAssignments(), Messages.EditWizardMasterDataMsgWeightEqualsZero);
    }

    @Test
    public void testExcessiveTotalWeightIsRejected()
    {
        addLink(designation, taxonomy.getClassificationById("C"), 1);

        assertError(designation.validateAssignments(),
                        MessageFormat.format(Messages.EditWizardMasterDataMsgWeightNot100Percent, taxonomy.getName(),
                                        Values.Weight.format(Classification.ONE_HUNDRED_PERCENT + 1)));
    }

    @Test
    public void testModelValidatesLaterTaxonomiesWithoutPageControls()
    {
        var secondTaxonomy = new TaxonomyBuilder().addClassification("D").addTo(client);
        var model = new EditSecurityModel(client, security);
        var secondDesignation = model.getDesignations().get(1);
        addLink(secondDesignation, secondTaxonomy.getClassificationById("D"), 0);

        assertTrue(model.getDesignations().get(0).validateAssignments().isOK());
        assertError(model.validateAssignments(), Messages.EditWizardMasterDataMsgWeightEqualsZero);

        secondDesignation.getLinks().get(0).setWeight(Classification.ONE_HUNDRED_PERCENT);
        assertTrue(model.validateAssignments().isOK());
    }

    private void addLink(TaxonomyDesignation target, Classification classification, int weight)
    {
        var link = new ClassificationLink();
        link.setClassification(classification);
        link.setWeight(weight);
        target.getLinks().add(link);
    }

    private void assertError(IStatus status, String message)
    {
        assertEquals(IStatus.ERROR, status.getSeverity());
        assertEquals(message, status.getMessage());
    }
}
