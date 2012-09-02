package name.abuchen.portfolio.ui;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.Sidebar.Entry;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.PageBook;

public class ClientEditor extends EditorPart
{
    private final class ActivateViewAction extends Action
    {
        private String view;
        private Object parameter;

        private ActivateViewAction(String label, String view)
        {
            this(label, view, null, null);
        }

        private ActivateViewAction(String text, String view, ImageDescriptor image)
        {
            this(text, view, null, image);
        }

        public ActivateViewAction(String text, String view, Object parameter, ImageDescriptor image)
        {
            super(text, image);
            this.view = view;
            this.parameter = parameter;
        }

        @Override
        public void run()
        {
            ClientEditor.this.activateView(view, parameter);
        }
    }

    private boolean isDirty = false;
    private IPath clientFile;
    private Client client;

    private Entry allSecurities;
    private Entry statementOfAssets;

    private PageBook book;
    private AbstractFinanceView view;

    // //////////////////////////////////////////////////////////////
    // init
    // //////////////////////////////////////////////////////////////

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        setSite(site);
        setInput(input);

        try
        {
            if (input instanceof ClientEditorInput)
            {
                clientFile = ((ClientEditorInput) input).getPath();

                if (clientFile != null)
                    client = ClientFactory.load(clientFile.toFile());
                else
                    client = new Client();
            }
            else if (input instanceof IPathEditorInput)
            {
                clientFile = ((IPathEditorInput) input).getPath();
                client = ClientFactory.load(clientFile.toFile());
            }
            else
            {
                throw new PartInitException(MessageFormat.format("Unsupported editor input: {0}", input.getClass() //$NON-NLS-1$
                                .getName()));
            }
        }
        catch (IOException e)
        {
            throw new PartInitException(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }

        if (clientFile != null)
            setPartName(clientFile.lastSegment());
        else
            setPartName(Messages.LabelUnsavedFile);

        if (!"no".equals(System.getProperty("name.abuchen.portfolio.auto-updates"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            new UpdateQuotesJob(client, false, 1000 * 60 * 10)
            {
                @Override
                protected void notifyFinished()
                {
                    notifyModelUpdated();
                }
            }.schedule(500);

            new UpdateQuotesJob(client)
            {
                @Override
                protected void notifyFinished()
                {
                    notifyModelUpdated();
                }
            }.schedule(1000);

            new UpdateCPIJob(client)
            {
                @Override
                protected void notifyFinished()
                {
                    notifyModelUpdated();
                }
            }.schedule(700);
        }
    }

    @Override
    public void createPartControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(1, 1).applyTo(container);

        Sidebar sidebar = new Sidebar(container, SWT.NONE);
        GridDataFactory.fillDefaults().hint(180, SWT.DEFAULT).grab(false, true).applyTo(sidebar);

        createGeneralDataSection(sidebar);
        createMasterDataSection(sidebar);
        createPerformanceSection(sidebar);
        createMiscSection(sidebar);

        book = new PageBook(container, SWT.NONE);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(book);

        sidebar.select(statementOfAssets);
    }

    private void createGeneralDataSection(final Sidebar sidebar)
    {
        final Entry section = new Entry(sidebar, Messages.LabelSecurities);
        section.setAction(new Action(Messages.LabelSecurities, PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS))
        {
            @Override
            public void run()
            {
                String name = askWatchlistName(Messages.WatchlistNewLabel);
                if (name == null)
                    return;

                Watchlist watchlist = new Watchlist();
                watchlist.setName(name);
                client.getWatchlists().add(watchlist);
                markDirty();

                createWathlistEntry(section, watchlist);
                sidebar.layout();
            }
        });

        allSecurities = new Entry(section, new ActivateViewAction(Messages.LabelAllSecurities, "SecurityList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_SECURITY)));

        for (Watchlist watchlist : client.getWatchlists())
            createWathlistEntry(section, watchlist);
    }

    private void createWathlistEntry(Entry section, final Watchlist watchlist)
    {
        final Entry entry = new Entry(section, watchlist.getName());
        entry.setAction(new ActivateViewAction(watchlist.getName(), "SecurityList", watchlist, //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_WATCHLIST)));

        entry.setContextMenu(new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                manager.add(new Action(Messages.WatchlistRename)
                {
                    @Override
                    public void run()
                    {
                        String newName = askWatchlistName(watchlist.getName());
                        if (newName != null)
                        {
                            watchlist.setName(newName);
                            markDirty();
                            entry.setLabel(newName);
                        }
                    }
                });

                manager.add(new Action(Messages.WatchlistDelete)
                {
                    @Override
                    public void run()
                    {
                        client.getWatchlists().remove(watchlist);
                        markDirty();
                        entry.dispose();
                        allSecurities.select();
                    }
                });
            }
        });

        entry.addDropSupport(DND.DROP_MOVE, new Transfer[] { SecurityTransfer.getTransfer() }, new DropTargetAdapter()
        {
            @Override
            public void drop(DropTargetEvent event)
            {
                if (SecurityTransfer.getTransfer().isSupportedType(event.currentDataType))
                {
                    Security security = SecurityTransfer.getTransfer().getSecurity();
                    if (security != null)
                    {
                        // if the security is dragged from another file, add to
                        // a deep copy to the client's securities list
                        if (!client.getSecurities().contains(security))
                        {
                            security = security.deepCopy();
                            client.getSecurities().add(security);
                        }

                        if (!watchlist.getSecurities().contains(security))
                            watchlist.addSecurity(security);

                        markDirty();

                        notifyModelUpdated();
                    }
                }
            }
        });
    }

    private String askWatchlistName(String initialValue)
    {
        InputDialog dlg = new InputDialog(getSite().getShell(), Messages.WatchlistEditDialog,
                        Messages.WatchlistEditDialogMsg, initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMasterDataSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelClientMasterData);
        new Entry(section, new ActivateViewAction(Messages.LabelAccounts, "AccountList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_ACCOUNT)));
        new Entry(section, new ActivateViewAction(Messages.LabelPortfolios, "PortfolioList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PORTFOLIO)));
    }

    private void createPerformanceSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelReports);

        statementOfAssets = new Entry(section, new ActivateViewAction(Messages.LabelStatementOfAssets,
                        "StatementOfAssets")); //$NON-NLS-1$
        new Entry(statementOfAssets,
                        new ActivateViewAction(Messages.ClientEditorLabelChart, "StatementOfAssetsHistory")); //$NON-NLS-1$
        new Entry(statementOfAssets, new ActivateViewAction(Messages.ClientEditorLabelHoldings, "HoldingsPieChart")); //$NON-NLS-1$
        new Entry(statementOfAssets, new ActivateViewAction(Messages.LabelAssetClasses, "StatementOfAssetsPieChart")); //$NON-NLS-1$
        new Entry(statementOfAssets, new ActivateViewAction(Messages.LabelAssetAllocation, "Category")); //$NON-NLS-1$

        Entry performance = new Entry(section, new ActivateViewAction(Messages.ClientEditorLabelPerformance,
                        "Performance")); //$NON-NLS-1$
        new Entry(performance, new ActivateViewAction(Messages.ClientEditorLabelChart, "PerformanceChart")); //$NON-NLS-1$
        new Entry(performance, new ActivateViewAction(Messages.LabelSecurities, "SecurityPerformance")); //$NON-NLS-1$
    }

    private void createMiscSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelGeneralData);
        new Entry(section, new ActivateViewAction(Messages.LabelConsumerPriceIndex, "ConsumerPriceIndexList")); //$NON-NLS-1$
        new Entry(section, new ActivateViewAction(new IndustryClassification().getRootCategory().getLabel(),
                        "IndustryClassificationDefinition")); //$NON-NLS-1$
    }

    protected void activateView(String target, Object parameter)
    {
        if (view != null && !view.getControl().isDisposed())
        {
            view.getControl().dispose();
            view.dispose();
            view = null;
        }

        try
        {
            Class<?> clazz = getClass().getClassLoader()
                            .loadClass("name.abuchen.portfolio.ui.views." + target + "View"); //$NON-NLS-1$ //$NON-NLS-2$
            if (clazz == null)
                return;

            view = (AbstractFinanceView) clazz.newInstance();
            view.init(this, parameter);
            view.createViewControl(book);

            book.showPage(view.getControl());
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setFocus()
    {
        book.setFocus();
    }

    public Client getClient()
    {
        return client;
    }

    /* package */void markDirty()
    {
        boolean oldIsDirty = isDirty;
        isDirty = true;

        if (!oldIsDirty)
            firePropertyChange(PROP_DIRTY);
    }

    public void notifyModelUpdated()
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                markDirty();

                if (view != null)
                    view.notifyModelUpdated();
            }
        });
    }

    // //////////////////////////////////////////////////////////////
    // save functions
    // //////////////////////////////////////////////////////////////

    @Override
    public boolean isDirty()
    {
        return isDirty;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return true;
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
        if (clientFile == null)
        {
            doSaveAs();
            return;
        }

        try
        {
            ClientFactory.save(client, clientFile.toFile());
            isDirty = false;
            firePropertyChange(PROP_DIRTY);
        }
        catch (IOException e)
        {
            ErrorDialog.openError(getSite().getShell(), Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    @Override
    public void doSaveAs()
    {
        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        if (clientFile != null)
        {
            dialog.setFileName(clientFile.lastSegment());
            dialog.setFilterPath(clientFile.toOSString());
        }
        else
        {
            dialog.setFileName(Messages.LabelUnnamedXml);
        }

        String path = dialog.open();
        if (path == null)
            return;

        try
        {
            File localFile = new File(path);

            IEditorInput newInput = new ClientEditorInput(new Path(path));

            ClientFactory.save(client, localFile);

            clientFile = new Path(path);

            setInput(newInput);
            setPartName(clientFile.lastSegment());

            isDirty = false;
            firePropertyChange(PROP_DIRTY);
        }
        catch (IOException e)
        {
            ErrorDialog.openError(getSite().getShell(), Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }
}
