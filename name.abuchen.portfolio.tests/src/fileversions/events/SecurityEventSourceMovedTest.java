package fileversions.events;

import static fileversions.FileHelper.find;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;

@SuppressWarnings("nls")
public class SecurityEventSourceMovedTest
{
    @Test
    public void testXMLFormat() throws IOException
    {
        Client client = ClientFactory.load(
                        SecurityEventSourceMovedTest.class.getResourceAsStream("client_with_security_events.xml"));

        assertEvents(client);
    }

    @Test
    public void testBinaryFormat() throws IOException
    {
        Client client = ClientFactory.load(find("client_with_security_events.portfolio"), null,
                        new NullProgressMonitor());

        assertEvents(client);

        // write and re-read the file (to ensure that the 'source' property is
        // moved)

        try (MockedStatic<Platform> mockedStatic = Mockito.mockStatic(Platform.class, invocation -> {
            if (invocation.getMethod().getName().equals("getOS"))
                return "junit";
            else
                return invocation.callRealMethod();
        }))
        {
            Path tempFile = Files.createTempFile("test", ".portfolio");
            ClientFactory.saveAs(client, tempFile.toFile(), null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
            Client rereadClient = ClientFactory.load(tempFile.toFile(), null, new NullProgressMonitor());
            assertEvents(rereadClient);
        }
    }

    private void assertEvents(Client client)
    {
        Security instrument1 = client.getSecurities().get(0);
        assertThat(instrument1.getEvents().size(), is(2));
        for (SecurityEvent event : instrument1.getEvents())
        {
            assertThat(event.getSource(), is(nullValue()));
        }

        Security instrument2 = client.getSecurities().get(1);
        assertThat(instrument2.getEvents().size(), is(21));
        for (SecurityEvent event : instrument2.getEvents())
        {
            assertThat(event.getSource(), is("divvydiary.com"));
        }
    }
}
