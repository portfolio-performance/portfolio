package name.abuchen.portfolio.junit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Set;

import name.abuchen.portfolio.util.TextUtil;

public class TestUtilities
{
    private TestUtilities()
    {}

    public static String read(Class<?> clazz, String resource)
    {
        try (Scanner scanner = new Scanner(clazz.getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("nls")
    public static Collection<Object[]> availableLanguages()
    {
        return Arrays.asList(new String[] { "" }, new String[] { "de" }, new String[] { "es" }, new String[] { "pt" },
                        new String[] { "pt_BR" }, new String[] { "nl" }, new String[] { "fr" }, new String[] { "it" },
                        new String[] { "cs" }, new String[] { "ru" }, new String[] { "sk" }, new String[] { "pl" },
                        new String[] { "zh" }, new String[] { "zh_TW" }, new String[] { "da" });
    }

    /**
     * Test that bundle strings contain correct formatting characters and do not
     * contain UTF8 placeholder characters.
     */
    public static void testBundleStrings(ResourceBundle bundle, String... skip)
    {
        Set<String> exclude = new HashSet<>(Arrays.asList(skip));

        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();

            if (exclude.contains(key))
                continue;

            try
            {
                String description = bundle.getBaseBundleName() + "_" + bundle.getLocale() + "_" + key; //$NON-NLS-1$ //$NON-NLS-2$
                String value = bundle.getString(key);

                String test = MessageFormat.format(value, (Object) null);
                assertThat(description, test, is(notNullValue()));

                // replacement character
                assertFalse(description, value.contains("\uFFFD")); //$NON-NLS-1$
                assertFalse(description, value.contains("\uFFEF")); //$NON-NLS-1$
                assertFalse(description, value.contains("\uFFBF")); //$NON-NLS-1$

                // check for leading and trailing whitespace
                assertThat(description, value, is(TextUtil.trim(value)));
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalArgumentException(bundle.getBaseBundleName() + " # " + key + " : " + e.getMessage(), //$NON-NLS-1$ //$NON-NLS-2$
                                e);
            }
        }
    }

}
