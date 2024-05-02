package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.css.swt.engine.CSSSWTEngineImpl;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Copied from
 * {@link org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngineManager} because
 * IThemeEngine implementation is hard coded.
 */
@SuppressWarnings("restriction")
public class CustomThemeEngineManager implements IThemeManager
{
    private static final String KEY = "name.abuchen.portfolio.ui.theme.IThemeEngine"; //$NON-NLS-1$

    @Override
    public IThemeEngine getEngineForDisplay(Display display)
    {
        IThemeEngine engine = (IThemeEngine) display.getData(KEY);

        if (engine == null)
        {
            engine = new CustomThemeEngine(display);
            engine.addCSSEngine(getCSSSWTEngine(display));
            display.setData(KEY, engine);
        }
        return engine;
    }

    private CSSEngine getCSSSWTEngine(Display display)
    {
        CSSEngine cssEngine = WidgetElement.getEngine(display);
        if (cssEngine != null)
            return cssEngine;
        cssEngine = new CSSSWTEngineImpl(display, true);
        cssEngine.setErrorHandler(e -> logError(e.getMessage(), e));
        WidgetElement.setEngine(display, cssEngine);
        return cssEngine;
    }

    private static void logError(String message, Throwable e)
    {
        PortfolioPlugin.log(message, e);
    }
}
