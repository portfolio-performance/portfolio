package name.abuchen.portfolio.datatransfer.csv;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.charset.UnsupportedCharsetException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class CSVConfigTest
{
    private static CSVConfig config(String encoding, String columns) throws ParseException
    {
        var config = new CSVConfig();
        config.fromJSON((JSONObject) JSONValue.parseWithException("""
                        {"label": "Test", "target": "account-transaction", "delimiter": ";",
                         "encoding": "%s", "skipLines": 0, "isFirstLineHeader": true, "columns": %s}
                        """.formatted(encoding, columns)));
        return config;
    }

    @Test(expected = UnsupportedCharsetException.class)
    public void testUnknownEncodingIsRejected() throws ParseException
    {
        config("NO-SUCH-CHARSET", "[{\"label\": \"Date\", \"field\": \"date\"}]");
    }

    @Test
    public void testColumnWithoutFormatGetsGuessedFormat() throws ParseException
    {
        var importer = new CSVImporter(new Client(), new File("unused.csv"));
        config("UTF-8", "[{\"label\": \"Date\", \"field\": \"date\"}]").writeTo(importer);

        assertNotNull(importer.getColumns()[0].getFormat());
    }
}
