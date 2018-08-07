package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Iterators;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;

@SuppressWarnings("nls")
public class FormattedDateTest
{
    @Test
    public void testMaxFailedAttempts()
    {
        assertMaxFailedAttempts("https://192.0.2.1/quotes.php?day={DATE:yyyy-MM-dd}", 100);
        assertMaxFailedAttempts("https://192.0.2.1/quotes.php?month={DATE:yyyy-MM}", 4);
        assertMaxFailedAttempts("https://192.0.2.1/quotes.php?year={DATE:yyyy}", 1);
    }

    @Test
    public void testVariations()
    {
        LocalDate date = LocalDate.now();
        assertVariations(new Security(),
                        Arrays.asList(date, date.minusMonths(1), date.minusMonths(2), date.minusMonths(3)));

        Security security = new Security();
        date = LocalDate.of(2017, 1, 1);
        security.addPrice(new SecurityPrice(date, 1000));
        assertVariations(security, Arrays.asList(date, date.plusMonths(1), date.plusMonths(2), date.plusMonths(3)));
    }

    private void assertMaxFailedAttempts(String variableURL, long maxFailedAttempts)
    {
        VariableURL url = Factory.fromString(variableURL);
        url.setSecurity(new Security());
        assertThat(url.getMaxFailedAttempts(), equalTo(maxFailedAttempts));
    }

    private void assertVariations(Security security, List<LocalDate> dates)
    {
        VariableURL url = Factory.fromString("https://192.0.2.1/quotes.php?month={DATE:yyyy-MM}");
        url.setSecurity(security);

        List<String> variations = new LinkedList<>();
        Iterators.limit(url.iterator(), dates.size()).forEachRemaining(variations::add);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<String> expectedVariations = new LinkedList<>();
        for (LocalDate date : dates)
        {
            expectedVariations.add("https://192.0.2.1/quotes.php?month=" + formatter.format(date));
        }

        assertThat(variations, equalTo(expectedVariations));
    }
}
