package name.abuchen.portfolio.ui.views.actions;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.LedgerAccountTransferToDepositRemovalConverter;
import name.abuchen.portfolio.model.LedgerAccountTypeToggleConverter;
import name.abuchen.portfolio.model.LedgerBuySellDeliveryConverter;
import name.abuchen.portfolio.model.LedgerBuySellReversalConverter;
import name.abuchen.portfolio.model.LedgerDeliveryDirectionConverter;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.LedgerPortfolioCompositeTypeConverter;
import name.abuchen.portfolio.model.LedgerTransferDirectionConverter;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.ui.Messages;

final class LedgerConversionSupport
{
    private LedgerConversionSupport()
    {
    }

    static boolean convertBuySellToDelivery(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        var converter = new LedgerBuySellDeliveryConverter(client);
        if (!converter.canConvert(transaction))
            return false;

        converter.convertBuySellToDelivery(transaction);
        return true;
    }

    static boolean convertDeliveryToBuySell(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        var converter = new LedgerBuySellDeliveryConverter(client);
        if (!converter.canConvertDeliveryToBuySell(transaction))
            return false;

        converter.convertDeliveryToBuySell(transaction);
        return true;
    }

    static boolean convertPortfolioCompositeType(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        var converter = new LedgerPortfolioCompositeTypeConverter(client);

        if (converter.canConvertSafely(transaction))
        {
            converter.convert(transaction);
            client.markDirty();
            return true;
        }

        if (converter.isLedgerBacked(transaction))
            throw new UnsupportedOperationException(
                            Messages.LedgerConvertPortfolioCompositeTypeActionUnsupportedLedgerBackedTransition);

        return false;
    }

    static Set<AccountTransferEntry> convertLedgerTransfersToDepositRemoval(Client client,
                    Collection<AccountTransaction> transactionList)
    {
        var ledgerTransferCreator = new LedgerAccountTransferTransactionCreator(client);
        var ledgerSplitConverter = new LedgerAccountTransferToDepositRemovalConverter(client);
        var ledgerEntries = Collections.newSetFromMap(new IdentityHashMap<AccountTransferEntry, Boolean>());

        for (AccountTransaction transaction : transactionList)
        {
            if (!(transaction.getCrossEntry() instanceof AccountTransferEntry entry))
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_UI_018
                                .message(MessageFormat.format(
                                                Messages.LedgerConvertTransferToDepositRemovalActionUnsupportedTransferEntry,
                                                transaction)));

            if (ledgerTransferCreator.isLedgerBacked(entry))
            {
                if (!ledgerSplitConverter.canSplit(entry))
                    throw new UnsupportedOperationException(
                                    LedgerDiagnosticCode.LEDGER_UI_019
                                                    .message(Messages.LedgerConvertTransferToDepositRemovalActionCannotConvertLedgerBackedTransfer));

                ledgerEntries.add(entry);
            }
        }

        ledgerEntries.forEach(ledgerSplitConverter::split);

        return Set.copyOf(ledgerEntries);
    }

    static boolean reverseBuySell(Client client, BuySellEntry entry)
    {
        var converter = new LedgerBuySellReversalConverter(client);
        if (!converter.canReverse(entry))
            return false;

        converter.reverse(entry);
        client.markDirty();
        return true;
    }

    static boolean reverseDelivery(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        var converter = new LedgerDeliveryDirectionConverter(client);
        if (!converter.canReverse(transaction))
            return false;

        converter.reverse(transaction);
        client.markDirty();
        return true;
    }

    static boolean reverseTransfer(Client client, CrossEntry entry)
    {
        var converter = new LedgerTransferDirectionConverter(client);

        if (entry instanceof PortfolioTransferEntry portfolioTransfer)
        {
            if (!converter.canReverse(portfolioTransfer))
                return false;

            converter.reverse(portfolioTransfer);
            client.markDirty();
            return true;
        }

        if (entry instanceof AccountTransferEntry accountTransfer)
        {
            if (!converter.canReverse(accountTransfer))
                return false;

            converter.reverse(accountTransfer);
            client.markDirty();
            return true;
        }

        return false;
    }

    static boolean toggleAccountType(Client client, TransactionPair<AccountTransaction> transaction)
    {
        var converter = new LedgerAccountTypeToggleConverter(client);
        if (!converter.canToggle(transaction))
            return false;

        converter.toggle(transaction);
        client.markDirty();
        return true;
    }
}
