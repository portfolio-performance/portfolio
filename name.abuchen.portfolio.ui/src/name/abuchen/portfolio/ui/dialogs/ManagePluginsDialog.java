package name.abuchen.portfolio.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.p2.P2Service;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.update.UpdateHelper;
import name.abuchen.portfolio.ui.util.P2Util;

public class ManagePluginsDialog extends Dialog
{
    private P2Service p2Service;
    private IEclipsePreferences preferences;
    private TableViewer installedUnitsTable;
    private Label progressLabel;

    private Map<URI, Collection<IInstallableUnit>> cachedInstallableUnits = new LinkedHashMap<>();
    private IWorkbench workbench;
    private EPartService partService;

    private boolean restartRequired = false;

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
        GridDataFactory.fillDefaults().grab(true, true).hint(610, 500).applyTo(area);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(area);

        createRepositoryPane(area);
        createPluginPane(area);

        return composite;
    }

    @Override
    public boolean close()
    {
        if (restartRequired)
        {
            Display.getDefault().syncExec(() -> {
                progressLabel.setText(Messages.ManagePluginsDialogOperationSuccessful);
                UpdateHelper.promptForRestart(partService, workbench);
            });
        }
        return super.close();
    }

    private void createRepositoryPane(Composite area)
    {
        Group topArea = new Group(area, SWT.SHADOW_IN);
        topArea.setText(Messages.ManagePluginsDialogRepositoriesPane);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(topArea);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(topArea);

        ListViewer availableRepositories = new ListViewer(topArea);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(availableRepositories.getList());
        GridDataFactory.fillDefaults().grab(true, true).applyTo(availableRepositories.getList());
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

        Composite buttonArea = new Composite(topArea, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonArea);
        buttonArea.setLayout(new GridLayout(1, false));
        Button addButton = new Button(buttonArea, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(addButton);
        addButton.setText(Messages.ManagePluginsDialogAdd);
        Button editButton = new Button(buttonArea, SWT.NONE);
        editButton.setEnabled(false);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(editButton);
        editButton.setText(Messages.ManagePluginsDialogEdit);
        Button removeButton = new Button(buttonArea, SWT.NONE);
        removeButton.setEnabled(false);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(removeButton);
        removeButton.setText(Messages.ManagePluginsDialogRemove);

        addButton.addSelectionListener(new AddRepositorySelectionListener(availableRepositories));
        editButton.addSelectionListener(new EditRepositorySelectionListener(availableRepositories));
        removeButton.addSelectionListener(new RemoveRepositorySelectionListener(availableRepositories));

        availableRepositories.addSelectionChangedListener(event -> {
            IStructuredSelection structuredSelection = availableRepositories.getStructuredSelection();
            if (!structuredSelection.isEmpty())
            {
                editButton.setEnabled(true);
                removeButton.setEnabled(true);
            }
        });
    }

    private void createPluginPane(Composite area)
    {
        Group mainArea = new Group(area, SWT.SHADOW_IN);
        mainArea.setText(Messages.ManagePluginsDialogHandlePlugins);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(mainArea);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(mainArea);

        Composite progressPane = new Composite(mainArea, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(progressPane);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(progressPane);
        progressLabel = new Label(progressPane, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).hint(100, 20).applyTo(progressLabel);

        installedUnitsTable = new TableViewer(mainArea,
                        SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        installedUnitsTable.getTable().setHeaderVisible(true);
        initInstalledUnitsTable(installedUnitsTable);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(installedUnitsTable.getTable());
        installedUnitsTable.setInput(preferences);

        Composite rightArea = new Composite(mainArea, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(rightArea);
        rightArea.setLayout(new GridLayout(1, false));
        Button installUpdateButton = new Button(rightArea, SWT.NONE);
        installUpdateButton.setText(Messages.ManagePluginsDialogInstallUpdate);
        installUpdateButton.setEnabled(false);
        Button uninstallButton = new Button(rightArea, SWT.NONE);
        uninstallButton.setText(Messages.ManagePluginsDialogUninstall);
        uninstallButton.setEnabled(false);

        Group detailsArea = new Group(mainArea, SWT.SHADOW_NONE);
        detailsArea.setText(Messages.ManagePluginsDialogPluginDetails);
        StyledText pluginDetailsText = new StyledText(detailsArea, SWT.V_SCROLL);
        pluginDetailsText.setEditable(false);
        pluginDetailsText.setWordWrap(true);
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1).hint(SWT.DEFAULT, 100).applyTo(detailsArea);
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
                    StyleRange[] styles = new StyleRange[3];

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
                    StyleRange latestChangesStyle = new StyleRange();
                    latestChangesStyle.fontStyle = SWT.BOLD;
                    latestChangesStyle.start = sb.length();
                    sb.append(Messages.ManagePluginsDialogPluginDetailsChangelogLatest);
                    latestChangesStyle.length = sb.length() - latestChangesStyle.start;
                    styles[1] = latestChangesStyle;
                    sb.append(System.lineSeparator());
                    String changelogLatest = ius.getChangelogLatest();
                    if (changelogLatest.isEmpty())
                    {
                        sb.append("---");
                    }
                    else
                    {
                        sb.append(changelogLatest);
                    }

                    sb.append(System.lineSeparator());
                    sb.append(System.lineSeparator());
                    StyleRange changelogHistoryStyle = new StyleRange();
                    changelogHistoryStyle.fontStyle = SWT.BOLD;
                    changelogHistoryStyle.start = sb.length();
                    sb.append(Messages.ManagePluginsDialogPluginDetailsChangelogHistory);
                    changelogHistoryStyle.length = sb.length() - changelogHistoryStyle.start;
                    styles[2] = changelogHistoryStyle;
                    sb.append(System.lineSeparator());
                    String changelogHistory = ius.getChangelogHistory();
                    if (changelogHistory.isEmpty())
                    {
                        sb.append("---");
                    }
                    else
                    {
                        sb.append(changelogHistory);
                    }

                    pluginDetailsText.setText(sb.toString());
                    pluginDetailsText.setStyleRanges(styles);
                }
            }
        });

        installUpdateButton.addSelectionListener(new SelectionListener()
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

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                // not used
            }
        });

        uninstallButton.addSelectionListener(new SelectionListener()
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

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                // not used
            }
        });
    }

    private void initInstalledUnitsTable(TableViewer installedUnitsTable)
    {
        ColumnViewerToolTipSupport.enableFor(installedUnitsTable, ToolTip.NO_RECREATE);
        TableViewerColumn column = new TableViewerColumn(installedUnitsTable, SWT.NONE);
        column.getColumn().setText(Messages.ManagePluginsDialogPluginId);
        column.getColumn().setWidth(290);
        column.setLabelProvider(new InstalledUnitsTableColumnProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((InstallableUnitState) element).getId();
            }
        });

        column = new TableViewerColumn(installedUnitsTable, SWT.NONE);
        column.getColumn().setText(Messages.ManagePluginsDialogPluginVersionInstalled);
        column.getColumn().setWidth(175);
        column.setLabelProvider(new InstalledUnitsTableColumnProvider()
        {
            @Override
            public String getText(Object element)
            {
                InstallableUnitState ius = (InstallableUnitState) element;
                if (ius.updatable)
                    return ius.getAvailableVersion() + "*";
                return ius.getAvailableVersion();
            }
        });

        installedUnitsTable.setContentProvider((IStructuredContentProvider) inputElement ->

        {
            if (inputElement instanceof IEclipsePreferences)
            {
                Collection<IInstallableUnit> installableUnits = fetchUnitsFromUpdateSite(
                                P2Util.toURIs((IEclipsePreferences) inputElement));

                p2Service.getInstalledPlugins().filter(iu -> !installableUnits.contains(iu)).forEach(iu -> {
                    installableUnits.add(iu);
                });

                return installableUnits.stream().map(InstallableUnitState::new).toArray(InstallableUnitState[]::new);
            }
            return new Object[0];
        });

    }

    private Collection<IInstallableUnit> fetchUnitsFromUpdateSite(Collection<URI> updateSites)
    {
        Collection<IInstallableUnit> installableUnits = new LinkedHashSet<>();
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
                        try
                        {
                            Set<IInstallableUnit> installNewUnits = p2Service
                                            .fetchInstallableUnitsFromUpdateSite(updateSite, monitor);

                            cachedInstallableUnits.put(updateSite, installNewUnits.stream().filter(iu -> {
                                String property = iu.getProperty("org.eclipse.equinox.p2.type.group"); //$NON-NLS-1$
                                return property != null && property.equals("true"); //$NON-NLS-1$
                            }).collect(Collectors.toList()));
                            Display.getDefault().asyncExec(() -> {
                                if (installedUnitsTable.getControl().isDisposed())
                                {
                                    installedUnitsTable.refresh();
                                }
                            });
                        }
                        catch (Exception e)
                        {
                            cachedInstallableUnits.remove(updateSite);
                            return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID,
                                            String.format("Fetching of installable units from %s canceled", updateSite), //$NON-NLS-1$
                                            e);
                        }
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
        progressLabel.setText(Messages.bind(Messages.ManagePluginsDialogStartProfileChangeOperation,
                        Messages.ManagePluginsDialogInstallation,
                        toInstall.stream().map(InstallableUnitState::getId).collect(Collectors.joining(", ")))); //$NON-NLS-1$
        IStatus status = p2Service.install(
                        toInstall.stream().map(InstallableUnitState::getIu).collect(Collectors.toList()),
                        P2Util.toURIs(preferences), new ProfileChangeUpdater());

        if (!status.isOK())
        {
            progressLabel.setText(Messages.ManagePluginsDialogInstallationFailed);
            PortfolioPlugin.log(status);
        }
    }

    private void uninstallIUs(Collection<InstallableUnitState> toUninstall)
    {
        progressLabel.setText(Messages.bind(Messages.ManagePluginsDialogStartProfileChangeOperation,
                        Messages.ManagePluginsDialogUninstallation,
                        toUninstall.stream().map(InstallableUnitState::getId).collect(Collectors.joining(", ")))); //$NON-NLS-1$

        IStatus status = p2Service.uninstall(
                        toUninstall.stream().map(InstallableUnitState::getInstalledIU).collect(Collectors.toList()),
                        new ProfileChangeUpdater());

        if (!status.isOK())
        {
            progressLabel.setText(Messages.ManagePluginsDialogUninstallationFailed);
            PortfolioPlugin.log(status);
        }
    }

    private void updateIUs(Collection<InstallableUnitState> toUpdate)
    {
        progressLabel.setText(Messages.bind(Messages.ManagePluginsDialogStartProfileChangeOperation,
                        Messages.ManagePluginsDialogUpdate,
                        toUpdate.stream().map(InstallableUnitState::getId).collect(Collectors.joining(", ")))); // $NON-NLS-1$

        IStatus status = p2Service.update(
                        toUpdate.stream().map(InstallableUnitState::getInstalledIU).collect(Collectors.toList()),
                        P2Util.toURIs(preferences), new ProfileChangeUpdater());

        if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
        {
            progressLabel.setText(Messages.ManagePluginsDialogNothingToUpdate);
        }

        if (!status.isOK())
        {
            progressLabel.setText(Messages.ManagePluginsDialogUpdateFailed);
            PortfolioPlugin.log(status);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        okButton.addSelectionListener(new SelectionListener()
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

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                // not used
            }
        });
        okButton.setFocus();
    }

    private abstract class InstalledUnitsTableColumnProvider extends ColumnLabelProvider
    {
        @Override
        public String getToolTipText(Object element)
        {
            StringBuilder sb = new StringBuilder();
            InstallableUnitState ius = (InstallableUnitState) element;
            sb.append(Messages.ManagePluginsDialogPluginVersionAvailable);
            sb.append(": ");
            sb.append(ius.getAvailableVersion());
            return sb.toString();
        }

        @Override
        public Point getToolTipShift(Object object)
        {
            return new Point(5, 5);
        }

        @Override
        public int getToolTipDisplayDelayTime(Object object)
        {
            return 100; // msec
        }

        @Override
        public int getToolTipTimeDisplayed(Object object)
        {
            return 5000; // msec
        }
    }

    private final class AddRepositorySelectionListener implements SelectionListener
    {
        private final ListViewer availableRepositories;

        private AddRepositorySelectionListener(ListViewer availableRepositories)
        {
            this.availableRepositories = availableRepositories;
        }

        @Override
        public void widgetSelected(SelectionEvent e)
        {
            InputDialog inputDialog = new InputDialog(getShell(), Messages.ManagePluginsDialogNewRepositoryDialogTitle,
                            Messages.ManagePluginsDialogNewRepositoryDialogText, "", updateSite -> {
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
                preferences.put(inputDialog.getValue(), inputDialog.getValue());
                availableRepositories.refresh();
                installedUnitsTable.refresh();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e)
        {
            // not used
        }
    }

    private final class EditRepositorySelectionListener implements SelectionListener
    {
        private final ListViewer availableRepositories;

        private EditRepositorySelectionListener(ListViewer availableRepositories)
        {
            this.availableRepositories = availableRepositories;
        }

        @Override
        public void widgetSelected(SelectionEvent e)
        {
            String selectedRepository = (String) availableRepositories.getStructuredSelection().getFirstElement();
            InputDialog inputDialog = new InputDialog(getShell(), Messages.ManagePluginsDialogEditRepositoryDialogTitle,
                            Messages.ManagePluginsDialogEditRepositoryDialogText, selectedRepository, updateSite -> {
                                try
                                {
                                    URI uri = new URI(updateSite);
                                    if (!uri.isAbsolute()) { return Messages.ManagePluginsDialogURIMustBeAbsolute; }
                                }
                                catch (URISyntaxException e1)
                                {
                                    return Messages.ManagePluginsDialogInvalidURI;
                                }
                                return null;
                            });
            if (inputDialog.open() == Window.OK)
            {
                preferences.remove(selectedRepository);
                preferences.put(inputDialog.getValue(), inputDialog.getValue());
                availableRepositories.refresh();
                installedUnitsTable.refresh();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e)
        {
            // not used
        }
    }

    private final class RemoveRepositorySelectionListener implements SelectionListener
    {
        private final ListViewer availableRepositories;

        private RemoveRepositorySelectionListener(ListViewer availableRepositories)
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
                installedUnitsTable.refresh();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e)
        {
            // not used
        }
    }

    private class InstallableUnitState
    {
        private String availableVersion;
        private String installedVersion;
        private String description;
        private String changelogLatest;
        private String changelogHistory;
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
            description = iu.getProperty("org.eclipse.equinox.p2.description");
            changelogLatest = iu.getProperty("changelog.latest");
            changelogHistory = iu.getProperty("changelog.history");

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

        public String getChangelogLatest()
        {
            return changelogLatest == null ? "" : changelogLatest;
        }

        public String getChangelogHistory()
        {
            return changelogHistory == null ? "" : changelogHistory;
        }

        public String getDescription()
        {
            return description == null ? "" : description;
        }
    }

    private class ProfileChangeUpdater extends JobChangeAdapter
    {
        @Override
        public void done(IJobChangeEvent event)
        {
            if (event.getResult().isOK())
            {
                restartRequired = true;
            }
        }
    }
}
