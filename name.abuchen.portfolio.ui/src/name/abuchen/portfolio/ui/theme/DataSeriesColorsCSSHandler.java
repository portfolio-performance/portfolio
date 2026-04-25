package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ColorSourceTracker;
import name.abuchen.portfolio.ui.util.DataSeriesColors;

@SuppressWarnings("restriction")
public class DataSeriesColorsCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof DataSeriesColorsElementAdapter adapter)
        {
            var colors = adapter.getColors();
            return applyTo(colors, property, value);
        }

        return false;
    }

    private boolean applyTo(DataSeriesColors colors, String property, CSSValue value)
    {
        switch (property)
        {
            case "totals-color": //$NON-NLS-1$
                colors.setTotalsColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "totals-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "invested-capital-color": //$NON-NLS-1$
                colors.setInvestedCapitalColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "invested-capital-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "absolute-invested-capital-color": //$NON-NLS-1$
                colors.setAbsoluteInvestedCapitalColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "absolute-invested-capital-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "transferals-color": //$NON-NLS-1$
                colors.setTransferalsColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "transferals-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "transferals-accumulated-color": //$NON-NLS-1$
                colors.setTransferalsAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "transferals-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "taxes-color": //$NON-NLS-1$
                colors.setTaxesColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "taxes-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "taxes-accumulated-color": //$NON-NLS-1$
                colors.setTaxesAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "taxes-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "absolute-delta-color": //$NON-NLS-1$
                colors.setAbsoluteDeltaColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "absolute-delta-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "absolute-delta-all-record-color": //$NON-NLS-1$
                colors.setAbsoluteDeltaAllRecordColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "absolute-delta-all-record-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "dividends-color": //$NON-NLS-1$
                colors.setDividendsColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "dividends-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "dividends-accumulated-color": //$NON-NLS-1$
                colors.setDividendsAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "dividends-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "interest-color": //$NON-NLS-1$
                colors.setInterestColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "interest-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "interest-accumulated-color": //$NON-NLS-1$
                colors.setInterestAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "interest-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "interest-charge-color": //$NON-NLS-1$
                colors.setInterestChargeColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "interest-charge-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "interest-charge-accumulated-color": //$NON-NLS-1$
                colors.setInterestChargeAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "interest-charge-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "earnings-color": //$NON-NLS-1$
                colors.setEarningsColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "earnings-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "earnings-accumulated-color": //$NON-NLS-1$
                colors.setEarningsAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "earnings-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "fees-color": //$NON-NLS-1$
                colors.setFeesColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "fees-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "fees-accumulated-color": //$NON-NLS-1$
                colors.setFeesAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "fees-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "performance-entire-portfolio-color": //$NON-NLS-1$
                colors.setPerformanceEntirePortfolioColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "performance-entire-portfolio-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "performance-positive-color": //$NON-NLS-1$
                colors.setPerformancePositiveColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "performance-positive-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "performance-negative-color": //$NON-NLS-1$
                colors.setPerformanceNegativeColor(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("DataSeries", "performance-negative-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            default:
                return false;
        }
    }
}
