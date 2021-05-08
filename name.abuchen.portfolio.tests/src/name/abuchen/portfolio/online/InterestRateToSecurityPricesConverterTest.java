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
    private static final double MAX_ERROR = 0.001d / 36000d;

    @Test
    public void testAct360Empty()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(Collections.emptyList());
        assertEquals(0, result.size()); // NOSONAR
    }

    @Test
    public void testAct360GaplessData()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(gaplessData);
        assertEquals(5, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 01, 02), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 01, 03), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 01, 04), resultList.get(3).getLeft());
        assertEquals(LocalDate.of(2021, 01, 05), resultList.get(4).getLeft());
        assertEquals(1d / 36000d, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(0d / 36000d, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(-1d / 36000d, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
        assertEquals(-.5d / 36000d, ((double) (resultList.get(4).getRight() - resultList.get(3).getRight()))
                        / resultList.get(3).getRight(), MAX_ERROR);
    }

    @Test
    public void testAct360DataWithGap()
    {
        InterestRateToSecurityPricesConverter converter = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360);

        Collection<LatestSecurityPrice> result = converter.convert(dataWithGap);
        assertEquals(6, result.size());

        List<Pair<LocalDate, Long>> resultList = toListAndCheck(result);
        assertEquals(LocalDate.of(2021, 01, 01), resultList.get(0).getLeft());
        assertEquals(LocalDate.of(2021, 01, 02), resultList.get(1).getLeft());
        assertEquals(LocalDate.of(2021, 01, 03), resultList.get(2).getLeft());
        assertEquals(LocalDate.of(2021, 01, 04), resultList.get(3).getLeft());
        assertEquals(LocalDate.of(2021, 01, 05), resultList.get(4).getLeft());
        assertEquals(LocalDate.of(2021, 01, 06), resultList.get(5).getLeft());
        assertEquals(.1d / 36000d, ((double) (resultList.get(1).getRight() - resultList.get(0).getRight()))
                        / resultList.get(0).getRight(), MAX_ERROR);
        assertEquals(.1d / 36000d, ((double) (resultList.get(2).getRight() - resultList.get(1).getRight()))
                        / resultList.get(1).getRight(), MAX_ERROR);
        assertEquals(.1d / 36000d, ((double) (resultList.get(3).getRight() - resultList.get(2).getRight()))
                        / resultList.get(2).getRight(), MAX_ERROR);
        assertEquals(-.123d / 36000d, ((double) (resultList.get(4).getRight() - resultList.get(3).getRight()))
                        / resultList.get(3).getRight(), MAX_ERROR);
        assertEquals(-.456d / 36000d, ((double) (resultList.get(5).getRight() - resultList.get(4).getRight()))
                        / resultList.get(4).getRight(), MAX_ERROR);
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
