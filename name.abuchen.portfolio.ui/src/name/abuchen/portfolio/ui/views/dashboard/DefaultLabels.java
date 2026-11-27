package name.abuchen.portfolio.ui.views.dashboard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.preferences.LanguagePreferencePage.Language;

/**
 * Keeps the default labels of dashboards and widgets in the language of the
 * user interface.
 * <p>
 * Labels are stored as text in the file. A default label is additionally
 * marked with {@link Dashboard.Config#DEFAULT_LABEL}: the value is either the
 * key of the message or {@link #WIDGET_TYPE} if the label is derived from the
 * widget type. When a dashboard is shown, marked labels are replaced with the
 * text of the current language. Labels edited by the user are marked as
 * {@link #CUSTOM} and kept as they are.
 * <p>
 * Files created before the marker existed are migrated: a label is marked if
 * it equals a default text in one of the supported languages. To avoid
 * mismatches between messages with the same text, only the defaults that are
 * possible for the widget type are compared.
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
    /* package */ static final String CUSTOM = "custom"; //$NON-NLS-1$

    private static final String KEY = Dashboard.Config.DEFAULT_LABEL.name();
    private static final String BUNDLE_NAME = "name.abuchen.portfolio.ui.messages"; //$NON-NLS-1$

    /** message bundles of all supported languages */
    private static List<ResourceBundle> bundles;

    /** message keys by their text in the current language */
    private static Map<String, List<String>> keysByText;

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
        var marker = widget.getConfiguration().get(KEY);
        return marker != null && !CUSTOM.equals(marker);
    }

    /**
     * Marks the label as edited by the user. Unlike a missing marker (files
     * created before the marker existed), it is excluded from the detection of
     * default labels.
     */
    public static void clear(Dashboard.Widget widget)
    {
        widget.getConfiguration().put(KEY, CUSTOM);
    }

    /**
     * Marks the name as edited by the user, see
     * {@link #clear(Dashboard.Widget)}.
     */
    public static void clear(Dashboard dashboard)
    {
        dashboard.getConfiguration().put(KEY, CUSTOM);
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
     * Updates a default label of the widget to the current language. Unmarked
     * labels are marked if they match a default text in any language.
     */
    public static void update(WidgetDelegate<?> delegate)
    {
        var widget = delegate.getWidget();
        update(widget, WidgetFactory.valueOf(widget.getType()).getLabel(), delegate.getDefaultLabelSuffix());
    }

    /**
     * @param typeLabel
     *            label of the widget type in the current language
     * @param suffix
     *            text appended to the label of the widget type, see
     *            {@link WidgetDelegate#getDefaultLabelSuffix()}
     */
    /* package */ static void update(Dashboard.Widget widget, String typeLabel, String suffix)
    {
        if (!widget.getConfiguration().containsKey(KEY))
            detect(widget, typeLabel, suffix);

        var marker = widget.getConfiguration().get(KEY);
        if (marker == null || CUSTOM.equals(marker))
            return;

        var label = WIDGET_TYPE.equals(marker) ? Optional.of(typeLabel + suffix) : currentText(marker);
        label.ifPresent(widget::setLabel);
    }

    /**
     * Updates a default name of the dashboard to the current language. An
     * unmarked name is marked if it matches the default name in any language.
     */
    public static void update(Dashboard dashboard)
    {
        var configuration = dashboard.getConfiguration();

        if (!configuration.containsKey(KEY) && dashboard.getName() != null
                        && textsOf(TemplateLabel.DASHBOARD.getKey()).contains(dashboard.getName()))
            configuration.put(KEY, TemplateLabel.DASHBOARD.getKey());

        var marker = configuration.get(KEY);
        if (marker != null && !CUSTOM.equals(marker))
            currentText(marker).ifPresent(dashboard::setName);
    }

    /**
     * Marks the label of the widget if it equals one of the default texts
     * possible for the widget type.
     *
     * @param typeLabel
     *            label of the widget type in the current language
     * @param suffix
     *            text appended to the label of the widget type, see
     *            {@link WidgetDelegate#getDefaultLabelSuffix()}
     */
    /* package */ static void detect(Dashboard.Widget widget, String typeLabel, String suffix)
    {
        var label = widget.getLabel();
        if (label == null)
            return;

        // compare with other languages only if the type label belongs to
        // exactly one message: if several messages share the text (e.g.
        // "Total"), their translations differ and could match a custom label
        var keys = keysOf(typeLabel);

        if (label.equals(typeLabel + suffix) || (keys.size() == 1
                        && textsOf(keys.get(0)).stream().anyMatch(t -> label.equals(t + suffix))))
        {
            markAsDefault(widget);
            return;
        }

        for (TemplateLabel template : TemplateLabel.values())
        {
            if (widget.getType().equals(template.getWidgetType()) && textsOf(template.getKey()).contains(label))
            {
                widget.getConfiguration().put(KEY, template.getKey());
                return;
            }
        }
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

    /**
     * Returns the texts of the message in all supported languages.
     */
    /* package */ static List<String> textsOf(String key)
    {
        var answer = new ArrayList<String>();
        currentText(key).ifPresent(answer::add);

        for (ResourceBundle bundle : getBundles())
        {
            if (bundle.containsKey(key))
                answer.add(bundle.getString(key));
        }

        return answer;
    }

    /**
     * Returns the keys of all messages with the given text in the current
     * language.
     */
    private static synchronized List<String> keysOf(String text)
    {
        if (keysByText == null)
        {
            keysByText = new HashMap<>();

            for (Field field : Messages.class.getFields())
            {
                currentText(field.getName()).ifPresent(
                                t -> keysByText.computeIfAbsent(t, k -> new ArrayList<>()).add(field.getName()));
            }
        }

        return keysByText.getOrDefault(text, List.of());
    }

    private static synchronized List<ResourceBundle> getBundles()
    {
        if (bundles == null)
        {
            var locales = new ArrayList<Locale>();
            locales.add(Locale.ROOT);
            for (Language language : Language.values())
            {
                if (language.getCode() != null)
                    locales.add(Locale.forLanguageTag(language.getCode()));
            }

            // regional translations which are not listed as language
            locales.add(Locale.forLanguageTag("pt-BR")); //$NON-NLS-1$
            locales.add(Locale.forLanguageTag("zh-TW")); //$NON-NLS-1$

            var control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

            bundles = locales.stream().map(locale -> {
                try
                {
                    return ResourceBundle.getBundle(BUNDLE_NAME, locale, DefaultLabels.class.getClassLoader(),
                                    control);
                }
                catch (MissingResourceException e)
                {
                    return null;
                }
            }).filter(Objects::nonNull).distinct().toList();
        }

        return bundles;
    }
}
