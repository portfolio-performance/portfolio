package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.ui.Messages;

/**
 * Stores a set of named configurations whereby one is the active configuration
 * at any given time. Each configuration has a name given by the user.
 */
public class ConfigurationStore
{
    public interface ConfigurationStoreOwner
    {
        void beforeConfigurationPicked();

        void onConfigurationPicked(String data);
    }

    private static final class InputValidator implements IInputValidator
    {
        @Override
        public String isValid(String newText)
        {
            return newText == null || newText.trim().isEmpty() ? Messages.ConfigurationErrorMissingValue : null;
        }
    }

    private static final String KEY_ACTIVE = "$picked"; //$NON-NLS-1$

    private final String identifier;
    private final Client client;
    private final IPreferenceStore preferences;
    private final ConfigurationStoreOwner listener;

    private final ConfigurationSet configSet;

    private Configuration active;

    private Menu contextMenu;

    public ConfigurationStore(String identifier, Client client, IPreferenceStore preferences,
                    ConfigurationStoreOwner listener)
    {
        this.identifier = identifier;
        this.client = client;
        this.preferences = preferences;
        this.listener = listener;

        this.configSet = client.getSettings().getConfigurationSet(identifier);

        // make one active (and there always must be one active)
        this.active = configSet.lookup(preferences.getString(identifier + KEY_ACTIVE))
                        .orElseGet(() -> configSet.getConfigurations().findFirst().orElseGet(() -> {
                            Configuration defaultConfig = new Configuration(Messages.ConfigurationStandard, null);
                            configSet.add(defaultConfig);
                            return defaultConfig;
                        }));

        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());
    }

    /**
     * Shows menu to manage views, e.g. create, copy, rename, and delete a view.
     * 
     * @param shell
     */
    public void showMenu(Shell shell)
    {
        if (contextMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(this::saveMenuAboutToShow);
            contextMenu = menuMgr.createContextMenu(shell);
        }
        contextMenu.setVisible(true);
    }

    /**
     * Disposes the configuration store.
     */
    public void dispose()
    {
        if (contextMenu != null && !contextMenu.isDisposed())
            contextMenu.dispose();
    }

    private void saveMenuAboutToShow(IMenuManager manager) // NOSONAR
    {
        configSet.getConfigurations().forEach(config -> {
            Action action = new SimpleAction(config.getName(), a -> activate(config));
            action.setChecked(active == config);
            manager.add(action);
        });

        manager.add(new Separator());

        manager.add(new SimpleAction(Messages.ConfigurationNew, a -> createNew(null)));
        manager.add(new SimpleAction(Messages.ConfigurationDuplicate, a -> createNew(active)));
        manager.add(new SimpleAction(Messages.ConfigurationRename, a -> rename(active)));
        manager.add(new ConfirmAction(Messages.ConfigurationDelete,
                        MessageFormat.format(Messages.ConfigurationDeleteConfirm, active.getName()),
                        a -> delete(active)));
    }

    private void createNew(Configuration template)
    {
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ConfigurationNew,
                        Messages.ChartSeriesPickerDialogMsg, template != null ? template.getName() : null,
                        new InputValidator());
        if (dlg.open() != InputDialog.OK)
            return;

        String name = dlg.getValue();

        listener.beforeConfigurationPicked();

        active = new Configuration(name, template != null ? template.getData() : null);

        configSet.add(active);
        client.markDirty();

        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());

        listener.onConfigurationPicked(active.getData());
    }

    private void rename(Configuration config)
    {
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ConfigurationRename,
                        Messages.ChartSeriesPickerDialogMsg, config.getName(), new InputValidator());

        if (dlg.open() != InputDialog.OK)
            return;

        config.setName(dlg.getValue());
        client.markDirty();
    }

    private void delete(Configuration config)
    {
        configSet.remove(config);

        listener.beforeConfigurationPicked();
        active = configSet.getConfigurations().findAny().orElseGet(() -> {
            Configuration defaultConfig = new Configuration(Messages.ConfigurationStandard, null);
            configSet.add(defaultConfig);
            return defaultConfig;
        });

        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());

        listener.onConfigurationPicked(active.getData());
    }

    private void activate(Configuration config)
    {
        listener.beforeConfigurationPicked();
        active = config;
        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());
        listener.onConfigurationPicked(config.getData());
    }

    public void updateActive(String data)
    {
        if (!Objects.equals(data, active.getData()))
        {
            active.setData(data);
            client.markDirty();
        }
    }

    public String getActive()
    {
        return active.getData();
    }

    public String getActiveName()
    {
        return active.getName();
    }

    public String getActiveUUID()
    {
        return active.getUUID();
    }

    public void insertMigratedConfiguration(String data)
    {
        active = new Configuration(Messages.ConfigurationStandard, data);
        configSet.add(active);
        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());
        client.markDirty();
    }
}
