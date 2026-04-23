package name.abuchen.portfolio.ui.theme;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.ColorGradientDefinitions;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DataSeriesColors;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.views.payments.PaymentsPalette;

@SuppressWarnings("restriction")
public class ThemeAddon
{
    @Inject
    private IEventBroker broker;

    @Inject
    @Optional
    public void onThemeChanged(@UIEventTopic(IThemeEngine.Events.THEME_CHANGED) Event event)
    {
        IThemeEngine engine = (IThemeEngine) event.getProperty(IThemeEngine.Events.THEME_ENGINE);
        engine.applyStyles(Colors.theme(), false);
        engine.applyStyles(DataSeriesColors.instance(), false);
        engine.applyStyles(PaymentsPalette.instance(), false);
        engine.applyStyles(ColorGradientDefinitions.redToGreen(), false);
        engine.applyStyles(ColorGradientDefinitions.orangeToBlue(), false);

        for (var scheme : ValueColorScheme.getAvailableSchemes())
            engine.applyStyles(scheme, false);

        broker.post(UIConstants.Event.Global.VALUE_COLOR_SCHEME_CHANGED,
                        ValueColorScheme.current().getIdentifier());
    }

}
