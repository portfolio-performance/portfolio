package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.wizards.client.ClientMigrationDialog;

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

    private List<Job> regularJobs = new ArrayList<>();
    private List<ClientInputListener> listeners = new ArrayList<>();

    private boolean isDirty;

    public ClientInput(File clientFile)
    {
        this(clientFile.getName(), null, clientFile);
        this.isDirty = false;
    }

    public ClientInput(String label, Client client)
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

    public void save()
    {
        setDirty(false);
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
