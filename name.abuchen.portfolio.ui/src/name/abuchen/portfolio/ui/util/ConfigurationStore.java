package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.ui.Images;
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

        default void onConfigurationSetUpdated()
        {
        }
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
    private final List<ConfigurationStoreOwner> listeners = new ArrayList<>();

    private final ConfigurationSet configSet;

    private Configuration active;

    public ConfigurationStore(String identifier, Client client, IPreferenceStore preferences,
                    ConfigurationStoreOwner listener)
    {
        this.identifier = identifier;
        this.client = client;
        this.preferences = preferences;
        this.listeners.add(listener);

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

    public void setToolBarManager(ToolBarManager toolBar)
    {
        createToolBarItems(toolBar);
        toolBar.update(true);

        this.listeners.add(new ConfigurationStoreOwner()
        {
            @Override
            public void onConfigurationPicked(String data)
            {
                onConfigurationSetUpdated();
            }

            @Override
            public void beforeConfigurationPicked()
            {
                // no changes to the toolbar before switching to a new
                // configuration
            }

            @Override
            public void onConfigurationSetUpdated()
            {
                if (toolBar.getControl().isDisposed())
                    return;
                toolBar.removeAll();
                createToolBarItems(toolBar);
                toolBar.update(true);

            }
        });
    }

    private void createToolBarItems(ToolBarManager toolBar)
    {
        configSet.getConfigurations().forEach(config -> {
            DropDown item = new DropDown(config.getName(),
                            config.equals(active) ? Images.VIEW_SELECTED : Images.VIEW);

            item.setMenuListener(manager -> {

                if (!config.equals(active))
                {
                    manager.add(new SimpleAction(Messages.MenuShow, a -> activate(config)));
                    manager.add(new Separator());
                }

                manager.add(new SimpleAction(Messages.ConfigurationDuplicate, a -> createNew(config)));
                manager.add(new SimpleAction(Messages.ConfigurationRename, a -> rename(config)));
                manager.add(new ConfirmAction(Messages.ConfigurationDelete,
                                MessageFormat.format(Messages.ConfigurationDeleteConfirm, config.getName()),
                                a -> delete(config)));

                int index = configSet.indexOf(config);
                if (index > 0)
                {
                    manager.add(new Separator());
                    manager.add(new SimpleAction(Messages.ChartBringToFront, a -> {
                        configSet.remove(config);
                        configSet.add(0, config);
                        toolBar.removeAll();
                        createToolBarItems(toolBar);
                        toolBar.update(true);
                    }));
                }
            });

            item.setDefaultAction(new SimpleAction(a -> activate(config)));

            toolBar.add(item);
        });

        Action createNew = new SimpleAction(a -> createNew(null));
        createNew.setImageDescriptor(Images.VIEW_PLUS.descriptor());
        createNew.setToolTipText(Messages.ConfigurationNew);
        toolBar.add(createNew);
    }

    private void createNew(Configuration template)
    {
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ConfigurationNew,
                        Messages.ChartSeriesPickerDialogMsg, template != null ? template.getName() : null,
                        new InputValidator());
        if (dlg.open() != InputDialog.OK)
            return;

        String name = dlg.getValue();

        listeners.forEach(ConfigurationStoreOwner::beforeConfigurationPicked);

        active = new Configuration(name, template != null ? template.getData() : null);

        configSet.add(active);
        client.touch();

        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());

        listeners.forEach(l -> l.onConfigurationPicked(active.getData()));
    }

    private void rename(Configuration config)
    {
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ConfigurationRename,
                        Messages.ChartSeriesPickerDialogMsg, config.getName(), new InputValidator());

        if (dlg.open() != InputDialog.OK)
            return;

        config.setName(dlg.getValue());
        client.touch();
        listeners.forEach(ConfigurationStoreOwner::onConfigurationSetUpdated);
    }

    private void delete(Configuration config)
    {
        configSet.remove(config);

        if (active != config)
        {
            listeners.forEach(ConfigurationStoreOwner::onConfigurationSetUpdated);
            return;
        }

        listeners.forEach(ConfigurationStoreOwner::beforeConfigurationPicked);
        active = configSet.getConfigurations().findAny().orElseGet(() -> {
            Configuration defaultConfig = new Configuration(Messages.ConfigurationStandard, null);
            configSet.add(defaultConfig);
            return defaultConfig;
        });

        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());

        listeners.forEach(l -> l.onConfigurationPicked(active.getData()));
    }

    private void activate(Configuration config)
    {
        listeners.forEach(ConfigurationStoreOwner::beforeConfigurationPicked);
        active = config;
        preferences.setValue(identifier + KEY_ACTIVE, active.getUUID());
        listeners.forEach(l -> l.onConfigurationPicked(config.getData()));
    }

    public void updateActive(String data)
    {
        if (!Objects.equals(data, active.getData()))
        {
            active.setData(data);
            client.touch();
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
        client.touch();
    }
}
