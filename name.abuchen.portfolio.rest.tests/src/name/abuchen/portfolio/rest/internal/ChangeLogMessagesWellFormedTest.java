package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
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

    @Test
    public void testEveryLocalePatternSubstitutesBothPlaceholders() throws Exception
    {
        Bundle bundle = FrameworkUtil.getBundle(Messages.class);
        var entries = bundle.findEntries("name/abuchen/portfolio/rest", "messages*.properties", false);
        assertNotNull("no message bundles found on the classpath", entries);

        int checked = 0;
        for (URL url : Collections.list(entries))
        {
            var props = new Properties();
            try (InputStream in = url.openStream())
            {
                props.load(in); // decodes \\uXXXX escapes
            }

            for (String key : PATTERN_KEYS)
            {
                String pattern = props.getProperty(key);
                if (pattern == null)
                    continue;

                String rendered = MessageFormat.format(pattern, "INSTRUMENT_ARG", "FILE_ARG");
                assertThat(url.getFile() + " [" + key + "] must substitute {0}", rendered,
                                containsString("INSTRUMENT_ARG"));
                assertThat(url.getFile() + " [" + key + "] must substitute {1}", rendered,
                                containsString("FILE_ARG"));
                checked++;
            }
        }

        // guard against a silently-empty sweep (wrong path / no matches)
        assertTrue("expected to check many locale patterns, only checked " + checked, checked >= 30);
    }
}
