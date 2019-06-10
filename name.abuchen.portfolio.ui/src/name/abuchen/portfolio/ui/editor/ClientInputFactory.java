package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.selection.SelectionService;

@Creatable
@Singleton
public class ClientInputFactory
{
    private Map<ClientInput, AtomicInteger> cache = new HashMap<>();

    @Inject
    private IEclipseContext context;

    @Inject
    private IEventBroker broker;

    @Inject
    private SelectionService selectionService;

    public synchronized ClientInput lookup(File clientFile)
    {
        Optional<ClientInput> input = cache.keySet().stream().filter(i -> clientFile.equals(i.getFile())).findAny();

        if (input.isPresent())
            return input.get();

        ClientInput answer = new ClientInput(clientFile.getName(), clientFile);
        ContextInjectionFactory.inject(answer, context);

        cache.put(answer, new AtomicInteger());

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

        cache.put(answer, new AtomicInteger());

        return answer;
    }

    public synchronized void incrementEditorCount(ClientInput clientInput)
    {
        cache.computeIfAbsent(clientInput, i -> new AtomicInteger()).incrementAndGet();
    }

    public synchronized void decrementEditorCount(ClientInput clientInput)
    {
        int newCount = cache.computeIfAbsent(clientInput, i -> new AtomicInteger()).decrementAndGet();

        if (newCount <= 0)
        {
            cache.remove(clientInput);

            selectionService.getSelection(clientInput.getClient()).ifPresent(s -> selectionService.setSelection(null));
        }
    }
}
