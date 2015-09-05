package name.abuchen.portfolio.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.LinkedList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.dialogs.PasswordDialog;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PortfolioPart implements LoadClientThread.Callback
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

    private File clientFile;
    private Client client;

    private PreferenceStore preferences = new PreferenceStore();
    private Job regularQuoteUpdateJob;

    private Composite container;
    private PageBook book;
    private AbstractFinanceView view;

    private Control focus;

    @Inject
    MDirtyable dirty;

    @Inject
    IEclipseContext context;

    @PostConstruct
    public void createComposite(Composite parent, MPart part) throws IOException
    {
        // is client available? (e.g. via new file wizard)
        Client attachedClient = (Client) part.getTransientData().get(Client.class.getName());
        if (attachedClient != null)
        {
            internalSetClient(attachedClient);
            dirty.setDirty(true);
        }

        // is file name available? (e.g. load file, open on startup)
        String filename = part.getPersistedState().get(UIConstants.Parameter.FILE);
        if (filename != null)
        {
            clientFile = new File(filename);
            loadPreferences();
        }

        if (attachedClient != null)
        {
            createContainerWithViews(parent);
        }
        else if (ClientFactory.isEncrypted(clientFile))
        {
            createContainerWithMessage(parent, MessageFormat.format(Messages.MsgOpenFile, clientFile.getName()), false,
                            true);
        }
        else
        {
            ProgressBar bar = createContainerWithMessage(parent,
                            MessageFormat.format(Messages.MsgLoadingFile, clientFile.getName()), true, false);

            new LoadClientThread(new ProgressMonitor(bar), this, clientFile, null).start();
        }
    }

    private void createContainerWithViews(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        SashForm sash = new SashForm(container, SWT.HORIZONTAL | SWT.SMOOTH);
        sash.setSashWidth(1);

        Composite navigationBar = new Composite(sash, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).margins(0, 0).applyTo(navigationBar);

        ClientEditorSidebar sidebar = new ClientEditorSidebar(this);
        Control control = sidebar.createSidebarControl(navigationBar);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(control);

        IEclipseContext childContext = context.createChild();
        childContext.set(Composite.class, navigationBar);
        childContext.set(Client.class, client);
        ClientProgressProvider provider = ContextInjectionFactory.make(ClientProgressProvider.class, childContext);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(provider.getControl());

        book = new PageBook(sash, SWT.NONE);

        SashHelper sashHelper = new SashHelper(PortfolioPart.class.getSimpleName() + "-sash", getPreferenceStore()); //$NON-NLS-1$
        sashHelper.setConstantWidth(new int[] { 180, -1 });
        sashHelper.attachTo(sash);

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

        Label image = new Label(container, SWT.NONE);
        image.setBackground(container.getBackground());
        image.setImage(PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_SMALL));

        FormData data = new FormData();
        data.top = new FormAttachment(50, -50);
        data.left = new FormAttachment(50, -24);
        image.setLayoutData(data);

        if (showPasswordField)
        {
            Text pwd = createPasswordField(container);

            data = new FormData();
            data.top = new FormAttachment(image, 10);
            data.left = new FormAttachment(image, 0, SWT.CENTER);
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
        label.setBackground(container.getBackground());
        label.setText(message);

        data = new FormData();
        data.top = new FormAttachment(image, 40);
        data.left = new FormAttachment(50, -100);
        data.width = 200;
        label.setLayoutData(data);

        return bar;
    }

    private Text createPasswordField(Composite container)
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
                        ProgressBar bar = createContainerWithMessage(
                                        parent,
                                        MessageFormat.format(Messages.MsgLoadingFile,
                                                        PortfolioPart.this.clientFile.getName()), true, false);
                        new LoadClientThread(new ProgressMonitor(bar), PortfolioPart.this, clientFile, password
                                        .toCharArray()).start();
                    }
                });
            }
        });

        return pwd;
    }

    @Override
    public void setClient(Client client)
    {
        internalSetClient(client);

        Display.getDefault().asyncExec(new BuildContainerRunnable()
        {
            @Override
            public void createContainer(Composite parent)
            {
                createContainerWithViews(parent);
            }
        });
    }

    public void internalSetClient(Client client)
    {
        this.client = client;
        this.dirty.setDirty(false);

        client.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                notifyModelUpdated();
            }
        });

        new ConsistencyChecksJob(client, false).schedule(100);
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
                createContainerWithMessage(parent, message, false, ClientFactory.isEncrypted(clientFile));
            }
        });
    }

    @Focus
    public void setFocus()
    {
        if (focus != null && !focus.isDisposed())
            focus.setFocus();
    }

    @PreDestroy
    public void destroy(MPart part)
    {
        if (clientFile != null)
            storePreferences();

        if (regularQuoteUpdateJob != null)
            regularQuoteUpdateJob.cancel();
    }

    @Persist
    public void save(MPart part, @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        if (clientFile == null)
        {
            doSaveAs(part, shell, null, null);
            return;
        }

        try
        {
            boolean useExperimentalCryptor = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getBoolean(PortfolioPlugin.Preferences.CRYPTO_USE_EXPERIMENTAL);
            
            part.getPersistedState().put(UIConstants.Parameter.FILE, clientFile.getAbsolutePath());
            ClientFactory.save(client, clientFile, null, null, useExperimentalCryptor);
            dirty.setDirty(false);

            storePreferences();
        }
        catch (IOException e)
        {
            ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public void doSaveAs(MPart part, Shell shell, String extension, String encryptionMethod)
    {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);

        // if an extension is given, make sure the file name proposal has the
        // right extension in the save as dialog
        String fileNameProposal = clientFile != null ? clientFile.getName() : part.getLabel();
        if (extension != null && !fileNameProposal.endsWith('.' + extension))
        {
            int p = fileNameProposal.lastIndexOf('.');
            fileNameProposal = (p > 0 ? fileNameProposal.substring(0, p + 1) : fileNameProposal + '.') + extension;
        }

        dialog.setFileName(fileNameProposal);
        dialog.setFilterPath(clientFile != null ? clientFile.getAbsolutePath() : System.getProperty("user.home")); //$NON-NLS-1$

        String path = dialog.open();
        if (path == null)
            return;

        // again make sure the extension is correct as the user might have
        // changed it in the save dialog
        if (extension != null && !path.endsWith('.' + extension))
            path += '.' + extension;

        File localFile = new File(path);
        char[] password = null;

        if (ClientFactory.isEncrypted(localFile))
        {
            PasswordDialog pwdDialog = new PasswordDialog(shell);
            if (pwdDialog.open() != PasswordDialog.OK)
                return;
            password = pwdDialog.getPassword().toCharArray();
        }

        try
        {
            clientFile = localFile;
            
            boolean useExperimentalCryptor = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getBoolean(PortfolioPlugin.Preferences.CRYPTO_USE_EXPERIMENTAL);

            part.getPersistedState().put(UIConstants.Parameter.FILE, clientFile.getAbsolutePath());
            ClientFactory.save(client, clientFile, encryptionMethod, password, useExperimentalCryptor);

            dirty.setDirty(false);
            part.setLabel(clientFile.getName());
            part.setTooltip(clientFile.getAbsolutePath());

            storePreferences();
        }
        catch (IOException e)
        {
            ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
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
        dirty.setDirty(true);
    }

    public void notifyModelUpdated()
    {
        Display.getDefault().asyncExec(new Runnable()
        {
            public void run()
            {
                markDirty();

                if (view != null && view.getControl() != null && !view.getControl().isDisposed())
                    view.notifyModelUpdated();
            }
        });
    }

    public void activateView(String target, Object parameter)
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
            view.setFocus();
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

    private void scheduleOnlineUpdateJobs()
    {
        if (!"no".equals(System.getProperty("name.abuchen.portfolio.auto-updates"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            regularQuoteUpdateJob = new UpdateQuotesJob(client, false, 1000 * 60 * 10);
            regularQuoteUpdateJob.schedule(500);

            new UpdateQuotesJob(client).schedule(1000);

            new UpdateCPIJob(client).schedule(700);
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
                catch (IOException | RuntimeException ignore)
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

    private File getPreferenceStoreFile(File file) throws IOException
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("MD5").digest(file.getAbsolutePath().getBytes()); //$NON-NLS-1$

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
