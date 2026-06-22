package name.abuchen.portfolio.math;

import jakarta.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

/**
 * Central singleton for relaxing the restriction of providing positive values
 * for the most input and CSV fields.
 * <p/>
 * This singleton is needed to make this feature _configurable_, i.e. it would
 * be much simpler if relaxed restriction would be the default.
 * <p/>
 * Also see https://github.com/portfolio-performance/portfolio/pull/5376
 */
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
