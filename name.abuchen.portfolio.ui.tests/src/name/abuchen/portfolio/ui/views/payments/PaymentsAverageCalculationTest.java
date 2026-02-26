package name.abuchen.portfolio.ui.views.payments;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link PaymentsAverageCalculator} to ensure correct average
 * calculations across different time periods.
 */
@SuppressWarnings("nls")
public class PaymentsAverageCalculationTest
{
    /**
     * Test average per year calculation with 13 months of data. Expected: 1200
     * (which is 100 per month × 12)
     */
    @Test
    public void testAveragePerYearWith13Months()
    {
        long totalSum = 1300L; // 13 months × 100 per month
        int noOfMonths = 13;

        long average = PaymentsAverageCalculator.calculateAveragePerYear(totalSum, noOfMonths);

        assertEquals("Average should be 1200 per year (100/month × 12)", 1200L, average);
    }

    /**
     * Test average per year calculation with exactly 12 months.
     */
    @Test
    public void testAveragePerYearWith12Months()
    {
        long totalSum = 1200L; // 12 months × 100 per month
        int noOfMonths = 12;

        long average = PaymentsAverageCalculator.calculateAveragePerYear(totalSum, noOfMonths);

        assertEquals("Average should be 1200 per year", 1200L, average);
    }

    /**
     * Test average per year calculation with 25 months of data.
     */
    @Test
    public void testAveragePerYearWith25Months()
    {
        long totalSum = 2500L; // 25 months × 100 per month
        int noOfMonths = 25;

        long average = PaymentsAverageCalculator.calculateAveragePerYear(totalSum, noOfMonths);

        assertEquals("Average should be 1200 per year (100/month × 12)", 1200L, average);
    }

    /**
     * Test average per year calculation with 1 month of data.
     */
    @Test
    public void testAveragePerYearWith1Month()
    {
        long totalSum = 100L;
        int noOfMonths = 1;

        long average = PaymentsAverageCalculator.calculateAveragePerYear(totalSum, noOfMonths);

        assertEquals("Average should be 1200 per year (100/month × 12)", 1200L, average);
    }

    /**
     * Test average per quarter calculation with 13 months of data. Expected:
     * 300 (which is 100 per month × 3)
     */
    @Test
    public void testAveragePerQuarterWith13Months()
    {
        long totalSum = 1300L; // 13 months × 100 per month
        int noOfMonths = 13;

        long average = PaymentsAverageCalculator.calculateAveragePerQuarter(totalSum, noOfMonths);

        assertEquals("Average should be 300 per quarter (100/month × 3)", 300L, average);
    }

    /**
     * Test average per quarter calculation with exactly 12 months.
     */
    @Test
    public void testAveragePerQuarterWith12Months()
    {
        long totalSum = 1200L; // 12 months × 100 per month
        int noOfMonths = 12;

        long average = PaymentsAverageCalculator.calculateAveragePerQuarter(totalSum, noOfMonths);

        assertEquals("Average should be 300 per quarter", 300L, average);
    }

    /**
     * Test average per quarter calculation with 7 months of data.
     */
    @Test
    public void testAveragePerQuarterWith7Months()
    {
        long totalSum = 700L; // 7 months × 100 per month
        int noOfMonths = 7;

        long average = PaymentsAverageCalculator.calculateAveragePerQuarter(totalSum, noOfMonths);

        assertEquals("Average should be 300 per quarter (100/month × 3)", 300L, average);
    }

    /**
     * Test average per month calculation.
     */
    @Test
    public void testAveragePerMonth()
    {
        long totalSum = 1300L;
        int noOfMonths = 13;

        long average = PaymentsAverageCalculator.calculateAveragePerMonth(totalSum, noOfMonths);

        assertEquals("Average per month should be 100", 100L, average);
    }

    /**
     * Test division by zero protection for year calculation.
     */
    @Test
    public void testAveragePerYearWithZeroMonths()
    {
        long totalSum = 0L;
        int noOfMonths = 0;

        long average = PaymentsAverageCalculator.calculateAveragePerYear(totalSum, noOfMonths);

        assertEquals("Should return 0 for zero months", 0L, average);
    }

    /**
     * Test division by zero protection for quarter calculation.
     */
    @Test
    public void testAveragePerQuarterWithZeroMonths()
    {
        long totalSum = 0L;
        int noOfMonths = 0;

        long average = PaymentsAverageCalculator.calculateAveragePerQuarter(totalSum, noOfMonths);

        assertEquals("Should return 0 for zero months", 0L, average);
    }

    /**
     * Test division by zero protection for month calculation.
     */
    @Test
    public void testAveragePerMonthWithZeroMonths()
    {
        long totalSum = 0L;
        int noOfMonths = 0;

        long average = PaymentsAverageCalculator.calculateAveragePerMonth(totalSum, noOfMonths);

        assertEquals("Should return 0 for zero months", 0L, average);
    }
}
