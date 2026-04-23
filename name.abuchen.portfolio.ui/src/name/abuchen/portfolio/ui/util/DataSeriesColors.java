package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;

public final class DataSeriesColors
{
    private static final DataSeriesColors INSTANCE = new DataSeriesColors();

    private Color totalsColor;

    private Color investedCapitalColor;
    private Color absoluteInvestedCapitalColor;

    private Color transferalsColor;
    private Color transferalsAccumulatedColor;

    private Color taxesColor;
    private Color taxesAccumulatedColor;

    private Color absoluteDeltaColor;
    private Color absoluteDeltaAllRecordColor;

    private Color dividendsColor;
    private Color dividendsAccumulatedColor;

    private Color interestColor;
    private Color interestAccumulatedColor;

    private Color interestChargeColor;
    private Color interestChargeAccumulatedColor;

    private Color earningsColor;
    private Color earningsAccumulatedColor;

    private Color feesColor;
    private Color feesAccumulatedColor;

    private Color performanceEntirePortfolioColor;
    private Color performancePositiveColor;
    private Color performanceNegativeColor;

    private DataSeriesColors()
    {
    }

    public static DataSeriesColors instance()
    {
        return INSTANCE;
    }

    private Color requireConfigured(Color color, String property)
    {
        if (color == null)
            throw new IllegalStateException("CSS data series color not configured: " + property); //$NON-NLS-1$

        return color;
    }

    public Color totalsColor()
    {
        return requireConfigured(totalsColor, "DataSeries.totals-color"); //$NON-NLS-1$
    }

    public void setTotalsColor(RGBA color)
    {
        this.totalsColor = Colors.getColor(color.rgb);
    }

    public Color investedCapitalColor()
    {
        return requireConfigured(investedCapitalColor, "DataSeries.invested-capital-color"); //$NON-NLS-1$
    }

    public void setInvestedCapitalColor(RGBA color)
    {
        this.investedCapitalColor = Colors.getColor(color.rgb);
    }

    public Color absoluteInvestedCapitalColor()
    {
        return requireConfigured(absoluteInvestedCapitalColor, "DataSeries.absolute-invested-capital-color"); //$NON-NLS-1$
    }

    public void setAbsoluteInvestedCapitalColor(RGBA color)
    {
        this.absoluteInvestedCapitalColor = Colors.getColor(color.rgb);
    }

    public Color transferalsColor()
    {
        return requireConfigured(transferalsColor, "DataSeries.transferals-color"); //$NON-NLS-1$
    }

    public void setTransferalsColor(RGBA color)
    {
        this.transferalsColor = Colors.getColor(color.rgb);
    }

    public Color transferalsAccumulatedColor()
    {
        return requireConfigured(transferalsAccumulatedColor, "DataSeries.transferals-accumulated-color"); //$NON-NLS-1$
    }

    public void setTransferalsAccumulatedColor(RGBA color)
    {
        this.transferalsAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color taxesColor()
    {
        return requireConfigured(taxesColor, "DataSeries.taxes-color"); //$NON-NLS-1$
    }

    public void setTaxesColor(RGBA color)
    {
        this.taxesColor = Colors.getColor(color.rgb);
    }

    public Color taxesAccumulatedColor()
    {
        return requireConfigured(taxesAccumulatedColor, "DataSeries.taxes-accumulated-color"); //$NON-NLS-1$
    }

    public void setTaxesAccumulatedColor(RGBA color)
    {
        this.taxesAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color absoluteDeltaColor()
    {
        return requireConfigured(absoluteDeltaColor, "DataSeries.absolute-delta-color"); //$NON-NLS-1$
    }

    public void setAbsoluteDeltaColor(RGBA color)
    {
        this.absoluteDeltaColor = Colors.getColor(color.rgb);
    }

    public Color absoluteDeltaAllRecordColor()
    {
        return requireConfigured(absoluteDeltaAllRecordColor, "DataSeries.absolute-delta-all-record-color"); //$NON-NLS-1$
    }

    public void setAbsoluteDeltaAllRecordColor(RGBA color)
    {
        this.absoluteDeltaAllRecordColor = Colors.getColor(color.rgb);
    }

    public Color dividendsColor()
    {
        return requireConfigured(dividendsColor, "DataSeries.dividends-color"); //$NON-NLS-1$
    }

    public void setDividendsColor(RGBA color)
    {
        this.dividendsColor = Colors.getColor(color.rgb);
    }

    public Color dividendsAccumulatedColor()
    {
        return requireConfigured(dividendsAccumulatedColor, "DataSeries.dividends-accumulated-color"); //$NON-NLS-1$
    }

    public void setDividendsAccumulatedColor(RGBA color)
    {
        this.dividendsAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color interestColor()
    {
        return requireConfigured(interestColor, "DataSeries.interest-color"); //$NON-NLS-1$
    }

    public void setInterestColor(RGBA color)
    {
        this.interestColor = Colors.getColor(color.rgb);
    }

    public Color interestAccumulatedColor()
    {
        return requireConfigured(interestAccumulatedColor, "DataSeries.interest-accumulated-color"); //$NON-NLS-1$
    }

    public void setInterestAccumulatedColor(RGBA color)
    {
        this.interestAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color interestChargeColor()
    {
        return requireConfigured(interestChargeColor, "DataSeries.interest-charge-color"); //$NON-NLS-1$
    }

    public void setInterestChargeColor(RGBA color)
    {
        this.interestChargeColor = Colors.getColor(color.rgb);
    }

    public Color interestChargeAccumulatedColor()
    {
        return requireConfigured(interestChargeAccumulatedColor, "DataSeries.interest-charge-accumulated-color"); //$NON-NLS-1$
    }

    public void setInterestChargeAccumulatedColor(RGBA color)
    {
        this.interestChargeAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color earningsColor()
    {
        return requireConfigured(earningsColor, "DataSeries.earnings-color"); //$NON-NLS-1$
    }

    public void setEarningsColor(RGBA color)
    {
        this.earningsColor = Colors.getColor(color.rgb);
    }

    public Color earningsAccumulatedColor()
    {
        return requireConfigured(earningsAccumulatedColor, "DataSeries.earnings-accumulated-color"); //$NON-NLS-1$
    }

    public void setEarningsAccumulatedColor(RGBA color)
    {
        this.earningsAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color feesColor()
    {
        return requireConfigured(feesColor, "DataSeries.fees-color"); //$NON-NLS-1$
    }

    public void setFeesColor(RGBA color)
    {
        this.feesColor = Colors.getColor(color.rgb);
    }

    public Color feesAccumulatedColor()
    {
        return requireConfigured(feesAccumulatedColor, "DataSeries.fees-accumulated-color"); //$NON-NLS-1$
    }

    public void setFeesAccumulatedColor(RGBA color)
    {
        this.feesAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color performanceEntirePortfolioColor()
    {
        return requireConfigured(performanceEntirePortfolioColor, "DataSeries.performance-entire-portfolio-color"); //$NON-NLS-1$
    }

    public void setPerformanceEntirePortfolioColor(RGBA color)
    {
        this.performanceEntirePortfolioColor = Colors.getColor(color.rgb);
    }

    public Color performancePositiveColor()
    {
        return requireConfigured(performancePositiveColor, "DataSeries.performance-positive-color"); //$NON-NLS-1$
    }

    public void setPerformancePositiveColor(RGBA color)
    {
        this.performancePositiveColor = Colors.getColor(color.rgb);
    }

    public Color performanceNegativeColor()
    {
        return requireConfigured(performanceNegativeColor, "DataSeries.performance-negative-color"); //$NON-NLS-1$
    }

    public void setPerformanceNegativeColor(RGBA color)
    {
        this.performanceNegativeColor = Colors.getColor(color.rgb);
    }
}
