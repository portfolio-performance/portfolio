package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerAccountTransferToDepositRemovalConverter.SplitResult;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.ProjectionMembership;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Splits a ledger-backed account transfer into separate deposit and removal transactions.
 * This class is part of the Ledger compatibility layer for existing action code. It keeps
 * generated transaction references mapped to the correct replacement booking.
 */
public final class LedgerAccountTransferToDepositRemovalConverter
{
    private static final BigDecimal FALLBACK_EXCHANGE_RATE = BigDecimal.ONE;

    private final Client client;

    public LedgerAccountTransferToDepositRemovalConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public SplitResult split(AccountTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        if (!(transfer.getSourceTransaction() instanceof LedgerBackedAccountTransaction sourceTransaction)
                        || !(transfer.getTargetTransaction() instanceof LedgerBackedAccountTransaction targetTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_011.message("Only ledger-backed account transfers can be split")); //$NON-NLS-1$

        var sourceEntry = sourceTransaction.getLedgerEntry();
        var targetEntry = targetTransaction.getLedgerEntry();

        if (sourceEntry != targetEntry)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_006
                            .message("Account transfer projections do not belong to the same ledger entry")); //$NON-NLS-1$

        var entry = sourceEntry;
        var sourceProjectionUUID = sourceTransaction.getLedgerProjectionRef().getUUID();
        var targetProjectionUUID = targetTransaction.getLedgerProjectionRef().getUUID();
        var sourceAccount = sourceTransaction.getLedgerProjectionRef().getAccount();
        var targetAccount = targetTransaction.getLedgerProjectionRef().getAccount();

        preflight(entry, sourceTransaction, targetTransaction);

        var sourcePosting = LedgerProjectionSupport.primaryPosting(entry, sourceTransaction.getLedgerProjectionRef());
        var targetPosting = LedgerProjectionSupport.primaryPosting(entry, targetTransaction.getLedgerProjectionRef());
        var removal = removalEntry(entry, sourceTransaction.getLedgerProjectionRef(), sourcePosting, targetPosting);
        var deposit = depositEntry(entry, targetTransaction.getLedgerProjectionRef(),
                        targetPosting);
        var executionRefUpdates = LedgerInvestmentPlanRefSupport.prepareAccountTransferSplitExecutionRefUpdates(client,
                        entry, sourceTransaction.getLedgerProjectionRef(), targetTransaction.getLedgerProjectionRef(),
                        removal, deposit);

        new LedgerMutationContext(client).splitEntry(entry, List.of(removal, deposit));
        executionRefUpdates.apply();

        return new SplitResult(find(sourceAccount, sourceProjectionUUID), find(targetAccount, targetProjectionUUID));
    }

    private void preflight(LedgerEntry entry, LedgerBackedAccountTransaction sourceTransaction,
                    LedgerBackedAccountTransaction targetTransaction)
    {
        if (entry.getType() != LedgerEntryType.CASH_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_012.message("Ledger entry is not an account transfer")); //$NON-NLS-1$

        var sourceProjection = sourceTransaction.getLedgerProjectionRef();
        var targetProjection = targetTransaction.getLedgerProjectionRef();

        if (sourceProjection.getRole() != LedgerProjectionRole.SOURCE_ACCOUNT
                        || targetProjection.getRole() != LedgerProjectionRole.TARGET_ACCOUNT)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_007
                            .message("Account transfer projection roles are malformed")); //$NON-NLS-1$

        var sourceProjectionInEntry = uniqueProjection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjectionInEntry = uniqueProjection(entry, LedgerProjectionRole.TARGET_ACCOUNT);

        if (sourceProjectionInEntry != sourceProjection || targetProjectionInEntry != targetProjection)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_008
                            .message("Selected projections do not match the ledger entry")); //$NON-NLS-1$

        var sourcePosting = LedgerProjectionSupport.primaryPosting(entry, sourceProjection);
        var targetPosting = LedgerProjectionSupport.primaryPosting(entry, targetProjection);

        if (sourcePosting == targetPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_013.message("Account transfer source and target postings are ambiguous")); //$NON-NLS-1$

        validateTransferPosting(sourcePosting, sourceProjection.getAccount());
        validateTransferPosting(targetPosting, targetProjection.getAccount());

        for (var posting : entry.getPostings())
        {
            if (posting != sourcePosting && posting != targetPosting)
                throw new UnsupportedOperationException(
                                LedgerDiagnosticCode.LEDGER_CONVERT_014.message("Ledger account transfers with unit postings cannot be split")); //$NON-NLS-1$
        }
    }

    private void validateTransferPosting(LedgerPosting posting, Account account)
    {
        if (posting.getType() != LedgerPostingType.CASH)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_015.message("Account transfer posting is not a cash posting")); //$NON-NLS-1$

        if (posting.getAccount() != account)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_009
                            .message("Account transfer posting owner does not match projection owner")); //$NON-NLS-1$

        if (posting.getSecurity() != null || posting.getShares() != 0L)
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_016.message("Ledger account transfers with security or shares cannot be split")); //$NON-NLS-1$

        if (!posting.getParameters().isEmpty())
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_017.message("Ledger account transfers with posting parameters cannot be split")); //$NON-NLS-1$
    }

    private LedgerEntry removalEntry(LedgerEntry source, LedgerProjectionRef sourceProjection,
                    LedgerPosting sourcePosting, LedgerPosting targetPosting)
    {
        var entry = baseEntry(source, source.getUUID(), LedgerEntryType.REMOVAL);
        var posting = cashPosting(sourcePosting, sourceProjection.getAccount(), targetPosting);
        var projection = accountProjection(sourceProjection, posting);

        entry.addPosting(posting);
        entry.addProjectionRef(projection);

        return entry;
    }

    private LedgerEntry depositEntry(LedgerEntry source, LedgerProjectionRef targetProjection,
                    LedgerPosting targetPosting)
    {
        var entry = baseEntry(source, null, LedgerEntryType.DEPOSIT);
        var posting = cashPosting(targetPosting, targetProjection.getAccount(), null);
        var projection = accountProjection(targetProjection, posting);

        entry.addPosting(posting);
        entry.addProjectionRef(projection);

        return entry;
    }

    private LedgerEntry baseEntry(LedgerEntry source, String uuid, LedgerEntryType type)
    {
        var entry = uuid != null ? new LedgerEntry(uuid) : new LedgerEntry();

        entry.setType(type);
        entry.setDateTime(source.getDateTime());
        entry.setNote(source.getNote());
        entry.setSource(source.getSource());
        entry.setUpdatedAt(source.getUpdatedAt());

        return entry;
    }

    private LedgerPosting cashPosting(LedgerPosting source, Account account, LedgerPosting fallbackForexPosting)
    {
        var posting = new LedgerPosting(source.getUUID());

        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setAmount(source.getAmount());
        posting.setCurrency(source.getCurrency());

        if (source.getForexAmount() != null && source.getForexCurrency() != null
                        && source.getExchangeRate() != null)
        {
            posting.setForexAmount(source.getForexAmount());
            posting.setForexCurrency(source.getForexCurrency());
            posting.setExchangeRate(source.getExchangeRate());
        }
        else if (fallbackForexPosting != null && !Objects.equals(source.getCurrency(), fallbackForexPosting.getCurrency()))
        {
            posting.setForexAmount(fallbackForexPosting.getAmount());
            posting.setForexCurrency(fallbackForexPosting.getCurrency());
            posting.setExchangeRate(FALLBACK_EXCHANGE_RATE);
        }
        else if (!Objects.equals(source.getCurrency(), account.getCurrencyCode()))
        {
            posting.setForexAmount(source.getAmount());
            posting.setForexCurrency(account.getCurrencyCode());
            posting.setExchangeRate(FALLBACK_EXCHANGE_RATE);
        }

        return posting;
    }

    private LedgerProjectionRef accountProjection(LedgerProjectionRef source, LedgerPosting posting)
    {
        var projection = new LedgerProjectionRef(source.getUUID());

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(source.getAccount());
        projection.setPrimaryPosting(posting);
        projection.setPostingGroupTargetUUID(source.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).stream()
                        .findFirst().map(ProjectionMembership::getPostingUUID).orElse(source.getPostingGroupUUID()));

        return projection;
    }

    private LedgerProjectionRef uniqueProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role)
                        .toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_010
                            .message("Ledger account transfer must have exactly one " + role + " projection")); //$NON-NLS-1$ //$NON-NLS-2$

        return projections.get(0);
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Split ledger account projection was not materialized: " + projectionUUID)); //$NON-NLS-1$
    }
}
