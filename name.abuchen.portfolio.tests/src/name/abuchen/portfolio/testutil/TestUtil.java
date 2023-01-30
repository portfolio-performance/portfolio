package name.abuchen.portfolio.testutil;

import static org.junit.Assert.assertEquals;

import name.abuchen.portfolio.money.Money;

public class TestUtil
{
    public static void assertMoneyApproximateEqual(Money expected, Money actual, long eps)
    {
        assertEquals(expected.getCurrencyCode(), actual.getCurrencyCode());
        assertEquals(expected.getAmount(), actual.getAmount(), eps);
    }
}
