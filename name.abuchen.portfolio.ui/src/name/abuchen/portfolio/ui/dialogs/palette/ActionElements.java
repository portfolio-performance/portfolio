package name.abuchen.portfolio.ui.dialogs.palette;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.handlers.UpdateQuotesHandler;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.CommandAction;

/* package */ class ActionElements implements ElementProvider
{
    private class UpdatePricesElement implements Element
    {
        @Override
        public String getTitel()
        {
            return Messages.JobLabelUpdateQuotes;
        }

        @Override
        public void execute()
        {
            // execute asynchronously to allow the command palette to close
            Display.getDefault().asyncExec(() -> CommandAction
                            .forCommand(context, getTitel(), UIConstants.Command.UPDATE_QUOTES).run());
        }
    }

    private class UpdatePricesForSelectedInstrumentsElement implements Element
    {
        private final int count;

        public UpdatePricesForSelectedInstrumentsElement(int count)
        {
            this.count = count;
        }

        @Override
        public String getTitel()
        {
            return MessageFormat.format(Messages.MenuUpdatePricesForSelectedInstruments, count);
        }

        @Override
        public void execute()
        {
            // execute asynchronously to allow the command palette to close
            Display.getDefault()
                            .asyncExec(() -> CommandAction.forCommand(context, getTitel(),
                                            UIConstants.Command.UPDATE_QUOTES, UIConstants.Parameter.FILTER,
                                            UpdateQuotesHandler.FilterType.SECURITY.name()).run());
        }
    }

    @Inject
    private PortfolioPart part;

    @Inject
    private SelectionService selectionService;

    @Inject
    private IEclipseContext context;

    @Override
    public List<Element> getElements()
    {
        List<Element> answer = new ArrayList<>();

        answer.add(new UpdatePricesElement());

        var selection = selectionService.getSelection(part.getClient());
        if (selection.isPresent())
            answer.add(new UpdatePricesForSelectedInstrumentsElement(selection.get().getSecurities().size()));

        return answer;
    }
}
