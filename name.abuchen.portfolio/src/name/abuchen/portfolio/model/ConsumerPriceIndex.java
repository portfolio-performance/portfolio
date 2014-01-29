package name.abuchen.portfolio.model;

import java.util.Comparator;

public class ConsumerPriceIndex
{
    public static class ByDate implements Comparator<ConsumerPriceIndex>
    {
        @Override
        public int compare(ConsumerPriceIndex p1, ConsumerPriceIndex p2)
        {
            if (p1.year != p2.year)
                return Integer.valueOf(p1.year).compareTo(p2.year);
            if (p1.month != p2.month)
                return Integer.valueOf(p1.month).compareTo(p2.month);
            return 0;
        }
    }

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
}
