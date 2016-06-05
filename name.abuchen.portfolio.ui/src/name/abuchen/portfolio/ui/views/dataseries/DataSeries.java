package name.abuchen.portfolio.ui.views.dataseries;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator.ClientDataSeries;

public final class DataSeries
{
    private Class<?> type;
    private Object instance;
    private String label;
    private boolean isLineChart = true;
    private boolean isBenchmark = false;
    private boolean isPortfolioPlus = false;

    private RGB color;

    private boolean showArea;
    private LineStyle lineStyle = LineStyle.SOLID;

    /* package */ DataSeries(Class<?> type, Object instance, String label, RGB color)
    {
        this.type = type;
        this.instance = instance;
        this.label = label;
        this.color = color;
    }

    public Class<?> getType()
    {
        return type;
    }

    public Object getInstance()
    {
        return instance;
    }

    public String getLabel()
    {
        return isBenchmark() ? label + Messages.ChartSeriesBenchmarkSuffix : label;
    }

    public String getSearchLabel()
    {
        StringBuilder buf = new StringBuilder();

        buf.append(label);

        if (instance instanceof Classification)
        {
            Classification parent = ((Classification) instance).getParent();
            buf.append(" (").append(parent.getPathName(true)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (isBenchmark())
            buf.append(Messages.ChartSeriesBenchmarkSuffix);

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

    public boolean isPortfolioPlus()
    {
        return isPortfolioPlus;
    }

    public void setPortfolioPlus(boolean isPortfolioPlus)
    {
        this.isPortfolioPlus = isPortfolioPlus;
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

    public Image getImage()
    {
        if (type == Security.class)
            return Images.SECURITY.image();
        else if (type == Account.class)
            return Images.ACCOUNT.image();
        else if (type == Portfolio.class)
            return Images.PORTFOLIO.image();
        else if (type == Classification.class)
            return Images.CATEGORY.image();
        else
            return null;
    }

    public String getUUID()
    {
        String prefix = isBenchmark() ? "[b]" : ""; //$NON-NLS-1$ //$NON-NLS-2$
        if (isPortfolioPlus())
            prefix += "[+]"; //$NON-NLS-1$

        if (type == Security.class)
            return prefix + Security.class.getSimpleName() + ((Security) instance).getUUID();
        else if (type == Client.class)
            return prefix + Client.class.getSimpleName() + "-" //$NON-NLS-1$
                            + ((ClientDataSeries) instance).name().toLowerCase(Locale.US);
        else if (type == Account.class)
            return prefix + Account.class.getSimpleName() + ((Account) instance).getUUID();
        else if (type == Portfolio.class)
            return prefix + Portfolio.class.getSimpleName() + ((Portfolio) instance).getUUID();
        else if (type == ConsumerPriceIndex.class)
            return prefix + ConsumerPriceIndex.class.getSimpleName();
        else if (type == Classification.class)
            return prefix + Classification.class.getSimpleName() + ((Classification) instance).getId();

        throw new UnsupportedOperationException(type.getName());
    }
}