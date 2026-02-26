package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

public class PresetValuesTest
{
    @Before
    public void setUp()
    {
        PresetValues.resetLastTransactionDate();
    }

    @Test
    public void testGetLastTransactionDateWithNoStoredDate()
    {
        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(LocalDate.now()));
    }

    @Test
    public void testSetAndGetLastTransactionDate()
    {
        LocalDate testDate = LocalDate.now().minusMonths(3);
        PresetValues.setLastTransactionDate(testDate);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(testDate));
    }

    @Test
    public void testResetClearsStoredDate()
    {
        LocalDate testDate = LocalDate.now().minusMonths(3);
        PresetValues.setLastTransactionDate(testDate);

        PresetValues.resetLastTransactionDate();

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(LocalDate.now()));
    }

    @Test
    public void testMultipleSetGetCycles()
    {
        LocalDate date1 = LocalDate.now().minusMonths(6);
        PresetValues.setLastTransactionDate(date1);
        assertThat(PresetValues.getLastTransactionDate(), is(date1));

        LocalDate date2 = LocalDate.now().minusMonths(3);
        PresetValues.setLastTransactionDate(date2);
        assertThat(PresetValues.getLastTransactionDate(), is(date2));

        LocalDate date3 = LocalDate.now().minusMonths(1);
        PresetValues.setLastTransactionDate(date3);
        assertThat(PresetValues.getLastTransactionDate(), is(date3));
    }
}
