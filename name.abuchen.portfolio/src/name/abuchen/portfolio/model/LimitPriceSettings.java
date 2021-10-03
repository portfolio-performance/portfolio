package name.abuchen.portfolio.model;

import org.eclipse.swt.graphics.Color;

public class LimitPriceSettings
{
    private interface PropertyKeys
    {
        String SHOW_RELATIVE_DIFF = "SHOW_RELATIVE_DIFF";//$NON-NLS-1$
        String SHOW_ABSOLUTE_DIFF = "SHOW_ABSOLUTE_DIFF";//$NON-NLS-1$
    }
    
    
    public LimitPriceSettings(TypedMap properties)
    {
        this.properties = properties;
    }
    
    /**
     * If null color from theme should be used
     */
    private Color limitExceededColor;
    /**
     * If null color from theme should be used
     */
    private Color limitUndercutColor;
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
    
    public void setLimitExceededColor(Color value)
    {
        limitExceededColor = value;
    }
    
    /**
     * If null color from theme should be used
     */
    public Color getLimitExceededColor()
    {
        return limitExceededColor;
    }
    
    public void setLimitUndercutColor(Color value)
    {
        limitUndercutColor = value;
    }
    
    /**
     * If null color from theme should be used
     */
    public Color getLimitUndercutColor()
    {
        return limitUndercutColor;
    }
    

}
