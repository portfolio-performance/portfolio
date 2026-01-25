package name.abuchen.portfolio.math;

public class NegativeValue
{

    public static final boolean ALLOW_CSV_NEGATIVE_VALUE = true;

    public static double maybeAbs(double value)
    {
        if (ALLOW_CSV_NEGATIVE_VALUE)
        {
            return value;
        }
        return Math.abs(value);
    }

    public static long maybeAbs(long value)
    {
        if (ALLOW_CSV_NEGATIVE_VALUE)
        {
            return value;
        }
        return Math.abs(value);
    }

}
