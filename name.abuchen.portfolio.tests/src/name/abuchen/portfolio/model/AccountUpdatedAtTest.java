package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

@SuppressWarnings("nls")
public class AccountUpdatedAtTest
{
    @Test
    public void testFreshAccountSavesWithoutNpe() throws Exception
    {
        Client client = new Client();
        client.addAccount(new Account("fresh")); // never mutated -> updatedAt was null

        var writer = new ProtobufWriter();
        var out = new ByteArrayOutputStream();
        writer.save(client, out); // must not NPE in saveAccounts

        var reloaded = writer.load(new ByteArrayInputStream(out.toByteArray()));
        assertThat(reloaded.getAccounts().get(0).getUpdatedAt(), notNullValue());
    }
}
