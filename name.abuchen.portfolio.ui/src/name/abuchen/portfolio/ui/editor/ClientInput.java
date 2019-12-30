package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.PasswordDialog;
import name.abuchen.portfolio.ui.jobs.CreateInvestmentPlanTxJob;
import name.abuchen.portfolio.ui.jobs.SyncOnlineSecuritiesJob;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.wizards.client.ClientMigrationDialog;

@SuppressWarnings("restriction")
public class ClientInput
{
    // compatibility: the value used to be stored in the AbstractHistoricView
    private static final String REPORTING_PERIODS_KEY = "AbstractHistoricView"; //$NON-NLS-1$

    private String label;
    private File clientFile;
    private Client client;

    private Navigation navigation;

    private PreferenceStore preferenceStore = new PreferenceStore();
    private ExchangeRateProviderFactory exchangeRateProviderFacory;
    private LinkedList<ReportingPeriod> reportingPeriods;

    private boolean isDirty = false;
    private List<Job> regularJobs = new ArrayList<>();
    private List<ClientInputListener> listeners = new ArrayList<>();

    @Inject
    private IEventBroker broker;

    @Inject
    private IEclipseContext context;

    @Inject
    @Preference
    private IEclipsePreferences preferences;

    /* protected */ ClientInput(String label, File clientFile)
    {
        this.label = label;
        this.clientFile = clientFile;
    }

    public void addListener(ClientInputListener listener)
    {
        this.listeners.add(listener);
    }

    public void removeListener(ClientInputListener listener)
    {
        this.listeners.remove(listener);
    }

    public boolean isDirty()
    {
        return isDirty;
    }

    /**
     * See {@link Client#markDirty}.
     */
    public void markDirty()
    {
        setDirty(true, true);
    }

    /**
     * See {@link Client#touch}.
     */
    public void touch()
    {
        setDirty(true, false);
    }

    private void setDirty(boolean isDirty, boolean recalculate)
    {
        this.isDirty = isDirty;
        this.listeners.forEach(l -> l.onDirty(this.isDirty));

        if (isDirty && recalculate)
            this.listeners.forEach(ClientInputListener::onRecalculationNeeded);
    }

    public String getLabel()
    {
        return label;
    }

    public Client getClient()
    {
        return client;
    }

    public File getFile()
    {
        return clientFile;
    }

    public Navigation getNavigation()
    {
        return navigation;
    }

    public ExchangeRateProviderFactory getExchangeRateProviderFacory()
    {
        return exchangeRateProviderFacory;
    }

    public PreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void savePreferences()
    {
        storePreferences(false);
    }

    public void save(Shell shell)
    {
        if (clientFile == null)
        {
            doSaveAs(shell, null, null);
            return;
        }

        BusyIndicator.showWhile(shell.getDisplay(), () -> {
            try
            {
                if (preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true))
                    createBackup(clientFile, "backup"); //$NON-NLS-1$

                ClientFactory.save(client, clientFile, null, null);
                storePreferences(false);

                broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
                setDirty(false, false);
                listeners.forEach(ClientInputListener::onSaved);
            }
            catch (IOException e)
            {
                ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                                new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        });
    }

    public void doSaveAs(Shell shell, String extension, String encryptionMethod) // NOSONAR
    {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setOverwrite(true);

        // if an extension is given, make sure the file name proposal has the
        // right extension in the save as dialog
        String fileNameProposal = clientFile != null ? clientFile.getName() : getLabel();
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

        clientFile = localFile;
        label = localFile.getName();
        char[] pwd = password;

        BusyIndicator.showWhile(shell.getDisplay(), () -> {
            try
            {
                ClientFactory.save(client, clientFile, encryptionMethod, pwd);
                storePreferences(true);

                broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
                setDirty(false, false);
                listeners.forEach(ClientInputListener::onSaved);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
                ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                                new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        });
    }

    public void createBackupAfterOpen()
    {
        if (clientFile != null && preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true))
            createBackup(clientFile, "backup-after-open"); //$NON-NLS-1$
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

    private void storePreferences(boolean forceWrite)
    {
        if (clientFile == null)
            return;

        storeReportingPeriods();

        if (!forceWrite && !preferenceStore.needsSaving())
            return;

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

    private void loadPreferences()
    {
        if (clientFile == null)
            return;

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

    private File getPreferenceStoreFile(File file) throws IOException
    {
        boolean storeNextToFile = preferences.getBoolean(UIConstants.Preferences.STORE_SETTINGS_NEXT_TO_FILE, false);

        if (storeNextToFile)
        {
            String filename = file.getName();
            int last = filename.lastIndexOf('.');
            if (last > 0)
                filename = filename.substring(0, last);

            return new File(file.getParentFile(), filename + ".settings"); //$NON-NLS-1$
        }
        else
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

    public LinkedList<ReportingPeriod> getReportingPeriods() // NOSONAR
    {
        if (reportingPeriods != null)
            return reportingPeriods;

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

        reportingPeriods = answer;

        return reportingPeriods;
    }

    private void storeReportingPeriods()
    {
        if (reportingPeriods == null)
            return;

        StringBuilder buf = new StringBuilder();
        for (ReportingPeriod p : reportingPeriods)
        {
            p.writeTo(buf);
            buf.append(';');
        }

        getPreferenceStore().setValue(REPORTING_PERIODS_KEY, buf.toString());
    }

    @Inject
    @Optional
    public void onExchangeRatesLoaded(@UIEventTopic(UIConstants.Event.ExchangeRates.LOADED) Object obj)
    {
        if (exchangeRateProviderFacory != null)
        {
            exchangeRateProviderFacory.clearCache();
            listeners.forEach(ClientInputListener::onRecalculationNeeded);
        }
    }

    private void scheduleOnlineUpdateJobs()
    {
        if (preferences.getBoolean(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, true))
        {
            Job initialQuoteUpdate = new UpdateQuotesJob(client,
                            EnumSet.of(UpdateQuotesJob.Target.LATEST, UpdateQuotesJob.Target.HISTORIC));
            initialQuoteUpdate.schedule(1000);

            CreateInvestmentPlanTxJob checkInvestmentPlans = new CreateInvestmentPlanTxJob(client,
                            exchangeRateProviderFacory);
            checkInvestmentPlans.startAfter(initialQuoteUpdate);
            checkInvestmentPlans.schedule(1100);

            int thirtyMinutes = 1000 * 60 * 30;
            Job job = new UpdateQuotesJob(client, EnumSet.of(UpdateQuotesJob.Target.LATEST)).repeatEvery(thirtyMinutes);
            job.schedule(thirtyMinutes);
            regularJobs.add(job);

            int sixHours = 1000 * 60 * 60 * 6;
            job = new UpdateQuotesJob(client, EnumSet.of(UpdateQuotesJob.Target.HISTORIC)).repeatEvery(sixHours);
            job.schedule(sixHours);
            regularJobs.add(job);

            new SyncOnlineSecuritiesJob(client).schedule(2000);
        }
    }

    /* package */ void setErrorMessage(String message)
    {
        this.listeners.forEach(l -> l.onError(message));
    }

    /* package */ void setClient(Client client)
    {
        if (this.client != null)
            throw new IllegalArgumentException();

        this.client = client;

        IEclipseContext c2 = EclipseContextFactory.create();
        c2.set(Client.class, client);
        this.exchangeRateProviderFacory = ContextInjectionFactory //
                        .make(ExchangeRateProviderFactory.class, this.context, c2);

        this.navigation = new Navigation(client);

        client.addPropertyChangeListener(event -> {

            boolean recalculate = !"touch".equals(event.getPropertyName()); //$NON-NLS-1$

            // convenience: Client#markDirty can be called on any thread, but
            // ClientInputListener#onDirty will always be called on the UI
            // thread

            if (Display.getDefault().getThread() == Thread.currentThread())
            {
                setDirty(true, recalculate);
            }
            else
            {
                Display.getDefault().asyncExec(() -> setDirty(true, recalculate));
            }
        });

        loadPreferences();

        scheduleOnlineUpdateJobs();

        this.listeners.forEach(ClientInputListener::onLoaded);

        if (client.getFileVersionAfterRead() < Client.VERSION_WITH_CURRENCY_SUPPORT)
        {
            Display.getDefault().asyncExec(() -> {
                Dialog dialog = new ClientMigrationDialog(Display.getDefault().getActiveShell(), client);
                dialog.open();
            });
        }
    }

    /* package */ void notifyListeners(Consumer<ClientInputListener> consumer)
    {
        this.listeners.forEach(consumer::accept);
    }
}
