package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

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
                return true;
            case "invested-capital-color": //$NON-NLS-1$
                colors.setInvestedCapitalColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "absolute-invested-capital-color": //$NON-NLS-1$
                colors.setAbsoluteInvestedCapitalColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "transferals-color": //$NON-NLS-1$
                colors.setTransferalsColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "transferals-accumulated-color": //$NON-NLS-1$
                colors.setTransferalsAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "taxes-color": //$NON-NLS-1$
                colors.setTaxesColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "taxes-accumulated-color": //$NON-NLS-1$
                colors.setTaxesAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "absolute-delta-color": //$NON-NLS-1$
                colors.setAbsoluteDeltaColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "absolute-delta-all-record-color": //$NON-NLS-1$
                colors.setAbsoluteDeltaAllRecordColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "dividends-color": //$NON-NLS-1$
                colors.setDividendsColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "dividends-accumulated-color": //$NON-NLS-1$
                colors.setDividendsAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "interest-color": //$NON-NLS-1$
                colors.setInterestColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "interest-accumulated-color": //$NON-NLS-1$
                colors.setInterestAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "interest-charge-color": //$NON-NLS-1$
                colors.setInterestChargeColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "interest-charge-accumulated-color": //$NON-NLS-1$
                colors.setInterestChargeAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "earnings-color": //$NON-NLS-1$
                colors.setEarningsColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "earnings-accumulated-color": //$NON-NLS-1$
                colors.setEarningsAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "fees-color": //$NON-NLS-1$
                colors.setFeesColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "fees-accumulated-color": //$NON-NLS-1$
                colors.setFeesAccumulatedColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "performance-entire-portfolio-color": //$NON-NLS-1$
                colors.setPerformanceEntirePortfolioColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "performance-positive-color": //$NON-NLS-1$
                colors.setPerformancePositiveColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "performance-negative-color": //$NON-NLS-1$
                colors.setPerformanceNegativeColor(CSSSWTColorHelper.getRGBA(value));
                return true;
            default:
                return false;
        }
    }
}
