package name.abuchen.portfolio.model;

import java.time.LocalDate;

public class LatestSecurityPrice extends SecurityPrice
{
    private long high;
    private long low;
    private long volume;

    private long previousClose;

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

    @Deprecated
    public long getPreviousClose()
    {
        return previousClose;
    }

    public void setPreviousClose(long previousClose)
    {
        this.previousClose = previousClose;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (high ^ (high >>> 32));
        result = prime * result + (int) (low ^ (low >>> 32));
        result = prime * result + (int) (previousClose ^ (previousClose >>> 32));
        result = prime * result + (int) (volume ^ (volume >>> 32));
        return result;
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
        if (high != other.high)
            return false;
        if (low != other.low)
            return false;
        if (previousClose != other.previousClose)
            return false;
        if (volume != other.volume)
            return false;
        return true;
    }
}
