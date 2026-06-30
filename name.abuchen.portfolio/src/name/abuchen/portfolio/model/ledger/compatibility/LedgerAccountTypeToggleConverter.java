package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Toggles ledger-backed account-only transactions between matching account transaction types.
 * This class is part of the Ledger compatibility layer for existing UI and action code. It
 * updates the Ledger entry instead of changing the projected legacy transaction directly.
 */
public final class LedgerAccountTypeToggleConverter
{
    private final Client client;

    public LedgerAccountTypeToggleConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public AccountTransaction toggle(TransactionPair<AccountTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedAccountTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_019.message("Only ledger-backed account transactions can be toggled")); //$NON-NLS-1$

        var entry = ledgerTransaction.getLedgerEntry();
        var projectionRef = ledgerTransaction.getLedgerProjectionRef();
        var account = projectionRef.getAccount();
        var projectionUUID = projectionRef.getUUID();

        preflight(entry, projectionRef, transaction, account);
        LedgerInvestmentPlanRefSupport.requireCurrentRefsResolveUniquely(client, entry);

        new LedgerMutationContext(client).mutateEntry(entry, this::toggle);

        return find(account, projectionUUID);
    }

    private void preflight(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<AccountTransaction> transaction, Account account)
    {
        if (targetType(entry.getType()) == null)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_011.message("Only ledger-backed deposit/removal and interest entries can be toggled")); //$NON-NLS-1$

        if (projectionRef.getRole() != LedgerProjectionRole.ACCOUNT)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_012.message("Only account projections can be toggled")); //$NON-NLS-1$

        if (transaction.getOwner() != account)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_013.message("Selected account does not own the ledger projection")); //$NON-NLS-1$

        var projection = requireOneProjection(entry);
        var primaryPosting = requirePrimaryPosting(entry, projection);

        if (projection != projectionRef)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_014.message("Selected projection is not the unique account projection")); //$NON-NLS-1$

        if (primaryPosting.getAccount() != projection.getAccount())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_015.message("Account projection and posting account do not match")); //$NON-NLS-1$
    }

    private void toggle(LedgerEntry entry)
    {
        entry.setType(targetType(entry.getType()));
    }

    private LedgerEntryType targetType(LedgerEntryType currentType)
    {
        if (currentType == null)
            return null;

        return switch (currentType)
        {
            case DEPOSIT -> LedgerEntryType.REMOVAL;
            case REMOVAL -> LedgerEntryType.DEPOSIT;
            case INTEREST -> LedgerEntryType.INTEREST_CHARGE;
            case INTEREST_CHARGE -> LedgerEntryType.INTEREST;
            case FEES -> LedgerEntryType.FEES_REFUND;
            case FEES_REFUND -> LedgerEntryType.FEES;
            case TAXES -> LedgerEntryType.TAX_REFUND;
            case TAX_REFUND -> LedgerEntryType.TAXES;
            default -> null;
        };
    }

    private LedgerPosting requirePrimaryPosting(LedgerEntry entry, LedgerProjectionRef projection)
    {
        var primaryPosting = LedgerProjectionSupport.primaryPosting(entry, projection);

        if (primaryPosting == null)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_016.message("Account projection primary posting is ambiguous")); //$NON-NLS-1$

        var expectedPostingType = postingType(entry.getType());

        if (primaryPosting.getType() != expectedPostingType)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_017.message("Account projection primary posting has unexpected type")); //$NON-NLS-1$

        return primaryPosting;
    }

    private LedgerPostingType postingType(LedgerEntryType entryType)
    {
        return switch (entryType)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE -> LedgerPostingType.CASH;
            case FEES, FEES_REFUND -> LedgerPostingType.FEE;
            case TAXES, TAX_REFUND -> LedgerPostingType.TAX;
            default -> null;
        };
    }

    private LedgerProjectionRef requireOneProjection(LedgerEntry entry)
    {
        var projections = entry.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_018.message("Ledger account-only entry must have exactly one account projection")); //$NON-NLS-1$

        return projections.get(0);
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Toggled ledger account projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }
}
