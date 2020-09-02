package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import name.abuchen.portfolio.ui.util.Colors;

class CellDataProvider
{
    private Color color;
    private Font font;
    private String text;

    public CellDataProvider(Color color, Font font, String text)
    {
        this.color = color;
        this.font = font;
        this.text = text;
    }
    
    public CellDataProvider(String text)
    {
        this(Colors.WHITE, null, text);
    }

    Color getBackground()
    {
        return color;
    }

    Font getFont()
    {
        return font;
    }

    String getText()
    {
        return text;
    }
}