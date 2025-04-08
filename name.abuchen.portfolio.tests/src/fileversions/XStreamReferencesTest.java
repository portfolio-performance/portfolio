package fileversions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ClientTestUtilities;

@SuppressWarnings("nls")
public class XStreamReferencesTest
{
    @Test
    public void testReadingWithReferencesAndWritingWithoutReferences() throws IOException
    {
        Client withReferences = ClientFactory
                        .load(XStreamReferencesTest.class.getResourceAsStream("client_with_relative_references.xml"));

        // check that the first two securities have the same latest price object
        // (is a reference in the XML file)

        assertThat(withReferences.getSecurities().get(0).getLatest() == withReferences.getSecurities().get(1)
                        .getLatest(), is(true));

        String xml = ClientTestUtilities.toString(withReferences);
        Client without = ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThat(without.getSecurities().get(0).getLatest() == without.getSecurities().get(1).getLatest(), is(false));
    }

    @Test
    public void testReadingWithIdReferencesAndWritingWithoutIdReferences() throws IOException
    {
        Client withReferences = ClientFactory
                        .load(XStreamReferencesTest.class.getResourceAsStream("client_with_id_references.xml"), true);

        // check that the first two securities have the same latest price object
        // (is a reference with "id" attribute in the XML file)

        assertThat(withReferences.getSecurities().get(0).getLatest() == withReferences.getSecurities().get(1)
                        .getLatest(), is(true));

        String xml = ClientTestUtilities.toString(withReferences, true);
        Client without = ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThat(without.getSecurities().get(0).getLatest() == without.getSecurities().get(1).getLatest(), is(false));
    }
}
