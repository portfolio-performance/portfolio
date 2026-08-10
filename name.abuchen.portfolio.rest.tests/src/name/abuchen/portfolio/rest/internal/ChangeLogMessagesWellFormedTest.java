package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.rest.Messages;

@SuppressWarnings("nls")
public class ChangeLogMessagesWellFormedTest
{
    // keys InstrumentChangeLog feeds through MessageFormat.format as PATTERNS,
    // each taking two arguments (instrument name, file label)
    private static final String[] PATTERN_KEYS = { "MsgApiInstrumentChanged", "MsgApiInstrumentDeleted" };

    // keys FileAccessRegistry feeds through MessageFormat.format as PATTERNS,
    // each taking one argument (the offending alias)
    private static final String[] SINGLE_ARG_PATTERN_KEYS = { "MsgErrorAliasAlreadyInUse",
                    "MsgErrorAliasMustMatchPattern", "MsgErrorAliasMustNotLookLikeUUID" };

    private record LocaleBundle(String name, Properties properties)
    {
    }

    @Test
    public void testEveryLocalePatternSubstitutesBothPlaceholders() throws Exception
    {
        int checked = 0;
        for (LocaleBundle bundle : localeBundles())
        {
            for (String key : PATTERN_KEYS)
            {
                String pattern = bundle.properties().getProperty(key);
                if (pattern == null)
                    continue;

                String rendered = MessageFormat.format(pattern, "INSTRUMENT_ARG", "FILE_ARG");
                assertThat(bundle.name() + " [" + key + "] must substitute {0}", rendered,
                                containsString("INSTRUMENT_ARG"));
                assertThat(bundle.name() + " [" + key + "] must substitute {1}", rendered,
                                containsString("FILE_ARG"));
                checked++;
            }
        }

        // guard against a silently-empty sweep (wrong path / no matches)
        assertTrue("expected to check many locale patterns, only checked " + checked, checked >= 30);
    }

    /**
     * The alias messages are patterns too: a single unescaped apostrophe - and
     * the romance languages write one in every other word - silently swallows
     * the alias the user has to correct.
     */
    @Test
    public void testEveryLocaleAliasMessageSubstitutesItsPlaceholder() throws Exception
    {
        int checked = 0;
        for (LocaleBundle bundle : localeBundles())
        {
            for (String key : SINGLE_ARG_PATTERN_KEYS)
            {
                String pattern = bundle.properties().getProperty(key);
                if (pattern == null)
                    continue;

                assertThat(bundle.name() + " [" + key + "] must substitute {0}",
                                MessageFormat.format(pattern, "ALIAS_ARG"), containsString("ALIAS_ARG"));
                checked++;
            }
        }

        assertTrue("expected to check many locale patterns, only checked " + checked, checked >= 45);
    }

    private List<LocaleBundle> localeBundles() throws Exception
    {
        Bundle bundle = FrameworkUtil.getBundle(Messages.class);
        var entries = bundle.findEntries("name/abuchen/portfolio/rest", "messages*.properties", false);
        assertNotNull("no message bundles found on the classpath", entries);

        var answer = new ArrayList<LocaleBundle>();
        for (URL url : Collections.list(entries))
        {
            var properties = new Properties();
            try (InputStream in = url.openStream())
            {
                properties.load(in); // decodes \\uXXXX escapes
            }
            answer.add(new LocaleBundle(url.getFile(), properties));
        }
        return answer;
    }
}
