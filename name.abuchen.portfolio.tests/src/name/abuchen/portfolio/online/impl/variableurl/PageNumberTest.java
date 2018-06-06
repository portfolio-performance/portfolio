package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Iterators;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("nls")
public class PageNumberTest
{
    @Test
    public void testMaxFailedAttempts()
    {
        assertThat(getURL().getMaxFailedAttempts(), equalTo(0L));
    }

    @Test
    public void testVariations()
    {
        List<String> variations = new LinkedList<>();
        Iterators.limit(getURL().iterator(), 4).forEachRemaining(variations::add);

        assertThat(variations, equalTo(Arrays.asList( //
                        "https://192.0.2.1/quotes.php?page=1", //
                        "https://192.0.2.1/quotes.php?page=2", //
                        "https://192.0.2.1/quotes.php?page=3", //
                        "https://192.0.2.1/quotes.php?page=4")));
    }

    private VariableURL getURL()
    {
        VariableURL variableURL = Factory.fromString("https://192.0.2.1/quotes.php?page={PAGE}");
        variableURL.setSecurity(new Security());
        return variableURL;
    }
}
