package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;

public final class DataSeriesColors
{
    private static final DataSeriesColors INSTANCE = new DataSeriesColors();

    private Color totalsColor = Colors.BLACK;

    private Color investedCapitalColor = Colors.GRAY;
    private Color absoluteInvestedCapitalColor = Colors.GRAY;

    private Color transferalsColor = Colors.GRAY;
    private Color transferalsAccumulatedColor = Colors.LIGHT_GRAY;

    private Color taxesColor = Colors.GRAY;
    private Color taxesAccumulatedColor = Colors.GRAY;

    private Color absoluteDeltaColor = Colors.GRAY;
    private Color absoluteDeltaAllRecordColor = Colors.GRAY;

    private Color dividendsColor = Colors.GRAY;
    private Color dividendsAccumulatedColor = Colors.GRAY;

    private Color interestColor = Colors.GRAY;
    private Color interestAccumulatedColor = Colors.GRAY;

    private Color interestChargeColor = Colors.GRAY;
    private Color interestChargeAccumulatedColor = Colors.GRAY;

    private Color earningsColor = Colors.GRAY;
    private Color earningsAccumulatedColor = Colors.GRAY;

    private Color feesColor = Colors.GRAY;
    private Color feesAccumulatedColor = Colors.GRAY;

    private Color performanceEntirePortfolioColor = Colors.BLACK;
    private Color performancePositiveColor = Colors.GRAY;
    private Color performanceNegativeColor = Colors.GRAY;

    private DataSeriesColors()
    {
    }

    public static DataSeriesColors instance()
    {
        return INSTANCE;
    }

    public Color totalsColor()
    {
        return totalsColor;
    }

    public void setTotalsColor(RGBA color)
    {
        this.totalsColor = Colors.getColor(color.rgb);
    }

    public Color investedCapitalColor()
    {
        return investedCapitalColor;
    }

    public void setInvestedCapitalColor(RGBA color)
    {
        this.investedCapitalColor = Colors.getColor(color.rgb);
    }

    public Color absoluteInvestedCapitalColor()
    {
        return absoluteInvestedCapitalColor;
    }

    public void setAbsoluteInvestedCapitalColor(RGBA color)
    {
        this.absoluteInvestedCapitalColor = Colors.getColor(color.rgb);
    }

    public Color transferalsColor()
    {
        return transferalsColor;
    }

    public void setTransferalsColor(RGBA color)
    {
        this.transferalsColor = Colors.getColor(color.rgb);
    }

    public Color transferalsAccumulatedColor()
    {
        return transferalsAccumulatedColor;
    }

    public void setTransferalsAccumulatedColor(RGBA color)
    {
        this.transferalsAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color taxesColor()
    {
        return taxesColor;
    }

    public void setTaxesColor(RGBA color)
    {
        this.taxesColor = Colors.getColor(color.rgb);
    }

    public Color taxesAccumulatedColor()
    {
        return taxesAccumulatedColor;
    }

    public void setTaxesAccumulatedColor(RGBA color)
    {
        this.taxesAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color absoluteDeltaColor()
    {
        return absoluteDeltaColor;
    }

    public void setAbsoluteDeltaColor(RGBA color)
    {
        this.absoluteDeltaColor = Colors.getColor(color.rgb);
    }

    public Color absoluteDeltaAllRecordColor()
    {
        return absoluteDeltaAllRecordColor;
    }

    public void setAbsoluteDeltaAllRecordColor(RGBA color)
    {
        this.absoluteDeltaAllRecordColor = Colors.getColor(color.rgb);
    }

    public Color dividendsColor()
    {
        return dividendsColor;
    }

    public void setDividendsColor(RGBA color)
    {
        this.dividendsColor = Colors.getColor(color.rgb);
    }

    public Color dividendsAccumulatedColor()
    {
        return dividendsAccumulatedColor;
    }

    public void setDividendsAccumulatedColor(RGBA color)
    {
        this.dividendsAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color interestColor()
    {
        return interestColor;
    }

    public void setInterestColor(RGBA color)
    {
        this.interestColor = Colors.getColor(color.rgb);
    }

    public Color interestAccumulatedColor()
    {
        return interestAccumulatedColor;
    }

    public void setInterestAccumulatedColor(RGBA color)
    {
        this.interestAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color interestChargeColor()
    {
        return interestChargeColor;
    }

    public void setInterestChargeColor(RGBA color)
    {
        this.interestChargeColor = Colors.getColor(color.rgb);
    }

    public Color interestChargeAccumulatedColor()
    {
        return interestChargeAccumulatedColor;
    }

    public void setInterestChargeAccumulatedColor(RGBA color)
    {
        this.interestChargeAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color earningsColor()
    {
        return earningsColor;
    }

    public void setEarningsColor(RGBA color)
    {
        this.earningsColor = Colors.getColor(color.rgb);
    }

    public Color earningsAccumulatedColor()
    {
        return earningsAccumulatedColor;
    }

    public void setEarningsAccumulatedColor(RGBA color)
    {
        this.earningsAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color feesColor()
    {
        return feesColor;
    }

    public void setFeesColor(RGBA color)
    {
        this.feesColor = Colors.getColor(color.rgb);
    }

    public Color feesAccumulatedColor()
    {
        return feesAccumulatedColor;
    }

    public void setFeesAccumulatedColor(RGBA color)
    {
        this.feesAccumulatedColor = Colors.getColor(color.rgb);
    }

    public Color performanceEntirePortfolioColor()
    {
        return performanceEntirePortfolioColor;
    }

    public void setPerformanceEntirePortfolioColor(RGBA color)
    {
        this.performanceEntirePortfolioColor = Colors.getColor(color.rgb);
    }

    public Color performancePositiveColor()
    {
        return performancePositiveColor;
    }

    public void setPerformancePositiveColor(RGBA color)
    {
        this.performancePositiveColor = Colors.getColor(color.rgb);
    }

    public Color performanceNegativeColor()
    {
        return performanceNegativeColor;
    }

    public void setPerformanceNegativeColor(RGBA color)
    {
        this.performanceNegativeColor = Colors.getColor(color.rgb);
    }
}
