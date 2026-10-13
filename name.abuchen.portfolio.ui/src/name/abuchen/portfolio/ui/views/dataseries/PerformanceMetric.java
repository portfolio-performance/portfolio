package name.abuchen.portfolio.ui.views.dataseries;

import name.abuchen.portfolio.ui.Messages;

public enum PerformanceMetric
{
    TTWROR(Messages.ColumnTTWROR), //
    IRR(Messages.ColumnIRR);

    private final String label;

    PerformanceMetric(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
