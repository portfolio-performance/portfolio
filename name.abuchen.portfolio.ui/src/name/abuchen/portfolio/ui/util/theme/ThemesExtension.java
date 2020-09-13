package name.abuchen.portfolio.ui.util.theme;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.internal.css.swt.definition.IThemesExtension;

@SuppressWarnings("restriction")
public class ThemesExtension implements IThemesExtension
{
    List<String> colors = new ArrayList<>();
    List<String> fonts = new ArrayList<>();

    @Override
    public void addFontDefinition(String symbolicName)
    {
        fonts.add(symbolicName);
    }

    public List<String> getFonts()
    {
        return fonts;
    }

    @Override
    public void addColorDefinition(String symbolicName)
    {
        colors.add(symbolicName);
    }

    public List<String> getColors()
    {
        return colors;
    }
}
