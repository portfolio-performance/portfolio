package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Iterators;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("nls")
public class ConstStringTest
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
        Iterators.limit(getURL().iterator(), 2).forEachRemaining(variations::add);

        assertThat(variations, equalTo(Collections.singletonList("https://192.0.2.1/quotes.php")));
    }

    private VariableURL getURL()
    {
        VariableURL variableURL = Factory.fromString("https://192.0.2.1/quotes.php");
        variableURL.setSecurity(new Security());
        return variableURL;
    }
}
