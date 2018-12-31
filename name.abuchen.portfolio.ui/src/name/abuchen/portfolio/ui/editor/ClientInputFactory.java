package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;

import name.abuchen.portfolio.model.ClientFactory;

@Creatable
@Singleton
public class ClientInputFactory
{
    private Map<File, WeakReference<ClientInput>> cache = new HashMap<>();

    @Inject
    private IEventBroker broker;

    public synchronized ClientInput lookup(File clientFile)
    {
        WeakReference<ClientInput> clientInput = cache.get(clientFile);

        if (clientInput != null)
        {
            ClientInput answer = clientInput.get();
            if (answer != null)
                return answer;
        }

        ClientInput answer = new ClientInput(clientFile);

        cache.put(clientFile, new WeakReference<>(answer));

        if (!ClientFactory.isEncrypted(clientFile))
            new LoadClientThread(answer, broker, new ProgressProvider(answer), null).start();

        return answer;
    }

}
