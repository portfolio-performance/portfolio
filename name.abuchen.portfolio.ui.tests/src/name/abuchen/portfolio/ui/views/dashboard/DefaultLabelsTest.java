package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.DefaultLabels.TemplateLabel;

@SuppressWarnings("nls")
public class DefaultLabelsTest
{
    private static final String KEY = Dashboard.Config.DEFAULT_LABEL.name();

    // widget types are given by name: accessing WidgetFactory requires a
    // display (images)
    private static final String HEADING = "HEADING";
    private static final String TTWROR = "TTWROR";
    private static final String TOTAL_SUM = "TOTAL_SUM";

    private static Dashboard.Widget widget(String type, String label)
    {
        var widget = new Dashboard.Widget();
        widget.setType(type);
        widget.setLabel(label);
        return widget;
    }

    @Test
    public void testTemplateLabelsExistAsMessages()
    {
        for (TemplateLabel label : TemplateLabel.values())
            assertThat(label.getKey(), DefaultLabels.currentText(label.getKey()).isPresent(), is(true));
    }

    @Test
    public void testHeadingInOtherLanguageIsDetected()
    {
        var english = widget(HEADING, "Key Indicators");
        DefaultLabels.detect(english, Messages.LabelHeading, "");
        assertThat(english.getConfiguration().get(KEY), is("LabelKeyIndicators"));

        var german = widget(HEADING, "Kennzahlen");
        DefaultLabels.detect(german, Messages.LabelHeading, "");
        assertThat(german.getConfiguration().get(KEY), is("LabelKeyIndicators"));
    }

    @Test
    public void testWidgetTypeLabelInOtherLanguageIsDetected()
    {
        var english = widget(TTWROR, "True Time-Weighted Rate of Return (cumulative)");
        DefaultLabels.detect(english, Messages.LabelTTWROR, "");
        assertThat(english.getConfiguration().get(KEY), is(DefaultLabels.WIDGET_TYPE));

        var vietnamese = widget(TTWROR, "T\u1EF7 l\u1EC7 sinh l\u1EDDi th\u1EDDi gian th\u1EF1c (c\u1ED9ng d\u1ED3n)");
        DefaultLabels.detect(vietnamese, Messages.LabelTTWROR, "");
        assertThat(vietnamese.getConfiguration().get(KEY), is(DefaultLabels.WIDGET_TYPE));
    }

    @Test
    public void testWidgetTypeLabelWithDataSeriesIsDetected()
    {
        var label = "True Time-Weighted Rate of Return (cumulative), Depot A";

        var withSuffix = widget(TTWROR, label);
        DefaultLabels.detect(withSuffix, Messages.LabelTTWROR, ", Depot A");
        assertThat(withSuffix.getConfiguration().get(KEY), is(DefaultLabels.WIDGET_TYPE));

        // the label must match the data series of the widget
        var otherSeries = widget(TTWROR, label);
        DefaultLabels.detect(otherSeries, Messages.LabelTTWROR, ", Depot B");
        assertThat(otherSeries.getConfiguration().get(KEY), is(nullValue()));
    }

    @Test
    public void testCustomLabelIsKept()
    {
        var widget = widget(HEADING, "My heading");
        DefaultLabels.detect(widget, Messages.LabelHeading, "");
        assertThat(widget.getConfiguration().get(KEY), is(nullValue()));
        assertThat(widget.getLabel(), is("My heading"));
    }

    @Test
    public void testOnlyDefaultsOfTheWidgetTypeAreCompared()
    {
        // a heading text used as label of an indicator widget is a custom
        // label
        var widget = widget(TTWROR, "Key Indicators");
        DefaultLabels.detect(widget, Messages.LabelTTWROR, "");
        assertThat(widget.getConfiguration().get(KEY), is(nullValue()));
    }

    @Test
    public void testUnmarkedDefaultLabelFollowsLanguage()
    {
        var widget = widget(HEADING, "Key Indicators");

        DefaultLabels.update(widget, Messages.LabelHeading, "");

        assertThat(widget.getLabel(), is(Messages.LabelKeyIndicators));
    }

    @Test
    public void testAmbiguousTypeLabelIsOnlyComparedInCurrentLanguage()
    {
        // in English, "Total" is the text of LabelTotalSum and ColumnTotal;
        // "Gesamtbetrag" is the German text of ColumnTotal only
        var widget = widget(TOTAL_SUM, "Gesamtbetrag");

        DefaultLabels.update(widget, Messages.LabelTotalSum, "");

        assertThat(widget.getLabel(), is("Gesamtbetrag"));
        assertThat(widget.getConfiguration().get(KEY), is(nullValue()));
    }

    @Test
    public void testEditedLabelIsNotDetected()
    {
        // the user deliberately entered the default text of another language
        var widget = widget(TTWROR, "True Time-Weighted Rate of Return (cumulative)");
        DefaultLabels.clear(widget);

        DefaultLabels.update(widget, Messages.LabelTTWROR, "");

        assertThat(widget.getLabel(), is("True Time-Weighted Rate of Return (cumulative)"));
        assertThat(DefaultLabels.isDefault(widget), is(false));
    }

    @Test
    public void testEditedDashboardNameIsNotDetected()
    {
        var dashboard = new Dashboard("1");
        DefaultLabels.setDefaultName(dashboard);
        DefaultLabels.clear(dashboard);
        dashboard.setName("Tableau de bord");

        DefaultLabels.update(dashboard);

        assertThat(dashboard.getName(), is("Tableau de bord"));
    }

    @Test
    public void testMarkedDashboardNameFollowsLanguage()
    {
        var dashboard = new Dashboard("1");
        DefaultLabels.setDefaultName(dashboard);
        dashboard.setName("Dashboard in another language");

        DefaultLabels.update(dashboard);

        assertThat(dashboard.getName(), is(Messages.LabelDashboard));
    }

    @Test
    public void testDefaultDashboardNameFollowsLanguage()
    {
        var dashboard = new Dashboard("1");
        dashboard.setName("B\u1EA3ng \u0111i\u1EC1u khi\u1EC3n");

        DefaultLabels.update(dashboard);

        assertThat(dashboard.getConfiguration().get(KEY), is("LabelDashboard"));
        assertThat(dashboard.getName(), is(Messages.LabelDashboard));
    }

    @Test
    public void testCustomDashboardNameIsKept()
    {
        var dashboard = new Dashboard("1");
        dashboard.setName("Mein Depot");

        DefaultLabels.update(dashboard);

        assertThat(dashboard.getConfiguration().get(KEY), is(nullValue()));
        assertThat(dashboard.getName(), is("Mein Depot"));
    }

    @Test
    public void testRenamedDashboardIsNotUpdated()
    {
        var dashboard = new Dashboard("1");
        DefaultLabels.setDefaultName(dashboard);
        DefaultLabels.clear(dashboard);
        dashboard.setName("Mein Depot");

        DefaultLabels.update(dashboard);

        assertThat(dashboard.getName(), is("Mein Depot"));
    }
}
