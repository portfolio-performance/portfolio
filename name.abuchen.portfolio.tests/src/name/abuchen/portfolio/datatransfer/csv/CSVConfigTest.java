package name.abuchen.portfolio.datatransfer.csv;

import java.nio.charset.UnsupportedCharsetException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.Test;

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
        config("NO-SUCH-CHARSET", "[]");
    }
}
