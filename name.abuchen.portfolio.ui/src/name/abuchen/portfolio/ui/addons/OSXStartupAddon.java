package name.abuchen.portfolio.ui.addons;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("restriction")
public class OSXStartupAddon
{
    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    @PostConstruct
    public void setupOSXApplicationMenu()
    {
        if ("Mac OS X".equals(System.getProperty("os.name"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            Menu systemMenu = Display.getDefault().getSystemMenu();
            if (systemMenu != null)
            {
                int prefsIndex = systemMenu.indexOf(getItem(systemMenu, SWT.ID_PREFERENCES));

                MenuItem updatesMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 1);
                updatesMenuItem.setText(Messages.SystemMenuCheckForUpdates);
                updatesMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent event)
                    {
                        executeCommand("name.abuchen.portfolio.ui.command.updateproduct"); //$NON-NLS-1$
                    }
                });
            }
        }
    }

    private MenuItem getItem(Menu menu, int id)
    {
        MenuItem[] items = menu.getItems();
        for (int i = 0; i < items.length; i++)
        {
            if (items[i].getID() == id)
                return items[i];
        }
        return null;
    }

    private void executeCommand(String command)
    {
        Command cmd = commandService.getCommand(command);
        ParameterizedCommand pCmd = new ParameterizedCommand(cmd, null);
        if (handlerService.canExecute(pCmd))
        {
            handlerService.executeHandler(pCmd);
        }
    }
}
