package name.abuchen.portfolio.ui.views.dashboard;

import name.abuchen.portfolio.ui.Messages;

public enum CostMethod
{
    FIFO(Messages.LabelCapitalGainsMethodFIFO, true), //
    MOVING_AVERAGE(Messages.LabelCapitalGainsMethodMovingAverage, false);

    private String label;
    private boolean useFifo;

    private CostMethod(String label, boolean useFifo)
    {
        this.label = label;
        this.useFifo = useFifo;
    }

    @Override
    public String toString()
    {
        return label;
    }

    public boolean useFifo()
    {
        return useFifo;
    }
}
