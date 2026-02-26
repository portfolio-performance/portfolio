package name.abuchen.portfolio.model;

@Deprecated(since = "2019")
/* package */class ConsumerPriceIndex
{
    private int year;
    private int month;
    private int index;

    @Deprecated(since = "2019")
    public int getYear()
    {
        return year;
    }

    @Deprecated(since = "2019")
    public void setYear(int year)
    {
        this.year = year;
    }

    @Deprecated(since = "2019")
    public int getMonth()
    {
        return month;
    }

    @Deprecated(since = "2019")
    public void setMonth(int month)
    {
        this.month = month;
    }

    @Deprecated(since = "2019")
    public int getIndex()
    {
        return index;
    }

    @Deprecated(since = "2019")
    public void setIndex(int index)
    {
        this.index = index;
    }

    @Override
    @Deprecated(since = "2019")
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + month;
        result = prime * result + year;
        return result;
    }

    @Override
    @Deprecated(since = "2019")
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConsumerPriceIndex other = (ConsumerPriceIndex) obj;
        if (index != other.index)
            return false;
        if (month != other.month)
            return false;
        if (year != other.year)
            return false;
        return true;
    }
}
