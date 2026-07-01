package name.abuchen.portfolio.mcp;

import java.io.File;

import name.abuchen.portfolio.model.Client;

public record OpenFile(String id, String label, File file, Client client, boolean dirty, Runnable saveAction)
{
    public void save()
    {
        if (saveAction != null)
            saveAction.run();
    }
}
