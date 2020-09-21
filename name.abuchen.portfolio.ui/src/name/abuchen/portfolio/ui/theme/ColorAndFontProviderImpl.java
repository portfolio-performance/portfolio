package name.abuchen.portfolio.ui.theme;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.internal.css.swt.definition.IColorAndFontProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import name.abuchen.portfolio.ui.PortfolioPlugin;

@SuppressWarnings("restriction")
@Component(property = EventConstants.EVENT_TOPIC + "=" + IThemeEngine.Events.THEME_CHANGED)
public class ColorAndFontProviderImpl implements IColorAndFontProvider, EventHandler
{
    private Map<String, RGB> colors = new HashMap<>();
    private Map<String, FontData[]> fonts = new HashMap<>();

    @Override
    public void handleEvent(Event event)
    {
        IThemeEngine engine = (IThemeEngine) event.getProperty(IThemeEngine.Events.THEME_ENGINE);

        ThemesExtension extension = new ThemesExtension();
        engine.applyStyles(extension, false);

        colors.clear();

        colors.put("org.eclipse.ui.workbench.ACTIVE_TAB_BG_START", //$NON-NLS-1$
                        Display.getDefault().getSystemColor(SWT.COLOR_TITLE_BACKGROUND).getRGB());

        colors.put("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END", //$NON-NLS-1$
                        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB());

        colors.put("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR", //$NON-NLS-1$
                        Display.getDefault().getSystemColor(SWT.COLOR_TITLE_FOREGROUND).getRGB());

        colors.put("org.eclipse.ui.workbench.INACTIVE_TAB_TEXT_COLOR", //$NON-NLS-1$
                        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND).getRGB());

        for (String symbolicName : extension.getColors())
        {
            ColorDefinition definition = new ColorDefinition(symbolicName);
            engine.applyStyles(definition, false);
            colors.put(symbolicName, definition.getValue());
        }

        fonts.clear();

        fonts.put("org.eclipse.ui.workbench.TAB_TEXT_FONT", Display.getDefault().getSystemFont().getFontData()); //$NON-NLS-1$

        for (String symbolicName : extension.getFonts())
        {
            FontDefinition definition = new FontDefinition(symbolicName);
            engine.applyStyles(definition, false);
            fonts.put(symbolicName, definition.getValue());
        }
    }

    @Override
    public FontData[] getFont(String symbolicName)
    {
        return fonts.computeIfAbsent(symbolicName, name -> {
            PortfolioPlugin.log("Missing font for symbolic name " + name); //$NON-NLS-1$
            return null;
        });
    }

    @Override
    public RGB getColor(String symbolicName)
    {
        return colors.computeIfAbsent(symbolicName, name -> {
            PortfolioPlugin.log("Missing color for symbolic name " + name); //$NON-NLS-1$
            return new RGB(0, 0, 0);
        });
    }

}
