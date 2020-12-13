package name.abuchen.portfolio.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TaxonomyLoadingTest
{
    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getLanguages()
    {
        return Arrays.asList(new String[] { "de" }, new String[] { "en" }, new String[] { "es" }, new String[] { "pt" },
                        new String[] { "nl" }, new String[] { "fr" });
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
            }
        }
        finally
        {
            Locale.setDefault(locale);
        }

    }
}
