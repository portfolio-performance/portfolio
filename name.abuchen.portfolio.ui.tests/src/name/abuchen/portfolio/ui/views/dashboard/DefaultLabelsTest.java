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

    @Test
    public void testTemplateLabelsExistAsMessages()
    {
        for (TemplateLabel label : TemplateLabel.values())
            assertThat(label.getKey(), DefaultLabels.currentText(label.getKey()).isPresent(), is(true));
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
