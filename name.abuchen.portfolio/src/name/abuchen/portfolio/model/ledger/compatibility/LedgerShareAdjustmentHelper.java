package name.abuchen.portfolio.model.ledger.compatibility;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongUnaryOperator;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerModelCopy;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Applies share adjustments to ledger-backed security postings.
 * This class is compatibility mutation support for stock split and similar write paths.
 * It updates Ledger truth instead of projected legacy rows.
 */
public final class LedgerShareAdjustmentHelper
{
    private LedgerShareAdjustmentHelper()
    {
    }

    public static Plan plan(Client client, Security security, List<Transaction> transactions,
                    LongUnaryOperator shareAdjustment)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(security);
        Objects.requireNonNull(transactions);
        Objects.requireNonNull(shareAdjustment);

        var adjustments = new LinkedHashMap<String, Adjustment>();

        for (var transaction : transactions)
        {
            if (!(transaction instanceof LedgerBackedTransaction ledgerBackedTransaction))
                continue;

            var posting = primaryPosting(ledgerBackedTransaction);

            if (posting.getSecurity() != security)
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_060.message("Selected Ledger posting does not match stock split security")); //$NON-NLS-1$

            adjustments.computeIfAbsent(posting.getUUID(), uuid -> new Adjustment(ledgerBackedTransaction.getLedgerEntry(),
                            posting, shareAdjustment.applyAsLong(posting.getShares())));
        }

        var plan = new Plan(client, List.copyOf(adjustments.values()));
        plan.validate();
        return plan;
    }

    public static Plan emptyPlan()
    {
        return new Plan(null, List.of());
    }

    private static LedgerPosting primaryPosting(LedgerBackedTransaction transaction)
    {
        var entry = transaction.getLedgerEntry();
        var projectionRef = transaction.getLedgerProjectionRef();

        if (projectionRef.getPrimaryPostingUUID() != null)
            return requirePostingInEntry(entry, projectionRef.getPrimaryPostingUUID());

        if (entry.getType().requiresTargetedProjectionRefs())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_058
                            .message("Targeted Ledger projection has no primary posting: " + projectionRef.getUUID())); //$NON-NLS-1$

        Optional<LedgerPosting> posting = switch (projectionRef.getRole())
        {
            case SOURCE_ACCOUNT -> accountPostings(entry, projectionRef).findFirst();
            case TARGET_ACCOUNT -> last(accountPostings(entry, projectionRef));
            case SOURCE_PORTFOLIO -> portfolioPostings(entry, projectionRef).findFirst();
            case TARGET_PORTFOLIO -> last(portfolioPostings(entry, projectionRef));
            case ACCOUNT, CASH_COMPENSATION -> accountPostings(entry, projectionRef).findFirst();
            case PORTFOLIO, DELIVERY, DELIVERY_INBOUND, DELIVERY_OUTBOUND, OLD_SECURITY_LEG, NEW_SECURITY_LEG ->
                portfolioPostings(entry, projectionRef).findFirst();
        };

        return posting.orElseThrow(() -> new IllegalArgumentException(
                        "No primary Ledger posting for projection " + projectionRef.getUUID())); //$NON-NLS-1$
    }

    private static Stream<LedgerPosting> accountPostings(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return entry.getPostings().stream().filter(posting -> posting.getAccount() == projectionRef.getAccount());
    }

    private static Stream<LedgerPosting> portfolioPostings(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return entry.getPostings().stream().filter(posting -> posting.getPortfolio() == projectionRef.getPortfolio());
    }

    private static Optional<LedgerPosting> last(Stream<LedgerPosting> stream)
    {
        var postings = stream.toList();
        return postings.isEmpty() ? Optional.empty() : Optional.of(postings.get(postings.size() - 1));
    }

    private static LedgerEntry requireEntryInLedger(Ledger ledger, String uuid)
    {
        return ledger.getEntries().stream() //
                        .filter(entry -> entry.getUUID().equals(uuid)) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Ledger entry not found: " + uuid)); //$NON-NLS-1$
    }

    private static LedgerPosting requirePostingInEntry(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getUUID().equals(uuid)) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Ledger posting not found in entry " + entry.getUUID() + ": " + uuid)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private record Adjustment(LedgerEntry entry, LedgerPosting posting, long newShares)
    {
    }

    public static final class Plan
    {
        private final Client client;
        private final List<Adjustment> adjustments;

        private Plan(Client client, List<Adjustment> adjustments)
        {
            this.client = client;
            this.adjustments = adjustments;
        }

        public boolean isLedgerBacked(Transaction transaction)
        {
            return transaction instanceof LedgerBackedTransaction;
        }

        public void apply()
        {
            if (adjustments.isEmpty())
                return;

            var affectedEntryUUIDs = adjustments.stream() //
                            .map(adjustment -> adjustment.entry().getUUID()) //
                            .distinct() //
                            .toList();

            new LedgerMutationContext(client).mutateEntries(affectedEntryUUIDs, ledger -> {
                for (var adjustment : adjustments)
                {
                    var entry = requireEntryInLedger(ledger, adjustment.entry().getUUID());

                    requirePostingInEntry(entry, adjustment.posting().getUUID()).setShares(adjustment.newShares());
                    entry.setUpdatedAt(Instant.now());
                }
            });
        }

        private void validate()
        {
            if (adjustments.isEmpty())
                return;

            var candidate = LedgerModelCopy.copyLedger(client.getLedger());

            for (var adjustment : adjustments)
            {
                var entry = requireEntryInLedger(candidate, adjustment.entry().getUUID());

                requirePostingInEntry(entry, adjustment.posting().getUUID()).setShares(adjustment.newShares());
            }

            var result = LedgerStructuralValidator.validate(candidate);

            if (!result.isOK())
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_061.message("Invalid Ledger share adjustment: " + result.format())); //$NON-NLS-1$
        }
    }
}
