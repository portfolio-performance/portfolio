package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

public class ConfigurationStore
{
    public interface ConfigurationStoreOwner
    {
        String getCurrentConfiguration();

        void handleConfigurationReset();

        void handleConfigurationPicked(String data);
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

        public String getData()
        {
            return data;
        }

        public void setData(String data)
        {
            this.data = data;
        }
    }

    private final String identifier;
    private final Client client;
    private final ConfigurationStoreOwner listener;

    private List<Configuration> storedConfigurations = new ArrayList<Configuration>();

    private Menu contextMenu;

    public ConfigurationStore(String identifier, Client client, ConfigurationStoreOwner listener)
    {
        this.identifier = identifier;
        this.client = client;
        this.listener = listener;

        loadStoredConfigurations();
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
        final String currentConfiguration = listener.getCurrentConfiguration();

        for (final Configuration config : storedConfigurations)
        {
            if (config.getData().equals(currentConfiguration))
            {
                Action action = new Action(config.getName())
                {
                    @Override
                    public void run()
                    {
                        listener.handleConfigurationReset();
                    }
                };
                action.setChecked(true);
                manager.add(action);
            }
            else
            {
                Action action = new Action(config.getName())
                {
                    @Override
                    public void run()
                    {
                        listener.handleConfigurationPicked(config.getData());
                    }
                };
                manager.add(action);
            }
        }

        manager.add(new Separator());

        manager.add(new Action(Messages.ChartSeriesPickerSave)
        {
            @Override
            public void run()
            {
                doSaveConfiguration(currentConfiguration);
            }
        });

        if (!storedConfigurations.isEmpty())
        {
            MenuManager configMenu = new MenuManager(Messages.ChartSeriesPickerDelete);
            for (final Configuration config : storedConfigurations)
            {
                configMenu.add(new Action(config.getName())
                {
                    @Override
                    public void run()
                    {
                        storedConfigurations.remove(config);
                        persistStoredConfigurations();
                    }
                });
            }
            manager.add(configMenu);
        }
    }

    private void doSaveConfiguration(String currentConfiguration)
    {
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ChartSeriesPickerDialogTitle,
                        Messages.ChartSeriesPickerDialogMsg, null, null);
        if (dlg.open() != InputDialog.OK)
            return;

        String name = dlg.getValue();

        boolean replace = false;
        for (Configuration config : storedConfigurations)
        {
            if (name.equals(config.getName()))
            {
                config.setData(currentConfiguration);
                replace = true;
                break;
            }
        }

        if (!replace)
            storedConfigurations.add(new Configuration(name, currentConfiguration));
        persistStoredConfigurations();
    }

    private void loadStoredConfigurations()
    {
        int index = 0;

        String config = client.getProperty(identifier + '$' + index);
        while (config != null)
        {
            String[] split = config.split(":="); //$NON-NLS-1$
            storedConfigurations.add(new Configuration(split[0], split[1]));

            index++;
            config = client.getProperty(identifier + '$' + index);
        }
    }

    private void persistStoredConfigurations()
    {
        for (int index = 0; index < storedConfigurations.size(); index++)
        {
            Configuration config = storedConfigurations.get(index);
            client.setProperty(identifier + '$' + index, config.getName() + ":=" + config.getData()); //$NON-NLS-1$
        }

        client.removeProperity(identifier + '$' + storedConfigurations.size());
    }

}
