package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class TokenStoreTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testTokenIsStableAcrossInstances() throws Exception
    {
        Path dir = tempFolder.getRoot().toPath();

        var first = new TokenStore(dir).getOrCreate();
        var second = new TokenStore(dir).getOrCreate();

        assertThat(first, is(second));
        assertThat(first.length() > 30, is(true));
        assertThat(Files.exists(dir.resolve("api-token")), is(true));
    }

    @Test
    public void testRegenerateChangesToken() throws Exception
    {
        var store = new TokenStore(tempFolder.getRoot().toPath());
        var original = store.getOrCreate();

        var regenerated = store.regenerate();

        assertThat(regenerated, is(not(original)));
        assertThat(store.getOrCreate(), is(regenerated));
    }
}
