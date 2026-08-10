package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class FileAccessRegistryTest
{
    private IEclipsePreferences node;
    private FileAccessRegistry registry;

    @Before
    public void setUp()
    {
        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        registry = new FileAccessRegistry(node);
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    @Test
    public void testEnsureRecordCreatesStableUuid()
    {
        var first = registry.ensureRecord("/tmp/a.portfolio");
        var second = registry.ensureRecord("/tmp/a.portfolio");
        assertThat(first.uuid(), is(second.uuid()));
    }

    @Test
    public void testResolveOnlyMatchesEnabledRecords()
    {
        var record = registry.ensureRecord("/tmp/a.portfolio");
        assertThat(registry.resolve(record.uuid()), instanceOf(FileAccessRegistry.Resolution.NotFound.class));

        registry.setEnabled("/tmp/a.portfolio", true);
        assertThat(registry.resolve(record.uuid()), instanceOf(FileAccessRegistry.Resolution.Match.class));
    }

    @Test
    public void testResolveByAliasAndAmbiguity()
    {
        registry.setEnabled("/tmp/a.portfolio", true);
        registry.setAlias("/tmp/a.portfolio", "main");
        assertThat(registry.resolve("main"), instanceOf(FileAccessRegistry.Resolution.Match.class));

        // duplicate alias is only possible by editing the preferences
        // directly (hand-edited prefs file); the registry must answer
        // Ambiguous instead of silently picking one
        registry.setEnabled("/tmp/b.portfolio", true);
        node.node(Base64.getUrlEncoder().withoutPadding()
                        .encodeToString("/tmp/b.portfolio".getBytes(StandardCharsets.UTF_8))).put("alias", "main");
        assertThat(registry.resolve("main"), instanceOf(FileAccessRegistry.Resolution.Ambiguous.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAliasMustBeUnique()
    {
        registry.setAlias("/tmp/a.portfolio", "main");
        registry.setAlias("/tmp/b.portfolio", "main");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAliasSyntaxIsRestricted()
    {
        registry.setAlias("/tmp/a.portfolio", "Main File!");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAliasMustNotParseAsUuid()
    {
        registry.setAlias("/tmp/a.portfolio", "123e4567-e89b-12d3-a456-426614174000");
    }

    /** the preference page validates the whole table before it stores anything */
    @Test
    public void testValidateAliasesRejectsABatchWithADuplicate()
    {
        var batch = new LinkedHashMap<String, String>();
        batch.put("/tmp/a.portfolio", "main");
        batch.put("/tmp/b.portfolio", "main");

        try
        {
            registry.validateAliases(batch);
            fail("expected the duplicate alias to be rejected");
        }
        catch (IllegalArgumentException e)
        {
            // ...and nothing of the batch was stored
            assertThat(registry.byPath("/tmp/a.portfolio").isPresent(), is(false));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateAliasesRejectsAnAliasTakenByAFileOutsideTheBatch()
    {
        registry.setAlias("/tmp/other.portfolio", "main");
        registry.validateAliases(Map.of("/tmp/a.portfolio", "main"));
    }

    /** the batch is validated as an end state, so files may swap aliases */
    @Test
    public void testValidateAliasesAllowsASwapWithinTheBatch()
    {
        registry.setAlias("/tmp/a.portfolio", "one");
        registry.setAlias("/tmp/b.portfolio", "two");

        var batch = new LinkedHashMap<String, String>();
        batch.put("/tmp/a.portfolio", "two");
        batch.put("/tmp/b.portfolio", "one");

        registry.validateAliases(batch);
    }

    /** ...and a file that gives up its alias frees it for another */
    @Test
    public void testValidateAliasesAllowsHandingAnAliasOver()
    {
        registry.setAlias("/tmp/a.portfolio", "main");

        var batch = new LinkedHashMap<String, String>();
        batch.put("/tmp/a.portfolio", null);
        batch.put("/tmp/b.portfolio", "main");

        registry.validateAliases(batch);
    }
}
