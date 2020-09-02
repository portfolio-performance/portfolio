package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import name.abuchen.portfolio.ui.Messages;

public enum HeatmapOrnament
{
    SUM(Messages.HeatmapOrnamentSum), //
    GEOMETRIC_MEAN(Messages.HeatmapOrnamentGeometricMean), //
    STANDARD_DEVIATION(Messages.HeatmapOrnamentStandardDeviation);

    private String label;

    private HeatmapOrnament(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
