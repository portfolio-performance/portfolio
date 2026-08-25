package name.abuchen.portfolio.math;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Distributes an integer total onto weighted pieces without losing or inventing
 * a minor unit.
 */
public final class Apportionment
{
    private Apportionment()
    {
    }

    /**
     * Distributes the given total proportionally to the given weights using the
     * largest remainder method, ties broken by the lowest index.
     * <p>
     * The pieces sum up to the total exactly - for any sign of the total - and
     * every piece is within one unit of the exact proportional value
     * {@code total * weight[i] / sum(weights)}. A weight of zero always
     * receives zero.
     *
     * @param total
     *            the amount to distribute; may be negative
     * @param weights
     *            the weights of the pieces; at least one, none negative, and
     *            not all zero
     * @return one value per weight, in the same order
     * @throws IllegalArgumentException
     *             if no weight is given, if a weight is negative, or if all
     *             weights are zero
     */
    public static long[] distribute(long total, long[] weights)
    {
        if (weights.length == 0)
            throw new IllegalArgumentException("no weights given"); //$NON-NLS-1$

        var sumOfWeights = BigInteger.ZERO;
        for (long weight : weights)
        {
            if (weight < 0)
                throw new IllegalArgumentException("negative weight " + weight); //$NON-NLS-1$
            sumOfWeights = sumOfWeights.add(BigInteger.valueOf(weight));
        }

        if (sumOfWeights.signum() == 0)
            throw new IllegalArgumentException("weights must not all be zero"); //$NON-NLS-1$

        var bigTotal = BigInteger.valueOf(total);

        long[] answer = new long[weights.length];
        BigInteger[] remainders = new BigInteger[weights.length];

        long distributed = 0;

        for (int ii = 0; ii < weights.length; ii++)
        {
            BigInteger[] quotientAndRemainder = bigTotal.multiply(BigInteger.valueOf(weights[ii]))
                            .divideAndRemainder(sumOfWeights);

            var quotient = quotientAndRemainder[0];
            var remainder = quotientAndRemainder[1];

            // round towards negative infinity: with a floored quotient the
            // remainder is in [0, sum) whatever the sign of the total, hence
            // the leftover below is in [0, number of pieces) and needs no
            // special casing for negative totals

            if (remainder.signum() < 0)
            {
                quotient = quotient.subtract(BigInteger.ONE);
                remainder = remainder.add(sumOfWeights);
            }

            answer[ii] = quotient.longValueExact();
            remainders[ii] = remainder;

            distributed += answer[ii];
        }

        // hand out the leftover one unit at a time, largest remainder first. A
        // zero weight has a zero remainder and therefore can never absorb a
        // unit: the leftover is always smaller than the number of pieces with
        // a remainder greater than zero.
        //
        // The pieces are ordered once instead of picking the largest remainder
        // repeatedly: the leftover grows with the number of pieces (it averages
        // half of them), so picking would be quadratic - noticeable once a sale
        // is matched against the hundreds of lots of a long running savings
        // plan

        long leftover = total - distributed;

        if (leftover > 0)
        {
            Integer[] order = new Integer[weights.length];
            for (int ii = 0; ii < order.length; ii++)
                order[ii] = ii;

            Arrays.sort(order, (a, b) -> {
                int byRemainder = remainders[b].compareTo(remainders[a]);
                return byRemainder != 0 ? byRemainder : Integer.compare(a, b);
            });

            for (int ii = 0; ii < leftover; ii++)
                answer[order[ii]]++;
        }

        return answer;
    }
}
