package name.abuchen.portfolio.ui;

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
        ResourceBundle resources = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
        TestUtilities.testBundleStrings(resources, skip);
    }
}
