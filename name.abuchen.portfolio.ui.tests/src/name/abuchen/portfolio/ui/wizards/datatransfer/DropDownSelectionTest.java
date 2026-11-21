package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class DropDownSelectionTest
{
    private static final List<String> PORTFOLIOS = List.of("Depot A", "Depot B", "Depot C");

    @Test
    public void testPreviousElementIsPreselected()
    {
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, "Depot C"::equals, "Depot A"), is(2));
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, "Depot C"::equals, null), is(2));
    }

    @Test
    public void testFirstDifferentElementWithoutPreviousElement()
    {
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, e -> false, "Depot A"), is(1));
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, e -> false, "Depot B"), is(0));
    }

    @Test
    public void testPreviousElementIsIgnoredIfItEqualsTheOtherSelection()
    {
        // e.g. the portfolio was changed to the previous offset portfolio
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, "Depot A"::equals, "Depot A"), is(1));
    }

    @Test
    public void testPreviousElementNoLongerAvailable()
    {
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, "Depot X"::equals, "Depot A"), is(1));
    }

    @Test
    public void testFirstElementWithoutOtherSelection()
    {
        // e.g. a transfer between two currencies: any account is valid
        assertThat(DropDownSelection.indexOf(PORTFOLIOS, e -> false, null), is(0));
    }

    @Test
    public void testOnlyOneElement()
    {
        // the transfer is marked as error, which is correct with only one
        // portfolio
        assertThat(DropDownSelection.indexOf(List.of("Depot A"), e -> false, "Depot A"), is(0));
    }

    @Test
    public void testNoElements()
    {
        assertThat(DropDownSelection.indexOf(List.of(), e -> true, "Depot A"), is(-1));
    }
}
