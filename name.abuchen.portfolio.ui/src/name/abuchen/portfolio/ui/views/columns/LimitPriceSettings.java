package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.model.TypedMap;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.ColorConversion;

public class LimitPriceSettings
{
    private interface PropertyKeys
    {
        String SHOW_RELATIVE_DIFF = "SHOW_RELATIVE_DIFF";//$NON-NLS-1$
        String SHOW_ABSOLUTE_DIFF = "SHOW_ABSOLUTE_DIFF";//$NON-NLS-1$
        String LIMIT_EXCEEDED_COLOR = "LIMIT_EXCEEDED_COLOR";//$NON-NLS-1$
        String LIMIT_UNDERCUT_COLOR = "LIMIT_UNDERCUT_COLOR";//$NON-NLS-1$
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
        return properties.getBoolean(PropertyKeys.SHOW_ABSOLUTE_DIFF).booleanValue();
    }
    
    public void setLimitExceededColor(Color value)
    {
        properties.putString(PropertyKeys.LIMIT_EXCEEDED_COLOR , ColorConversion.toHex(value.getRGBA()));
    }
    
    public Color getLimitExceededColor()
    {
        if(!properties.containsKey(PropertyKeys.LIMIT_EXCEEDED_COLOR))
            properties.putString(PropertyKeys.LIMIT_EXCEEDED_COLOR, ColorConversion.toHex(Colors.theme().greenBackground().getRGBA())); // default value
        
        return new Color(ColorConversion.hex2RGBA(properties.getString(PropertyKeys.LIMIT_EXCEEDED_COLOR)));
    }
    
    public void setLimitUndercutColor(Color value)
    {
        properties.putString(PropertyKeys.LIMIT_UNDERCUT_COLOR , ColorConversion.toHex(value.getRGBA()));
    }
    
    public Color getLimitUndercutColor()
    {
        if(!properties.containsKey(PropertyKeys.LIMIT_UNDERCUT_COLOR))
            properties.putString(PropertyKeys.LIMIT_UNDERCUT_COLOR, ColorConversion.toHex(Colors.theme().redBackground().getRGBA())); // default value
        
        return new Color(ColorConversion.hex2RGBA(properties.getString(PropertyKeys.LIMIT_UNDERCUT_COLOR)));
    }
    
    
    

}
