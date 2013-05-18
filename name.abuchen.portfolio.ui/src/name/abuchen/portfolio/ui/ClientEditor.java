package name.abuchen.portfolio.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
    private boolean isDirty = false;
    private IPath clientFile;
    private Client client;

    private PreferenceStore preferences = new PreferenceStore();

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
                {
                    client = ClientFactory.load(clientFile.toFile());
                    isDirty = false;
                }
                else
                {
                    client = ((ClientEditorInput) input).getClient();
                    isDirty = true;
                }
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

        client.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                markDirty();
            }
        });

        loadPreferences();

        if (clientFile != null)
            setPartName(clientFile.lastSegment());
        else
            setPartName(Messages.LabelUnsavedFile);

        new ConsistencyChecksJob(this, client, false).schedule(100);
        scheduleOnlineUpdateJobs();
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
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(1, 1).applyTo(container);

        ClientEditorSidebar sidebar = new ClientEditorSidebar(this);
        Control control = sidebar.createSidebarControl(container);
        GridDataFactory.fillDefaults().hint(180, SWT.DEFAULT).grab(false, true).applyTo(control);

        book = new PageBook(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(book);

        sidebar.selectDefaultView();
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
        book.setFocus();
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
            ClientFactory.save(client, clientFile.toFile());
            isDirty = false;
            firePropertyChange(PROP_DIRTY);

            storePreferences();
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

            storePreferences();
        }
        catch (IOException e)
        {
            ErrorDialog.openError(getSite().getShell(), Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    // //////////////////////////////////////////////////////////////
    // preference store functions
    // //////////////////////////////////////////////////////////////

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
