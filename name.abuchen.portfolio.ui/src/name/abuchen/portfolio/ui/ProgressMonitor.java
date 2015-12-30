package name.abuchen.portfolio.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

public class ProgressMonitor implements IProgressMonitor
{
    private final ProgressBar bar;

    public ProgressMonitor(ProgressBar progressIndicator)
    {
        this.bar = progressIndicator;
    }

    @Override
    public void beginTask(final String name, final int totalWork)
    {
        Display.getDefault().asyncExec(() -> {
            if (!bar.isDisposed())
            {
                bar.setMaximum(totalWork);
                bar.setToolTipText(name);
            }
        });
    }

    @Override
    public void worked(final int work)
    {
        Display.getDefault().asyncExec(() -> {
            if (!bar.isDisposed())
                bar.setSelection(bar.getSelection() + work);
        });
    }

    @Override
    public void subTask(String name)
    {
        // ignore - not supported
    }

    @Override
    public void setTaskName(String name)
    {
        // ignore - not supported
    }

    @Override
    public void setCanceled(boolean value)
    {
        // ignore - not supported
    }

    @Override
    public boolean isCanceled()
    {
        return false;
    }

    @Override
    public void internalWorked(double work)
    {
        // ignore - not supported
    }

    @Override
    public void done()
    {
        // ignore - not supported
    }
}
