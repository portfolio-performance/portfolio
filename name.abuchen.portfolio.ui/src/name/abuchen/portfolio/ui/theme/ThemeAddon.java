package name.abuchen.portfolio.ui.theme;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ValueColorScheme;

@SuppressWarnings("restriction")
public class ThemeAddon
{
    private Display display;
    private Listener settingsListener;
    private boolean lastSystemDarkTheme;
    private final CustomThemeEngineManager themeEngineManager = new CustomThemeEngineManager();

    @PostConstruct
    public void trackSystemTheme(Display display)
    {
        if ("Mac OS X".equals(System.getProperty("os.name"))) //$NON-NLS-1$ //$NON-NLS-2$
            return;

        this.display = display;
        this.lastSystemDarkTheme = Display.isSystemDarkTheme();
        this.settingsListener = event -> {
            var configuredTheme = ThemePreferences.getConfiguredThemeId();
            boolean isDark = Display.isSystemDarkTheme();

            if (configuredTheme.isPresent())
                return;

            if (isDark == lastSystemDarkTheme)
                return;

            lastSystemDarkTheme = isDark;
            IThemeEngine engine = themeEngineManager.getEngineForDisplay(display);
            engine.setTheme(isDark ? UIConstants.Theme.DARK : UIConstants.Theme.LIGHT, false);
        };

        display.addListener(SWT.Settings, settingsListener);
    }

    @Inject
    @Optional
    public void onThemeChanged(@UIEventTopic(IThemeEngine.Events.THEME_CHANGED) Event event)
    {
        IThemeEngine engine = (IThemeEngine) event.getProperty(IThemeEngine.Events.THEME_ENGINE);
        engine.applyStyles(Colors.theme(), false);

        for (var scheme : ValueColorScheme.getAvailableSchemes())
            engine.applyStyles(scheme, false);
    }

    @PreDestroy
    public void stopTrackingSystemTheme()
    {
        if (display != null && !display.isDisposed() && settingsListener != null)
            display.removeListener(SWT.Settings, settingsListener);
    }
}
