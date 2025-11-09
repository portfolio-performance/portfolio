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
        // Reset static state before each test to avoid test pollution
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
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        PresetValues.setLastTransactionDate(testDate);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(testDate));
    }

    @Test
    public void testDateExactlyOneYearOld()
    {
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        PresetValues.setLastTransactionDate(oneYearAgo);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(oneYearAgo));
    }

    @Test
    public void testDateOlderThanOneYearReturnsCurrentDate()
    {
        LocalDate twoYearsAgo = LocalDate.now().minusYears(2);
        PresetValues.setLastTransactionDate(twoYearsAgo);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(LocalDate.now()));
    }

    @Test
    public void testResetClearsStoredDate()
    {
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        PresetValues.setLastTransactionDate(testDate);

        PresetValues.resetLastTransactionDate();

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(LocalDate.now()));
    }

    @Test
    public void testMultipleSetGetCycles()
    {
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        PresetValues.setLastTransactionDate(date1);
        assertThat(PresetValues.getLastTransactionDate(), is(date1));

        LocalDate date2 = LocalDate.of(2024, 2, 20);
        PresetValues.setLastTransactionDate(date2);
        assertThat(PresetValues.getLastTransactionDate(), is(date2));

        LocalDate date3 = LocalDate.of(2024, 3, 30);
        PresetValues.setLastTransactionDate(date3);
        assertThat(PresetValues.getLastTransactionDate(), is(date3));
    }

    @Test
    public void testRecentDateIsRetained()
    {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        PresetValues.setLastTransactionDate(yesterday);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(yesterday));
    }

    @Test
    public void testDateOneYearMinusOneDayIsRetained()
    {
        LocalDate almostOneYearAgo = LocalDate.now().minusYears(1).plusDays(1);
        PresetValues.setLastTransactionDate(almostOneYearAgo);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(almostOneYearAgo));
    }

    @Test
    public void testDateOneYearPlusOneDayReturnsCurrentDate()
    {
        LocalDate justOverOneYearAgo = LocalDate.now().minusYears(1).minusDays(1);
        PresetValues.setLastTransactionDate(justOverOneYearAgo);

        LocalDate result = PresetValues.getLastTransactionDate();
        assertThat(result, is(LocalDate.now()));
    }
}
