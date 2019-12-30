package name.abuchen.portfolio.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;

/* package */ class ProgressProvider
{
    private final ClientInput clientInput;

    private volatile int totalWork;
    private volatile int worked;

    public ProgressProvider(ClientInput clientInput)
    {
        this.clientInput = clientInput;
    }

    public int getTotalWork()
    {
        return totalWork;
    }

    public int getWorked()
    {
        return worked;
    }

    public IProgressMonitor createMonitor()
    {
        return new NullProgressMonitor()
        {
            @Override
            public void beginTask(String name, int totalWork)
            {
                ProgressProvider.this.totalWork = totalWork;
            }

            @Override
            public void worked(int work)
            {
                ProgressProvider.this.worked += work;

                Display.getDefault().asyncExec(() -> clientInput.notifyListeners(l -> l.onLoading(totalWork, worked)));
            }
        };
    }

}
