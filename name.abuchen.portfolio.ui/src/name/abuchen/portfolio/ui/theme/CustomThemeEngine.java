package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.UIConstants;

@SuppressWarnings("restriction")
public class CustomThemeEngine extends ThemeEngine
{

    public CustomThemeEngine(Display display)
    {
        super(display);
    }

    @Override
    public void restore(String alternateTheme)
    {
        // if the preferences contain an (existing) theme id, then we activate
        // the theme

        // if the preferences do not contain any theme id (or a valid theme id),
        // then we activate a theme based on the system mode

        // we have to override/extend the eclipse classes because the default
        // dark theme is hard-coded to eclipse theme which is missing relevant
        // CSS directory, for example for the charts

        var prefThemeId = ThemePreferences.getConfiguredThemeId();

        if (prefThemeId.isPresent())
        {
            for (ITheme t : getThemes())
            {
                if (prefThemeId.get().equals(t.getId()))
                {
                    setTheme(t, false);
                    return;
                }
            }
        }

        boolean isDark = Display.isSystemDarkTheme();
        setTheme(isDark ? UIConstants.Theme.DARK : UIConstants.Theme.LIGHT, false);
    }

}
