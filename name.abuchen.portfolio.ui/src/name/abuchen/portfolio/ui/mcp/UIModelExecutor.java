package name.abuchen.portfolio.ui.mcp;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.mcp.MCPException;
import name.abuchen.portfolio.mcp.ModelExecutor;

public class UIModelExecutor implements ModelExecutor
{
    @Override
    public <T> T execute(Callable<T> task) throws Exception
    {
        Display display = Display.getDefault();
        if (display == null || display.isDisposed())
            throw new MCPException("UI is not available");

        if (display.getThread() == Thread.currentThread())
            return task.call();

        var result = new AtomicReference<T>();
        var error = new AtomicReference<Exception>();

        display.syncExec(() -> {
            try
            {
                result.set(task.call());
            }
            catch (Exception e)
            {
                error.set(e);
            }
        });

        if (error.get() != null)
            throw error.get();

        return result.get();
    }
}
