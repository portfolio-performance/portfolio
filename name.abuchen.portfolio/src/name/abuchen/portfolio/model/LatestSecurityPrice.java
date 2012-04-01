package name.abuchen.portfolio.model;

import java.util.Date;

public class LatestSecurityPrice extends SecurityPrice
{
    private int high;
    private int low;
    private int volume;

    private int previousClose;

    public LatestSecurityPrice()
    {}

    public LatestSecurityPrice(Date time, int price)
    {
        super(time, price);
    }

    public int getHigh()
    {
        return high;
    }

    public void setHigh(int high)
    {
        this.high = high;
    }

    public int getLow()
    {
        return low;
    }

    public void setLow(int low)
    {
        this.low = low;
    }

    public int getVolume()
    {
        return volume;
    }

    public void setVolume(int volume)
    {
        this.volume = volume;
    }

    public int getPreviousClose()
    {
        return previousClose;
    }

    public void setPreviousClose(int previousClose)
    {
        this.previousClose = previousClose;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + high;
        result = prime * result + low;
        result = prime * result + previousClose;
        result = prime * result + volume;
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

    @Override
    @SuppressWarnings("nls")
    public String toString()
    {
        return String.format("%tF: %,10.2f (High: %,10.2f, Low: %,10.2f, Volume: %,10d, Prev Close: %,10.2f)",
                        getTime(), getValue() / 100d, high / 100d, low / 100d, volume, previousClose / 100d);
    }

}
