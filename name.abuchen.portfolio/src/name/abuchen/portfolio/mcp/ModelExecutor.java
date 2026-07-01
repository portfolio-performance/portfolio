package name.abuchen.portfolio.mcp;

import java.util.concurrent.Callable;

public interface ModelExecutor
{
    <T> T execute(Callable<T> task) throws Exception;
}
