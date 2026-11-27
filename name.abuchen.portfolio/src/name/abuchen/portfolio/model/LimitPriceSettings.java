package name.abuchen.portfolio.model;

import java.text.DecimalFormat;
import java.util.OptionalLong;
import java.util.StringJoiner;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.ColorConversion;

public class LimitPriceSettings
{
    private interface PropertyKeys // NOSONAR
    {
        String SHOW_RELATIVE_DIFF = "SHOW_RELATIVE_DIFF";//$NON-NLS-1$
        String SHOW_ABSOLUTE_DIFF = "SHOW_ABSOLUTE_DIFF";//$NON-NLS-1$
        String LIMIT_EXCEEDED_POSITIVELY_COLOR = "LIMIT_EXCEEDED_POSITIVELY_COLOR";//$NON-NLS-1$
        String LIMIT_EXCEEDED_NEGATIVELY_COLOR = "LIMIT_EXCEEDED_NEGATIVELY_COLOR";//$NON-NLS-1$
    }

    public LimitPriceSettings(TypedMap properties)
    {
        this.properties = properties;
    }

    private final TypedMap properties;

    public void setShowRelativeDiff(boolean value)
    {
        properties.putBoolean(PropertyKeys.SHOW_RELATIVE_DIFF, value);
    }

    public boolean getShowRelativeDiff()
    {
        return properties.getBoolean(PropertyKeys.SHOW_RELATIVE_DIFF);
    }

    public void setShowAbsoluteDiff(boolean value)
    {
        properties.putBoolean(PropertyKeys.SHOW_ABSOLUTE_DIFF, value);
    }

    public boolean getShowAbsoluteDiff()
    {
        return properties.getBoolean(PropertyKeys.SHOW_ABSOLUTE_DIFF);
    }

    public String getFullLabel(LimitPrice limit, SecurityPrice price)
    {
        if (!limit.isStatic())
            return limit.toString();

        return formatStaticLabel(limit.toString(), limit.getValue(), price);
    }

    public String getFullLabel(LimitPrice limit, OptionalLong resolvedQuote, SecurityPrice price)
    {
        if (limit.isStatic())
            return getFullLabel(limit, price);

        if (resolvedQuote == null || resolvedQuote.isEmpty())
            return limit.toString() + " (\u2014)"; //$NON-NLS-1$

        long limitQuote = resolvedQuote.getAsLong();
        var quoteLabel = Values.CalculatedQuote.format(limitQuote);
        var distanceSuffix = formatDistanceSuffix(limitQuote, price);

        if (distanceSuffix.isEmpty())
            return limit.toString() + " (" + quoteLabel + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        else
            return limit.toString() + ": " + quoteLabel + distanceSuffix; //$NON-NLS-1$
    }

    private String formatStaticLabel(String baseLabel, long limitQuote, SecurityPrice price)
    {
        if (price == null)
            return baseLabel;

        return baseLabel + formatDistanceSuffix(limitQuote, price);
    }

    private String formatDistanceSuffix(long limitQuote, SecurityPrice price)
    {
        if (price == null)
            return ""; //$NON-NLS-1$

        var joiner = new StringJoiner(" / ", " (", ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        joiner.setEmptyValue(""); //$NON-NLS-1$
        if (getShowAbsoluteDiff())
        {
            double absDistance = (limitQuote - price.getValue()) / Values.Quote.divider();
            var df = new DecimalFormat("+#.##;-#.##"); //$NON-NLS-1$
            joiner.add(df.format(absDistance));
        }

        if (getShowRelativeDiff())
        {
            var df = new DecimalFormat("+#.#%;-#.#%"); //$NON-NLS-1$
            joiner.add(df.format(LimitPrice.calculateRelativeDistance(limitQuote, price.getValue())));
        }

        return joiner.toString();
    }

    public void setLimitExceededPositivelyColor(Color value)
    {
        if (value != null)
            properties.putString(PropertyKeys.LIMIT_EXCEEDED_POSITIVELY_COLOR, ColorConversion.toHex(value.getRGBA()));
        else
            properties.remove(PropertyKeys.LIMIT_EXCEEDED_POSITIVELY_COLOR);
    }

    public Color getLimitExceededPositivelyColor()
    {
        return getLimitExceededPositivelyColor(null);
    }

    public Color getLimitExceededPositivelyColor(Color fallback)
    {
        String hex = properties.getString(PropertyKeys.LIMIT_EXCEEDED_POSITIVELY_COLOR);
        return hex != null ? new Color(ColorConversion.hex2RGBA(hex)) : fallback;
    }

    public void setLimitExceededNegativelyColor(Color value)
    {
        if (value != null)
            properties.putString(PropertyKeys.LIMIT_EXCEEDED_NEGATIVELY_COLOR, ColorConversion.toHex(value.getRGBA()));
        else
            properties.remove(PropertyKeys.LIMIT_EXCEEDED_NEGATIVELY_COLOR);
    }

    public Color getLimitExceededNegativelyColor()
    {
        return getLimitExceededNegativelyColor(null);
    }

    public Color getLimitExceededNegativelyColor(Color fallback)
    {
        String hex = properties.getString(PropertyKeys.LIMIT_EXCEEDED_NEGATIVELY_COLOR);
        return hex != null ? new Color(ColorConversion.hex2RGBA(hex)) : fallback;
    }
}
