package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.model.TypedMap;
import name.abuchen.portfolio.util.ColorConversion;

public class LimitPriceSettings
{
    private interface PropertyKeys // NOSONAR
    {
        String SHOW_RELATIVE_DIFF = "SHOW_RELATIVE_DIFF";//$NON-NLS-1$
        String SHOW_ABSOLUTE_DIFF = "SHOW_ABSOLUTE_DIFF";//$NON-NLS-1$
        String LIMIT_EXCEEDED_POSITIVELY_COLOR = "LIMIT_EXCEEDED_POSITIVELY_COLOR";//$NON-NLS-1$
        String LIMIT_UNDERCUT_NEGATIVELY_COLOR = "LIMIT_UNDERCUT_NEGATIVELY_COLOR";//$NON-NLS-1$
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

    public void setLimitExceededPositivelyColor(Color value)
    {
        if (value != null)
            properties.putString(PropertyKeys.LIMIT_EXCEEDED_POSITIVELY_COLOR, ColorConversion.toHex(value.getRGBA()));
        else
            properties.remove(PropertyKeys.LIMIT_EXCEEDED_POSITIVELY_COLOR);
    }

    public Color getLimitExceededPositivelyColor()
    {
        String hex = properties.getString(PropertyKeys.LIMIT_EXCEEDED_POSITIVELY_COLOR);
        return hex != null ? new Color(ColorConversion.hex2RGBA(hex)) : null;
    }

    public void setLimitExceededNegativelyColor(Color value)
    {
        if (value != null)
            properties.putString(PropertyKeys.LIMIT_UNDERCUT_NEGATIVELY_COLOR, ColorConversion.toHex(value.getRGBA()));
        else
            properties.remove(PropertyKeys.LIMIT_UNDERCUT_NEGATIVELY_COLOR);
    }

    public Color getLimitExceededNegativelyColor()
    {
        String hex = properties.getString(PropertyKeys.LIMIT_UNDERCUT_NEGATIVELY_COLOR);
        return hex != null ? new Color(ColorConversion.hex2RGBA(hex)) : null;
    }

}
