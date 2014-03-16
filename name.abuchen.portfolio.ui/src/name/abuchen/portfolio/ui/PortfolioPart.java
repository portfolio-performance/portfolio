package name.abuchen.portfolio.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class PortfolioPart
{
    private File clientFile;
    private Client client;

    private PreferenceStore preferences = new PreferenceStore();

    private PageBook book;
    private AbstractFinanceView view;

    @Inject
    MDirtyable dirty;

    @PostConstruct
    public void createComposite(Composite parent, MPart part) throws IOException
    {
        loadClient(part);
        loadPreferences();

        client.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                markDirty();
            }
        });

        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(1, 1).applyTo(container);

        ClientEditorSidebar sidebar = new ClientEditorSidebar(new ClientEditor(this));
        Control control = sidebar.createSidebarControl(container);
        GridDataFactory.fillDefaults().hint(180, SWT.DEFAULT).grab(false, true).applyTo(control);

        book = new PageBook(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(book);

        sidebar.selectDefaultView();

        new ConsistencyChecksJob(new ClientEditor(this), client, false).schedule(100);
        scheduleOnlineUpdateJobs();
    }

    private void loadClient(MPart part) throws IOException
    {
        String filename = part.getPersistedState().get(UIConstants.Parameter.FILE);

        if (filename != null)
        {
            clientFile = new File(filename);
            client = ClientFactory.load(clientFile);
        }
        else
        {
            client = new Client();
        }
    }

    @Focus
    public void setFocus()
    {
        book.setFocus();
    }

    @Persist
    public void save(MPart part, @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        if (clientFile == null)
        {
            doSaveAs(shell);
            return;
        }

        try
        {
            part.getPersistedState().put(UIConstants.Parameter.FILE, clientFile.getAbsolutePath());
            ClientFactory.save(client, clientFile);
            dirty.setDirty(false);

            storePreferences();
        }
        catch (IOException e)
        {
            ErrorDialog.openError(shell, Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    private void doSaveAs(Shell shell)
    {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);

        if (clientFile != null)
        {
            dialog.setFileName(clientFile.getName());
            dialog.setFilterPath(clientFile.getAbsolutePath());
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
            File clientFile = new File(path);
            ClientFactory.save(client, clientFile);

            dirty.setDirty(false);
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

                if (view != null)
                    view.notifyModelUpdated();
            }
        });
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
            view.init(new ClientEditor(this), parameter);
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

    private File getPreferenceStoreFile(File file) throws IOException
    {
        try
        {
            // FIXME - is the digest created identical to IPath.toOSString()
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
