package name.abuchen.portfolio.ui.theme;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.ui.util.Colors;

@SuppressWarnings("restriction")
public class ThemeAddon
{
    @Inject
    @Optional
    public void onThemeChanged(@UIEventTopic(IThemeEngine.Events.THEME_CHANGED) Event event)
    {
        IThemeEngine engine = (IThemeEngine) event.getProperty(IThemeEngine.Events.THEME_ENGINE);
        engine.applyStyles(Colors.theme(), false);
    }

}
