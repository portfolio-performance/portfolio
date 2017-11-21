package name.abuchen.portfolio.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.p2.P2Service;
import name.abuchen.portfolio.p2.P2Service.ProfileChangeContext;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.update.UpdateHelper;
import name.abuchen.portfolio.ui.util.P2Util;

public class ManagePluginsDialog extends Dialog
{
    public class PluginPane
    {
        private class InstallableUnitState
        {
            private String availableVersion;
            private String installedVersion;
            private String description;
            private String changelog;
            private String id;

            private boolean updatable = false;
            private IInstallableUnit iu;
            private IInstallableUnit installedIU;

            public InstallableUnitState(IInstallableUnit iu)
            {
                this.iu = iu;
                // available version
                availableVersion = iu.getVersion().toString();

                // description & changelog
                description = iu.getProperty("org.eclipse.equinox.p2.description"); //$NON-NLS-1$
                changelog = iu.getProperty("portfolio.plugin.changelog"); //$NON-NLS-1$

                // id
                String name = iu.getProperty(IInstallableUnit.PROP_NAME, Locale.getDefault().getLanguage());
                if (name != null && !name.isEmpty())
                {
                    id = name;
                }
                else
                {
                    id = iu.getId();
                }

                // installed version
                installedIU = p2Service.findInstalledVersion(iu);
                if (installedIU != null)
                {
                    Version version = installedIU.getVersion();
                    installedVersion = version.toString();
                    if (iu.getVersion().compareTo(version) > 0)
                    {
                        updatable = true;
                    }
                }
                else
                {
                    installedVersion = Messages.ManagePluginsDialogPluginNotInstalled;
                }
            }

            public boolean isInstalled()
            {
                return installedIU != null;
            }

            public IInstallableUnit getIu()
            {
                return iu;
            }

            public IInstallableUnit getInstalledIU()
            {
                return installedIU;
            }

            public String getId()
            {
                return id;
            }

            public String getAvailableVersion()
            {
                return availableVersion;
            }

            public String getInstalledVersion()
            {
                return installedVersion;
            }

            public String getChangelog()
            {
                return changelog == null ? "" : changelog; //$NON-NLS-1$
            }

            public String getDescription()
            {
                return description == null ? "" : description; //$NON-NLS-1$
            }
        }

        private TableViewer installedUnitsTable;
        private Composite progressPane;
        private Button installUpdateButton;
        private Button uninstallButton;

        public PluginPane(Composite area)
        {
            createPane(area);
        }

        public void refresh()
        {
            if (!installedUnitsTable.getControl().isDisposed())
            {
                installedUnitsTable.refresh();
                installUpdateButton.setEnabled(false);
                uninstallButton.setEnabled(false);
            }
        }

        private void createPane(Composite area)
        {
            Group mainArea = new Group(area, SWT.SHADOW_IN);
            mainArea.setText(Messages.ManagePluginsDialogHandlePlugins);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(mainArea);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(mainArea);

            progressPane = new Composite(mainArea, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(progressPane);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(progressPane);

            installedUnitsTable = new TableViewer(mainArea,
                            SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            installedUnitsTable.getTable().setHeaderVisible(true);
            initInstalledUnitsTable(installedUnitsTable);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(installedUnitsTable.getTable());
            installedUnitsTable.setInput(preferences);

            Composite rightArea = new Composite(mainArea, SWT.NONE);
            GridDataFactory.fillDefaults().grab(false, true).applyTo(rightArea);
            rightArea.setLayout(new GridLayout(1, false));
            installUpdateButton = new Button(rightArea, SWT.NONE);
            installUpdateButton.setText(Messages.ManagePluginsDialogInstallUpdate);
            installUpdateButton.setEnabled(false);
            uninstallButton = new Button(rightArea, SWT.NONE);
            uninstallButton.setText(Messages.ManagePluginsDialogUninstall);
            uninstallButton.setEnabled(false);

            Group detailsArea = new Group(mainArea, SWT.SHADOW_NONE);
            detailsArea.setText(Messages.ManagePluginsDialogPluginDetails);
            StyledText pluginDetailsText = new StyledText(detailsArea, SWT.V_SCROLL);
            pluginDetailsText.setEditable(false);
            pluginDetailsText.setWordWrap(true);
            GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(detailsArea);
            GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(detailsArea);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(pluginDetailsText);

            GridDataFactory.fillDefaults().grab(true, false).applyTo(installUpdateButton);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(uninstallButton);

            installedUnitsTable.addSelectionChangedListener(event -> {
                IStructuredSelection structuredSelection = installedUnitsTable.getStructuredSelection();
                if (!structuredSelection.isEmpty())
                {
                    Object selectedIU = structuredSelection.getFirstElement();
                    if (selectedIU instanceof InstallableUnitState)
                    {
                        InstallableUnitState ius = (InstallableUnitState) selectedIU;
                        installUpdateButton.setEnabled(!ius.isInstalled() || ius.updatable);
                        uninstallButton.setEnabled(ius.isInstalled());

                        StringBuilder sb = new StringBuilder();
                        StyleRange[] styles = new StyleRange[2];

                        StyleRange descriptionStyle = new StyleRange();
                        descriptionStyle.fontStyle = SWT.BOLD;
                        descriptionStyle.start = sb.length();
                        sb.append(Messages.ManagePluginsDialogPluginDetailsDescription);
                        descriptionStyle.length = sb.length() - descriptionStyle.start;
                        styles[0] = descriptionStyle;
                        sb.append(System.lineSeparator());
                        sb.append(ius.getDescription());

                        sb.append(System.lineSeparator());
                        sb.append(System.lineSeparator());
                        StyleRange changelogStyle = new StyleRange();
                        changelogStyle.fontStyle = SWT.BOLD;
                        changelogStyle.start = sb.length();
                        sb.append(Messages.ManagePluginsDialogPluginDetailsChangelog);
                        changelogStyle.length = sb.length() - changelogStyle.start;
                        styles[1] = changelogStyle;
                        sb.append(System.lineSeparator());
                        String changelog = ius.getChangelog();
                        if (changelog.isEmpty())
                        {
                            sb.append("---"); //$NON-NLS-1$
                        }
                        else
                        {
                            sb.append(changelog);
                        }

                        pluginDetailsText.setText(sb.toString());
                        pluginDetailsText.setStyleRanges(styles);
                    }
                }
            });

            installUpdateButton.addSelectionListener(new SelectionAdapter()
            {
                @SuppressWarnings("unchecked")
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    IStructuredSelection structuredSelection = installedUnitsTable.getStructuredSelection();
                    if (!structuredSelection.isEmpty())
                    {
                        InstallableUnitState ius = (InstallableUnitState) structuredSelection.getFirstElement();

                        if (!ius.isInstalled())
                        {
                            installIUs(structuredSelection.toList());
                        }
                        else if (ius.updatable)
                        {
                            updateIUs(structuredSelection.toList());
                        }
                    }
                }
            });

            uninstallButton.addSelectionListener(new SelectionAdapter()
            {
                @SuppressWarnings("unchecked")
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    IStructuredSelection structuredSelection = installedUnitsTable.getStructuredSelection();
                    if (!structuredSelection.isEmpty())
                    {
                        uninstallIUs(structuredSelection.toList());
                    }
                }
            });
        }

        private void initInstalledUnitsTable(TableViewer installedUnitsTable)
        {
            TableViewerColumn column = new TableViewerColumn(installedUnitsTable, SWT.NONE);
            column.getColumn().setText(Messages.ManagePluginsDialogPluginId);
            column.getColumn().setWidth(240);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((InstallableUnitState) element).getId();
                }
            });

            column = new TableViewerColumn(installedUnitsTable, SWT.NONE);
            column.getColumn().setText(Messages.ManagePluginsDialogPluginVersionInstalled);
            column.getColumn().setWidth(125);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    InstallableUnitState ius = (InstallableUnitState) element;
                    if (ius.isInstalled())
                    {
                        return ius.getInstalledVersion();
                    }
                    else
                    {
                        return Messages.ManagePluginsDialogPluginNotInstalled;
                    }
                }
            });

            column = new TableViewerColumn(installedUnitsTable, SWT.NONE);
            column.getColumn().setText(Messages.ManagePluginsDialogPluginVersionAvailable);
            column.getColumn().setWidth(125);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    InstallableUnitState ius = (InstallableUnitState) element;
                    return ius.getAvailableVersion();
                }
            });

            installedUnitsTable.setContentProvider((IStructuredContentProvider) inputElement -> {
                if (inputElement instanceof IEclipsePreferences)
                {
                    Set<IInstallableUnit> installablePlugins = fetchUnitsFromUpdateSite(
                                    P2Util.toURIs((IEclipsePreferences) inputElement));

                    p2Service.getInstalledPlugins().forEach(iu -> {
                        if (!installablePlugins.contains(iu))
                        {
                            installablePlugins.add(iu);
                        }
                    });

                    return installablePlugins.stream().map(InstallableUnitState::new)
                                    .toArray(InstallableUnitState[]::new);
                }
                return new Object[0];
            });
        }

        private Set<IInstallableUnit> fetchUnitsFromUpdateSite(Collection<URI> updateSites)
        {
            Set<IInstallableUnit> installableUnits = new TreeSet<>((iu1, iu2) -> {
                return iu1.getId().compareTo(iu2.getId());
            });
            for (URI updateSite : updateSites)
            {
                Collection<IInstallableUnit> cachedUpdateSites = cachedInstallableUnits.get(updateSite);
                if (cachedUpdateSites == null || cachedUpdateSites.isEmpty())
                {
                    cachedInstallableUnits.put(updateSite, Collections.emptyList());
                    new Job(String.format("Fetch installable units from %s", updateSite)) //$NON-NLS-1$
                    {
                        @Override
                        protected IStatus run(IProgressMonitor monitor)
                        {
                            JobProgressBar progressBar = new JobProgressBar(progressPane);
                            progressBar.setText(Messages.bind(Messages.ManagePluginsDialogFetchUpdateSite, updateSite));
                            try
                            {
                                List<IInstallableUnit> installableUnits = p2Service.fetchPluginsFromUpdateSite(
                                                updateSite, SubMonitor.convert(progressBar, 10));

                                cachedInstallableUnits.put(updateSite, installableUnits);
                            }
                            catch (Exception e)
                            {
                                cachedInstallableUnits.remove(updateSite);
                                progressBar.setText(Messages.ManagePluginsDialogError + ": " + e.getMessage()); //$NON-NLS-1$
                                return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID,
                                                String.format("Fetching of installable units from %s canceled", //$NON-NLS-1$
                                                                updateSite),
                                                e);
                            }
                            progressBar.setText(Messages.bind(Messages.ManagePluginsDialogFetched, updateSite));
                            progressBar.done();
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
                else
                {
                    installableUnits.addAll(cachedUpdateSites);
                }
            }
            return installableUnits;
        }

        private void installIUs(Collection<InstallableUnitState> toInstall)
        {
            changeIUs(p2Service.install,
                            toInstall.stream().map(InstallableUnitState::getIu).collect(Collectors.toList()),
                            Messages.ManagePluginsDialogInstallation, Messages.ManagePluginsDialogInstallationFailed);
        }

        private void uninstallIUs(Collection<InstallableUnitState> toUninstall)
        {
            changeIUs(p2Service.uninstall,
                            toUninstall.stream().map(InstallableUnitState::getInstalledIU).collect(Collectors.toList()),
                            Messages.ManagePluginsDialogUninstallation,
                            Messages.ManagePluginsDialogUninstallationFailed);
        }

        private void updateIUs(Collection<InstallableUnitState> toUpdate)
        {
            changeIUs(p2Service.uninstall,
                            toUpdate.stream().map(InstallableUnitState::getInstalledIU).collect(Collectors.toList()),
                            Messages.ManagePluginsDialogUpdate, Messages.ManagePluginsDialogUpdateFailed);
        }

        private void changeIUs(Function<ProfileChangeContext, IStatus> changeOperation, List<IInstallableUnit> units,
                        String changeType, String failureMessage)
        {
            JobProgressBar jobProgressBar = new JobProgressBar(progressPane);
            ProfileChangeContext context = new ProfileChangeContext(units, P2Util.toURIs(preferences), jobProgressBar);
            try
            {
                jobProgressBar.setText(Messages.bind(Messages.ManagePluginsDialogStartProfileChangeOperation,
                                changeType, units.stream().map(iu -> {
                                    String name = iu.getProperty(IInstallableUnit.PROP_NAME,
                                                    Locale.getDefault().getLanguage());
                                    if (name != null && !name.isEmpty())
                                    {
                                        return name;
                                    }
                                    else
                                    {
                                        return iu.getId();
                                    }
                                }).collect(Collectors.joining(", ")))); //$NON-NLS-1$

                IStatus status = changeOperation.apply(context);

                if (status.isOK())
                {
                    restartRequired = true;
                }
                else
                {
                    jobProgressBar.setText(failureMessage);
                    jobProgressBar.setTooltip(status.toString());
                    jobProgressBar.error();
                    PortfolioPlugin.log(status);
                }
            }
            catch (Exception ex)
            {
                jobProgressBar.setText(failureMessage);
                jobProgressBar.setTooltip(ex.toString());
                jobProgressBar.error();
                PortfolioPlugin.log(ex);
            }
        }
    }

    private class RepositoryPane
    {

        private final class RemoveRepositorySelectionListener extends SelectionAdapter
        {
            private final TableViewer availableRepositories;

            private RemoveRepositorySelectionListener(TableViewer availableRepositories)
            {
                this.availableRepositories = availableRepositories;
            }

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IStructuredSelection structuredSelection = availableRepositories.getStructuredSelection();
                if (!structuredSelection.isEmpty())
                {
                    for (Object item : structuredSelection.toList())
                    {
                        if (item != null)
                        {
                            preferences.remove(item.toString());
                        }
                    }
                    availableRepositories.refresh();
                    pluginPane.refresh();
                }
            }
        }

        private final class AddRepositorySelectionListener extends SelectionAdapter
        {
            private final TableViewer availableRepositories;

            private AddRepositorySelectionListener(TableViewer availableRepositories)
            {
                this.availableRepositories = availableRepositories;
            }

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                InputDialog inputDialog = new InputDialog(getShell(),
                                Messages.ManagePluginsDialogNewRepositoryDialogTitle,
                                Messages.ManagePluginsDialogNewRepositoryDialogText, "", updateSite -> { //$NON-NLS-1$
                                    try
                                    {
                                        URI uri = new URI(updateSite);
                                        if (!uri.isAbsolute())
                                            return Messages.ManagePluginsDialogURIMustBeAbsolute;
                                    }
                                    catch (URISyntaxException e1)
                                    {
                                        return Messages.ManagePluginsDialogInvalidURI;
                                    }
                                    return null;
                                });
                if (inputDialog.open() == Window.OK)
                {
                    preferences.put(inputDialog.getValue(), "false"); //$NON-NLS-1$
                    availableRepositories.refresh();
                    pluginPane.refresh();
                }
            }
        }

        public RepositoryPane(Composite area)
        {
            createPane(area);
        }

        private void createPane(Composite area)
        {
            Group topArea = new Group(area, SWT.SHADOW_IN);
            topArea.setText(Messages.ManagePluginsDialogRepositoriesPane);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(topArea);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(topArea);

            CheckboxTableViewer availableRepositories = CheckboxTableViewer.newCheckList(topArea, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(availableRepositories.getTable());
            GridDataFactory.fillDefaults().grab(true, true).applyTo(availableRepositories.getTable());
            availableRepositories.setContentProvider((IStructuredContentProvider) inputElement -> {
                if (inputElement instanceof IEclipsePreferences)
                {
                    try
                    {
                        return ((IEclipsePreferences) inputElement).keys();
                    }
                    catch (BackingStoreException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                }
                return new Object[0];
            });
            availableRepositories.setInput(preferences);

            try
            {
                for (String updateSite : preferences.keys())
                {
                    availableRepositories.setChecked(updateSite, preferences.getBoolean(updateSite, false));
                }
            }
            catch (BackingStoreException e)
            {
                PortfolioPlugin.log(e);
            }

            Composite buttonArea = new Composite(topArea, SWT.NONE);
            GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonArea);
            buttonArea.setLayout(new GridLayout(1, false));
            Button addButton = new Button(buttonArea, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(addButton);
            addButton.setText(Messages.ManagePluginsDialogAdd);
            Button removeButton = new Button(buttonArea, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(removeButton);
            removeButton.setText(Messages.ManagePluginsDialogRemove);

            removeButton.addSelectionListener(new RemoveRepositorySelectionListener(availableRepositories));
            addButton.addSelectionListener(new AddRepositorySelectionListener(availableRepositories));

            availableRepositories.addCheckStateListener(event -> {
                preferences.put(event.getElement().toString(), String.valueOf(event.getChecked()));
                availableRepositories.refresh();
                pluginPane.refresh();
            });
        }
    }

    private class JobProgressBar implements PaintListener, IProgressMonitor
    {
        private Composite progressPane;
        private ProgressBar progressBar;
        private boolean canceled;
        private String text = ""; //$NON-NLS-1$
        private Composite parent;
        private Button cancelButton;

        public JobProgressBar(final Composite parent)
        {
            this.parent = parent;
            show();
        }

        private void show()
        {
            executeIfNotDisposed(() -> {
                progressPane = new Composite(parent, SWT.NONE);
                GridDataFactory.fillDefaults().grab(true, false).applyTo(progressPane);
                GridLayoutFactory.fillDefaults().numColumns(2).applyTo(progressPane);

                progressBar = new ProgressBar(progressPane, SWT.SMOOTH);
                progressBar.setBounds(0, 0, 170, 17);
                GridDataFactory.fillDefaults().grab(true, false).hint(100, 20).applyTo(progressBar);
                progressBar.addPaintListener(this);

                this.parent.getParent().layout();
            });
        }

        public void setText(String text)
        {
            this.text = text;
            executeIfNotDisposed(() -> progressBar.redraw());
        }

        public void setTooltip(String text)
        {
            executeIfNotDisposed(() -> progressBar.setToolTipText(text));
        }

        @Override
        public void beginTask(String name, int totalWork)
        {
            if (totalWork > 0)
            {
                executeIfNotDisposed(() -> progressBar.setMaximum(progressBar.getMaximum() + totalWork));
            }

            if (name != null && !name.isEmpty())
            {
                setText(name);
            }
        }

        public void error()
        {
            executeIfNotDisposed(() -> {
                cancelButton = new Button(progressPane, SWT.NONE);
                cancelButton.setText("x");
                GridDataFactory.fillDefaults().grab(false, false).applyTo(cancelButton);
                cancelButton.addSelectionListener(new SelectionAdapter()
                {

                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        done();
                    }
                });
                this.parent.getParent().layout();
            });
        }

        @Override
        public void done()
        {
            executeIfNotDisposed(() -> {
                progressPane.dispose();
                pluginPane.refresh();
                this.parent.getParent().layout();
            });
        }

        @Override
        public void internalWorked(double work)
        {
            worked((int) Math.round(work));
        }

        @Override
        public boolean isCanceled()
        {
            return this.canceled;
        }

        @Override
        public void setCanceled(boolean canceled)
        {
            this.canceled = canceled;
        }

        @Override
        public void setTaskName(String name)
        {
            // do not use the task name for anything
        }

        @Override
        public void subTask(String name)
        {
            if (name != null && !name.isEmpty())
            {
                setText(name);
            }
        }

        @Override
        public void worked(int work)
        {
            executeIfNotDisposed(() -> progressBar.setSelection(progressBar.getSelection() + work));
        }

        public void paintControl(PaintEvent e)
        {
            Point point = progressBar.getSize();
            FontDescriptor boldDescriptor = FontDescriptor.createFrom(progressBar.getFont()).setHeight(8);
            Font font = boldDescriptor.createFont(progressBar.getDisplay());
            e.gc.setFont(font);
            e.gc.setForeground(progressBar.getDisplay().getSystemColor(SWT.COLOR_BLACK));

            FontMetrics fontMetrics = e.gc.getFontMetrics();
            int stringHeight = fontMetrics.getHeight();

            e.gc.drawString(text, 10, (point.y - stringHeight) / 2, true);
            font.dispose();
        }

        private void executeIfNotDisposed(Runnable runnable)
        {
            if (parent != null)
            {
                if (parent.isDisposed())
                {
                    setCanceled(true);
                }
                else
                {
                    parent.getDisplay().asyncExec(runnable);
                }
            }
        }
    }

    private P2Service p2Service;
    private IEclipsePreferences preferences;
    private Map<URI, Collection<IInstallableUnit>> cachedInstallableUnits = new LinkedHashMap<>();
    private IWorkbench workbench;
    private EPartService partService;

    private boolean restartRequired = false;
    private PluginPane pluginPane;

    public ManagePluginsDialog(final Shell parentShell, final IEclipsePreferences preferences,
                    final IWorkbench workbench, final EPartService partService, final P2Service p2Service)
    {
        super(parentShell);
        this.preferences = preferences;
        this.workbench = workbench;
        this.partService = partService;
        this.p2Service = p2Service;
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(Messages.ManagePluginsDialogTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite area = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(635, 500).applyTo(area);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(area);

        new RepositoryPane(area);
        pluginPane = new PluginPane(area);

        return composite;
    }

    @Override
    public boolean close()
    {
        if (restartRequired)
        {
            Display.getDefault().asyncExec(() -> UpdateHelper.promptForRestart(partService, workbench));
        }
        return super.close();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        okButton.addSelectionListener(new SelectionAdapter()
        {

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try
                {
                    preferences.flush();
                }
                catch (BackingStoreException ex)
                {
                    PortfolioPlugin.log(ex);
                }
            }
        });
        okButton.setFocus();
    }
}
