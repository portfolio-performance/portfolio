package name.abuchen.portfolio.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
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

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.dialogs.PasswordDialog;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.ExceptionView;
import name.abuchen.portfolio.ui.wizards.client.ClientMigrationDialog;

@SuppressWarnings("restriction")
public class PortfolioPart implements LoadClientThread.Callback
{
    private abstract class BuildContainerRunnable implements Runnable
    {
        @Override
        public final void run()
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

    // compatibility: the value used to be stored in the AbstractHistoricView
    private static final String REPORTING_PERIODS_KEY = "AbstractHistoricView"; //$NON-NLS-1$

    private File clientFile;
    private Client client;

    private PreferenceStore preferenceStore = new PreferenceStore();
    private List<Job> regularJobs = new ArrayList<>();

    private Composite container;
    private PageBook book;
    private AbstractFinanceView view;

    private Control focus;

    @Inject
    MDirtyable dirty;

    @Inject
    IEclipseContext context;

    @Inject
    IEventBroker broker;

    @Inject
    @Preference
    IEclipsePreferences preferences;

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
        String filename = part.getPersistedState().get(UIConstants.File.PERSISTED_STATE_KEY);
        if (filename != null)
        {
            clientFile = new File(filename);
            broker.post(UIConstants.Event.File.OPENED, clientFile.getAbsolutePath());
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

            new LoadClientThread(broker, new ProgressMonitor(bar), this, clientFile, null).start();
        }
    }

    private void createContainerWithViews(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        Composite sash = new Composite(container, SWT.NONE);
        SashLayout sashLayout = new SashLayout(sash, SWT.HORIZONTAL | SWT.BEGINNING);
        sash.setLayout(sashLayout);

        Composite navigationBar = new Composite(sash, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).applyTo(navigationBar);

        ClientEditorSidebar sidebar = new ClientEditorSidebar(this);
        Control control = sidebar.createSidebarControl(navigationBar);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(control);
        
        sashLayout.addQuickNavigation(manager -> sidebar.menuAboutToShow(manager));
                
        Composite divider = new Composite(navigationBar, SWT.NONE);
        divider.setBackground(Colors.SIDEBAR_BORDER);
        GridDataFactory.fillDefaults().span(0, 2).hint(1, SWT.DEFAULT).applyTo(divider);

        ClientProgressProvider provider = make(ClientProgressProvider.class, client, navigationBar);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(provider.getControl());

        book = new PageBook(sash, SWT.NONE);

        // restore & save size of navigation bar
        final String sashIdentifier = PortfolioPart.class.getSimpleName() + "-newsash"; //$NON-NLS-1$
        int size = getPreferenceStore().getInt(sashIdentifier);
        navigationBar.setLayoutData(new SashLayoutData(size != 0 ? size : 180));
        sash.addDisposeListener(e -> getPreferenceStore().setValue(sashIdentifier,
                        ((SashLayoutData) navigationBar.getLayoutData()).getSize()));

        sidebar.selectDefaultView();

        focus = book;
    }

    /**
     * Creates window with logo and message. Optional a progress bar (while loading)
     * or a password input field (if encrypted).
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
        image.setImage(Images.LOGO_48.image());

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
                        ProgressBar bar = createContainerWithMessage(parent, MessageFormat.format(
                                        Messages.MsgLoadingFile, PortfolioPart.this.clientFile.getName()), true, false);
                        new LoadClientThread(broker, new ProgressMonitor(bar), PortfolioPart.this, clientFile,
                                        password.toCharArray()).start();
                    }
                });
            }
        });

        return pwd;
    }

    @Override
    public void setClient(Client client)
    {
        // additional safeguard: make a copy of the file that could be
        // successfully read b/c we get reports of corrupted files

        if (clientFile != null && preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true))
            createBackup(clientFile, "backup-after-open"); //$NON-NLS-1$

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
        this.context.set(Client.class, client);

        client.addPropertyChangeListener(event -> notifyModelUpdated());

        if (client.getFileVersionAfterRead() < Client.VERSION_WITH_CURRENCY_SUPPORT)
        {
            Display.getDefault().asyncExec(() -> {
                Dialog dialog = new ClientMigrationDialog(Display.getDefault().getActiveShell(), client);
                dialog.open();
            });
        }

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
    public void destroy()
    {
        if (clientFile != null)
            storePreferences();

        regularJobs.forEach(Job::cancel);
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
            part.getPersistedState().put(UIConstants.File.PERSISTED_STATE_KEY, clientFile.getAbsolutePath());

            if (preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true))
                createBackup(clientFile, "backup"); //$NON-NLS-1$

            ClientFactory.save(client, clientFile, null, null);
            broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
            dirty.setDirty(false);

            storePreferences();
        }
        catch (IOException e)
        {
            ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                            new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    private void createBackup(File file, String suffix)
    {
        try
        {
            // keep original extension in order to be able to open the backup
            // file directly from within PP
            String filename = file.getName();
            int l = filename.lastIndexOf('.');
            String backupName = l > 0 ? filename.substring(0, l) + '.' + suffix + filename.substring(l)
                            : filename + '.' + suffix;

            Path sourceFile = file.toPath();
            Path backupFile = sourceFile.resolveSibling(backupName);
            Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
                            Messages.LabelError, e.getMessage()));
        }
    }

    public void doSaveAs(MPart part, Shell shell, String extension, String encryptionMethod) // NOSONAR
    {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setOverwrite(true);

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

            part.getPersistedState().put(UIConstants.File.PERSISTED_STATE_KEY, clientFile.getAbsolutePath());
            ClientFactory.save(client, clientFile, encryptionMethod, password);
            broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());

            dirty.setDirty(false);
            part.setLabel(clientFile.getName());
            part.setTooltip(clientFile.getAbsolutePath());

            storePreferences();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                            new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public Client getClient()
    {
        return client;
    }

    public IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    /* package */void markDirty()
    {
        dirty.setDirty(true);
    }

    @Inject
    @Optional
    public void onExchangeRatesLoaded(@UIEventTopic(UIConstants.Event.ExchangeRates.LOADED) Object obj)
    {
        // update view w/o marking the model dirty
        if (view != null && view.getControl() != null && !view.getControl().isDisposed())
            view.notifyModelUpdated();
    }

    public void notifyModelUpdated()
    {
        Display.getDefault().asyncExec(() -> {
            markDirty();

            if (view != null && view.getControl() != null && !view.getControl().isDisposed())
                view.notifyModelUpdated();
        });
    }

    @SuppressWarnings("unchecked")
    public void activateView(String target, Object parameter)
    {
        disposeView();

        try
        {
            Class<?> clazz = getClass().getClassLoader()
                            .loadClass("name.abuchen.portfolio.ui.views." + target + "View"); //$NON-NLS-1$ //$NON-NLS-2$
            if (clazz == null)
                return;

            createView((Class<AbstractFinanceView>) clazz, parameter);
        }
        catch (Exception e)
        {
            PortfolioPlugin.log(e);

            createView(ExceptionView.class, e);
        }
    }

    private void createView(Class<? extends AbstractFinanceView> clazz, Object parameter)
    {
        IEclipseContext viewContext = this.context.createChild();
        viewContext.set(Client.class, this.client);
        viewContext.set(IPreferenceStore.class, this.preferenceStore);
        viewContext.set(PortfolioPart.class, this);

        view = ContextInjectionFactory.make(clazz, viewContext);
        viewContext.set(AbstractFinanceView.class, view);
        view.setContext(viewContext);
        view.init(this, parameter);
        view.createViewControl(book);

        book.showPage(view.getControl());
        view.setFocus();
    }

    private void disposeView()
    {
        if (view != null)
        {
            view.getContext().dispose();

            if (!view.getControl().isDisposed())
                view.getControl().dispose();
            view = null;
        }
    }

    private void scheduleOnlineUpdateJobs()
    {
        if (!"no".equals(System.getProperty("name.abuchen.portfolio.auto-updates"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            new UpdateQuotesJob(client, EnumSet.of(UpdateQuotesJob.Target.LATEST, UpdateQuotesJob.Target.HISTORIC))
                            .schedule(1000);

            int tenMinutes = 1000 * 60 * 10;
            Job job = new UpdateQuotesJob(client, EnumSet.of(UpdateQuotesJob.Target.LATEST)).repeatEvery(tenMinutes);
            job.schedule(tenMinutes);
            regularJobs.add(job);

            int sixHours = 1000 * 60 * 60 * 6;
            job = new UpdateQuotesJob(client, EnumSet.of(UpdateQuotesJob.Target.HISTORIC)).repeatEvery(sixHours);
            job.schedule(sixHours);
            regularJobs.add(job);

            new UpdateCPIJob(client).schedule(1000);
        }
    }

    // //////////////////////////////////////////////////////////////
    // preference store functions
    // //////////////////////////////////////////////////////////////

    public LinkedList<ReportingPeriod> loadReportingPeriods() // NOSONAR
    {
        LinkedList<ReportingPeriod> answer = new LinkedList<>();

        String config = getPreferenceStore().getString(REPORTING_PERIODS_KEY);
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

    public void storeReportingPeriods(List<ReportingPeriod> periods)
    {
        StringBuilder buf = new StringBuilder();
        for (ReportingPeriod p : periods)
        {
            p.writeTo(buf);
            buf.append(';');
        }

        getPreferenceStore().setValue(REPORTING_PERIODS_KEY, buf.toString());
    }

    private void storePreferences()
    {
        if (clientFile != null && preferenceStore.needsSaving())
        {
            try
            {
                preferenceStore.setFilename(getPreferenceStoreFile(clientFile).getAbsolutePath());
                preferenceStore.save();
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
                preferenceStore.setFilename(preferenceFile.getAbsolutePath());
                if (preferenceFile.exists())
                {
                    preferenceStore.load();
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

    private <T> T make(Class<T> type, Object... parameters)
    {
        IEclipseContext c2 = EclipseContextFactory.create();
        if (parameters != null)
            for (Object param : parameters)
                c2.set(param.getClass().getName(), param);
        return ContextInjectionFactory.make(type, this.context, c2);
    }
}
