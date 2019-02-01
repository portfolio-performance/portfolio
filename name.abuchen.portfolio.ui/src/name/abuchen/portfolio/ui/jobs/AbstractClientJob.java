package name.abuchen.portfolio.ui.jobs;

import org.eclipse.core.runtime.jobs.Job;

import name.abuchen.portfolio.model.Client;

public abstract class AbstractClientJob extends Job
{
    private final Client client;

    public AbstractClientJob(Client client, String name)
    {
        super(name);
        this.client = client;
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return client.equals(family);
    }

    public Client getClient()
    {
        return client;
    }
}
