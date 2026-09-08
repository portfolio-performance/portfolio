package name.abuchen.portfolio.math;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

@SuppressWarnings("nls")
public class ApportionmentTest
{
    private static long sum(long[] values)
    {
        long answer = 0;
        for (long value : values)
            answer += value;
        return answer;
    }

    @Test
    public void testSinglePieceGetsEverything()
    {
        assertArrayEquals(new long[] { 100001 }, Apportionment.distribute(100001, new long[] { 7 }));
        assertArrayEquals(new long[] { 0 }, Apportionment.distribute(0, new long[] { 7 }));
    }

    @Test
    public void testExactDivisionNeedsNoCorrection()
    {
        assertArrayEquals(new long[] { 100, 100, 100 }, Apportionment.distribute(300, new long[] { 1, 1, 1 }));
    }

    @Test
    public void testLeftoverGoesToTheLargestRemainder()
    {
        // 100 x 1/6, 2/6, 3/6 = 16.67, 33.33, 50.00; the flooring leaves one
        // unit over which goes to the largest remainder (.67)
        assertArrayEquals(new long[] { 17, 33, 50 }, Apportionment.distribute(100, new long[] { 1, 2, 3 }));
    }

    @Test
    public void testTiesAreBrokenByLowestIndex()
    {
        // three equal remainders of 1/3, two units to distribute: the first two
        // pieces get them
        assertArrayEquals(new long[] { 334, 334, 333 }, Apportionment.distribute(1001, new long[] { 1, 1, 1 }));

        // ... and one unit to distribute goes to the first piece only
        assertArrayEquals(new long[] { 334, 333, 333 }, Apportionment.distribute(1000, new long[] { 1, 1, 1 }));
    }

    @Test
    public void testTiesAreBrokenByLowestIndexAcrossManyPieces()
    {
        // with a thousand equal weights every remainder is the same, so the
        // 999 units of the leftover go to the first 999 pieces. Ordering a
        // large number of pieces is the case where the tie break stops being
        // obvious - and where a sale spanning the lots of a savings plan lands

        long[] weights = new long[1000];
        Arrays.fill(weights, 100_000_000L);

        long[] pieces = Apportionment.distribute(1000 * 1000L - 1, weights);

        for (int ii = 0; ii < pieces.length; ii++)
            assertThat("piece " + ii, pieces[ii], is(ii < 999 ? 1000L : 999L));

        assertThat(sum(pieces), is(1000 * 1000L - 1));
    }

    @Test
    public void testZeroWeightsGetZero()
    {
        assertArrayEquals(new long[] { 0, 334, 334, 0, 333 },
                        Apportionment.distribute(1001, new long[] { 0, 1, 1, 0, 1 }));
    }

    @Test
    public void testZeroTotalIsDistributedAsZero()
    {
        assertArrayEquals(new long[] { 0, 0, 0 }, Apportionment.distribute(0, new long[] { 1, 2, 3 }));
    }

    @Test
    public void testNegativeTotal()
    {
        // -1001 x 1/3 = -333.67 each; flooring gives -334 three times, i.e. one
        // unit too much, which is handed back to the largest remainder
        assertArrayEquals(new long[] { -333, -334, -334 }, Apportionment.distribute(-1001, new long[] { 1, 1, 1 }));

        // sign is the only difference: the pieces still sum up exactly
        assertThat(sum(Apportionment.distribute(-100, new long[] { 1, 2, 3 })), is(-100L));
    }

    @Test
    public void testEveryPieceIsWithinOneUnitOfTheExactValue()
    {
        long total = 1000001;
        long[] weights = { 3, 5, 7, 11, 13 };

        long[] pieces = Apportionment.distribute(total, weights);

        BigInteger sumOfWeights = BigInteger.valueOf(sum(weights));

        for (int ii = 0; ii < weights.length; ii++)
        {
            BigInteger exact = BigInteger.valueOf(total).multiply(BigInteger.valueOf(weights[ii]));
            BigInteger scaled = BigInteger.valueOf(pieces[ii]).multiply(sumOfWeights);

            assertTrue("piece " + ii + " is off by more than one unit",
                            scaled.subtract(exact).abs().compareTo(sumOfWeights) < 0);
        }
    }

    @Test
    public void testSumIsAlwaysTheTotal()
    {
        var random = new Random(42);

        for (int run = 0; run < 1000; run++)
        {
            long total = random.nextLong(-100_000_000L, 100_000_000L);

            long[] weights = new long[1 + random.nextInt(10)];
            long sumOfWeights = 0;
            for (int ii = 0; ii < weights.length; ii++)
            {
                weights[ii] = random.nextLong(0, 1_000_000_000_000L);
                sumOfWeights += weights[ii];
            }

            if (sumOfWeights == 0)
                continue;

            long[] pieces = Apportionment.distribute(total, weights);

            assertThat(sum(pieces), is(total));

            for (int ii = 0; ii < weights.length; ii++)
            {
                if (weights[ii] == 0)
                    assertThat(pieces[ii], is(0L));
            }
        }
    }

    @Test
    public void testProductsDoNotOverflow()
    {
        // Values.Share has 8 decimals, so 10,000 shares are 10^12 units. Times
        // an amount of EUR 10 million in cents that is 10^21 - two orders of
        // magnitude beyond a long, which is why the intermediate products are
        // computed in BigInteger

        long total = 1_000_000_001L;
        long[] weights = { 1_000_000_000_000L, 1_000_000_000_000L, 1_000_000_000_000L };

        assertArrayEquals(new long[] { 333_333_334L, 333_333_334L, 333_333_333L },
                        Apportionment.distribute(total, weights));
    }

    @Test
    public void testInvalidArgumentsAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> Apportionment.distribute(100, new long[0]));
        assertThrows(IllegalArgumentException.class, () -> Apportionment.distribute(100, new long[] { 1, -1 }));
        assertThrows(IllegalArgumentException.class, () -> Apportionment.distribute(100, new long[] { 0, 0 }));
        assertThrows(IllegalArgumentException.class, () -> Apportionment.distribute(0, new long[] { 0 }));
    }
}
