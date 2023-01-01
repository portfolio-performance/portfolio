package name.abuchen.portfolio.junit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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

    private void test(String bundleName)
    {
        ResourceBundle resources = ResourceBundle.getBundle(bundleName, new Locale(language));

        Enumeration<String> keys = resources.getKeys();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();

            try
            {
                String value = resources.getString(key);

                String test = MessageFormat.format(value, (Object) null);
                assertThat(test, is(notNullValue()));
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalArgumentException(bundleName + " # " + key + " : " + e.getMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }
}
