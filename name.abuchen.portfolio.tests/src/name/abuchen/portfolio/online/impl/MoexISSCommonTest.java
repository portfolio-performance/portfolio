package name.abuchen.portfolio.online.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;

@SuppressWarnings("nls")
public class MoexISSCommonTest
{
    private final MoexISSCommon parser = new MoexISSCommon();

    @Test
    public void testFaceValueAppliedPerHistoryRow()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"LOW\",\"HIGH\",\"VOLUME\",\"FACEVALUE\"],"
                        + "\"data\":[[\"2024-01-01\",98.56,97.50,99.00,123456,1000],"
                        + "[\"2024-01-02\",98.56,97.50,99.00,234567,740]]}}";

        List<LatestSecurityPrice> prices = parser.parse(json, true);

        assertEquals(2, prices.size());
        assertEquals(98560000000L, prices.get(0).getValue());
        assertEquals(97500000000L, prices.get(0).getLow());
        assertEquals(99000000000L, prices.get(0).getHigh());
        assertEquals(123456L, prices.get(0).getVolume());
        assertEquals(72934400000L, prices.get(1).getValue());
        assertEquals(72150000000L, prices.get(1).getLow());
        assertEquals(73260000000L, prices.get(1).getHigh());
        assertEquals(234567L, prices.get(1).getVolume());
    }

    @Test
    public void testDecimalFaceValue()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"FACEVALUE\"],"
                        + "\"data\":[[\"2024-01-01\",98.56,214.4]]}}";

        List<LatestSecurityPrice> prices = parser.parse(json, true);

        assertEquals(1, prices.size());
        assertEquals(21131264000L, prices.get(0).getValue());
    }

    @Test
    public void testQuotePrecisionForSharesAndBonds()
    {
        String sharesJson = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\"],"
                        + "\"data\":[[\"2024-01-01\",1.123456789]]}}";
        String bondsJson = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"FACEVALUE\"],"
                        + "\"data\":[[\"2024-01-01\",1.23456789,33.3]]}}";

        assertEquals(112345679L, parser.parse(sharesJson, false).get(0).getValue());
        assertEquals(41111111L, parser.parse(bondsJson, true).get(0).getValue());
    }

    @Test
    public void testFinalQuoteScalingRoundsHalfUp()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\"],"
                        + "\"data\":[[\"2024-01-01\",1.000000005]]}}";

        assertEquals(100000001L, parser.parse(json, false).get(0).getValue());
    }

    @Test
    public void testBondQuoteScalingRoundsHalfUpAtQuotePrecision()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"FACEVALUE\"],"
                        + "\"data\":[[\"2024-01-01\",50.0000005,1]]}}";

        assertEquals(50000001L, parser.parse(json, true).get(0).getValue());
    }

    @Test
    public void testInvalidFaceValuesAreSkippedAndLaterValidRowIsParsed()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"FACEVALUE\"],"
                        + "\"data\":[[\"2024-01-01\",98.56],[\"2024-01-02\",98.56,null],"
                        + "[\"2024-01-03\",98.56,0],[\"2024-01-04\",98.56,-100],"
                        + "[\"2024-01-05\",98.56,740]]}}";

        List<LatestSecurityPrice> prices = parser.parse(json, true);

        assertEquals(1, prices.size());
        assertEquals(72934400000L, prices.get(0).getValue());
    }

    @Test
    public void testOverflowAndMalformedFaceValuesAreSkippedBeforeValidRow()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"FACEVALUE\"],"
                        + "\"data\":[[\"2024-01-01\",98.56,1e100],[\"2024-01-02\",98.56,\"not-a-number\"],"
                        + "[\"2024-01-03\",98.56,740]]}}";

        List<LatestSecurityPrice> prices = parser.parse(json, true);

        assertEquals(1, prices.size());
        assertEquals(72934400000L, prices.get(0).getValue());
    }

    @Test
    public void testBondWithoutFaceValueColumnReturnsNoPrices()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\"],"
                        + "\"data\":[[\"2024-01-01\",98.56]]}}";

        assertTrue(parser.parse(json, true).isEmpty());
    }

    @Test
    public void testSharesDoNotRequireFaceValue()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\"],"
                        + "\"data\":[[\"2024-01-01\",98.56]]}}";

        assertEquals(9856000000L, parser.parse(json, false).get(0).getValue());
    }

    @Test
    public void testMissingAndNullOptionalFieldsRemainUnavailable()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"LOW\",\"HIGH\",\"VOLUME\"],"
                        + "\"data\":[[\"2024-01-01\",98.56,null,null,null],[\"2024-01-02\",98.56]]}}";

        List<LatestSecurityPrice> prices = parser.parse(json, false);

        assertEquals(2, prices.size());
        for (LatestSecurityPrice price : prices)
        {
            assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getLow());
            assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getHigh());
            assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getVolume());
        }
    }

    @Test
    public void testNullDateAndCloseRowsAreSkippedBeforeValidRow()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\"],"
                        + "\"data\":[[null,98.56],[\"2024-01-02\",null],[\"2024-01-03\",98.56]]}}";

        List<LatestSecurityPrice> prices = parser.parse(json, false);

        assertEquals(1, prices.size());
        assertEquals(9856000000L, prices.get(0).getValue());
    }

    @Test
    public void testAccruedInterestIsNotAddedToCleanPrice()
    {
        String json = "{\"history\":{\"columns\":[\"TRADEDATE\",\"CLOSE\",\"FACEVALUE\",\"ACCINT\"],"
                        + "\"data\":[[\"2024-01-01\",98.56,1000,5.25]]}}";

        assertEquals(98560000000L, parser.parse(json, true).get(0).getValue());
    }
}
