package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.macros.Today;

@SuppressWarnings("nls")
public class TodayMacroTest
{

    @Test
    public void testToday()
    {
        Today today = new Today("TODAY");
        assertThat(today.resolve(new Security()), is(LocalDate.now().toString()));
    }

    @Test
    public void testTodayWithFormat()
    {
        Today today = new Today("TODAY:yyyy-MM-dd");
        assertThat(today.resolve(new Security()), is(LocalDate.now().toString()));
    }

    @Test
    public void testMinusOneYear()
    {
        Today today = new Today("TODAY:yyyy-MM-dd:-P1Y");
        assertThat(today.resolve(new Security()), is(LocalDate.now().minusYears(1).toString()));
    }

    @Test
    public void testPlusTwoYears()
    {
        Today today = new Today("TODAY:yyyy-MM-dd:P2Y");
        assertThat(today.resolve(new Security()), is(LocalDate.now().plusYears(2).toString()));
    }

    @Test
    public void testMinusTwoMonths()
    {
        Today today = new Today("TODAY:yyyy-MM-dd:-P2M");
        assertThat(today.resolve(new Security()), is(LocalDate.now().minusMonths(2).toString()));
    }
}
