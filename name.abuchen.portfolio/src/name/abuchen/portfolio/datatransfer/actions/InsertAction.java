package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

public class InsertAction implements ImportAction
{
    private final Client client;
    private boolean convertBuySellToDelivery = false;
    private boolean removeDividends = false;
    private boolean investmentPlanItem = false;

    public InsertAction(Client client)
    {
        this.client = client;
    }

    public void setConvertBuySellToDelivery(boolean flag)
    {
        this.convertBuySellToDelivery = flag;
    }

    public void setRemoveDividends(boolean flag)
    {
        this.removeDividends = flag;
    }

    public void setInvestmentPlanItem(boolean flag)
    {
        this.investmentPlanItem = flag;
    }

    @Override
    public Status process(Security security)
    {
        // might have been added via a transaction
        if (!client.getSecurities().contains(security))
            client.addSecurity(security);
        return Status.OK_STATUS;
    }

    @Override
    public Status process(Security security, SecurityPrice price)
    {
        security.addPrice(price);
        return Status.OK_STATUS;
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        if (transaction.getSecurity() != null)
            process(transaction.getSecurity());

        if (transaction.getType() == AccountTransaction.Type.DIVIDENDS)
        {
            new LedgerDividendTransactionCreator(client).create(account, transaction.getDateTime(),
                            transaction.getAmount(), transaction.getCurrencyCode(), transaction.getSecurity(),
                            transaction.getShares(), transaction.getExDate(), null, null, transaction.getUnits().toList(),
                            transaction.getNote(), transaction.getSource());
        }
        else
        {
            new LedgerAccountOnlyTransactionCreator(client).create(account, transaction.getType(),
                            transaction.getDateTime(), transaction.getAmount(), transaction.getCurrencyCode(),
                            transaction.getSecurity(), transaction.getUnits().toList(), transaction.getNote(),
                            transaction.getSource());
        }

        if (removeDividends && transaction.getType() == AccountTransaction.Type.DIVIDENDS)
        {
            new LedgerAccountOnlyTransactionCreator(client).create(account, AccountTransaction.Type.REMOVAL,
                            transaction.getDateTime(), transaction.getAmount(), transaction.getCurrencyCode(), null,
                            List.of(), transaction.getNote(), transaction.getSource());
        }

        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        process(transaction.getSecurity());

        new LedgerDeliveryTransactionCreator(client).create(portfolio, transaction.getType(), transaction.getDateTime(),
                        transaction.getAmount(), transaction.getCurrencyCode(), transaction.getSecurity(),
                        transaction.getShares(), null, null, transaction.getUnits().toList(), transaction.getNote(),
                        transaction.getSource());
        return Status.OK_STATUS;
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        process(entry.getPortfolioTransaction().getSecurity());

        // when importing transactions that have already been generated by an
        // investment plan, find the existing item and update it
        if (investmentPlanItem)
        {
            DetectDuplicatesAction action = new DetectDuplicatesAction(client);
            List<Transaction> matchingInvestmentPlanTransactions = new ArrayList<>();
            PortfolioTransaction t = entry.getPortfolioTransaction();

            // search for a match in existing investment plan transactions
            List<InvestmentPlan> plans = client.getPlans();
            Iterator<InvestmentPlan> i = plans.stream()
                            .filter(p -> p.getSecurity() != null && p.getSecurity().equals(t.getSecurity())).iterator();

            while (i.hasNext())
            {
                List<Transaction> transactions = i.next().getTransactions(client).stream() //
                                .map(pair -> (Transaction) pair.getTransaction()).toList();
                matchingInvestmentPlanTransactions.addAll(action.findInvestmentPlanTransactions(t, transactions));
            }

            if (matchingInvestmentPlanTransactions.size() > 1)
                return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);

            if (matchingInvestmentPlanTransactions.size() == 1)
            {
                Transaction existingTransaction = matchingInvestmentPlanTransactions.get(0);
                if (existingTransaction instanceof LedgerBackedTransaction)
                {
                    updateLedgerBackedInvestmentPlanTransaction(existingTransaction, entry);
                    return Status.OK_STATUS;
                }

                existingTransaction.setDateTime(t.getDateTime());
                existingTransaction.setNote(t.getNote());
                existingTransaction.setSource(t.getSource());
                existingTransaction.setShares(t.getShares());
                existingTransaction.setAmount(t.getAmount());
                existingTransaction.clearUnits();
                t.getUnits().forEach(existingTransaction::addUnit);

                if (existingTransaction.getCrossEntry() != null)
                {
                    Transaction crossTransaction = existingTransaction.getCrossEntry()
                                    .getCrossTransaction(existingTransaction);
                    crossTransaction.setDateTime(t.getDateTime());
                    crossTransaction.setAmount(t.getAmount());
                    crossTransaction.setNote(t.getNote());
                    crossTransaction.setSource(t.getSource());
                }
                return Status.OK_STATUS;
            }
        }

        if (convertBuySellToDelivery)
        {
            PortfolioTransaction t = entry.getPortfolioTransaction();

            new LedgerDeliveryTransactionCreator(client).create(portfolio,
                            t.getType() == PortfolioTransaction.Type.BUY ? PortfolioTransaction.Type.DELIVERY_INBOUND
                                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                            t.getDateTime(), t.getAmount(), t.getCurrencyCode(), t.getSecurity(), t.getShares(), null,
                            null, t.getUnits().toList(), t.getNote(), t.getSource());
        }
        else
        {
            PortfolioTransaction t = entry.getPortfolioTransaction();
            new LedgerBuySellTransactionCreator(client).create(portfolio, account, t.getType(), t.getDateTime(),
                            t.getAmount(), t.getCurrencyCode(), t.getSecurity(), t.getShares(), t.getUnits().toList(),
                            t.getNote(), t.getSource());
        }

        return Status.OK_STATUS;
    }

    private void updateLedgerBackedInvestmentPlanTransaction(Transaction existingTransaction, BuySellEntry importedEntry)
    {
        if (!(existingTransaction instanceof PortfolioTransaction existingPortfolioTransaction))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_003.message(
                                            Messages.LedgerInsertActionInvestmentPlanLegacySettersNotSupported));

        PortfolioTransaction importedPortfolioTransaction = importedEntry.getPortfolioTransaction();

        switch (existingPortfolioTransaction.getType())
        {
            case BUY:
                updateGeneratedBuy(existingPortfolioTransaction, importedPortfolioTransaction);
                break;
            case DELIVERY_INBOUND:
                updateGeneratedInboundDelivery(existingPortfolioTransaction, importedPortfolioTransaction);
                break;
            case SELL, DELIVERY_OUTBOUND, TRANSFER_IN, TRANSFER_OUT:
                throw new UnsupportedOperationException(
                                LedgerDiagnosticCode.LEDGER_UI_004.message(MessageFormat.format(
                                                Messages.LedgerInsertActionUnsupportedInvestmentPlanTransactionUpdateType,
                                                existingPortfolioTransaction.getType())));
            default:
                throw new UnsupportedOperationException(
                                LedgerDiagnosticCode.LEDGER_UI_005.message(MessageFormat.format(
                                                Messages.LedgerInsertActionUnsupportedInvestmentPlanTransactionUpdateType,
                                                existingPortfolioTransaction.getType())));
        }
    }

    private void updateGeneratedBuy(PortfolioTransaction existingPortfolioTransaction,
                    PortfolioTransaction importedPortfolioTransaction)
    {
        if (importedPortfolioTransaction.getType() != PortfolioTransaction.Type.BUY)
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_006.message(
                                            Messages.LedgerInsertActionGeneratedBuyTypeMismatch));

        if (!(existingPortfolioTransaction.getCrossEntry() instanceof BuySellEntry existingEntry))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_007
                                            .message(Messages.LedgerInsertActionGeneratedBuyMissingBuySellEntry));

        new LedgerBuySellTransactionCreator(client).update(existingEntry, existingEntry.getPortfolio(),
                        existingEntry.getAccount(), PortfolioTransaction.Type.BUY,
                        importedPortfolioTransaction.getDateTime(), importedPortfolioTransaction.getAmount(),
                        importedPortfolioTransaction.getCurrencyCode(), importedPortfolioTransaction.getSecurity(),
                        importedPortfolioTransaction.getShares(), importedPortfolioTransaction.getUnits().toList(),
                        importedPortfolioTransaction.getNote(), importedPortfolioTransaction.getSource());
    }

    private void updateGeneratedInboundDelivery(PortfolioTransaction existingPortfolioTransaction,
                    PortfolioTransaction importedPortfolioTransaction)
    {
        if (importedPortfolioTransaction.getType() != PortfolioTransaction.Type.BUY)
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_008.message(
                                            Messages.LedgerInsertActionGeneratedDeliveryTypeMismatch));

        new LedgerDeliveryTransactionCreator(client).update(existingPortfolioTransaction,
                        ownerOf(existingPortfolioTransaction), PortfolioTransaction.Type.DELIVERY_INBOUND,
                        importedPortfolioTransaction.getDateTime(), importedPortfolioTransaction.getAmount(),
                        importedPortfolioTransaction.getCurrencyCode(), importedPortfolioTransaction.getSecurity(),
                        importedPortfolioTransaction.getShares(), null, null,
                        importedPortfolioTransaction.getUnits().toList(), importedPortfolioTransaction.getNote(),
                        importedPortfolioTransaction.getSource());
    }

    private Portfolio ownerOf(PortfolioTransaction transaction)
    {
        return client.getPortfolios().stream().filter(portfolio -> portfolio.getTransactions().contains(transaction))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        Messages.LedgerInsertActionGeneratedDeliveryOwnerNotFound));
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        var sourceTransaction = entry.getSourceTransaction();
        var targetTransaction = entry.getTargetTransaction();
        var sourceForex = sourceTransaction.getUnit(Transaction.Unit.Type.GROSS_VALUE);

        new LedgerAccountTransferTransactionCreator(client).create(source, target, sourceTransaction.getDateTime(),
                        sourceTransaction.getAmount(), sourceTransaction.getCurrencyCode(), targetTransaction.getAmount(),
                        targetTransaction.getCurrencyCode(), sourceForex.map(Transaction.Unit::getForex).orElse(null),
                        sourceForex.map(Transaction.Unit::getExchangeRate).orElse(null), sourceTransaction.getNote(),
                        sourceTransaction.getSource());

        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        process(entry.getSourceTransaction().getSecurity());

        var sourceTransaction = entry.getSourceTransaction();

        new LedgerPortfolioTransferTransactionCreator(client).create(source, target, sourceTransaction.getSecurity(),
                        sourceTransaction.getDateTime(), sourceTransaction.getShares(), sourceTransaction.getAmount(),
                        sourceTransaction.getCurrencyCode(), sourceTransaction.getNote(), sourceTransaction.getSource());

        return Status.OK_STATUS;
    }
}
