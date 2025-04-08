package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.money.Values;

public class ProtobufWriterAdditionalTest
{

    @Test
    public void testSavingFutureDates() throws IOException
    {
        Client client = new Client();
        Security security = new Security();
        security.setName(ProtobufWriterAdditionalTest.class.getName());
        security.setUpdatedAt(Instant.now());
        security.addPrice(new SecurityPrice(LocalDate.now().plusDays(10), Values.Quote.factorize(100)));
        client.addSecurity(security);

        // convert to binary format and back

        ProtobufWriter protobufWriter = new ProtobufWriter();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        ByteArrayInputStream in = new ByteArrayInputStream(stream.toByteArray());
        Client newClient = protobufWriter.load(in);

        String expected = ClientTestUtilities.toString(client);
        String actual = ClientTestUtilities.toString(newClient);

        assertThat(actual, is(expected));
    }
}
