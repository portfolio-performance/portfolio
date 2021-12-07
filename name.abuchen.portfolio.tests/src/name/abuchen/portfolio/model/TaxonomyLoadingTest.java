package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.TestUtilities;

@RunWith(Parameterized.class)
public class TaxonomyLoadingTest
{
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getLanguages()
    {
        return TestUtilities.availableLanguages();
    }

    private String language;

    public TaxonomyLoadingTest(String language)
    {
        this.language = language;
    }

    @Test
    public void loadTaxonomyDefinitionsInAllLanguages()
    {
        Locale locale = Locale.getDefault();

        try
        {
            Locale.setDefault(Locale.forLanguageTag(language));

            for (TaxonomyTemplate template : TaxonomyTemplate.list())
            {
                template.build();

                Taxonomy original = template.buildOriginal();

                // throws exception if template contains duplicate keys
                Map<String, Classification> id2classification = original.getAllClassifications() //
                                .stream().collect(Collectors.toMap(Classification::getId, c -> c));

                assertThat(id2classification, is(notNullValue()));
            }
        }
        finally
        {
            Locale.setDefault(locale);
        }

    }
}
