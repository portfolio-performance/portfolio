package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.AbstractTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.selection.SelectionService;

public class NewTransactionHandler
{
    @Execute
    public void execute(MMenuItem menuItem, SelectionService selectionService)
    {
        PortfolioPart part = (PortfolioPart) menuItem.getTransientData().get(PortfolioPart.class.getName());

        @SuppressWarnings("unchecked")
        Class<? extends AbstractTransactionDialog> dialog = (Class<? extends AbstractTransactionDialog>) menuItem
                        .getTransientData().get(AbstractTransactionDialog.class.getName());

        Object transaction = menuItem.getTransientData().get(UIConstants.Parameter.NAME);

        part.getCurrentView().ifPresent(view -> {
            OpenDialogAction action = new OpenDialogAction(view, transaction.toString() + "..."); //$NON-NLS-1$
            action.type(dialog);

            if (Enum.class.isAssignableFrom(transaction.getClass()))
                action.parameters(transaction);

            selectionService.getSelection(part.getClient()).ifPresent(s -> action.with(s.getSecurity()));

            action.run();
        });
    }
}
