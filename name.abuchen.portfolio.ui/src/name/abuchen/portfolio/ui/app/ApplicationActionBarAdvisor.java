package name.abuchen.portfolio.ui.app;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.handlers.IHandlerService;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor
{
    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
    }

    protected void makeActions(IWorkbenchWindow window)
    {}

    protected void fillMenuBar(IMenuManager menuBar)
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
                    public void widgetSelected(SelectionEvent event)
                    {
                        try
                        {
                            IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(
                                            IHandlerService.class);
                            handlerService.executeCommand("name.abuchen.portfolio.ui.commands.updateCommand", null); //$NON-NLS-1$
                        }
                        catch (CommandException e)
                        {
                            PortfolioPlugin.log(e);
                            ErrorDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, null,
                                            new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage()));
                        }
                    };
                });

                MenuItem newAppMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 2);
                newAppMenuItem.setText(Messages.SystemMenuShowErrorLog);
                newAppMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    public void widgetSelected(SelectionEvent event)
                    {
                        try
                        {
                            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                            window.getActivePage().showView("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
                        }
                        catch (PartInitException e)
                        {
                            PortfolioPlugin.log(e);
                        }
                    };
                });

                MenuItem newWelcomeMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 3);
                newWelcomeMenuItem.setText(Messages.SystemMenuWelcome);
                newWelcomeMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    public void widgetSelected(SelectionEvent event)
                    {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        IAction introAction = ActionFactory.INTRO.create(window);
                        introAction.run();
                    };
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
}
