package name.abuchen.portfolio.ui.views.dataseries;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.swtchart.LineStyle;

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
        TOTALS, INVESTED_CAPITAL, ABSOLUTE_INVESTED_CAPITAL, TRANSFERALS, TAXES, TAXES_ACCUMULATED, ABSOLUTE_DELTA, ABSOLUTE_DELTA_ALL_RECORDS, //
        DIVIDENDS, DIVIDENDS_ACCUMULATED, INTEREST, INTEREST_ACCUMULATED, DELTA_PERCENTAGE, INTEREST_CHARGE, INTEREST_CHARGE_ACCUMULATED, //
        EARNINGS, EARNINGS_ACCUMULATED, FEES, FEES_ACCUMULATED;
    }

    public static final Map<ClientDataSeries, String> statementOfAssetsDataSeriesLabels = new EnumMap<>(ClientDataSeries.class)
    {
        private static final long serialVersionUID = 1319016001158914537L;

        {
            put(ClientDataSeries.TOTALS, Messages.LabelTotalSum);
            put(ClientDataSeries.TRANSFERALS, Messages.LabelTransferals);
            put(ClientDataSeries.INVESTED_CAPITAL, Messages.LabelInvestedCapital);
            put(ClientDataSeries.ABSOLUTE_INVESTED_CAPITAL, Messages.LabelAbsoluteInvestedCapital);
            put(ClientDataSeries.ABSOLUTE_DELTA, Messages.LabelDelta);
            put(ClientDataSeries.ABSOLUTE_DELTA_ALL_RECORDS, Messages.LabelAbsoluteDelta);
            put(ClientDataSeries.TAXES, Messages.ColumnTaxes);
            put(ClientDataSeries.TAXES_ACCUMULATED, Messages.LabelAccumulatedTaxes);
            put(ClientDataSeries.DIVIDENDS, Messages.LabelDividends);
            put(ClientDataSeries.DIVIDENDS_ACCUMULATED, Messages.LabelAccumulatedDividends);
            put(ClientDataSeries.INTEREST, Messages.LabelInterest);
            put(ClientDataSeries.INTEREST_ACCUMULATED, Messages.LabelAccumulatedInterest);
            put(ClientDataSeries.INTEREST_CHARGE, Messages.LabelInterestCharge);
            put(ClientDataSeries.INTEREST_CHARGE_ACCUMULATED, Messages.LabelAccumulatedInterestCharge);
            put(ClientDataSeries.EARNINGS, Messages.LabelEarnings);
            put(ClientDataSeries.EARNINGS_ACCUMULATED, Messages.LabelAccumulatedEarnings);
            put(ClientDataSeries.FEES, Messages.LabelFees);
            put(ClientDataSeries.FEES_ACCUMULATED, Messages.LabelFeesAccumulated);
        }
    };

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
        TYPE_PARENT("Type-Parent-", i -> ((GroupedDataSeries) i).getId()), //$NON-NLS-1$
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
    private Object[] groups;
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

    /* package */ DataSeries(Type type, Object[] groups, Object instance, String label, RGB color)
    {
        this.type = type;
        this.groups = groups;
        this.instance = instance;
        this.label = label;
        this.color = color;
    }

    /* package */ DataSeries(Type type, Object group, Object instance, String label, RGB color)
    {
        this.type = type;
        this.groups = group != null ? Arrays.asList(group).toArray() : null;
        this.instance = instance;
        this.label = label;
        this.color = color;
    }

    public Type getType()
    {
        return type;
    }

    public Object[] getGroups()
    {
        return groups;
    }

    public Object getInstance()
    {
        return instance;
    }

    public String getLabel()
    {
        if (instance instanceof GroupedDataSeries c && groups.length > 0)
            return groups[groups.length - 1] + " - " + label; //$NON-NLS-1$

        return isBenchmark() ? label + " " + Messages.ChartSeriesBenchmarkSuffix : label; //$NON-NLS-1$
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getSearchLabel()
    {
        StringBuilder buf = new StringBuilder();

        buf.append(label);

        if (instance instanceof Classification classification)
        {
            Classification parent = classification.getParent();

            if (parent.getParent() != null)
                buf.append(" (").append(parent.getPathName(false)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (isBenchmark())
            buf.append(" ").append(Messages.ChartSeriesBenchmarkSuffix); //$NON-NLS-1$

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
        switch (type)
        {
            case SECURITY:
            case SECURITY_BENCHMARK:
                return Images.SECURITY.image();
            case ACCOUNT:
            case ACCOUNT_PRETAX:
                return Images.ACCOUNT.image();
            case PORTFOLIO:
            case PORTFOLIO_PRETAX:
            case PORTFOLIO_PLUS_ACCOUNT:
            case PORTFOLIO_PLUS_ACCOUNT_PRETAX:
                return Images.PORTFOLIO.image();
            case CLASSIFICATION:
                return Images.CATEGORY.image();
            case CLIENT_FILTER:
            case CLIENT_FILTER_PRETAX:
                return Images.FILTER_OFF.image();
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
        return getSearchLabel() + " [" + getUUID() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
