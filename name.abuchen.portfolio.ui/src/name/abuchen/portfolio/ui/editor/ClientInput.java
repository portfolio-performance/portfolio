package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.PasswordDialog;
import name.abuchen.portfolio.ui.wizards.client.ClientMigrationDialog;

@SuppressWarnings("restriction")
public class ClientInput
{
    public interface ClientInputListener
    {
        void onLoading(int totalWork, int worked);

        void onLoaded();

        void onError(String message);

        void onSaved();

        void onDirty(boolean isDirty);
    }

    private String label;
    private File clientFile;
    private Client client;
    private PreferenceStore preferenceStore = new PreferenceStore();

    private List<ClientInputListener> listeners = new ArrayList<>();

    private boolean isDirty;

    @Inject
    private IEventBroker broker;

    @Inject
    @Preference
    private IEclipsePreferences preferences;

    private ClientInput(File clientFile)
    {
        this(clientFile.getName(), null, clientFile);
        this.isDirty = false;
    }

    private ClientInput(String label, Client client)
    {
        this(label, client, null);
        this.isDirty = true;
    }

    private ClientInput(String label, Client client, File clientFile)
    {
        this.label = label;
        this.client = client;
        this.clientFile = clientFile;

        if (client != null)
            client.addPropertyChangeListener(event -> markDirty());
    }

    public static ClientInput createFor(File clientFile, IEclipseContext context)
    {
        ClientInput answer = new ClientInput(clientFile);
        ContextInjectionFactory.inject(answer, context);
        return answer;
    }

    public static ClientInput createFor(String label, Client client, IEclipseContext context)
    {
        ClientInput answer = new ClientInput(label, client);
        ContextInjectionFactory.inject(answer, context);
        return answer;
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

    public void markDirty()
    {
        setDirty(true);
    }

    private void setDirty(boolean isDirty)
    {
        this.isDirty = isDirty;
        this.listeners.forEach(l -> l.onDirty(this.isDirty));
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

    public PreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void save(Shell shell)
    {
        if (clientFile == null)
        {
            doSaveAs(shell, null, null);
            return;
        }

        try
        {
            if (preferences.getBoolean(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true))
                createBackup(clientFile, "backup"); //$NON-NLS-1$

            ClientFactory.save(client, clientFile, null, null);

            broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
            setDirty(false);
            listeners.forEach(ClientInputListener::onSaved);
        }
        catch (IOException e)
        {
            ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(),
                            new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
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

        try
        {
            clientFile = localFile;
            label = localFile.getName();

            ClientFactory.save(client, clientFile, encryptionMethod, password);

            broker.post(UIConstants.Event.File.SAVED, clientFile.getAbsolutePath());
            setDirty(false);
            listeners.forEach(ClientInputListener::onSaved);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
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

    /* package */ void setErrorMessage(String message)
    {
        this.listeners.forEach(l -> l.onError(message));
    }

    /* package */ void setClient(Client client)
    {
        if (this.client != null)
            throw new IllegalArgumentException();

        this.client = client;

        client.addPropertyChangeListener(event -> markDirty());

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
