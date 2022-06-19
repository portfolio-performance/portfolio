package name.abuchen.portfolio.ui.dialogs.palette;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.dialogs.transactions.AbstractTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;

/* package */ class TransactionElements implements ElementProvider
{
    private class AccountTransactionElement implements Element
    {
        private final Class<? extends AbstractTransactionDialog> dialog;
        private final Object transaction;
        private final Optional<SecuritySelection> selection;

        public AccountTransactionElement(Class<? extends AbstractTransactionDialog> dialog, Object transaction,
                        Optional<SecuritySelection> selection)
        {
            this.dialog = dialog;
            this.transaction = transaction;
            this.selection = selection;
        }

        @Override
        public String getTitel()
        {
            return transaction.toString();
        }

        @Override
        public String getSubtitle()
        {
            return selection.isPresent() && hasSecurity()
                            ? MessageFormat.format(Messages.LabelNewTransactionForSecurity,
                                            selection.get().getSecurity().getName())
                            : Messages.LabelNewTransaction;
        }

        private boolean hasSecurity()
        {
            if (transaction instanceof PortfolioTransaction.Type)
                return true;

            return EnumSet.of(AccountTransaction.Type.DIVIDENDS, AccountTransaction.Type.TAXES,
                            AccountTransaction.Type.TAX_REFUND, AccountTransaction.Type.FEES,
                            AccountTransaction.Type.FEES_REFUND).contains(transaction);
        }

        @Override
        public Images getImage()
        {
            return Images.NEW_TRANSACTION;
        }

        @Override
        public void execute()
        {
            part.getCurrentView().ifPresent(view -> {
                OpenDialogAction action = new OpenDialogAction(view, transaction.toString() + "..."); //$NON-NLS-1$
                action.type(dialog);

                if (Enum.class.isAssignableFrom(transaction.getClass()))
                    action.parameters(transaction);

                if (selection.isPresent())
                    action.with(selection.get().getSecurity());

                action.run();
            });
        }
    }

    @Inject
    private PortfolioPart part;

    @Inject
    private SelectionService selectionService;

    @Override
    public List<Element> getElements()
    {
        Optional<SecuritySelection> selection = selectionService.getSelection(part.getClient());

        List<Element> elements = new ArrayList<>();

        for (final AccountTransaction.Type type : EnumSet.of( //
                        AccountTransaction.Type.DEPOSIT, //
                        AccountTransaction.Type.REMOVAL, //
                        AccountTransaction.Type.TAXES, //
                        AccountTransaction.Type.TAX_REFUND, //
                        AccountTransaction.Type.FEES, //
                        AccountTransaction.Type.FEES_REFUND, //
                        AccountTransaction.Type.INTEREST, //
                        AccountTransaction.Type.INTEREST_CHARGE, //
                        AccountTransaction.Type.DIVIDENDS))
        {
            elements.add(new AccountTransactionElement(AccountTransactionDialog.class, type, selection));
        }

        for (final PortfolioTransaction.Type type : EnumSet.of( //
                        PortfolioTransaction.Type.BUY, //
                        PortfolioTransaction.Type.SELL, //
                        PortfolioTransaction.Type.DELIVERY_INBOUND, //
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND))
        {
            elements.add(new AccountTransactionElement(SecurityTransactionDialog.class, type, selection));
        }

        if (part.getClient().getActivePortfolios().size() > 1)
        {
            elements.add(new AccountTransactionElement(SecurityTransferDialog.class, Messages.LabelSecurityTransfer,
                            selection));
        }

        if (part.getClient().getActiveAccounts().size() > 1)
        {
            elements.add(new AccountTransactionElement(AccountTransferDialog.class, Messages.LabelAccountTransfer,
                            selection));
        }

        return elements;
    }
}
