package name.abuchen.portfolio.math;

import jakarta.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

@Singleton
@Creatable
public class NegativeValue
{
    private boolean negativeValueAllowed = false;

    public boolean isNegativeValueAllowed()
    {
        return negativeValueAllowed;
    }

    public void setNegativeValueAllowed(boolean value)
    {
        negativeValueAllowed = value;
    }

    public double maybeAbs(double value)
    {
        if (negativeValueAllowed)
        {
            return value;
        }
        return Math.abs(value);
    }

    public long maybeAbs(long value)
    {
        if (negativeValueAllowed)
        {
            return value;
        }
        return Math.abs(value);
    }

}
