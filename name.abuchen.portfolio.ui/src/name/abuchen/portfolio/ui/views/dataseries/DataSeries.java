package name.abuchen.portfolio.ui.views.dataseries;

import java.util.Locale;
import java.util.function.Function;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swtchart.LineStyle;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;

/**
 * A data series available to add to charts.
 */
public final class DataSeries implements Adaptable
{
    /**
     * The use case determines the selection of data series available.
     */
    public enum UseCase
    {
        STATEMENT_OF_ASSETS, PERFORMANCE, RETURN_VOLATILITY
    }

    /**
     * Data series available for the Client type.
     */
    public enum ClientDataSeries
    {
        TOTALS(Messages.LabelTotalSum), //
        TRANSFERALS(Messages.LabelTransferals), //
        TRANSFERALS_ACCUMULATED(Messages.LabelAccumulatedTransferals), //
        INVESTED_CAPITAL(Messages.LabelInvestedCapital), //
        ABSOLUTE_INVESTED_CAPITAL(Messages.LabelAbsoluteInvestedCapital), //
        ABSOLUTE_DELTA(Messages.LabelDelta), //
        ABSOLUTE_DELTA_ALL_RECORDS(Messages.LabelAbsoluteDelta), //
        DIVIDENDS(Messages.LabelDividends), //
        DIVIDENDS_ACCUMULATED(Messages.LabelAccumulatedDividends), //
        INTEREST(Messages.LabelInterest), //
        INTEREST_ACCUMULATED(Messages.LabelAccumulatedInterest), //
        INTEREST_CHARGE(Messages.LabelInterestCharge), //
        INTEREST_CHARGE_ACCUMULATED(Messages.LabelAccumulatedInterestCharge), //
        EARNINGS(Messages.LabelEarnings), //
        EARNINGS_ACCUMULATED(Messages.LabelAccumulatedEarnings), //
        FEES(Messages.LabelFees), //
        FEES_ACCUMULATED(Messages.LabelFeesAccumulated), //
        TAXES(Messages.ColumnTaxes), //
        TAXES_ACCUMULATED(Messages.LabelAccumulatedTaxes), //

        DELTA_PERCENTAGE(Messages.LabelAggregationDaily);

        private String label;

        private ClientDataSeries(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    /**
     * Type of objects for which the PerformanceIndex is calculated.
     */
    public enum Type
    {
        CLIENT("Client-", i -> ((ClientDataSeries) i).name().toLowerCase(Locale.US)), //$NON-NLS-1$
        CLIENT_PRETAX("Client-PreTax-", i -> ((ClientDataSeries) i).name().toLowerCase(Locale.US)), //$NON-NLS-1$
        SECURITY("Security", i -> ((Security) i).getUUID()), //$NON-NLS-1$
        SECURITY_BENCHMARK("[b]Security", i -> ((Security) i).getUUID()), //$NON-NLS-1$
        ACCOUNT("Account", i -> ((Account) i).getUUID()), //$NON-NLS-1$
        ACCOUNT_PRETAX("Account-PreTax", i -> ((Account) i).getUUID()), //$NON-NLS-1$
        PORTFOLIO("Portfolio", i -> ((Portfolio) i).getUUID()), //$NON-NLS-1$
        DERIVED_DATA_SERIES("Derived-", i -> ((DerivedDataSeries) i).getUUID()), //$NON-NLS-1$
        PORTFOLIO_PRETAX("Portfolio-PreTax", i -> ((Portfolio) i).getUUID()), //$NON-NLS-1$
        PORTFOLIO_PLUS_ACCOUNT("[+]Portfolio", i -> ((Portfolio) i).getUUID()), //$NON-NLS-1$
        PORTFOLIO_PLUS_ACCOUNT_PRETAX("[+]Portfolio-PreTax", i -> ((Portfolio) i).getUUID()), //$NON-NLS-1$
        CLASSIFICATION("Classification", i -> ((Classification) i).getId()), //$NON-NLS-1$
        CLIENT_FILTER("ClientFilter", i -> ((ClientFilterMenu.Item) i).getId()), //$NON-NLS-1$ $
        CLIENT_FILTER_PRETAX("ClientFilter-PreTax", i -> ((ClientFilterMenu.Item) i).getId()); //$NON-NLS-1$

        private final String label;
        private final Function<Object, String> uuidProvider;

        Type(String label, Function<Object, String> uuidProvider)
        {
            this.label = label;
            this.uuidProvider = uuidProvider;
        }

        String buildUUID(Object instance)
        {
            return label + uuidProvider.apply(instance);
        }
    }

    private Type type;
    private Object group;
    private Object instance;
    private String label;
    private boolean isLineChart = true;
    private boolean isBenchmark = false;
    private int lineWidth = 2;

    private RGB color;

    private boolean showArea;
    private LineStyle lineStyle = LineStyle.SOLID;

    /**
     * indicates whether the data series is visible or (temporarily) removed
     * from the chart
     */
    private boolean isVisible = true;

    /* package */ DataSeries(Type type, Object instance, String label, RGB color)
    {
        this(type, null, instance, label, color);
    }

    /* package */ DataSeries(Type type, Object group, Object instance, String label, RGB color)
    {
        this.type = type;
        this.group = group;
        this.instance = instance;
        this.label = label;
        this.color = color;
    }

    public Type getType()
    {
        return type;
    }

    public Object getGroup()
    {
        return group;
    }

    public Object getInstance()
    {
        return instance;
    }

    public String getLabel()
    {
        return isBenchmark() ? label + " " + Messages.ChartSeriesBenchmarkSuffix : label; //$NON-NLS-1$
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * The label used in the data series picker dialog.
     */
    public String getDialogLabel()
    {
        StringBuilder buf = new StringBuilder();

        if (instance instanceof DerivedDataSeries derived)
        {
            buf.append(derived.getBaseDataSeries().getDialogLabel());
        }
        else
        {
            buf.append(label);

            if (instance instanceof Classification classification)
            {
                Classification parent = classification.getParent();

                if (parent.getParent() != null)
                    buf.append(" (").append(parent.getPathName(false)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (isBenchmark())
                buf.append(" ").append(Messages.ChartSeriesBenchmarkSuffix); //$NON-NLS-1$
        }

        return buf.toString();
    }

    public void setColor(RGB color)
    {
        this.color = color;
    }

    public RGB getColor()
    {
        return color;
    }

    public boolean isLineChart()
    {
        return isLineChart;
    }

    public void setLineChart(boolean isLineChart)
    {
        this.isLineChart = isLineChart;
    }

    public boolean isBenchmark()
    {
        return isBenchmark;
    }

    public void setBenchmark(boolean isBenchmark)
    {
        this.isBenchmark = isBenchmark;
    }

    public boolean isShowArea()
    {
        return showArea;
    }

    public void setShowArea(boolean showArea)
    {
        this.showArea = showArea;
    }

    public LineStyle getLineStyle()
    {
        return lineStyle;
    }

    public void setLineStyle(LineStyle lineStyle)
    {
        this.lineStyle = lineStyle;
    }

    public int getLineWidth()
    {
        return lineWidth;
    }

    public void setLineWidth(int lineWidth)
    {
        this.lineWidth = lineWidth;
    }

    public Image getImage()
    {
        switch (instance instanceof DerivedDataSeries derived ? derived.getBaseDataSeries().getType() : type)
        {
            case SECURITY, SECURITY_BENCHMARK:
                return Images.SECURITY.image();
            case ACCOUNT, ACCOUNT_PRETAX:
                return Images.ACCOUNT.image();
            case PORTFOLIO, PORTFOLIO_PRETAX, PORTFOLIO_PLUS_ACCOUNT, PORTFOLIO_PLUS_ACCOUNT_PRETAX:
                return Images.PORTFOLIO.image();
            case CLASSIFICATION:
                return Images.CATEGORY.image();
            case CLIENT_FILTER, CLIENT_FILTER_PRETAX:
                return Images.GROUPEDACCOUNTS.image();
            default:
                return null;
        }
    }

    public String getUUID()
    {
        return this.type.buildUUID(instance);
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public void setVisible(boolean isVisible)
    {
        this.isVisible = isVisible;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Named.class && instance instanceof Named)
            return type.cast(instance);
        else if (type == Security.class && instance instanceof Security)
            return type.cast(instance);
        else if (type == Account.class && instance instanceof Account)
            return type.cast(instance);
        else
            return null;
    }

    @Override
    public String toString()
    {
        return getLabel() + " [" + getUUID() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
