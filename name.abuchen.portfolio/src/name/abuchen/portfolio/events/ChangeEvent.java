package name.abuchen.portfolio.events;

import name.abuchen.portfolio.model.Client;

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
        return this.client.equals(client);
    }
}
