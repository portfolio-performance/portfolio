package name.abuchen.portfolio.mcp;

import java.util.concurrent.Callable;

public class SyncModelExecutor implements ModelExecutor
{
    @Override
    public <T> T execute(Callable<T> task) throws Exception
    {
        return task.call();
    }
}
