package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Semantic edit helpers for native Corporate Action Ledger entries.
 */
public final class LedgerCorporateActionEditSupport
{
    private LedgerCorporateActionEditSupport()
    {
    }

    public static Optional<LedgerPosting> findPostingBySemanticKey(LedgerEntry entry, LedgerLegRole role,
                    String localKey)
    {
        return findPostingBySemanticKey(entry, role, localKey, null);
    }

    public static Optional<LedgerPosting> findPostingBySemanticKey(LedgerEntry entry, LedgerLegRole role,
                    String localKey, String groupKey)
    {
        var matches = matchingPostings(entry, role, localKey, groupKey);

        if (matches.size() > 1)
            throw new IllegalArgumentException("Corporate Action semantic posting key is ambiguous: " + role //$NON-NLS-1$
                            + "/" + localKey); //$NON-NLS-1$

        return matches.stream().findFirst();
    }

    public static LedgerPosting postingBySemanticKey(LedgerEntry entry, LedgerLegRole role, String localKey,
                    String groupKey)
    {
        return findPostingBySemanticKey(entry, role, localKey, groupKey)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Corporate Action semantic posting was not found: " + role + "/" //$NON-NLS-1$ //$NON-NLS-2$
                                                        + localKey));
    }

    public static void mutatePosting(Client client, LedgerEntry entry, LedgerLegRole role, String localKey,
                    String groupKey, Consumer<LedgerPosting> mutation)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(mutation);

        new LedgerMutationContext(client).mutateEntry(entry, liveEntry -> {
            mutation.accept(postingBySemanticKey(liveEntry, role, localKey, groupKey));

            var result = LedgerNativeEntryDefinitionValidator.validate(liveEntry);
            if (!result.isOK())
                throw LedgerNativeEntryAssembler.issue(
                                LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED,
                                result.format());
        });
    }

    private static List<LedgerPosting> matchingPostings(LedgerEntry entry, LedgerLegRole role, String localKey,
                    String groupKey)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(role);
        Objects.requireNonNull(localKey);

        if (entry.getType() != LedgerEntryType.CORPORATE_ACTION)
            return List.of();

        return entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == postingType(role)) //
                        .filter(posting -> posting.getCorporateActionLeg() == corporateActionLeg(role)) //
                        .filter(posting -> localKey.equals(posting.getLocalKey())) //
                        .filter(posting -> groupKey == null || groupKey.equals(posting.getGroupKey())) //
                        .toList();
    }

    private static LedgerPostingType postingType(LedgerLegRole role)
    {
        return switch (role)
        {
            case SOURCE_SECURITY_LEG, TARGET_SECURITY_LEG, SECURITY_CONTEXT_LEG, RECEIVED_SECURITY_LEG,
                            DISTRIBUTED_SECURITY_LEG -> LedgerPostingType.SECURITY;
            case DISTRIBUTED_RIGHT_LEG -> LedgerPostingType.RIGHT;
            case SOURCE_BOND_LEG -> LedgerPostingType.BOND;
            case CASH_LEG -> LedgerPostingType.CASH;
            case CASH_COMPENSATION_LEG -> LedgerPostingType.CASH_COMPENSATION;
            case ACCRUED_INTEREST_LEG -> LedgerPostingType.ACCRUED_INTEREST;
            case PRINCIPAL_REDEMPTION_LEG -> LedgerPostingType.PRINCIPAL_REDEMPTION;
            case FEE_LEG -> LedgerPostingType.FEE;
            case TAX_LEG -> LedgerPostingType.TAX;
            case FOREX_CONTEXT_LEG -> LedgerPostingType.FOREX;
        };
    }

    private static CorporateActionLeg corporateActionLeg(LedgerLegRole role)
    {
        return switch (role)
        {
            case SOURCE_SECURITY_LEG, SOURCE_BOND_LEG -> CorporateActionLeg.SOURCE_SECURITY;
            case TARGET_SECURITY_LEG, RECEIVED_SECURITY_LEG -> CorporateActionLeg.TARGET_SECURITY;
            case SECURITY_CONTEXT_LEG -> CorporateActionLeg.SECURITY_CONTEXT;
            case DISTRIBUTED_SECURITY_LEG -> CorporateActionLeg.DISTRIBUTED_SECURITY;
            case DISTRIBUTED_RIGHT_LEG -> CorporateActionLeg.RIGHT_SECURITY;
            case CASH_LEG -> null;
            case CASH_COMPENSATION_LEG -> CorporateActionLeg.CASH_COMPENSATION;
            case ACCRUED_INTEREST_LEG -> CorporateActionLeg.ACCRUED_INTEREST;
            case PRINCIPAL_REDEMPTION_LEG -> CorporateActionLeg.PRINCIPAL;
            case FEE_LEG -> CorporateActionLeg.FEE;
            case TAX_LEG -> CorporateActionLeg.TAX;
            case FOREX_CONTEXT_LEG -> null;
        };
    }
}
