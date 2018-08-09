package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.online.impl.variableurl.urls.ConstURL;
import name.abuchen.portfolio.online.impl.variableurl.urls.DateURL;
import name.abuchen.portfolio.online.impl.variableurl.urls.PageURL;

@SuppressWarnings("nls")
public class FactoryTest
{
    @Test
    public void testCreateConstString()
    {
        assertUrlType("https://192.0.2.1/quotes.php", ConstURL.class);
    }

    @Test
    public void testCreateFormattedDate()
    {
        assertUrlType("https://192.0.2.1/quotes.php?month={DATE:yyyy-MM}", DateURL.class);
    }

    @Test
    public void testCreatePageNumber()
    {
        assertUrlType("https://192.0.2.1/quotes.php?page={PAGE}", PageURL.class);
    }

    @Test
    public void testBadMacro()
    {
        assertUrlType("https://192.0.2.1/quotes.php?whatever={BAD_MACRO}", ConstURL.class);
    }

    @Test
    public void testMixedMacros()
    {
        assertUrlType("https://192.0.2.1/quotes.php?month={DATE:yyyy-MM}&page={PAGE}", ConstURL.class);
    }

    private void assertUrlType(String varUrl, Class<?> type)
    {
        assertThat(Factory.fromString(varUrl), instanceOf(type));
    }
}
