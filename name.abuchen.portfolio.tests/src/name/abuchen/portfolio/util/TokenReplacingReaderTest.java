//
// Thanks to Jakob Jenkov for his tutorial at
// http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
//

package name.abuchen.portfolio.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import name.abuchen.portfolio.util.TokenReplacingReader.ITokenResolver;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("nls")
public class TokenReplacingReaderTest
{

    public static class MockTokenResolver implements ITokenResolver
    {
        @Override
        public String resolveToken(String tokenName)
        {
            if ("token1".equals(tokenName))
                return "123";
            if ("token2".equals(tokenName))
                return "";

            return "";
        }
    }

    @Test
    public void test() throws IOException
    {
        String sourceString = "this is a ${token1} and ${token2}";
        Reader reader = new StringReader(sourceString);

        try (TokenReplacingReader tokenReplacingReader = new TokenReplacingReader(reader, new MockTokenResolver()))
        {
            Assert.assertEquals('t', (char) tokenReplacingReader.read());
            Assert.assertEquals('h', (char) tokenReplacingReader.read());
            Assert.assertEquals('i', (char) tokenReplacingReader.read());
            Assert.assertEquals('s', (char) tokenReplacingReader.read());
            Assert.assertEquals(' ', (char) tokenReplacingReader.read());
            Assert.assertEquals('i', (char) tokenReplacingReader.read());
            Assert.assertEquals('s', (char) tokenReplacingReader.read());
            Assert.assertEquals(' ', (char) tokenReplacingReader.read());
            Assert.assertEquals('a', (char) tokenReplacingReader.read());
            Assert.assertEquals(' ', (char) tokenReplacingReader.read());
            Assert.assertEquals('1', (char) tokenReplacingReader.read());
            Assert.assertEquals('2', (char) tokenReplacingReader.read());
            Assert.assertEquals('3', (char) tokenReplacingReader.read());
            Assert.assertEquals(' ', (char) tokenReplacingReader.read());
            Assert.assertEquals('a', (char) tokenReplacingReader.read());
            Assert.assertEquals('n', (char) tokenReplacingReader.read());
            Assert.assertEquals('d', (char) tokenReplacingReader.read());
            Assert.assertEquals(' ', (char) tokenReplacingReader.read());
            Assert.assertEquals(-1, tokenReplacingReader.read());
        }
    }
}
