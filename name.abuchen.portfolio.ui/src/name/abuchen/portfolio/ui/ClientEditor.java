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
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
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

    private boolean isDirty = false;
    private IPath clientFile;
    private Client client;

    private SashForm sash;
    private PageBook book;
    private AbstractFinanceView view;

    private IHyperlinkListener listener = new IHyperlinkListener()
    {
        public void linkActivated(HyperlinkEvent e)
        {
            String target = (String) e.getHref();
            activateView(target);
        }

        public void linkEntered(HyperlinkEvent e)
        {}

        public void linkExited(HyperlinkEvent e)
        {}
    };

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
        sash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);

        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        Form form = toolkit.createForm(sash);

        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 20;
        form.getBody().setLayout(layout);

        Section section = createGeneralDataSection(toolkit, form);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(section);

        section = createMasterDataSection(toolkit, form);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(section);

        section = createPerformanceSection(toolkit, form);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(section);

        form.getBody().layout();

        book = new PageBook(sash, SWT.NONE);

        sash.setWeights(new int[] { 1, 4 });

        activateView("StatementOfAssets"); //$NON-NLS-1$
    }

    private Section createGeneralDataSection(FormToolkit toolkit, Form form)
    {
        Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
        section.setText(Messages.ClientEditorLabelGeneralData);
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new TableWrapLayout());
        FormText text = toolkit.createFormText(sectionClient, true);
        text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

        StringBuilder buf = new StringBuilder();
        buf.append("<form>"); //$NON-NLS-1$
        buf.append("<li><a href=\"SecurityList\">").append(Messages.LabelSecurities).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li><a href=\"ConsumerPriceIndexList\">").append(Messages.LabelConsumerPriceIndex).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("</form>"); //$NON-NLS-1$
        text.setText(buf.toString(), true, false);

        text.addHyperlinkListener(listener);

        section.setClient(sectionClient);
        return section;
    }

    private Section createMasterDataSection(FormToolkit toolkit, Form form)
    {
        Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
        section.setText(Messages.ClientEditorLabelClientMasterData);
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new TableWrapLayout());
        FormText text = toolkit.createFormText(sectionClient, true);
        text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

        StringBuilder buf = new StringBuilder();
        buf.append("<form>"); //$NON-NLS-1$
        buf.append("<li><a href=\"AccountList\">").append(Messages.LabelAccounts).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li><a href=\"PortfolioList\">").append(Messages.LabelPortfolios).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("</form>"); //$NON-NLS-1$
        text.setText(buf.toString(), true, false);

        text.addHyperlinkListener(listener);

        section.setClient(sectionClient);
        return section;
    }

    private Section createPerformanceSection(FormToolkit toolkit, Form form)
    {
        Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
        section.setText(Messages.ClientEditorLabelReports);
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new TableWrapLayout());
        FormText text = toolkit.createFormText(sectionClient, true);
        text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

        StringBuilder buf = new StringBuilder();
        buf.append("<form>"); //$NON-NLS-1$
        buf.append("<li><a href=\"StatementOfAssets\">").append(Messages.LabelStatementOfAssets).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li bindent=\"20\"><a href=\"StatementOfAssetsHistory\">").append(Messages.ClientEditorLabelChart).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li bindent=\"20\"><a href=\"HoldingsPieChart\">").append(Messages.ClientEditorLabelHoldings).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li bindent=\"20\"><a href=\"StatementOfAssetsPieChart\">").append(Messages.LabelAssetClasses).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li bindent=\"20\"><a href=\"Category\">").append(Messages.LabelAssetAllocation).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$

        buf.append("<li><a href=\"Performance\">").append(Messages.ClientEditorLabelPerformance).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li bindent=\"20\"><a href=\"PerformanceChart\">").append(Messages.ClientEditorLabelChart).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("<li bindent=\"20\"><a href=\"SecurityPerformance\">").append(Messages.LabelSecurities).append("</a></li>"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("</form>"); //$NON-NLS-1$
        text.setText(buf.toString(), true, false);

        text.addHyperlinkListener(listener);

        section.setClient(sectionClient);
        return section;
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
