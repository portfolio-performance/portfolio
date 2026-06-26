package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Moves account or portfolio owners for ledger-backed cross-entry transactions.
 * This class is compatibility mutation support. Contributor code should use these
 * family-specific paths instead of legacy delete, insert, or cross-entry replay.
 */
public final class LedgerOwnerPatchHelper
{
    private final LedgerMutationContext mutationContext;

    public LedgerOwnerPatchHelper(Client client)
    {
        this(new LedgerMutationContext(client));
    }

    LedgerOwnerPatchHelper(LedgerMutationContext mutationContext)
    {
        this.mutationContext = Objects.requireNonNull(mutationContext);
    }

    public void moveAccountOnly(LedgerBackedAccountTransaction transaction, Account newAccount)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(newAccount);

        var entry = transaction.getLedgerEntry();

        if (!isAccountOnly(entry.getType()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_046.message("Unsupported account-only owner patch for " + entry.getType())); //$NON-NLS-1$

        moveAccountProjection(entry, LedgerProjectionRole.ACCOUNT, newAccount);
    }

    public void moveDelivery(LedgerBackedPortfolioTransaction transaction, Portfolio newPortfolio)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(newPortfolio);

        var entry = transaction.getLedgerEntry();

        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_047.message("Unsupported delivery owner patch for " + entry.getType())); //$NON-NLS-1$

        movePortfolioProjection(entry, transaction.getLedgerProjectionRef().getRole(), newPortfolio);
    }

    public void moveBuySellAccountSide(LedgerEntry entry, Account newAccount)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(newAccount);
        requireBuySell(entry);

        moveAccountProjection(entry, LedgerProjectionRole.ACCOUNT, newAccount);
    }

    public void moveBuySellAccountSide(BuySellEntry entry, Account newAccount)
    {
        Objects.requireNonNull(entry);
        moveBuySellAccountSide(ledgerEntry(entry.getAccountTransaction(), entry.getPortfolioTransaction()),
                        newAccount);
    }

    public void moveBuySellPortfolioSide(LedgerEntry entry, Portfolio newPortfolio)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(newPortfolio);
        requireBuySell(entry);

        movePortfolioProjection(entry, LedgerProjectionRole.PORTFOLIO, newPortfolio);
    }

    public void moveBuySellPortfolioSide(BuySellEntry entry, Portfolio newPortfolio)
    {
        Objects.requireNonNull(entry);
        moveBuySellPortfolioSide(ledgerEntry(entry.getAccountTransaction(), entry.getPortfolioTransaction()),
                        newPortfolio);
    }

    public void moveAccountTransferSource(LedgerEntry entry, Account newAccount)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(newAccount);
        requireType(entry, LedgerEntryType.CASH_TRANSFER);

        moveAccountProjection(entry, LedgerProjectionRole.SOURCE_ACCOUNT, newAccount);
    }

    public void moveAccountTransferSource(AccountTransferEntry entry, Account newAccount)
    {
        Objects.requireNonNull(entry);
        moveAccountTransferSource(ledgerEntry(entry.getSourceTransaction(), entry.getTargetTransaction()),
                        newAccount);
    }

    public void moveAccountTransferTarget(LedgerEntry entry, Account newAccount)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(newAccount);
        requireType(entry, LedgerEntryType.CASH_TRANSFER);

        moveAccountProjection(entry, LedgerProjectionRole.TARGET_ACCOUNT, newAccount);
    }

    public void moveAccountTransferTarget(AccountTransferEntry entry, Account newAccount)
    {
        Objects.requireNonNull(entry);
        moveAccountTransferTarget(ledgerEntry(entry.getSourceTransaction(), entry.getTargetTransaction()),
                        newAccount);
    }

    public void movePortfolioTransferSource(LedgerEntry entry, Portfolio newPortfolio)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(newPortfolio);
        requireType(entry, LedgerEntryType.SECURITY_TRANSFER);

        movePortfolioProjection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO, newPortfolio);
    }

    public void movePortfolioTransferSource(PortfolioTransferEntry entry, Portfolio newPortfolio)
    {
        Objects.requireNonNull(entry);
        movePortfolioTransferSource(ledgerEntry(entry.getSourceTransaction(), entry.getTargetTransaction()),
                        newPortfolio);
    }

    public void movePortfolioTransferTarget(LedgerEntry entry, Portfolio newPortfolio)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(newPortfolio);
        requireType(entry, LedgerEntryType.SECURITY_TRANSFER);

        movePortfolioProjection(entry, LedgerProjectionRole.TARGET_PORTFOLIO, newPortfolio);
    }

    public void movePortfolioTransferTarget(PortfolioTransferEntry entry, Portfolio newPortfolio)
    {
        Objects.requireNonNull(entry);
        movePortfolioTransferTarget(ledgerEntry(entry.getSourceTransaction(), entry.getTargetTransaction()),
                        newPortfolio);
    }

    private void moveAccountProjection(LedgerEntry entry, LedgerProjectionRole role, Account newAccount)
    {
        mutationContext.mutateEntry(entry, editedEntry -> {
            var projection = uniqueProjection(editedEntry, role);
            var posting = LedgerProjectionSupport.primaryPosting(editedEntry, projection);

            projection.setAccount(newAccount);
            projection.setPortfolio(null);
            posting.setAccount(newAccount);
        });
    }

    private void movePortfolioProjection(LedgerEntry entry, LedgerProjectionRole role, Portfolio newPortfolio)
    {
        mutationContext.mutateEntry(entry, editedEntry -> {
            var projection = uniqueProjection(editedEntry, role);
            var posting = LedgerProjectionSupport.primaryPosting(editedEntry, projection);

            projection.setPortfolio(newPortfolio);
            projection.setAccount(null);
            posting.setPortfolio(newPortfolio);
        });
    }

    private LedgerProjectionRef uniqueProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_042
                            .message("Expected one projection for role " + role + " but found " //$NON-NLS-1$ //$NON-NLS-2$
                                            + projections.size()));

        return projections.get(0);
    }

    private boolean isAccountOnly(LedgerEntryType type)
    {
        return switch (type)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND, DIVIDENDS -> true;
            default -> false;
        };
    }

    private void requireBuySell(LedgerEntry entry)
    {
        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_048.message("Unsupported buy/sell owner patch for " + entry.getType())); //$NON-NLS-1$
    }

    private void requireType(LedgerEntry entry, LedgerEntryType type)
    {
        if (entry.getType() != type)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_049.message("Unsupported owner patch for " + entry.getType())); //$NON-NLS-1$
    }

    private LedgerEntry ledgerEntry(Transaction... transactions)
    {
        for (var transaction : transactions)
        {
            if (transaction instanceof LedgerBackedTransaction ledgerBackedTransaction)
                return ledgerBackedTransaction.getLedgerEntry();
        }

        throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_050.message("Ledger-backed transaction not found")); //$NON-NLS-1$
    }
}
