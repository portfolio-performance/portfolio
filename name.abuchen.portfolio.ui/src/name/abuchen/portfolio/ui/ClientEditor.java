package name.abuchen.portfolio.ui;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.PageBook;

public class ClientEditor extends EditorPart
{
    public static class ClientEditorInput extends PlatformObject implements IPathEditorInput, IPersistableElement
    {
        private IPath path;

        public ClientEditorInput()
        {}

        public ClientEditorInput(IPath path)
        {
            this.path = path;
        }

        @Override
        public boolean exists()
        {
            return path != null && path.toFile().exists();
        }

        @Override
        public ImageDescriptor getImageDescriptor()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return Messages.LabelPortfolioPerformanceFile;
        }

        @Override
        public IPersistableElement getPersistable()
        {
            return path != null ? this : null;
        }

        @Override
        public String getToolTipText()
        {
            return getName();
        }

        @Override
        public IPath getPath()
        {
            return path;
        }

        @Override
        public void saveState(IMemento memento)
        {
            if (path != null)
                memento.putString("file", path.toOSString()); //$NON-NLS-1$
        }

        @Override
        public String getFactoryId()
        {
            return "name.abuchen.portfolio.ui.factory"; //$NON-NLS-1$
        }
    }

    private final class ActivateViewAction extends Action
    {
        private String view;

        private ActivateViewAction(String label, String view)
        {
            super(label);
            this.view = view;
        }

        private ActivateViewAction(String text, String view, ImageDescriptor image)
        {
            super(text, image);
            this.view = view;
        }

        @Override
        public void run()
        {
            ClientEditor.this.activateView(view);
        }
    }

    private boolean isDirty = false;
    private IPath clientFile;
    private Client client;

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
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(0, 0).applyTo(container);

        Sidebar sidebar = new Sidebar(container, SWT.BORDER);
        GridDataFactory.fillDefaults().hint(180, SWT.DEFAULT).grab(false, true).applyTo(sidebar);

        createGeneralDataSection(sidebar);
        createMasterDataSection(sidebar);
        createPerformanceSection(sidebar);

        book = new PageBook(container, SWT.NONE);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(book);

        sidebar.select(7);
    }

    private void createGeneralDataSection(Sidebar sidebar)
    {
        sidebar.addSection(Messages.ClientEditorLabelGeneralData);
        sidebar.addItem(new ActivateViewAction(Messages.LabelSecurities, "SecurityList", //$NON-NLS-1$
                        PortfolioPlugin.getDefault().getImageRegistry().getDescriptor(PortfolioPlugin.IMG_SECURITY)));
        sidebar.addItem(new ActivateViewAction(Messages.LabelConsumerPriceIndex, "ConsumerPriceIndexList")); //$NON-NLS-1$
    }

    private void createMasterDataSection(Sidebar sidebar)
    {
        sidebar.addSection(Messages.ClientEditorLabelClientMasterData);
        sidebar.addItem(new ActivateViewAction(Messages.LabelAccounts, "AccountList", //$NON-NLS-1$
                        PortfolioPlugin.getDefault().getImageRegistry().getDescriptor(PortfolioPlugin.IMG_ACCOUNT)));
        sidebar.addItem(new ActivateViewAction(Messages.LabelPortfolios, "PortfolioList", //$NON-NLS-1$
                        PortfolioPlugin.getDefault().getImageRegistry().getDescriptor(PortfolioPlugin.IMG_PORTFOLIO)));
    }

    private void createPerformanceSection(Sidebar sidebar)
    {
        sidebar.addSection(Messages.ClientEditorLabelReports);
        sidebar.addItem(new ActivateViewAction(Messages.LabelStatementOfAssets, "StatementOfAssets")); //$NON-NLS-1$
        sidebar.addSubItem(new ActivateViewAction(Messages.ClientEditorLabelChart, "StatementOfAssetsHistory")); //$NON-NLS-1$
        sidebar.addSubItem(new ActivateViewAction(Messages.ClientEditorLabelHoldings, "HoldingsPieChart")); //$NON-NLS-1$
        sidebar.addSubItem(new ActivateViewAction(Messages.LabelAssetClasses, "StatementOfAssetsPieChart")); //$NON-NLS-1$
        sidebar.addSubItem(new ActivateViewAction(Messages.LabelAssetAllocation, "Category")); //$NON-NLS-1$
        sidebar.addItem(new ActivateViewAction(Messages.ClientEditorLabelPerformance, "Performance")); //$NON-NLS-1$
        sidebar.addSubItem(new ActivateViewAction(Messages.ClientEditorLabelChart, "PerformanceChart")); //$NON-NLS-1$
        sidebar.addSubItem(new ActivateViewAction(Messages.LabelSecurities, "SecurityPerformance")); //$NON-NLS-1$
    }

    protected void activateView(String target)
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
            view.init(this);
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
