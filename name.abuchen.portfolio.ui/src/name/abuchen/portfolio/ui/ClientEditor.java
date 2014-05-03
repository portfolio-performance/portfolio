package name.abuchen.portfolio.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.LinkedList;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.dialogs.PasswordDialog;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.PageBook;

public class ClientEditor extends EditorPart implements LoadClientThread.Callback
{
    private abstract class BuildContainerRunnable implements Runnable
    {
        @Override
        public void run()
        {
            if (container != null && !container.isDisposed())
            {
                Composite parent = container.getParent();
                parent.setRedraw(false);
                try
                {
                    container.dispose();
                    createContainer(parent);
                    parent.layout(true);
                }
                finally
                {
                    parent.setRedraw(true);
                }
            }
        }

        public abstract void createContainer(Composite parent);
    }

    private boolean isDirty = false;
    private IPath clientFile;
    private Client client;

    private PreferenceStore preferences = new PreferenceStore();

    private Composite container;
    private PageBook book;
    private AbstractFinanceView view;

    private Control focus;

    // //////////////////////////////////////////////////////////////
    // init
    // //////////////////////////////////////////////////////////////

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        setSite(site);
        setInput(input);

        if (input instanceof ClientEditorInput)
        {
            clientFile = ((ClientEditorInput) input).getPath();
            client = ((ClientEditorInput) input).getClient();
        }
        else if (input instanceof IPathEditorInput)
        {
            clientFile = ((IPathEditorInput) input).getPath();
            client = null;
        }
        else
        {
            throw new PartInitException(MessageFormat.format("Unsupported editor input: {0}", input.getClass() //$NON-NLS-1$
                            .getName()));
        }

        if (client != null)
        {
            client = ((ClientEditorInput) input).getClient();
            setClient(client);
            isDirty = clientFile == null;
        }

        loadPreferences();

        if (clientFile != null)
            setPartName(clientFile.lastSegment());
        else
            setPartName(Messages.LabelUnsavedFile);
    }

    @Override
    public void setClient(Client client)
    {
        this.client = client;
        this.isDirty = false;

        client.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                markDirty();
            }
        });

        Display.getDefault().asyncExec(new BuildContainerRunnable()
        {
            @Override
            public void createContainer(Composite parent)
            {
                createContainerWithViews(parent);
            }
        });

        new ConsistencyChecksJob(this, client, false).schedule(100);
        scheduleOnlineUpdateJobs();
    }

    @Override
    public void setErrorMessage(final String message)
    {
        Display.getDefault().asyncExec(new BuildContainerRunnable()
        {
            @Override
            public void createContainer(Composite parent)
            {
                createContainerWithMessage(parent, message, false, ClientFactory.isEncrypted(clientFile.toFile()));
            }
        });
    }

    private void scheduleOnlineUpdateJobs()
    {
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
        if (client != null)
        {
            createContainerWithViews(parent);
        }
        else if (ClientFactory.isEncrypted(clientFile.toFile()))
        {
            createContainerWithMessage(parent, MessageFormat.format(Messages.MsgOpenFile, getPartName()), false, true);
        }
        else
        {
            ProgressBar bar = createContainerWithMessage(parent,
                            MessageFormat.format(Messages.MsgLoadingFile, getPartName()), true, false);

            new LoadClientThread(new ProgressMonitor(bar), this, clientFile.toFile(), null).start();
        }
    }

    private void createContainerWithViews(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(1, 1).applyTo(container);

        ClientEditorSidebar sidebar = new ClientEditorSidebar(this);
        Control control = sidebar.createSidebarControl(container);
        GridDataFactory.fillDefaults().hint(180, SWT.DEFAULT).grab(false, true).applyTo(control);

        book = new PageBook(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(book);

        sidebar.selectDefaultView();

        focus = book;
    }

    /**
     * Creates window with logo and message. Optional a progress bar (while
     * loading) or a password input field (if encrypted).
     */
    private ProgressBar createContainerWithMessage(Composite parent, String message, boolean showProgressBar,
                    boolean showPasswordField)
    {
        ProgressBar bar = null;

        container = new Composite(parent, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        container.setLayout(new FormLayout());

        Label image = new Label(container, SWT.BORDER);
        image.setImage(PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_SMALL));

        FormData data = new FormData();
        data.top = new FormAttachment(50, -50);
        data.left = new FormAttachment(50, -24);
        image.setLayoutData(data);

        if (showPasswordField)
        {
            Text pwd = createPasswordField(parent);

            data = new FormData();
            data.top = new FormAttachment(image, 10);
            data.left = new FormAttachment(50, -50);
            data.width = 100;
            pwd.setLayoutData(data);

            focus = pwd;
        }
        else if (showProgressBar)
        {
            bar = new ProgressBar(container, SWT.SMOOTH);

            data = new FormData();
            data.top = new FormAttachment(image, 10);
            data.left = new FormAttachment(50, -100);
            data.width = 200;
            bar.setLayoutData(data);
        }

        Label label = new Label(container, SWT.CENTER | SWT.WRAP);
        label.setText(message);

        data = new FormData();
        data.top = new FormAttachment(image, 40);
        data.left = new FormAttachment(50, -100);
        data.width = 200;
        label.setLayoutData(data);

        return bar;
    }

    private Text createPasswordField(Composite parent)
    {
        final Text pwd = new Text(container, SWT.PASSWORD | SWT.BORDER);
        pwd.setFocus();
        pwd.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                final String password = pwd.getText();
                Display.getDefault().syncExec(new BuildContainerRunnable()
                {
                    @Override
                    public void createContainer(Composite parent)
                    {
                        ProgressBar bar = createContainerWithMessage(parent,
                                        MessageFormat.format(Messages.MsgLoadingFile, getPartName()), true, false);
                        new LoadClientThread(new ProgressMonitor(bar), ClientEditor.this, clientFile.toFile(), password
                                        .toCharArray()).start();
                    }
                });
            }
        });

        return pwd;
    }

    protected void activateView(String target, Object parameter)
    {
        disposeView();

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

    private void disposeView()
    {
        if (view != null && !view.getControl().isDisposed())
        {
            view.getControl().dispose();
            view = null;
        }
    }

    @Override
    public void setFocus()
    {
        if (focus != null)
            focus.setFocus();
    }

    public Client getClient()
    {
        return client;
    }

    public IPreferenceStore getPreferenceStore()
    {
        return preferences;
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
    public void dispose()
    {
        storePreferences();
        super.dispose();
    }

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
            ClientFactory.save(client, clientFile.toFile(), null);
            isDirty = false;
            firePropertyChange(PROP_DIRTY);

            storePreferences();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
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

        File localFile = new File(path);
        char[] password = null;

        if (ClientFactory.isEncrypted(localFile))
        {
            PasswordDialog pwdDialog = new PasswordDialog(getSite().getShell());
            if (pwdDialog.open() != PasswordDialog.OK)
                return;
            password = pwdDialog.getPassword().toCharArray();
        }

        try
        {
            IEditorInput newInput = new ClientEditorInput(new Path(path));

            ClientFactory.save(client, localFile, password);

            clientFile = new Path(path);

            setInput(newInput);
            setPartName(clientFile.lastSegment());

            isDirty = false;
            firePropertyChange(PROP_DIRTY);

            storePreferences();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            ErrorDialog.openError(getSite().getShell(), Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    // //////////////////////////////////////////////////////////////
    // preference store functions
    // //////////////////////////////////////////////////////////////

    // compatibility: the value used to be stored in the AbstractHistoricView
    private static final String IDENTIFIER = "AbstractHistoricView"; //$NON-NLS-1$

    public LinkedList<ReportingPeriod> loadReportingPeriods()
    {
        LinkedList<ReportingPeriod> answer = new LinkedList<ReportingPeriod>();

        String config = getPreferenceStore().getString(IDENTIFIER);
        if (config != null && config.trim().length() > 0)
        {
            String[] codes = config.split(";"); //$NON-NLS-1$
            for (String c : codes)
            {
                try
                {
                    answer.add(ReportingPeriod.from(c));
                }
                catch (IOException ignore)
                {
                    PortfolioPlugin.log(ignore);
                }
            }
        }

        if (answer.isEmpty())
        {
            for (int ii = 1; ii <= 5; ii++)
                answer.add(new ReportingPeriod.LastX(ii, 0));
        }

        return answer;
    }

    public void storeReportingPeriods(LinkedList<ReportingPeriod> periods)
    {
        StringBuilder buf = new StringBuilder();
        for (ReportingPeriod p : periods)
        {
            p.writeTo(buf);
            buf.append(';');
        }

        getPreferenceStore().setValue(IDENTIFIER, buf.toString());
    }

    private void storePreferences()
    {
        if (clientFile != null && preferences.needsSaving())
        {
            try
            {
                preferences.setFilename(getPreferenceStoreFile(clientFile).getAbsolutePath());
                preferences.save();
            }
            catch (IOException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }
    }

    private void loadPreferences()
    {
        if (clientFile != null)
        {
            try
            {
                File preferenceFile = getPreferenceStoreFile(clientFile);
                preferences.setFilename(preferenceFile.getAbsolutePath());
                if (preferenceFile.exists())
                {
                    preferences.load();
                }
            }
            catch (IOException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }
    }

    private File getPreferenceStoreFile(IPath file) throws IOException
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("MD5").digest(file.toOSString().getBytes()); //$NON-NLS-1$

            StringBuilder filename = new StringBuilder();
            filename.append("prf_"); //$NON-NLS-1$
            for (int i = 0; i < digest.length; i++)
                filename.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            filename.append(".txt"); //$NON-NLS-1$

            return new File(PortfolioPlugin.getDefault().getStateLocation().toFile(), filename.toString());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IOException(e);
        }
    }
}
