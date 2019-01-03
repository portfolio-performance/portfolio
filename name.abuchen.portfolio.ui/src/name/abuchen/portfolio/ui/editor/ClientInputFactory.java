package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;

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
        Optional<WeakReference<ClientInput>> clientInput = cache.stream()
                        .filter(r -> r.get() != null && clientFile.equals(r.get().getFile())).findAny();

        if (clientInput.isPresent())
        {
            ClientInput answer = clientInput.get().get();
            if (answer != null)
                return answer;
        }

        ClientInput answer = ClientInput.createFor(clientFile, context);

        cache.add(new WeakReference<>(answer));

        if (!ClientFactory.isEncrypted(clientFile))
            new LoadClientThread(answer, broker, new ProgressProvider(answer), null).start();

        return answer;
    }

}
