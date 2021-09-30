package name.abuchen.portfolio.model;

import org.eclipse.swt.graphics.Color;

public class LimitPriceSettings
{
    private boolean showRelativeDiff;
    private boolean showAbsoluteDiff;
    private Color limitExceededColor;
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
    
    public Color getLimitExceededColor()
    {
        return limitExceededColor;
    }
    
    public void setLimitUndercutColor(Color value)
    {
        limitUndercutColor = value;
    }
    
    public Color getLimitUndercutColor()
    {
        return limitUndercutColor;
    }
    
    public LimitPriceSettings()
    {
        showRelativeDiff = false;
        showAbsoluteDiff = false;
        // TODO: default color from theme (reference missing?)
        //limitExceededColor = Colors.theme().greenBackground();
        //limitUndercutColor = Colors.theme().redBackground();
    }
}
