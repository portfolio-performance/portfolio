package name.abuchen.portfolio.model;

import name.abuchen.portfolio.Messages;

public enum CostMethod
{
    FIFO(Messages.LabelCostMethodFIFO, Messages.LabelCostMethodFIFOAbbr), //
    MOVING_AVERAGE(Messages.LabelCostMethodMovingAverage, Messages.LabelCostMethodMovingAverageAbbr);

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

}
