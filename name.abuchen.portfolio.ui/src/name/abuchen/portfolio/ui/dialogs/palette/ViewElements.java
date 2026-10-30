package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.handlers.ActivateDiscreetModeHandler;

/* package */ class ViewElements implements ElementProvider
{
    private static class DiscreetModeElement implements Element
    {
        @Override
        public String getTitel()
        {
            return DiscreetMode.isActive() ? Messages.MenuDeactivateDiscreetMode : Messages.MenuActivateDiscreetMode;
        }


        @Override
        public void execute()
        {
            ActivateDiscreetModeHandler.toggle();
        }
    }

    @Override
    public List<Element> getElements()
    {
        List<Element> answer = new ArrayList<>();

        answer.add(new DiscreetModeElement());

        return answer;
    }
}
