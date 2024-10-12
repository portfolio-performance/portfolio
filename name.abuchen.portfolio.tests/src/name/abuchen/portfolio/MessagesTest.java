package name.abuchen.portfolio;

import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.junit.TestUtilities;

@RunWith(Parameterized.class)
public class MessagesTest
{
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getLanguages()
    {
        return TestUtilities.availableLanguages();
    }

    private String language;

    public MessagesTest(String language)
    {
        this.language = language;
    }

    @Test
    public void testModelLabels()
    {
        test("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$
    }

    @Test
    public void testMessages()
    {
        test("name.abuchen.portfolio.messages"); //$NON-NLS-1$
    }

    @Test
    public void testHolidayNameLabels()
    {
        test("name.abuchen.portfolio.util.holiday-names"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesAssetClassesLabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.assetclasses"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesIndustrySimpleLabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.industry-simple"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesIndustryGICSLabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.industry-gics"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesKommerLabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.kommer"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesRegionsLabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.regions"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesRegionsMSCILabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.regions-msci"); //$NON-NLS-1$
    }

    @Test
    public void testTaxonomyTemplatesSecurityTypeLabels()
    {
        test("name.abuchen.portfolio.model.taxonomy_templates.security-type"); //$NON-NLS-1$
    }

    @Test
    public void testMoneyCurrenciesPLabels()
    {
        test("name.abuchen.portfolio.money.currencies"); //$NON-NLS-1$
    }

    @Test
    public void testEurostatHICPLabels()
    {
        test("name.abuchen.portfolio.online.impl.eurostathicp-labels"); //$NON-NLS-1$
    }

    @Test
    public void testExchangeLabels()
    {
        test("name.abuchen.portfolio.online.impl.exchange-labels"); //$NON-NLS-1$
    }

    @Test
    public void testAggregationLabels()
    {
        test("name.abuchen.portfolio.snapshot.labels"); //$NON-NLS-1$
    }

    private void test(String bundleName, String... skip)
    {
        ResourceBundle resources = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
        TestUtilities.testBundleStrings(resources, skip);
    }
}
