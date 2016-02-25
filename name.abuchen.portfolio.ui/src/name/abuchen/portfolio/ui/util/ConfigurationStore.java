package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
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
import name.abuchen.portfolio.ui.Messages;

public class ConfigurationStore
{
    public interface ConfigurationStoreOwner
    {
        void beforeConfigurationPicked();

        void onConfigurationPicked(String data);
    }

    private static final class Configuration
    {
        private String name;
        private String data;

        public Configuration(String name, String data)
        {
            this.name = name;
            this.data = data;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getData()
        {
            return data;
        }

        public void setData(String data)
        {
            this.data = data;
        }

        String serialize()
        {
            return name + ":=" + data; //$NON-NLS-1$
        }
    }

    private static final class InputValidator implements IInputValidator
    {
        @Override
        public String isValid(String newText)
        {
            if (newText == null || newText.trim().isEmpty())
                return Messages.ConfigurationErrorMissingValue;

            if (newText.indexOf(":=") >= 0) //$NON-NLS-1$
                return Messages.ConfigurationErrorIllegalCharacters;

            return null;
        }
    }

    private static final String ACTIVE = "$active"; //$NON-NLS-1$

    private final String identifier;
    private final Client client;
    private final IPreferenceStore preferences;
    private final ConfigurationStoreOwner listener;

    private Configuration active;
    private List<Configuration> configurations = new ArrayList<Configuration>();

    private Menu contextMenu;

    public ConfigurationStore(String identifier, Client client, IPreferenceStore preferences,
                    ConfigurationStoreOwner listener)
    {
        this.identifier = identifier;
        this.client = client;
        this.preferences = preferences;
        this.listener = listener;

        loadConfigurations();
    }

    public void showSaveMenu(Shell shell)
    {
        if (contextMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    saveMenuAboutToShow(manager);
                }
            });
            contextMenu = menuMgr.createContextMenu(shell);
        }
        contextMenu.setVisible(true);
    }

    public void dispose()
    {
        if (contextMenu != null && !contextMenu.isDisposed())
            contextMenu.dispose();
    }

    private void saveMenuAboutToShow(IMenuManager manager)
    {
        for (final Configuration config : configurations)
        {
            Action action = new Action(config.getName())
            {
                @Override
                public void run()
                {
                    activate(config);
                }
            };
            action.setChecked(active == config);
            manager.add(action);
        }

        manager.add(new Separator());

        manager.add(new Action(Messages.ConfigurationNew)
        {
            @Override
            public void run()
            {
                createNew(null);
            }
        });

        manager.add(new Action(Messages.ConfigurationDuplicate)
        {
            @Override
            public void run()
            {
                createNew(active);
            }
        });

        manager.add(new Action(Messages.ConfigurationRename)
        {
            @Override
            public void run()
            {
                rename(active);
            }
        });

        manager.add(new Action(Messages.ConfigurationDelete)
        {
            @Override
            public void run()
            {
                delete(active);
            }
        });

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
        configurations.add(active);

        client.setProperty(identifier + '$' + (configurations.size() - 1), active.serialize());
        preferences.setValue(identifier + ACTIVE, configurations.size() - 1);

        listener.onConfigurationPicked(active.getData());
    }

    private void rename(Configuration config)
    {
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ConfigurationRename,
                        Messages.ChartSeriesPickerDialogMsg, config.getName(), new InputValidator());

        if (dlg.open() != InputDialog.OK)
            return;

        config.setName(dlg.getValue());
        client.setProperty(identifier + '$' + configurations.indexOf(config), config.serialize());
    }

    private void delete(Configuration config)
    {
        configurations.remove(config);

        // make sure at least one configuration exists
        if (configurations.isEmpty())
            configurations.add(new Configuration(Messages.ConfigurationStandard, null));

        listener.beforeConfigurationPicked();
        active = configurations.get(0);
        storeConfigurations();
        listener.onConfigurationPicked(active.getData());
    }

    private void activate(Configuration config)
    {
        listener.beforeConfigurationPicked();
        active = config;
        preferences.setValue(identifier + ACTIVE, configurations.indexOf(config));
        listener.onConfigurationPicked(config.getData());
    }

    public void updateActive(String data)
    {
        active.setData(data);
        client.setProperty(identifier + '$' + configurations.indexOf(active), active.serialize());
    }

    public String getActive()
    {
        return active.getData();
    }
    
    public String getActiveName()
    {
        return active.getName();
    }

    public void insertMigratedConfiguration(String data)
    {
        active = new Configuration(Messages.ConfigurationStandard, data);
        configurations.add(0, active);

        storeConfigurations();
    }

    private void loadConfigurations()
    {
        int index = 0;

        String config = client.getProperty(identifier + '$' + index);
        while (config != null)
        {
            String[] split = config.split(":="); //$NON-NLS-1$
            if (split.length == 2)
                configurations.add(new Configuration(split[0], split[1]));

            index++;
            config = client.getProperty(identifier + '$' + index);
        }

        // make sure at least on configuration exists at all times
        if (configurations.isEmpty())
            configurations.add(new Configuration(Messages.ConfigurationStandard, null));

        // read active configuration
        try
        {
            int activeIndex = preferences.getInt(identifier + ACTIVE);
            if (activeIndex >= 0 && activeIndex < configurations.size())
                active = configurations.get(activeIndex);
        }
        catch (NumberFormatException e)
        {
            // ignore -> use first
        }

        // make sure one configuration is active
        if (active == null)
            active = configurations.get(0);
    }

    private void storeConfigurations()
    {
        preferences.setValue(identifier + ACTIVE, configurations.indexOf(active));
        for (int index = 0; index < configurations.size(); index++)
        {
            Configuration config = configurations.get(index);
            client.setProperty(identifier + '$' + index, config.serialize());
        }
        client.removeProperty(identifier + '$' + configurations.size());
    }

}
