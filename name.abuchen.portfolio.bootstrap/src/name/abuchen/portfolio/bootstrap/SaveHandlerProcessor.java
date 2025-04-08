package name.abuchen.portfolio.bootstrap;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.ISaveHandler;

// see http://www.eclipse.org/forums/index.php/t/369989/
public class SaveHandlerProcessor
{
    private final MWindow window;
    private final IEventBroker eventBroker;

    @Inject
    public SaveHandlerProcessor(@Named("name.abuchen.portfolio.ui.window.mainwindow") MWindow window,
                    IEventBroker eventBroker)
    {
        this.window = window;
        this.eventBroker = eventBroker;
    }

    @Execute
    void installIntoContext()
    {
        eventBroker.subscribe(UIEvents.Context.TOPIC_CONTEXT, event -> {
            if (!UIEvents.isSET(event))
                return;

            if (!window.equals(event.getProperty("ChangedElement")) || window.getContext() == null) //$NON-NLS-1$
                return;

            window.getContext().runAndTrack(new RunAndTrack()
            {
                private final ISaveHandler saveHandler = new CustomSaveHandler();

                @Override
                public boolean changed(IEclipseContext context)
                {
                    Object value = context.get(ISaveHandler.class);

                    if (!saveHandler.equals(value))
                        context.set(ISaveHandler.class, saveHandler);

                    return true;
                }

            });
        });
    }
}
