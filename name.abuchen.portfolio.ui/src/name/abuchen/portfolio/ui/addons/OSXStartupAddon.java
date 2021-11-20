package name.abuchen.portfolio.ui.addons;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

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

                MenuItem noteworthyMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 2);
                noteworthyMenuItem.setText(Messages.SystemMenuNewAndNoteworthy);
                noteworthyMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent event)
                    {
                        executeCommand("name.abuchen.portfolio.ui.command.openBrowser", //$NON-NLS-1$
                                        UIConstants.Parameter.URL,
                                        "https://forum.portfolio-performance.info/t/sunny-neues-nennenswertes/23/last"); //$NON-NLS-1$
                    }
                });

                MenuItem changelogMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 3);
                changelogMenuItem.setText(Messages.SystemMenuChangelog);
                changelogMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent event)
                    {
                        executeCommand("name.abuchen.portfolio.ui.command.openBrowser", //$NON-NLS-1$
                                        UIConstants.Parameter.URL,
                                        "https://www.portfolio-performance.info/portfolio/versions.html"); //$NON-NLS-1$
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

    private void executeCommand(String command, String... parameters)
    {
        try
        {
            Command cmd = commandService.getCommand(command);

            List<Parameterization> parameterizations = new ArrayList<>();
            if (parameters != null)
            {
                for (int ii = 0; ii < parameters.length; ii = ii + 2)
                {
                    IParameter p = cmd.getParameter(parameters[ii]);
                    parameterizations.add(new Parameterization(p, parameters[ii + 1]));
                }
            }

            ParameterizedCommand pCmd = new ParameterizedCommand(cmd,
                            parameterizations.toArray(new Parameterization[0]));
            if (handlerService.canExecute(pCmd))
                handlerService.executeHandler(pCmd);
        }
        catch (NotDefinedException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
        }
    }
}
