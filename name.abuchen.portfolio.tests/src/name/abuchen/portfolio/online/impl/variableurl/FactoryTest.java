package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Iterator;

import org.junit.Test;

import name.abuchen.portfolio.online.impl.variableurl.urls.ConstURL;
import name.abuchen.portfolio.online.impl.variableurl.urls.DateURL;
import name.abuchen.portfolio.online.impl.variableurl.urls.PageURL;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;

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
    public void testTodayMacro()
    {
        VariableURL url = Factory.fromString("https://192.0.2.1/quotes.php?whatever={TODAY}");
        Iterator<String> iterator = url.iterator();
        assertThat(iterator.next(), is("https://192.0.2.1/quotes.php?whatever=" + LocalDate.now()));
        assertThat(iterator.hasNext(), is(false));
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
