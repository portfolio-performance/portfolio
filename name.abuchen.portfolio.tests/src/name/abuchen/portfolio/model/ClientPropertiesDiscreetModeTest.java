package name.abuchen.portfolio.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Issue #5685: the discreet mode must be kept when the file is closed and
 * reopened, hence it needs to be part of the file.
 */
@SuppressWarnings("nls")
public class ClientPropertiesDiscreetModeTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testNewFileHasNoStoredDiscreetMode()
    {
        ClientProperties properties = new ClientProperties(new Client());

        // without a stored value the application keeps its own discreet mode
        assertFalse(properties.hasDiscreetMode());
        assertFalse(properties.isDiscreetMode());
    }

    @Test
    public void testDiscreetModeSurvivesSaveAndLoad() throws IOException
    {
        Client client = new Client();
        ClientProperties properties = new ClientProperties(client);

        properties.setDiscreetMode(true);
        assertTrue(properties.hasDiscreetMode());
        assertTrue(properties.isDiscreetMode());

        Client read = saveAndRead(client);

        ClientProperties readProperties = new ClientProperties(read);
        assertTrue(readProperties.hasDiscreetMode());
        assertTrue(readProperties.isDiscreetMode());
        assertEquals(Boolean.toString(true), read.getProperty(ClientProperties.Keys.DISCREET_MODE));
    }

    @Test
    public void testDeactivatedDiscreetModeSurvivesSaveAndLoad() throws IOException
    {
        Client client = new Client();
        ClientProperties properties = new ClientProperties(client);

        properties.setDiscreetMode(false);
        assertTrue(properties.hasDiscreetMode());
        assertFalse(properties.isDiscreetMode());

        ClientProperties readProperties = new ClientProperties(saveAndRead(client));

        // an explicitly deactivated mode must not be ignored when reading the
        // file, otherwise the file would be reopened with hidden amounts
        assertTrue(readProperties.hasDiscreetMode());
        assertFalse(readProperties.isDiscreetMode());
    }

    private Client saveAndRead(Client client) throws IOException
    {
        File file = tempFolder.newFile("test.xml");
        ClientFactory.save(client, file);
        return ClientFactory.load(file, null, new NullProgressMonitor());
    }
}
