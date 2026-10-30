package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.handlers.ActivateDiscreetModeHandler;

/* package */ class ViewElements implements ElementProvider
{
    private static class DiscreetModeElement implements Element
    {
        private Client client;
        private IEventBroker broker;

        public DiscreetModeElement(Client client, IEventBroker broker)
        {
            this.client = client;
            this.broker = broker;
        }

        @Override
        public String getTitel()
        {
            return DiscreetMode.isActive() ? Messages.MenuDeactivateDiscreetMode : Messages.MenuActivateDiscreetMode;
        }


        @Override
        public void execute()
        {
            ActivateDiscreetModeHandler.toggle(client, broker);
        }
    }

    @Inject
    private IEventBroker broker;

    @Inject
    private IEclipseContext context;

    @Override
    public List<Element> getElements()
    {
        List<Element> answer = new ArrayList<>();

        answer.add(new DiscreetModeElement(context.get(Client.class), broker));

        return answer;
    }
}
