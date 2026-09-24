package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    // every pattern the change log formats, with the number of its arguments
    private static final Map<String, Integer> ALL_PATTERNS = Map.ofEntries( //
                    Map.entry("MsgApiEntityChanged", 3), //
                    Map.entry("MsgApiEntityCreated", 3), //
                    Map.entry("MsgApiEntityDeleted", 3), //
                    Map.entry("MsgApiFieldChanged", 3), //
                    Map.entry("MsgApiFileOpened", 1), //
                    Map.entry("MsgApiFileSaved", 1), //
                    Map.entry("MsgApiImportCommitted", 3), //
                    Map.entry("MsgApiInstrumentChanged", 2), //
                    Map.entry("MsgApiInstrumentDeleted", 2), //
                    Map.entry("MsgApiInstrumentFieldChanged", 3), //
                    Map.entry("MsgApiPlanTransactionsGenerated", 3), //
                    Map.entry("MsgApiPricesUpdated", 4), //
                    Map.entry("MsgApiStockSplitApplied", 5), //
                    Map.entry("MsgApiTransactionCreated", 3), //
                    Map.entry("MsgApiTransactionDeleted", 3), //
                    Map.entry("MsgApiTransactionUpdated", 3));

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

    @Test
    public void testEveryChangeLogPatternIsPresentAndSubstitutesAllPlaceholders() throws Exception
    {
        int files = 0;
        for (URL url : bundles())
        {
            var props = load(url);
            for (var entry : ALL_PATTERNS.entrySet())
            {
                var pattern = props.getProperty(entry.getKey());
                assertNotNull(url.getFile() + " lacks " + entry.getKey(), pattern);

                var arguments = new Object[entry.getValue()];
                for (int ii = 0; ii < arguments.length; ii++)
                    arguments[ii] = "ARG" + ii + "_";

                var rendered = MessageFormat.format(pattern, arguments);
                for (int ii = 0; ii < arguments.length; ii++)
                    assertThat(url.getFile() + " [" + entry.getKey() + "] must substitute {" + ii + "}", rendered,
                                    containsString("ARG" + ii + "_"));
            }
            files++;
        }
        assertTrue("expected all locale files, found " + files, files >= 19);
    }

    @Test
    public void testEveryLocaleHasTheSameSortedKeys() throws Exception
    {
        Set<String> english = null;
        for (URL url : bundles())
        {
            if (url.getFile().endsWith("/messages.properties"))
                english = load(url).stringPropertyNames();
        }
        assertNotNull(english);

        for (URL url : bundles())
        {
            assertThat(url.getFile(), load(url).stringPropertyNames(), is(english));

            // keys in sorted order, as the translation tooling writes them
            var keys = new ArrayList<String>();
            try (var reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.ISO_8859_1)))
            {
                reader.lines().filter(line -> !line.isBlank() && !line.startsWith("#"))
                                .map(line -> line.substring(0, line.indexOf('=')).strip()).forEach(keys::add);
            }
            var sorted = new ArrayList<>(keys);
            Collections.sort(sorted);
            assertThat(url.getFile() + " keys must be sorted", keys, is(sorted));
        }
    }

    private static List<URL> bundles()
    {
        Bundle bundle = FrameworkUtil.getBundle(Messages.class);
        var entries = bundle.findEntries("name/abuchen/portfolio/rest", "messages*.properties", false);
        assertNotNull("no message bundles found on the classpath", entries);
        return Collections.list(entries);
    }

    private static Properties load(URL url) throws Exception
    {
        var props = new Properties();
        try (InputStream in = url.openStream())
        {
            props.load(in);
        }
        return props;
    }
}
