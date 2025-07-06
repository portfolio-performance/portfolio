package name.abuchen.portfolio.model;

import name.abuchen.portfolio.Messages;

public enum CostMethod
{
    FIFO(Messages.LabelCapitalGainsMethodFIFO, Messages.LabelCapitalGainsMethodFIFO), //
    MOVING_AVERAGE(Messages.LabelCapitalGainsMethodMovingAverage, Messages.LabelCapitalGainsMethodMovingAverageAbbr);

    private String label;
    private String abbreviation;

    private CostMethod(String label, String abbreviation)
    {
        this.label = label;
        this.abbreviation = abbreviation;
    }

    @Override
    public String toString()
    {
        return label;
    }

    public String getLabel()
    {
        return label;
    }

    public String getAbbreviation()
    {
        return abbreviation;
    }

    public boolean useFifo()
    {
        return this == FIFO;
    }
}
