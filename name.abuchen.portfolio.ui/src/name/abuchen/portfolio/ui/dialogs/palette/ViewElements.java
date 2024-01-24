package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;

import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;

/* package */ class ViewElements implements ElementProvider
{
    private static class DiscreetModeElement implements Element
    {
        private IEventBroker broker;

        public DiscreetModeElement(IEventBroker broker)
        {
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
            DiscreetMode.setActive(!DiscreetMode.isActive());
            broker.post(UIConstants.Event.Global.DISCREET_MODE, DiscreetMode.isActive());
        }
    }
    
    @Inject
    private IEventBroker broker;

    @Override
    public List<Element> getElements()
    {
        List<Element> answer = new ArrayList<>();

        answer.add(new DiscreetModeElement(broker));

        return answer;
    }
}
