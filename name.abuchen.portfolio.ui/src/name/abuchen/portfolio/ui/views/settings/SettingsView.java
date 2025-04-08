package name.abuchen.portfolio.ui.views.settings;

import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.AbstractTabbedView;
import name.abuchen.portfolio.ui.views.AttributeSettingsPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;

public class SettingsView extends AbstractTabbedView<AbstractTabbedView.Tab>
{
    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelSettings;
    }

    @Override
    protected List<AbstractTabbedView.Tab> createTabs()
    {
        return Arrays.asList(make(BookmarksListTab.class), //
                        make(AttributeListTab.class, AttributeListTab.Mode.SECURITY),
                        make(AttributeListTab.class, AttributeListTab.Mode.ACCOUNT),
                        make(AttributeListTab.class, AttributeListTab.Mode.PORTFOLIO),
                        make(AttributeListTab.class, AttributeListTab.Mode.INVESTMENT_PLAN));
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(AttributeSettingsPane.class));
    }

}
