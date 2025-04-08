package name.abuchen.portfolio.util;

public class ArraysUtil
{
    private ArraysUtil()
    {
    }

    public static double[] toDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = input[ii] / divider;
        return answer;
    }

    public static double[] accumulateAndToDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        long current = 0;
        for (int ii = 0; ii < answer.length; ii++)
        {
            current += input[ii];
            answer[ii] = current / divider;
        }
        return answer;
    }

    public static long[] add(long[] a, long[] b)
    {
        if (a.length != b.length)
            throw new IllegalArgumentException("length mismatch " + a.length + " != " + b.length); //$NON-NLS-1$ //$NON-NLS-2$

        long[] result = new long[a.length];
        for (int ii = 0; ii < result.length; ii++)
            result[ii] = a[ii] + b[ii];

        return result;
    }

}
