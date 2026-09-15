package name.abuchen.portfolio.ui.addons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

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
import name.abuchen.portfolio.ui.theme.CustomThemeEngineManager;
import name.abuchen.portfolio.ui.theme.ThemePreferences;
import name.abuchen.portfolio.ui.update.UpdateHelper;

public class OSXStartupAddon
{
    private static final int THEME_POLL_INTERVAL_MS = 2000;
    private static final String APPLE_INTERFACE_STYLE_ABSENT = "AppleInterfaceStyle) does not exist"; //$NON-NLS-1$

    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    private Display display;
    private boolean lastSystemDarkTheme;
    private Runnable themePoller;
    private final CustomThemeEngineManager themeEngineManager = new CustomThemeEngineManager();
    private final ExecutorService themeProbeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MacOS Theme Poller"); //$NON-NLS-1$
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean isThemeProbeRunning = new AtomicBoolean();

    @PostConstruct
    public void setupOSXApplicationMenu()
    {
        if ("Mac OS X".equals(System.getProperty("os.name"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            Menu systemMenu = Display.getDefault().getSystemMenu();
            if (systemMenu != null)
            {
                int prefsIndex = systemMenu.indexOf(getItem(systemMenu, SWT.ID_PREFERENCES));

                var prefsItem = getItem(systemMenu, SWT.ID_PREFERENCES);
                if (isAtLeastMacOS13() && prefsItem != null)
                {
                    // "Preferences" changed to "Settings" since MacOS 13
                    prefsItem.setText(Messages.LabelSettings + "..."); //$NON-NLS-1$
                }

                if (UpdateHelper.isInAppUpdateEnabled())
                {
                    MenuItem updatesMenuItem = new MenuItem(systemMenu, SWT.CASCADE, ++prefsIndex);
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

                MenuItem noteworthyMenuItem = new MenuItem(systemMenu, SWT.CASCADE, ++prefsIndex);
                noteworthyMenuItem.setText(Messages.SystemMenuNewAndNoteworthy);
                noteworthyMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent event)
                    {
                        String url = "de".equals(Locale.getDefault().getLanguage()) //$NON-NLS-1$
                                        ? "https://forum.portfolio-performance.info/t/sunny-neues-nennenswertes/23/last" //$NON-NLS-1$
                                        : "https://forum.portfolio-performance.info/t/new-noteworthy/17945/last"; //$NON-NLS-1$

                        executeCommand("name.abuchen.portfolio.ui.command.openBrowser", //$NON-NLS-1$
                                        UIConstants.Parameter.URL, url);
                    }
                });

                MenuItem changelogMenuItem = new MenuItem(systemMenu, SWT.CASCADE, ++prefsIndex);
                changelogMenuItem.setText(Messages.SystemMenuChangelog);
                changelogMenuItem.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent event)
                    {
                        executeCommand("name.abuchen.portfolio.ui.command.openBrowser", //$NON-NLS-1$
                                        UIConstants.Parameter.URL, "https://github.com/portfolio-performance/portfolio/releases"); //$NON-NLS-1$
                    }
                });

            }
        }
    }

    @PostConstruct
    public void monitorSystemTheme(Display display)
    {
        if (!"Mac OS X".equals(System.getProperty("os.name"))) //$NON-NLS-1$ //$NON-NLS-2$
            return;

        this.display = display;
        Boolean initialSystemDarkTheme = detectMacOSDarkMode();
        this.lastSystemDarkTheme = initialSystemDarkTheme != null ? initialSystemDarkTheme.booleanValue()
                        : UIConstants.Theme.DARK.equals(themeEngineManager.getEngineForDisplay(display).getActiveTheme().getId());

        this.themePoller = new Runnable()
        {
            @Override
            public void run()
            {
                if (OSXStartupAddon.this.display == null || OSXStartupAddon.this.display.isDisposed())
                    return;

                OSXStartupAddon.this.display.timerExec(THEME_POLL_INTERVAL_MS, this);
                pollSystemThemeAsync();
            }
        };

        this.display.timerExec(THEME_POLL_INTERVAL_MS, themePoller);
    }

    @PreDestroy
    public void stopMonitoringSystemTheme()
    {
        if (display != null && !display.isDisposed() && themePoller != null)
            display.timerExec(-1, themePoller);

        themeProbeExecutor.shutdownNow();
    }

    private boolean isAtLeastMacOS13()
    {
        var osVersion = System.getProperty("os.version"); //$NON-NLS-1$
        if (osVersion == null)
            return false;

        try
        {
            var parts = osVersion.split("\\."); //$NON-NLS-1$
            int major = Integer.parseInt(parts[0]);
            return major >= 13;
        }
        catch (NumberFormatException | IndexOutOfBoundsException e)
        {
            return false;
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

    private void pollSystemThemeAsync()
    {
        if (!isThemeProbeRunning.compareAndSet(false, true))
            return;

        try
        {
            themeProbeExecutor.execute(() -> {
                try
                {
                    Boolean isDark = detectMacOSDarkMode();
                    if (isDark == null)
                        return;

                    Display currentDisplay = OSXStartupAddon.this.display;
                    if (currentDisplay == null || currentDisplay.isDisposed())
                        return;

                    currentDisplay.asyncExec(() -> applyDetectedTheme(isDark.booleanValue()));
                }
                finally
                {
                    isThemeProbeRunning.set(false);
                }
            });
        }
        catch (RejectedExecutionException e)
        {
            isThemeProbeRunning.set(false);
        }
    }

    private void applyDetectedTheme(boolean isDark)
    {
        if (display == null || display.isDisposed() || isDark == lastSystemDarkTheme)
            return;

        var configuredTheme = ThemePreferences.getConfiguredThemeId();
        lastSystemDarkTheme = isDark;

        if (configuredTheme.isEmpty())
        {
            // SWT.Settings is not reliable for macOS appearance changes in this application,
            // so poll the system setting and update the automatic theme explicitly.
            String themeId = isDark ? UIConstants.Theme.DARK : UIConstants.Theme.LIGHT;
            themeEngineManager.getEngineForDisplay(display).setTheme(themeId, false);
        }
    }

    private Boolean detectMacOSDarkMode()
    {
        Process process = null;

        try
        {
            process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").start(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            String output = new String(process.getInputStream().readAllBytes()).trim();
            String error = new String(process.getErrorStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode == 0)
                return Boolean.valueOf("Dark".equalsIgnoreCase(output)); //$NON-NLS-1$

            if (isMissingAppleInterfaceStyle(error))
                return Boolean.FALSE;

            return null;
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            return null;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        finally
        {
            if (process != null)
                process.destroy();
        }
    }

    private boolean isMissingAppleInterfaceStyle(String error)
    {
        return error != null && error.contains(APPLE_INTERFACE_STYLE_ABSENT);
    }
}
