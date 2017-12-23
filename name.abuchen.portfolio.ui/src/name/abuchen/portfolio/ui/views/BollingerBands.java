// Source based on SimpleMovingAverage.java by alex0711 

package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

public class BollingerBands

{
    public static final int MIN_AVERAGE_PRICES_PER_WEEK = 2;
    private int BollingerBandsDays;
    private double BollingerBandsFactor;
    private Security security;
    private LocalDate startDate;
    private ChartLineSeriesAxes BollingerBandsLowerBand;
    private ChartLineSeriesAxes BollingerBandsMiddleBand;
    private ChartLineSeriesAxes BollingerBandsUpperBand;
    private int calculatedMinimumDays;
    private List<SecurityPrice> prices;
    private List<LocalDate> datesBollingerBands;
    private List<Double> valuesBollingerBandsLowerBands;
    private List<Double> valuesBollingerBandsMiddleBands;
    private List<Double> valuesBollingerBandsUpperBands;

    public BollingerBands(int BollingerBandsDays, double BollingerBandsFactor, Security security, LocalDate startDate)
    {
        this.BollingerBandsDays = BollingerBandsDays;
        this.BollingerBandsFactor = BollingerBandsFactor;
        this.security = security;
        this.startDate = startDate;
        this.BollingerBandsLowerBand = new ChartLineSeriesAxes();
        this.BollingerBandsMiddleBand = new ChartLineSeriesAxes();
        this.BollingerBandsUpperBand = new ChartLineSeriesAxes();
        this.datesBollingerBands = new ArrayList<>();
        this.valuesBollingerBandsLowerBands = new ArrayList<>();
        this.valuesBollingerBandsMiddleBands = new ArrayList<>();
        this.valuesBollingerBandsUpperBands = new ArrayList<>();
        this.calculatedMinimumDays = getMinimumDaysForBollingerBands();
        this.calculateBollingerBands();
    }

    /**
     * Returns the calculated Simple Moving Average
     * 
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated Bollinger Bands
     */
    public ChartLineSeriesAxes getLowerBands()
    {
        return this.BollingerBandsLowerBand;
    }

    public ChartLineSeriesAxes getMiddleBands()
    {
        return this.BollingerBandsMiddleBand;
    }

    public ChartLineSeriesAxes getUpperBands()
    {
        return this.BollingerBandsUpperBand;
    }

    public BollingerBands calculateBollingerBands(int BollingerBandsDays, double BollingerBandsFactor, Security security, LocalDate startDate)
    {
        return new BollingerBands(BollingerBandsDays, BollingerBandsFactor, security, startDate);
    }

    /**
     * Calculates the Simple Moving Average for the given range of days from the
     * given startDate on. The method returns an object containing the X and Y
     * Axes of the generated bollinger bands
     */
    private void calculateBollingerBands()
    {
        if (security == null)
            return;

        this.prices = security.getPricesIncludingLatest();
        int index;

        SecurityPrice startPrice = null;

        if (prices == null || prices.size() < BollingerBandsDays + 3)
            return;

        if (startDate == null)
        {
            startPrice = this.getStartPrice();
            // in case no valid start date could be determined, return null
            if (startPrice == null)
                return;
            index = prices.indexOf(startPrice);
            if (index >= prices.size())
                return;
        }
        else
        {
            startPrice = this.getStartPriceFromStartDate();
            // in case no valid start date could be determined, return null
            if (startPrice == null)
                return;
            index = prices.indexOf(startPrice);
            if (index >= prices.size())
                return;
        }

        for (; index < prices.size(); index++)
        {
            if (index < BollingerBandsDays) continue; 
            LocalDate nextDate = prices.get(index).getDate();
            LocalDate isBefore = nextDate.plusDays(1);
            LocalDate isAfter = prices.get(index - BollingerBandsDays + 2).getDate();
            List<SecurityPrice> filteredPrices = this.getFilteredList(isBefore, isAfter);

            if (filteredPrices.size() < calculatedMinimumDays)
                continue; // skip this date and try to calculate bollinger bands for next
                          // entry

            double sum = filteredPrices.stream().mapToLong(SecurityPrice::getValue).sum();
            double QuotePriceAverage = sum / Values.Quote.divider() / filteredPrices.size();
            double tempQuotePriceVariance = 0;
            for (int i = 0; i < filteredPrices.size(); i++)
            {
                tempQuotePriceVariance += ((filteredPrices.get(i).getValue() / Values.Quote.divider()) - QuotePriceAverage) * ((filteredPrices.get(i).getValue() / Values.Quote.divider()) - QuotePriceAverage);
            }

            Double StandardDeviationBollingerBands = Math.sqrt(tempQuotePriceVariance / (filteredPrices.size() - 1)) * BollingerBandsFactor;
            Double valueBollingerBandsLowerBands = QuotePriceAverage;
            Double valueBollingerBandsUpperBands = QuotePriceAverage;
            valueBollingerBandsLowerBands -= StandardDeviationBollingerBands;
            valueBollingerBandsUpperBands += StandardDeviationBollingerBands;
            
            valuesBollingerBandsLowerBands.add(valueBollingerBandsLowerBands);
            valuesBollingerBandsMiddleBands.add(QuotePriceAverage);
            valuesBollingerBandsUpperBands.add(valueBollingerBandsUpperBands);
            datesBollingerBands.add(prices.get(index).getDate());
        }
        LocalDate[] tmpDates = datesBollingerBands.toArray(new LocalDate[0]);
        Double[] tmpPricesLower = valuesBollingerBandsLowerBands.toArray(new Double[0]);
        Double[] tmpPricesMiddle = valuesBollingerBandsMiddleBands.toArray(new Double[0]);
        Double[] tmpPricesUpper = valuesBollingerBandsUpperBands.toArray(new Double[0]);

        this.BollingerBandsLowerBand.setDates(TimelineChart.toJavaUtilDate(tmpDates));
        this.BollingerBandsLowerBand.setValues(ArrayUtils.toPrimitive(tmpPricesLower));
        this.BollingerBandsMiddleBand.setDates(TimelineChart.toJavaUtilDate(tmpDates));
        this.BollingerBandsMiddleBand.setValues(ArrayUtils.toPrimitive(tmpPricesMiddle));
        this.BollingerBandsUpperBand.setDates(TimelineChart.toJavaUtilDate(tmpDates));
        this.BollingerBandsUpperBand.setValues(ArrayUtils.toPrimitive(tmpPricesUpper));
    }

    public int getMinimumDaysForBollingerBands()
    {
        int weeks = BollingerBandsDays / 7;
        int minDays = weeks * MIN_AVERAGE_PRICES_PER_WEEK;
        return minDays > 0 ? minDays : 1;
    }

    public SecurityPrice getStartPriceFromStartDate()
    {
        // get Date of first possible bollinger bands calculation beginning from startDate
        int index = Math.abs(
                        Collections.binarySearch(prices, new SecurityPrice(startDate, 0), new SecurityPrice.ByDate()));

        if (index >= prices.size())
            return null;
        return determineStartPrice(startDate);
    }

    public SecurityPrice getStartPrice()
    {
        // get Date of first possible bollinger bands calculation
        LocalDate BollingerBandsPeriodEnd = prices.get(0).getDate().plusDays(BollingerBandsDays - 1L);
        int index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(BollingerBandsPeriodEnd, 0),
                        new SecurityPrice.ByDate()));
        if (index >= prices.size())
            return null;

        return determineStartPrice(BollingerBandsPeriodEnd);
    }

    private SecurityPrice determineStartPrice(LocalDate BollingerBandsPeriodEnd)
    {
        // check if an bollinger bands can be calculated for this Date
        List<SecurityPrice> filteredPrices = null;
        LocalDate isBefore = BollingerBandsPeriodEnd.plusDays(1);
        LocalDate isAfter = BollingerBandsPeriodEnd.minusDays(BollingerBandsDays);
        LocalDate lastDate = prices.get(prices.size() - 1).getDate();
        filteredPrices = this.getFilteredList(isBefore, isAfter);

        int i = 1;
        while (!this.checkListIsValidForBollingerBands(filteredPrices))
        {
            if (isBefore.plusDays(i).isAfter(lastDate) || isAfter.plusDays(i).isAfter(lastDate))
                return null;

            filteredPrices = this.getFilteredList(isBefore.plusDays(i), isAfter.plusDays(i));
            i++;
        }

        return filteredPrices.get(filteredPrices.size() - 1);
    }

    private boolean checkListIsValidForBollingerBands(List<SecurityPrice> filteredPrices)
    {
        return filteredPrices.size() < calculatedMinimumDays ? false : true;
    }

    private List<SecurityPrice> getFilteredList(LocalDate isBefore, LocalDate isAfter)
    {
        return prices.stream().filter(p -> p.getDate().isAfter(isAfter) && p.getDate().isBefore(isBefore))
                        .collect(Collectors.toList());
    }

}