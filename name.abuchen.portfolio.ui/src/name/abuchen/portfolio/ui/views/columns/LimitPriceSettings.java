package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.model.TypedMap;
import name.abuchen.portfolio.ui.util.Colors;

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
        properties.putRGBA(PropertyKeys.LIMIT_EXCEEDED_COLOR , value.getRGBA());
    }
    
    public Color getLimitExceededColor()
    {
        if(!properties.containsKey(PropertyKeys.LIMIT_EXCEEDED_COLOR))
            properties.putRGBA(PropertyKeys.LIMIT_EXCEEDED_COLOR, Colors.theme().greenBackground().getRGBA()); // default value
        
        return new Color(properties.getRGBA(PropertyKeys.LIMIT_EXCEEDED_COLOR));
    }
    
    public void setLimitUndercutColor(Color value)
    {
        properties.putRGBA(PropertyKeys.LIMIT_UNDERCUT_COLOR , value.getRGBA());
    }
    
    public Color getLimitUndercutColor()
    {
        if(!properties.containsKey(PropertyKeys.LIMIT_UNDERCUT_COLOR))
            properties.putRGBA(PropertyKeys.LIMIT_UNDERCUT_COLOR, Colors.theme().redBackground().getRGBA()); // default value
        
        return new Color(properties.getRGBA(PropertyKeys.LIMIT_UNDERCUT_COLOR));
    }
    

}
