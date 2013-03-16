package name.abuchen.portfolio.model;


public class ConsumerPriceIndex implements Comparable<ConsumerPriceIndex>
{
    private int year;
    private int month;
    private int index;

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    public int getMonth()
    {
        return month;
    }

    public void setMonth(int month)
    {
        this.month = month;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    @Override
    public int compareTo(ConsumerPriceIndex o)
    {
        if (this.year != o.year)
            return Integer.valueOf(this.year).compareTo(o.year);
        if (this.month != o.month)
            return Integer.valueOf(this.month).compareTo(o.month);
        return Integer.valueOf(this.index).compareTo(o.index);
    }
}
