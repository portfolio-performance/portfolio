package name.abuchen.portfolio.online;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Pair;

public class InterestRateToSecurityPricesConverter
{
    public enum InterestRateType
    {
        /**
         * act/360: On each actual date (i.e. 365 or 366 times per year), apply
         * an interest rate that is 1/360 of the annualized interest rate.
         * Typically used in the Euro area.
         */
        ACT_360;
    }

    public enum Maturity
    {
        OVER_NIGHT, ONE_MONTH, TWO_MONTHS, THREE_MONTHS, SIX_MONTHS, ONE_YEAR, TWO_YEARS, THREE_YEARS, FIVE_YEARS, SEVEN_YEARS, TEN_YEARS;
    }

    public enum Interval
    {
        DAILY, MONTHLY
    }

    private static final long START_VALUE = 100L * Values.Quote.factor();

    private InterestRateType interestRateType;

    public InterestRateToSecurityPricesConverter(InterestRateType interestRateType)
    {
        this.interestRateType = interestRateType;
    }

    /**
     * Calculate security prices based on interest rates. This method assumes
     * that interest is paid every day at midnight with the latest interest rate
     * from {@code interestRates}. It produces one security price per interest
     * rate in {@code interestRates}. Price changes of fixed price securities
     * are taken into account using the modified duration. The last interest
     * rate is ignored (only the day is used to compute the last price).
     * 
     * @param interestRates
     *            Annualized interest rates as percent (i.e. for 1.5% p.a. the
     *            double {@code 1.5d}).
     */
    public Collection<LatestSecurityPrice> convert(List<Pair<LocalDate, Double>> interestRates, Interval interval,
                    Maturity maturity)
    {
        if (interestRates.isEmpty())
            return Collections.emptyList();

        Collections.sort(interestRates, (o1, o2) -> o1.getLeft().compareTo(o2.getLeft()));

        long lastValue = START_VALUE;
        List<LatestSecurityPrice> results = new ArrayList<>();
        results.add(toLatestSecurityPrice(interestRates.get(0).getKey(), lastValue));

        for (int nextInterestRateIndex = 0; nextInterestRateIndex < interestRates.size() - 1; nextInterestRateIndex++)
        {
            double overNightInterestRate = interestRates.get(nextInterestRateIndex).getRight();
            long overNightReturn = 0;

            switch (interestRateType)
            {
                case ACT_360:
                    overNightReturn = Math.round(lastValue * overNightInterestRate / 36000d);
                    break;
                default: // Necessary for checkstyle
                    assert false;
            }
            long days = ChronoUnit.DAYS.between(interestRates.get(nextInterestRateIndex).getLeft(),
                            interestRates.get(nextInterestRateIndex + 1).getLeft());

            lastValue += days * overNightReturn;
            if (maturity != Maturity.OVER_NIGHT)
            {
                double modifiedDuration = getModifiedDuration(maturity,
                                interestRates.get(nextInterestRateIndex).getRight() / 100d)
                                * (interestRates.get(nextInterestRateIndex).getRight()
                                                - interestRates.get(nextInterestRateIndex + 1).getRight())
                                / 100d;
                lastValue *= (1d + modifiedDuration);
            }
            results.add(toLatestSecurityPrice(interestRates.get(nextInterestRateIndex + 1).getLeft(), lastValue));
        }
        return results;
    }

    private LatestSecurityPrice toLatestSecurityPrice(LocalDate date, long value)
    {
        return new LatestSecurityPrice(date, value, LatestSecurityPrice.NOT_AVAILABLE,
                        LatestSecurityPrice.NOT_AVAILABLE, LatestSecurityPrice.NOT_AVAILABLE);
    }

    private static double getModifiedDuration(Maturity maturity, double interestRate)
    {
        double macaulayDuration = getMacaulayDuration(maturity, interestRate);
        double paymentsPerYear = 0;
        switch (maturity)
        {
            case OVER_NIGHT:
                return 0;
            case ONE_MONTH:
                paymentsPerYear = 12d;
                break;
            case TWO_MONTHS:
                paymentsPerYear = 6d;
                break;
            case THREE_MONTHS:
                paymentsPerYear = 4d;
                break;
            case SIX_MONTHS:
                paymentsPerYear = 2d;
                break;
            case ONE_YEAR:
            case TWO_YEARS:
            case THREE_YEARS:
            case FIVE_YEARS:
            case SEVEN_YEARS:
            case TEN_YEARS:
                paymentsPerYear = 1d;
                break;
            default:
                assert false;
        }
        return macaulayDuration / (1 + interestRate / paymentsPerYear);
    }

    /**
     * Calculates the Macaulay duration in years where we assume the following
     * coupon payments:
     * <p>
     * - for maturity one year or less: All interest rates are payed at maturity
     * - for maturity of two or more years: Interest rates are paid annually
     * (last payment at maturity)
     */
    private static double getMacaulayDuration(Maturity maturity, double interestRate)
    {
        int years = 0;
        switch (maturity)
        {
            case OVER_NIGHT:
                return 0;
            case ONE_MONTH:
                return 1d / 12d;
            case TWO_MONTHS:
                return 1d / 6d;
            case THREE_MONTHS:
                return 1d / 4d;
            case SIX_MONTHS:
                return 1d / 2d;
            case ONE_YEAR:
                return 1d;
            case TWO_YEARS:
                years = 2;
                break;
            case THREE_YEARS:
                years = 3;
                break;
            case FIVE_YEARS:
                years = 5;
                break;
            case SEVEN_YEARS:
                years = 7;
                break;
            case TEN_YEARS:
                years = 10;
                break;
            default:
                assert false;
        }
        double presentValueWeightedDuration = 0d;
        double totalPresentValue = 0d;

        // Interest payments
        for (int i = 1; i <= years; i++) // 1-indexed loop!
        {
            double thisCashflowPresentValue = interestRate / Math.pow(1 + interestRate, i);
            totalPresentValue += thisCashflowPresentValue;
            presentValueWeightedDuration += thisCashflowPresentValue * i;
        }

        // Payback in the end
        double paybackPresentValue = 1 / Math.pow(1 + interestRate, years);
        totalPresentValue += paybackPresentValue;
        presentValueWeightedDuration += paybackPresentValue * years;

        return presentValueWeightedDuration / totalPresentValue;
    }
}
