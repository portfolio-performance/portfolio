package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.ui.Messages;

public enum Average
{
    AVERAGE(Messages.HeatmapOrnamentAverage);

    private String label;

    private Average(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}