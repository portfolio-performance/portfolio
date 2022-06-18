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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.PasswordDialog;
import name.abuchen.portfolio.ui.jobs.AutoSaveJob;
import name.abuchen.portfolio.ui.jobs.CreateInvestmentPlanTxJob;
import name.abuchen.portfolio.ui.jobs.SyncOnlineSecuritiesJob;
import name.abuchen.portfolio.ui.jobs.UpdateDividendsJob;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.preferences.BackupMode;
import name.abuchen.portfolio.ui.wizards.client.ClientMigrationDialog;

public class ClientInput
{
    // compatibility: the value used to be stored in the AbstractHistoricView
    private static final String REPORTING_PERIODS_KEY = "AbstractHistoricView"; //$NON-NLS-1$

    public static final String DEFAULT_RELATIVE_BACKUP_FOLDER = "backups"; //$NON-NLS-1$

    private String label;
    private File clientFile;
    private Client client;

    private Navigation navigation;

    private PreferenceStore preferenceStore = new PreferenceStore();
    private ExchangeRateProviderFactory exchangeRateProviderFacory;
    private List<ReportingPeriod> reportingPeriods;

    private boolean isDirty = false;
    private List<Job> regularJobs = new ArrayList<>();
    private List<Runnable> disposeJobs = new ArrayList<>();
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

    /**
     * Called when the last editor for a given ClientInput is closed
     */
    public void dispose()
    {
        for (Job job : regularJobs)
            job.cancel();

        for (Runnable runnable : disposeJobs)
            runnable.run();

        this.client = null;
        this.clientFile = null;
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
            doSaveAs(shell, null, EnumSet.of(SaveFlag.XML));
            return;
        }

        BusyIndicator.showWhile(shell.getDisplay(), () -> {
            try
            {
                if (preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true))
                    createBackup(clientFile, "backup"); //$NON-NLS-1$

                ClientFactory.save(client, clientFile);
                storePreferences(false);

                broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
                setDirty(false, false);
                listeners.forEach(ClientInputListener::onSaved);
            }
            catch (IOException e)
            {
                ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                                new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        });
    }

    public void doSaveAs(Shell shell, String extension, Set<SaveFlag> flags) // NOSONAR
    {
        String fileNameProposal = clientFile != null ? clientFile.getName() : getLabel();
        File localFile = pickFile(shell, extension, fileNameProposal);
        if (localFile == null)
            return;

        char[] password = null;

        if (flags.contains(SaveFlag.ENCRYPTED))
        {
            PasswordDialog pwdDialog = new PasswordDialog(shell);
            if (pwdDialog.open() != Window.OK)
                return;
            password = pwdDialog.getPassword().toCharArray();
        }

        clientFile = localFile;
        label = localFile.getName();
        char[] pwd = password;

        BusyIndicator.showWhile(shell.getDisplay(), () -> {
            try
            {
                ClientFactory.saveAs(client, clientFile, pwd, flags);
                storePreferences(true);

                broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
                setDirty(false, false);
                listeners.forEach(ClientInputListener::onSaved);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
                ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                                new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        });
    }

    /**
     * Exports the current data into a new file without changing any of the
     * editor settings.
     */
    public void doExportAs(Shell shell, String extension, Set<SaveFlag> flags)
    {
        if (flags.contains(SaveFlag.ENCRYPTED))
            throw new IllegalArgumentException();

        String fileNameProposal = clientFile != null ? clientFile.getName() : getLabel();
        if (!fileNameProposal.endsWith('.' + extension))
            fileNameProposal += '.' + extension;
        File localFile = pickFile(shell, extension, fileNameProposal);
        if (localFile == null)
            return;

        BusyIndicator.showWhile(shell.getDisplay(), () -> {
            try
            {
                ClientFactory.exportAs(client, localFile, null, flags);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
                ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                                new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        });
    }

    private File pickFile(Shell shell, String extension, String fileNameProposal)
    {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setOverwrite(true);

        // if an extension is given, make sure the file name proposal has the
        // right extension in the save as dialog
        if (extension != null && !fileNameProposal.endsWith('.' + extension))
        {
            int p = fileNameProposal.lastIndexOf('.');
            fileNameProposal = (p > 0 ? fileNameProposal.substring(0, p + 1) : fileNameProposal + '.') + extension;
        }

        dialog.setFileName(fileNameProposal);

        // On macOS, the save as dialog does not seem to accept the existing
        // directory path. However, when using the user home directory, it
        // works. Something to do with the sandbox? Or requires a newer version
        // of SWT?

        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            dialog.setFilterPath(System.getProperty("user.home")); //$NON-NLS-1$
        else
            dialog.setFilterPath(clientFile != null ? clientFile.getAbsoluteFile().getParent()
                            : System.getProperty("user.home")); //$NON-NLS-1$

        String path = dialog.open();
        if (path == null)
            return null;

        // again make sure the extension is correct as the user might have
        // changed it in the save dialog
        if (extension != null && !path.endsWith('.' + extension))
            path += '.' + extension;

        return new File(path);
    }

    /**
     * autoSave is called from AutoSaveJob and uses the file generated during
     * startup, if set in preferences
     */
    public void autoSave()
    {
        if (clientFile != null)
        {
            // generate the same name used when creating the initial autosave
            // file
            // see @scheduleAutoSaveJob

            String backupName = constructFilename(clientFile, "autosave"); //$NON-NLS-1$
            File autosaveFile = clientFile.toPath().resolveSibling(backupName).toFile();

            try
            {
                ClientFactory.save(client, autosaveFile);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }
        }
    }

    private long getAutoSavePrefs()
    {
        int delay = preferences.getInt(UIConstants.Preferences.AUTO_SAVE_FILE, 0);
        return delay > 0 ? 1000 * 60 * delay : 0;
    }

    public void createBackupAfterOpen()
    {
        if (clientFile != null && preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true)
                        && preferences.getInt(UIConstants.Preferences.AUTO_SAVE_FILE, 0) == 0)
            createBackup(clientFile, "backup-after-open"); //$NON-NLS-1$
    }

    private void createBackup(File file, String suffix)
    {
        try
        {
            // keep original extension in order to be able to open the backup
            // file directly from within PP
            String backupName = constructFilename(file, suffix);
            Path sourceFile = file.toPath();
            Path backupFile = getBackupFilePath(sourceFile, backupName);

            Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
                            Messages.LabelError, e.getMessage()));
        }
    }

    private Path getBackupFilePath(Path sourceFile, String backupName)
    {
        BackupMode mode = BackupMode.getDefault();

        try
        {
            mode = BackupMode.valueOf(preferences.get(UIConstants.Preferences.BACKUP_MODE, mode.name()));
        }
        catch (IllegalArgumentException ignore)
        {
            // use the standard backup mode instead
        }

        if (mode == BackupMode.ABSOLUTE_FOLDER)
        {
            String folder = preferences.get(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE, null);

            if (folder != null && !folder.isBlank())
            {
                Path path = Path.of(folder);
                if (Files.exists(path) && Files.isDirectory(path))
                    return path.resolve(backupName);
            }
        }
        else if (mode == BackupMode.RELATIVE_FOLDER)
        {
            String folderName = preferences.get(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE,
                            DEFAULT_RELATIVE_BACKUP_FOLDER);

            Path folder = sourceFile.resolveSibling(folderName).normalize();

            if (Files.exists(folder))
            {
                if (Files.isDirectory(folder))
                    return folder.resolve(backupName);
            }
            else
            {
                try
                {
                    return Files.createDirectories(folder).resolve(backupName);
                }
                catch (IOException | SecurityException e)
                {
                    PortfolioPlugin.log(e);

                    // in case of error, we continue and write backup as a
                    // sibling (default mode)
                }
            }
        }

        return sourceFile.resolveSibling(backupName);
    }

    private String constructFilename(File file, String suffix)
    {
        String filename = file.getName();
        int l = filename.lastIndexOf('.');
        return l > 0 ? filename.substring(0, l) + '.' + suffix + filename.substring(l) : filename + '.' + suffix;
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

    public List<ReportingPeriod> getReportingPeriods()
    {
        if (reportingPeriods != null)
            return reportingPeriods;

        List<ReportingPeriod> answer = new ArrayList<>();

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
            for (int ii = 1; ii <= 3; ii++)
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
            buf.append(p.getCode()).append(';');

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

    @Inject
    @Optional
    public void onDiscreedModeChanged(@UIEventTopic(UIConstants.Event.Global.DISCREET_MODE) Object obj)
    {
        listeners.forEach(ClientInputListener::onRecalculationNeeded);
    }

    private void scheduleOnlineUpdateJobs()
    {
        if (preferences.getBoolean(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, true))
        {
            Predicate<Security> onlyActive = s -> !s.isRetired();

            Job initialQuoteUpdate = new UpdateQuotesJob(client, onlyActive,
                            EnumSet.of(UpdateQuotesJob.Target.LATEST, UpdateQuotesJob.Target.HISTORIC));
            initialQuoteUpdate.schedule(1000);

            CreateInvestmentPlanTxJob checkInvestmentPlans = new CreateInvestmentPlanTxJob(client,
                            exchangeRateProviderFacory);
            checkInvestmentPlans.startAfter(initialQuoteUpdate);
            checkInvestmentPlans.schedule(1100);

            int thirtyMinutes = 1000 * 60 * 30;
            Job job = new UpdateQuotesJob(client, onlyActive, EnumSet.of(UpdateQuotesJob.Target.LATEST))
                            .repeatEvery(thirtyMinutes);
            job.schedule(thirtyMinutes);
            regularJobs.add(job);

            int sixHours = 1000 * 60 * 60 * 6;
            job = new UpdateQuotesJob(client, onlyActive, EnumSet.of(UpdateQuotesJob.Target.HISTORIC))
                            .repeatEvery(sixHours);
            job.schedule(sixHours);
            regularJobs.add(job);

            new SyncOnlineSecuritiesJob(client).schedule(5000);
            new UpdateDividendsJob(getClient()).schedule(7000);
        }
    }

    private void scheduleAutoSaveJob()
    {
        IPreferenceChangeListener listener = event -> {
            if (event.getKey().contentEquals(UIConstants.Preferences.AUTO_SAVE_FILE))
            {
                for (Job j : regularJobs)
                {
                    if (j instanceof AutoSaveJob)
                    {
                        ((AutoSaveJob) j).setDelay(getAutoSavePrefs());
                        ((AutoSaveJob) j).schedule(getAutoSavePrefs());
                        ((AutoSaveJob) j).wakeUp(getAutoSavePrefs());
                    }
                }
            }
        };
        preferences.addPreferenceChangeListener(listener);
        this.disposeJobs.add(() -> preferences.removePreferenceChangeListener(listener));

        long delay = getAutoSavePrefs();

        Job job = new AutoSaveJob(this, delay);
        regularJobs.add(job);
        if (delay > 0)
            job.schedule(delay);
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

        this.navigation = ContextInjectionFactory.make(Navigation.class, this.context, c2);

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

        scheduleAutoSaveJob();

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
