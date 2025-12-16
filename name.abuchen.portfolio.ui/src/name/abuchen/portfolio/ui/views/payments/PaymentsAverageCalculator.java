package name.abuchen.portfolio.ui.views.payments;

/**
 * Utility class for calculating average payments per time period.
 */
public class PaymentsAverageCalculator
{
    private PaymentsAverageCalculator()
    {
        // Utility class, no instances
    }

    /**
     * Calculates the average payment per year.
     *
     * @param totalSum
     *            The total sum of all payments
     * @param noOfMonths
     *            The number of months covered
     * @return The average payment per year, or 0 if noOfMonths is 0
     */
    public static long calculateAveragePerYear(long totalSum, int noOfMonths)
    {
        if (noOfMonths == 0)
            return 0L;

        return Math.round(totalSum / (noOfMonths / 12.0));
    }

    /**
     * Calculates the average payment per quarter.
     *
     * @param totalSum
     *            The total sum of all payments
     * @param noOfMonths
     *            The number of months covered
     * @return The average payment per quarter, or 0 if noOfMonths is 0
     */
    public static long calculateAveragePerQuarter(long totalSum, int noOfMonths)
    {
        if (noOfMonths == 0)
            return 0L;

        return Math.round(totalSum / (noOfMonths / 3.0));
    }

    /**
     * Calculates the average payment per month.
     *
     * @param totalSum
     *            The total sum of all payments
     * @param noOfMonths
     *            The number of months covered
     * @return The average payment per month, or 0 if noOfMonths is 0
     */
    public static long calculateAveragePerMonth(long totalSum, int noOfMonths)
    {
        if (noOfMonths == 0)
            return 0L;

        return Math.round(totalSum / (double) noOfMonths);
    }
}
