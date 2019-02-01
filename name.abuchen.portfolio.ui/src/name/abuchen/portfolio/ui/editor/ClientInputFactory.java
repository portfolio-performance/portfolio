package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

@Creatable
@Singleton
public class ClientInputFactory
{
    private Set<WeakReference<ClientInput>> cache = new HashSet<>();

    @Inject
    private IEclipseContext context;

    @Inject
    private IEventBroker broker;

    public synchronized ClientInput lookup(File clientFile)
    {
        ClientInput clientInput = null;

        for (WeakReference<ClientInput> ref : cache)
        {
            ClientInput candidate = ref.get();
            if (candidate == null)
                continue;
            if (clientFile.equals(candidate.getFile()))
                clientInput = candidate;
        }

        if (clientInput != null)
            return clientInput;

        ClientInput answer = new ClientInput(clientFile.getName(), clientFile);
        ContextInjectionFactory.inject(answer, context);

        cache.add(new WeakReference<>(answer));

        if (!ClientFactory.isEncrypted(clientFile))
            new LoadClientThread(answer, broker, new ProgressProvider(answer), null).start();

        return answer;
    }

    public synchronized ClientInput create(String label, Client client)
    {
        ClientInput answer = new ClientInput(label, null);
        ContextInjectionFactory.inject(answer, context);
        answer.setClient(client);
        answer.touch();

        cache.add(new WeakReference<>(answer));

        return answer;
    }
}
