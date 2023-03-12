package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class TaxonomyConfig implements WidgetConfig
{
    private WidgetDelegate<?> delegate;
    private Taxonomy taxonomy;

    public TaxonomyConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;

        String uuid = delegate.getWidget().getConfiguration().get(Dashboard.Config.TAXONOMY.name());

        if (uuid != null)
            delegate.getClient().getTaxonomies().stream().filter(t -> uuid.equals(t.getId())).findFirst()
                            .ifPresent(t -> this.taxonomy = t);

        if (taxonomy == null && !delegate.getClient().getTaxonomies().isEmpty())
            taxonomy = delegate.getClient().getTaxonomies().get(0);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                        new LabelOnly(taxonomy != null ? taxonomy.getName() : Messages.LabelNoName));

        MenuManager subMenu = new MenuManager(Messages.LabelTaxonomies);

        delegate.getClient().getTaxonomies().forEach(t -> {
            SimpleAction action = new SimpleAction(t.getName(), a -> {
                taxonomy = t;
                delegate.getWidget().getConfiguration().put(Dashboard.Config.TAXONOMY.name(), t.getId());

                delegate.update();
                delegate.getClient().touch();
            });
            action.setChecked(this.taxonomy == t);
            subMenu.add(action);
        });

        manager.add(subMenu);
    }

    public Taxonomy getTaxonomy()
    {
        return taxonomy;
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelTaxonomies + ": " //$NON-NLS-1$
                        + (taxonomy != null ? taxonomy.getName() : Messages.LabelNoName);
    }

}
