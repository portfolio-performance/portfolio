package name.abuchen.portfolio.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.TestUtilities;

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
    public void testMessages()
    {
        test("name.abuchen.portfolio.ui.messages", "LabelJSONPathHint"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testSampleMessages()
    {
        test("name.abuchen.portfolio.ui.parts.samplemessages"); //$NON-NLS-1$
    }

    @Test
    public void testSettingsLabels()
    {
        test("name.abuchen.portfolio.ui.views.settings.labels"); //$NON-NLS-1$
    }

    @Test
    public void testViewLabels()
    {
        test("name.abuchen.portfolio.ui.views.labels"); //$NON-NLS-1$
    }

    private void test(String bundleName, String... skip)
    {
        Set<String> exclude = new HashSet<>(Arrays.asList(skip));

        ResourceBundle resources = ResourceBundle.getBundle(bundleName, new Locale(language));

        Enumeration<String> keys = resources.getKeys();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();

            if (exclude.contains(key))
                continue;

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
