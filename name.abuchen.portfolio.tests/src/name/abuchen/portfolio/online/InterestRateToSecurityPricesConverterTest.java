package name.abuchen.portfolio.online;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.InterestRateType;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.Interval;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.Maturity;
import name.abuchen.portfolio.util.Pair;

public class InterestRateToSecurityPricesConverterTest
{
    private static final List<Pair<LocalDate, Double>> gaplessData;
    static
    {
        gaplessData = new ArrayList<>();
        gaplessData.add(new Pair<>(LocalDate.of(2021, 01, 01), 1d));
        gaplessData.add(new Pair<>(LocalDate.of(2021, 01, 02), 0d));
        gaplessData.add(new Pair<>(LocalDate.of(2021, 01, 03), -1d));
        gaplessData.add(new Pair<>(LocalDate.of(2021, 01, 04), -.5d));
    }
    private static final List<Pair<LocalDate, Double>> dataWithGap;
    static
    {
        dataWithGap = new ArrayList<>();
        dataWithGap.add(new Pair<>(LocalDate.of(2021, 01, 01), .1d));
        dataWithGap.add(new Pair<>(LocalDate.of(2021, 01, 04), -.123d));
        dataWithGap.add(new Pair<>(LocalDate.of(2021, 01, 05), -.4565d));
    }
    private static final List<Pair<LocalDate, Double>> monthlyConstantInterestRate;
    static
    {
        monthlyConstantInterestRate = new ArrayList<>();
        monthlyConstantInterestRate.add(new Pair<>(LocalDate.of(2021, 01, 01), 1d));
        monthlyConstantInterestRate.add(new Pair<>(LocalDate.of(2021, 02, 01), 1d));
        monthlyConstantInterestRate.add(new Pair<>(LocalDate.of(2021, 03, 01), 1d));
        monthlyConstantInterestRate.add(new Pair<>(LocalDate.of(2021, 04, 01), 1d));
    }
    private static final List<Pair<LocalDate, Double>> monthlyChangingInterestRate;
    static
    {
        monthlyChangingInterestRate = new ArrayList<>();
        monthlyChangingInterestRate.add(new Pair<>(LocalDate.of(2021, 01, 01), 1d));
        monthlyChangingInterestRate.add(new Pair<>(LocalDate.of(2021, 02, 01), 0d));
        monthlyChangingInterestRate.add(new Pair<>(LocalDate.of(2021, 03, 01), 2d));
        monthlyChangingInterestRate.add(new Pair<>(LocalDate.of(2021, 04, 01), 1d));
    }
    private static final double MAX_ERROR = 0.001d / 36000d;

    @Test
    public void testAct360Empty()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(Collections.emptyList(), Interval.DAILY, Maturity.OVER_NIGHT);
        assertEquals(0, result.size()); // NOSONAR
    }

    @Test
    public void testAct360GaplessData()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(gaplessData, Interval.DAILY, Maturity.OVER_NIGHT);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 01, 02), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 01, 03), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 01, 04), resultList.get(3).getLeft());
        //assertEquals(LocalDate.of(2021, 01, 05), resultList.get(4).getLeft());
        assertEquals(1d / 36000d, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(-1d / 36000d, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
        //assertEquals(-.5d / 36000d, ((double) (resultList.get(4).getRight() - resultList.get(3).getRight()))
        //                / resultList.get(3).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360DataWithGap()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(dataWithGap, Interval.DAILY, Maturity.OVER_NIGHT);
        assertEquals(3, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 01, 04), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 01, 05), resultList.get(2).getLeft());
        assertEquals(.1d / 36000d * 3d, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(-.123d / 36000d, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstantOverNight()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.OVER_NIGHT);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant1Month()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.ONE_MONTH);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant2Months()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.TWO_MONTHS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant3Months()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.THREE_MONTHS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant6Months()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.SIX_MONTHS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant1Year()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.ONE_YEAR);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant2Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.TWO_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant3Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.THREE_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant5Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.FIVE_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant7Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.SEVEN_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyConstant10Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyConstantInterestRate, Interval.MONTHLY, Maturity.TEN_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChangingOverNight()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.OVER_NIGHT);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging1Month()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate,
                        Interval.MONTHLY, Maturity.ONE_MONTH);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d/12d/(1d + 0.01d/12d))
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d/12d/(1d + 0.00d/12d))
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d/12d/(1d + 0.02d/12d))
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging2Months()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.TWO_MONTHS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d/6d/(1d + 0.01d/6d))
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d/6d/(1d + 0.00d/6d))
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d/6d/(1d + 0.02d/6d))
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging3Months()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.THREE_MONTHS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d/4d/(1d + 0.01d/4d))
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d/4d/(1d + 0.00d/4d))
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d/4d/(1d + 0.02d/4d))
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging6Months()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.SIX_MONTHS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d/2d/(1d + 0.01d/2d))
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d/2d/(1d + 0.00d/2d))
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d/2d/(1d + 0.02d/2d))
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging1Year()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.ONE_YEAR);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01/1.01)
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02/1d)
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight()/ (1 + 0.01/1.02)
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging2Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.TWO_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d * 1.99009901/1.01) 
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d * 2d/1d)
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d * 1.980392157/1.02)
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging3Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.THREE_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d * 2.970395059/1.01)
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d * 3d/1d)
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d * 2.941560938/1.02)
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging5Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.FIVE_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d * 4.901965552/1.01)
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d * 5d/1d)
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d * 4.807728699/1.02)
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging7Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.SEVEN_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d * 6.795476475/1.01)
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d * 7d/1d)
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d * 6.601430891/1.02)
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360MonthlyChanging10Yeras()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(monthlyChangingInterestRate, Interval.MONTHLY, Maturity.TEN_YEARS);
        assertEquals(4, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 02, 01), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 03, 01), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 04, 01), resultList.get(3).getLeft());
        assertEquals(1d / 36000d * 31, ((double) (resultList.get(1).getRight() / (1 + 0.01d * 9.566017576/1.01)
                        - resultList.get(0).getRight())) / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d * 28, ((double) (resultList.get(2).getRight() / (1 - 0.02d * 10d/1d)
                        - resultList.get(1).getRight())) / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(2d / 36000d * 31, ((double) (resultList.get(3).getRight() / (1 + 0.01d * 9.162236706/1.02)
                        - resultList.get(2).getRight())) / resultList.get(2).getRight(), MAX_ERROR);
    }

    private List<Pair<LocalDate, Long>> toListAndCheck(Collection<LatestSecurityPrice> collection)
    {
        List<Pair<LocalDate, Long>> result = new ArrayList<>();
        for (LatestSecurityPrice latestSecurityPrice : collection)
        {
            assertEquals(LatestSecurityPrice.NOT_AVAILABLE, latestSecurityPrice.getHigh());
            assertEquals(LatestSecurityPrice.NOT_AVAILABLE, latestSecurityPrice.getLow());
            assertEquals(LatestSecurityPrice.NOT_AVAILABLE, latestSecurityPrice.getVolume());

            result.add(new Pair<>(latestSecurityPrice.getDate(), latestSecurityPrice.getValue()));
        }
        Collections.sort(result, (o1, o2) -> o1.getLeft().compareTo(o2.getLeft()));
        return result;
    }
}
