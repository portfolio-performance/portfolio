package name.abuchen.portfolio.ui;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import name.abuchen.portfolio.model.Client;

public abstract class AbstractClientJob extends Job
{
    private static final class ClientSchedulingRule implements ISchedulingRule
    {
        private final Client client;

        public ClientSchedulingRule(Client client)
        {
            this.client = client;
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule)
        {
            return rule instanceof ClientSchedulingRule && ((ClientSchedulingRule) rule).client.equals(client);
        }

        @Override
        public boolean contains(ISchedulingRule rule)
        {
            return isConflicting(rule);
        }
    }

    private final Client client;

    public AbstractClientJob(Client client, String name)
    {
        super(name);
        this.client = client;

        setRule(new ClientSchedulingRule(client));
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
