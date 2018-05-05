package name.abuchen.portfolio.ui.views.dataseries;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ConfigurationStore;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;
import name.abuchen.portfolio.ui.util.SimpleAction;

/**
 * The DataSeriesConfigurator manages the currently available set of data
 * series.
 */
public class DataSeriesConfigurator extends BasicDataSeriesConfigurator implements ConfigurationStoreOwner
{

    private final String identifier;
    private final IPreferenceStore preferences;
    private final ConfigurationStore store;

    private Menu configContextMenu;

    public DataSeriesConfigurator(AbstractFinanceView view, DataSeries.UseCase useCase)
    {
        super(view.getClient(), new DataSeriesSet(view.getClient(), view.getPreferenceStore(), useCase));

        this.identifier = view.getClass().getSimpleName() + IDENTIFIER_POSTFIX;
        this.preferences = view.getPreferenceStore();
        this.store = new ConfigurationStore(identifier, view.getClient(), preferences, this);

        this.setSelectedData(store.getActive());

        view.getControl().addDisposeListener(e -> DataSeriesConfigurator.this.widgetDisposed());
    }

    public String getConfigurationName()
    {
        return store.getActiveName();
    }

    /**
     * Shows the menu to add and remove data series from the current set of data
     * series.
     * 
     * @param shell
     */
    public void showMenu(Shell shell)
    {
        if (configContextMenu == null)
            configContextMenu = createMenu(shell, this::configMenuAboutToShow);
        configContextMenu.setVisible(true);
    }

    /**
     * Shows the menu to manage sets of data series, e.g. add, create, delete
     * sets.
     * 
     * @param shell
     */
    public void showSaveMenu(Shell shell)
    {
        store.showMenu(shell);
    }

    private Menu createMenu(Shell shell, IMenuListener listener)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);
        return menuMgr.createContextMenu(shell);
    }

    private void widgetDisposed()
    {
        if (configContextMenu != null && !configContextMenu.isDisposed())
            configContextMenu.dispose();

        store.dispose();
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        super.configMenuAboutToShow(manager);

        manager.add(new SimpleAction(Messages.MenuResetChartSeries, a -> doResetSeries(null)));
    }

    protected void doResetSeries(String config)
    {
        setSeriesDataSet(new DataSeriesSet(getClient(), preferences, getUseCase()), config);
    }

    @Override
    protected void fireUpdate()
    {
        super.fireUpdate();
        store.updateActive(new DataSeriesSerializer().toString(getSelectedDataSeries()));
    }

    @Override
    protected String getActiveUUID()
    {
        return store.getActiveUUID();
    }

    @Override
    public void beforeConfigurationPicked()
    {
        // do nothing - all configuration changes are stored via #updateActive
    }

    @Override
    public void onConfigurationPicked(String data)
    {
        this.doResetSeries(data);
    }
}
