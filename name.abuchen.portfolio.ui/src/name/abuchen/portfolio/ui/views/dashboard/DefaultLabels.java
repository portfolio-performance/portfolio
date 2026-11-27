package name.abuchen.portfolio.ui.views.dashboard;

import java.lang.reflect.Modifier;
import java.util.Optional;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;

/**
 * Keeps the default labels of dashboards and widgets in the language of the
 * user interface.
 * <p>
 * Labels are stored as text in the file. A default label is additionally
 * marked with {@link Dashboard.Config#DEFAULT_LABEL}: the value is either the
 * key of the message or {@link #WIDGET_TYPE} if the label is derived from the
 * widget type. When a dashboard is shown, marked labels are replaced with the
 * text of the current language. Labels edited by the user are not marked and
 * are kept as they are.
 */
public final class DefaultLabels
{
    /**
     * Messages used as labels by the dashboard templates.
     */
    public enum TemplateLabel
    {
        // widget types are given by name: accessing WidgetFactory requires
        // a display (images)

        DASHBOARD("LabelDashboard", null), //$NON-NLS-1$
        DASHBOARD_USE("DescriptionDashboardUse", "DESCRIPTION"), //$NON-NLS-1$ //$NON-NLS-2$
        KEY_INDICATORS("LabelKeyIndicators", "HEADING"), //$NON-NLS-1$ //$NON-NLS-2$
        LAST_DAY("LabelTTWROROneDay", "HEADING"), //$NON-NLS-1$ //$NON-NLS-2$
        RISK_INDICATORS("LabelRiskIndicators", "HEADING"), //$NON-NLS-1$ //$NON-NLS-2$
        CALCULATION("PerformanceTabCalculation", "HEADING"), //$NON-NLS-1$ //$NON-NLS-2$
        EARNINGS("LabelEarnings", "HEADING"); //$NON-NLS-1$ //$NON-NLS-2$

        private final String key;
        private final String widgetType;

        private TemplateLabel(String key, String widgetType)
        {
            this.key = key;
            this.widgetType = widgetType;
        }

        public String getKey()
        {
            return key;
        }

        /**
         * Returns the name of the widget type using this label or null for the
         * name of a dashboard.
         */
        public String getWidgetType()
        {
            return widgetType;
        }

        public String getText()
        {
            return currentText(key).orElseThrow(() -> new IllegalArgumentException(key));
        }
    }

    /* package */ static final String WIDGET_TYPE = "type"; //$NON-NLS-1$

    private static final String KEY = Dashboard.Config.DEFAULT_LABEL.name();

    private DefaultLabels()
    {
    }

    /**
     * Marks the label of the widget as default label of the widget type.
     */
    public static void markAsDefault(Dashboard.Widget widget)
    {
        widget.getConfiguration().put(KEY, WIDGET_TYPE);
    }

    /**
     * Sets the label of the widget to the given template label.
     */
    public static void setLabel(Dashboard.Widget widget, TemplateLabel label)
    {
        widget.setLabel(label.getText());
        widget.getConfiguration().put(KEY, label.getKey());
    }

    /**
     * Sets the name of the dashboard to the default name.
     */
    public static void setDefaultName(Dashboard dashboard)
    {
        dashboard.setName(TemplateLabel.DASHBOARD.getText());
        dashboard.getConfiguration().put(KEY, TemplateLabel.DASHBOARD.getKey());
    }

    public static boolean isDefault(Dashboard.Widget widget)
    {
        return widget.getConfiguration().containsKey(KEY);
    }

    /**
     * Removes the marker, e.g. after the user edited the label.
     */
    public static void clear(Dashboard.Widget widget)
    {
        widget.getConfiguration().remove(KEY);
    }

    /**
     * Removes the marker, e.g. after the user renamed the dashboard.
     */
    public static void clear(Dashboard dashboard)
    {
        dashboard.getConfiguration().remove(KEY);
    }

    /**
     * Resets the label of the widget to the default label of the widget type.
     */
    public static void reset(WidgetDelegate<?> delegate)
    {
        markAsDefault(delegate.getWidget());
        delegate.getWidget().setLabel(delegate.getDefaultLabel());
    }

    /**
     * Updates a default label of the widget to the current language.
     */
    public static void update(WidgetDelegate<?> delegate)
    {
        var widget = delegate.getWidget();
        var marker = widget.getConfiguration().get(KEY);
        if (marker == null)
            return;

        var label = WIDGET_TYPE.equals(marker) ? Optional.of(delegate.getDefaultLabel()) : currentText(marker);
        label.ifPresent(widget::setLabel);
    }

    /**
     * Updates a default name of the dashboard to the current language.
     */
    public static void update(Dashboard dashboard)
    {
        var marker = dashboard.getConfiguration().get(KEY);
        if (marker != null)
            currentText(marker).ifPresent(dashboard::setName);
    }

    /**
     * Returns the text of the message in the current language.
     */
    /* package */ static Optional<String> currentText(String key)
    {
        try
        {
            var field = Messages.class.getField(key);
            return field.getType() == String.class && Modifier.isStatic(field.getModifiers())
                            ? Optional.ofNullable((String) field.get(null))
                            : Optional.empty();
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            return Optional.empty();
        }
    }
}
