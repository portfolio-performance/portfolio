package name.abuchen.portfolio.online;

import java.time.LocalDate;
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

    private static final long START_VALUE = 100L * Values.Quote.factor();

    private InterestRateType interestRateType;

    public InterestRateToSecurityPricesConverter(InterestRateType interestRateType)
    {
        this.interestRateType = interestRateType;
    }

    public Collection<LatestSecurityPrice> convert(List<Pair<LocalDate, Double>> interestRates)
    {
        if (interestRates.isEmpty())
            return Collections.emptyList();

        Collections.sort(interestRates, (o1, o2) -> o1.getLeft().compareTo(o2.getLeft()));

        LocalDate lastDate = interestRates.get(0).getKey();
        long lastValue = START_VALUE;
        List<LatestSecurityPrice> results = new ArrayList<>();
        results.add(toLatestSecurityPrice(lastDate, lastValue));
        int nextInterestRateIndex = 0;
        do
        {
            while ((nextInterestRateIndex + 1) < interestRates.size()
                            && !interestRates.get(nextInterestRateIndex + 1).getLeft().isAfter(lastDate))
                nextInterestRateIndex++;

            double overNightInterestRate = interestRates.get(nextInterestRateIndex).getRight();
            long overNightReturn = 0;

            switch (interestRateType)
            {
                case ACT_360:
                    overNightReturn = Math.round(lastValue * overNightInterestRate / 36000);
                    break;
                default: // Necessary for checkstyle
            }

            lastDate = lastDate.plusDays(1);
            lastValue += overNightReturn;
            results.add(toLatestSecurityPrice(lastDate, lastValue));

        }
        while (nextInterestRateIndex + 1 < interestRates.size());
        return results;
    }

    private LatestSecurityPrice toLatestSecurityPrice(LocalDate date, long value)
    {
        return new LatestSecurityPrice(date, value, LatestSecurityPrice.NOT_AVAILABLE,
                        LatestSecurityPrice.NOT_AVAILABLE, LatestSecurityPrice.NOT_AVAILABLE);
    }
}
