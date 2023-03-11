package name.abuchen.portfolio.online;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final int SCALE = 20;
    
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

    private static final BigDecimal START_VALUE = new BigDecimal(100).multiply(Values.Quote.getBigDecimalFactor());

    private InterestRateType interestRateType;

    public InterestRateToSecurityPricesConverter(InterestRateType interestRateType)
    {
        this.interestRateType = interestRateType;
    }

    /**
     * See {@link #convert(List, Interval, Maturity)}, but the result is represented with BigDecimal.
     */
    public Collection<Pair<LocalDate, BigDecimal>> convertBigDecimal(List<Pair<LocalDate, BigDecimal>> interestRates, Interval interval,
                    Maturity maturity)
    {
        if (interestRates.isEmpty())
            return Collections.emptyList();

        Collections.sort(interestRates, (o1, o2) -> o1.getLeft().compareTo(o2.getLeft()));

        BigDecimal lastValue = START_VALUE;
        List<Pair<LocalDate, BigDecimal>> results = new ArrayList<>();
        results.add(new Pair<LocalDate, BigDecimal>(interestRates.get(0).getKey(), lastValue));

        for (int nextInterestRateIndex = 0; nextInterestRateIndex < interestRates.size() - 1; nextInterestRateIndex++)
        {
            BigDecimal overNightInterestRate = interestRates.get(nextInterestRateIndex).getRight();
            BigDecimal overNightReturn = BigDecimal.ZERO;

            switch (interestRateType)
            {
                case ACT_360:
                    overNightReturn = lastValue.multiply(overNightInterestRate.divide(new BigDecimal(360 * 100), SCALE, RoundingMode.HALF_EVEN));
                    break;
                default: // Necessary for checkstyle
                    assert false;
            }
            long days = ChronoUnit.DAYS.between(interestRates.get(nextInterestRateIndex).getLeft(),
                            interestRates.get(nextInterestRateIndex + 1).getLeft());

            lastValue = lastValue.add(new BigDecimal(days).multiply(overNightReturn));
            if (maturity != Maturity.OVER_NIGHT)
            {
                BigDecimal modifiedDuration = getModifiedDuration(maturity,
                                interestRates.get(nextInterestRateIndex).getRight().divide(new BigDecimal(100)))
                                .multiply(interestRates.get(nextInterestRateIndex).getRight()
                                                .subtract(interestRates.get(nextInterestRateIndex + 1).getRight()))
                                .divide(new BigDecimal(100));
                lastValue = lastValue.multiply(BigDecimal.ONE.add(modifiedDuration));
            }
            results.add(new Pair<LocalDate, BigDecimal>(interestRates.get(nextInterestRateIndex + 1).getLeft(), lastValue));
        }
        return results;
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
    public Collection<LatestSecurityPrice> convert(List<Pair<LocalDate, BigDecimal>> interestRates, Interval interval,
                    Maturity maturity)
    {
        return convertBigDecimal(interestRates, interval, maturity).stream()
                        .map(pair -> toLatestSecurityPrice(pair.getLeft(), pair.getRight())).toList();
    }

    public static LatestSecurityPrice toLatestSecurityPrice(LocalDate date, BigDecimal value)
    {
        return new LatestSecurityPrice(date, value.setScale(0, RoundingMode.HALF_UP).longValue(), LatestSecurityPrice.NOT_AVAILABLE,
                        LatestSecurityPrice.NOT_AVAILABLE, LatestSecurityPrice.NOT_AVAILABLE);
    }

    private static BigDecimal getModifiedDuration(Maturity maturity, BigDecimal interestRate)
    {
        BigDecimal macaulayDuration = getMacaulayDuration(maturity, interestRate);
        int paymentsPerYear = 0;
        switch (maturity)
        {
            case OVER_NIGHT:
                return BigDecimal.ZERO;
            case ONE_MONTH:
                paymentsPerYear = 12;
                break;
            case TWO_MONTHS:
                paymentsPerYear = 6;
                break;
            case THREE_MONTHS:
                paymentsPerYear = 4;
                break;
            case SIX_MONTHS:
                paymentsPerYear = 2;
                break;
            case ONE_YEAR:
            case TWO_YEARS:
            case THREE_YEARS:
            case FIVE_YEARS:
            case SEVEN_YEARS:
            case TEN_YEARS:
                paymentsPerYear = 1;
                break;
            default:
                assert false;
        }
        return macaulayDuration.divide(BigDecimal.ONE.add(
                        interestRate.divide(new BigDecimal(paymentsPerYear), SCALE, RoundingMode.HALF_EVEN)), SCALE, RoundingMode.HALF_EVEN);
    }

    /**
     * Calculates the Macaulay duration in years where we assume the following
     * coupon payments:
     * <p>
     * - for maturity one year or less: All interest rates are payed at maturity
     * - for maturity of two or more years: Interest rates are paid annually
     * (last payment at maturity)
     */
    private static BigDecimal getMacaulayDuration(Maturity maturity, BigDecimal interestRate)
    {
        int years = 0;
        switch (maturity)
        {
            case OVER_NIGHT:
                return BigDecimal.ZERO;
            case ONE_MONTH:
                return BigDecimal.ONE.divide(new BigDecimal(12), SCALE, RoundingMode.HALF_EVEN);
            case TWO_MONTHS:
                return BigDecimal.ONE.divide(new BigDecimal(6), SCALE, RoundingMode.HALF_EVEN);
            case THREE_MONTHS:
                return BigDecimal.ONE.divide(new BigDecimal(4));
            case SIX_MONTHS:
                return BigDecimal.ONE.divide(new BigDecimal(2));
            case ONE_YEAR:
                return BigDecimal.ONE;
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
        BigDecimal presentValueWeightedDuration = BigDecimal.ZERO;
        BigDecimal totalPresentValue = BigDecimal.ZERO;

        // Interest payments
        for (int i = 1; i <= years; i++) // 1-indexed loop!
        {
            BigDecimal thisCashflowPresentValue = interestRate.divide(interestRate.add(BigDecimal.ONE).pow(i), SCALE, RoundingMode.HALF_EVEN);
            totalPresentValue = totalPresentValue.add(thisCashflowPresentValue);
            presentValueWeightedDuration = presentValueWeightedDuration.add(thisCashflowPresentValue.multiply(new BigDecimal(i)));
        }

        // Payback in the end
        BigDecimal paybackPresentValue = BigDecimal.ONE.divide(BigDecimal.ONE.add(interestRate).pow(years), SCALE, RoundingMode.HALF_EVEN);
        totalPresentValue = totalPresentValue.add(paybackPresentValue);
        presentValueWeightedDuration = presentValueWeightedDuration.add(paybackPresentValue.multiply(new BigDecimal(years)));

        return presentValueWeightedDuration.divide(totalPresentValue, SCALE, RoundingMode.HALF_EVEN);
    }
}
