package name.abuchen.portfolio.model;

import org.eclipse.swt.graphics.Color;

public class LimitPriceSettings
{
    private boolean showRelativeDiff;
    private boolean showAbsoluteDiff;
    /**
     * If null color from theme should be used
     */
    private Color limitExceededColor;
    /**
     * If null color from theme should be used
     */
    private Color limitUndercutColor;
    
    
    public void setShowRelativeDiff(boolean value)
    {
        showRelativeDiff = value;        
    }
    
    public boolean getShowRelativeDiff()
    {
        return showRelativeDiff;
    }
    
    public void setShowAbsoluteDiff(boolean value)
    {
        showAbsoluteDiff = value;        
    }
    
    public boolean getShowAbsoluteDiff()
    {
        return showAbsoluteDiff;
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
    
    public LimitPriceSettings()
    {
        showRelativeDiff = false;
        showAbsoluteDiff = false;
    }
}
