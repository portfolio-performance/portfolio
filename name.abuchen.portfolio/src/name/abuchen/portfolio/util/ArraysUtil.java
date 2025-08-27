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

    public static long[] add(long[]... arrays)
    {
        if (arrays.length == 0)
        throw new IllegalArgumentException("At least one array is required");

        int length = arrays[0].length;
        // Validate that all arrays have the same length
        for (int i = 1; i < arrays.length; i++) {
            if (arrays[i].length != length)
                throw new IllegalArgumentException("Length mismatch at array " + i + ": " + arrays[i].length + " != " + length); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        long[] result = new long[length];
        for (long[] array : arrays) {
            for (int i = 0; i < length; i++) {
                result[i] += array[i];
            }
        }

        return result;
    }

}
