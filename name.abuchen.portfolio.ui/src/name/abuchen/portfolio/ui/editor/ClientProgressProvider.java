package name.abuchen.portfolio.ui.editor;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ProgressMonitorFactory;

public class ClientProgressProvider extends ProgressProvider
{
    private class MonitorImpl implements IProgressMonitor
    {
        private boolean isCanceled = false;

        @Override
        public void beginTask(final String name, int totalWork)
        {
            internalSetText(name);
        }

        @Override
        public void done()
        {
            internalSetText(""); //$NON-NLS-1$
        }

        @Override
        public void internalWorked(double work)
        {}

        @Override
        public boolean isCanceled()
        {
            return isCanceled;
        }

        @Override
        public void setCanceled(boolean value)
        {
            this.isCanceled = value;
        }

        @Override
        public void setTaskName(String name)
        {
            internalSetText(name);
        }

        @Override
        public void subTask(final String name)
        {
            internalSetText(name);
        }

        @Override
        public void worked(int work)
        {}

        private void internalSetText(final String text)
        {
            sync.asyncExec(() -> {
                if (!label.isDisposed())
                    label.setText(text);
            });
        }
    }

    @Inject
    private Client client;

    @Inject
    private ProgressMonitorFactory factory;

    @Inject
    private UISynchronize sync;

    private Label label;

    @PostConstruct
    public void setup()
    {
        factory.addProgressProvider(this);
    }

    protected void disposed()
    {
        factory.removeProgressProvider(this);
    }

    @PostConstruct
    public void createComposite(Composite parent)
    {
        label = new Label(parent, SWT.LEFT);
        label.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        label.setBackground(Colors.getColor(249, 250, 250));
        label.setText(""); //$NON-NLS-1$

        parent.addDisposeListener(e -> disposed());
    }

    @Override
    public IProgressMonitor createMonitor(Job job)
    {
        if (job.belongsTo(client))
        {
            final MonitorImpl monitor = new MonitorImpl();
            job.addJobChangeListener(new JobChangeAdapter()
            {
                @Override
                public void done(IJobChangeEvent event)
                {
                    monitor.done();
                }
            });
            return monitor;
        }
        else
        {
            return null;
        }
    }

    public Control getControl()
    {
        return label;
    }
}
