package name.abuchen.portfolio.events;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;

public class ChangeEvent
{
    private final Client client;
    private final Object subject;

    public ChangeEvent(Client client, Object subject)
    {
        this.client = client;
        this.subject = subject;
    }

    public Client getClient()
    {
        return client;
    }

    public Object getSubject()
    {
        return subject;
    }

    /**
     * Returns true if the event applies for the given Client.
     */
    public boolean appliesTo(Client client)
    {
        return ReadOnlyClient.unwrap(this.client).equals(ReadOnlyClient.unwrap(client));
    }
}
