package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.Objects;

public class LatestSecurityPrice extends SecurityPrice
{
    private long high;
    private long low;
    private long volume;

    @Deprecated
    /* package */ transient long previousClose;

    public static final long NOT_AVAILABLE = -1L;

    public LatestSecurityPrice()
    {
    }

    public LatestSecurityPrice(LocalDate date, long price)
    {
        super(date, price);
    }

    public LatestSecurityPrice(LocalDate date, long price, long high, long low, long volume)
    {
        super(date, price);

        this.high = high;
        this.low = low;
        this.volume = volume;
    }

    public long getHigh()
    {
        return high;
    }

    public void setHigh(long high)
    {
        this.high = high;
    }

    public long getLow()
    {
        return low;
    }

    public void setLow(long low)
    {
        this.low = low;
    }

    public long getVolume()
    {
        return volume;
    }

    public void setVolume(long volume)
    {
        this.volume = volume;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash(high, low, volume);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        LatestSecurityPrice other = (LatestSecurityPrice) obj;
        return super.equals(other) && high == other.high && low == other.low && volume == other.volume;
    }
}
