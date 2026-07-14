package name.abuchen.portfolio.model.ledger.configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Defines which primary postings each ledger entry type requires and how the
 * roles of a posting follow from its posting type. This table is the single
 * source of truth for the semantic shape of an entry: the structural validator
 * checks entries against it, and semantic role, direction, and unit role of a
 * posting are derived from it.
 *
 * <p>
 * When an entry type uses the same posting type for more than one primary
 * posting (transfers), the direction cannot be derived and becomes required
 * input; {@link #isDirectionRequired(LedgerEntryType, LedgerPostingType)}
 * reports exactly these cases.
 * </p>
 */
public final class LedgerRoleRules
{
    public enum OwnerKind
    {
        ACCOUNT,
        PORTFOLIO
    }

    /**
     * Describes one required primary posting of an entry type: which posting
     * type carries it, the semantic role, movement direction, and projection
     * role it must have, and which owner reference must be present.
     */
    public record PrimaryRule(LedgerPostingType postingType, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingDirection direction, LedgerProjectionRole projectionRole, OwnerKind ownerKind)
    {
        public PrimaryRule
        {
            Objects.requireNonNull(postingType);
            Objects.requireNonNull(semanticRole);
            Objects.requireNonNull(direction);
            Objects.requireNonNull(projectionRole);
            Objects.requireNonNull(ownerKind);
        }
    }

    /**
     * Describes how a unit posting (fee, tax, gross value) is derived from its
     * posting type. Unit postings carry no direction.
     */
    public record UnitRule(LedgerPostingType postingType, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingUnitRole unitRole)
    {
        public UnitRule
        {
            Objects.requireNonNull(postingType);
            Objects.requireNonNull(semanticRole);
            Objects.requireNonNull(unitRole);
        }
    }

    private static final Map<LedgerEntryType, List<PrimaryRule>> PRIMARY_RULES = new EnumMap<>(LedgerEntryType.class);

    private static final Map<LedgerPostingType, UnitRule> UNIT_RULES = new EnumMap<>(LedgerPostingType.class);

    static
    {
        cashOnly(LedgerEntryType.DEPOSIT);
        cashOnly(LedgerEntryType.REMOVAL);
        cashOnly(LedgerEntryType.INTEREST);
        cashOnly(LedgerEntryType.INTEREST_CHARGE);
        cashOnly(LedgerEntryType.DIVIDENDS);

        accountOnly(LedgerEntryType.FEES, LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE);
        accountOnly(LedgerEntryType.FEES_REFUND, LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE);
        accountOnly(LedgerEntryType.TAXES, LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX);
        accountOnly(LedgerEntryType.TAX_REFUND, LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX);

        PRIMARY_RULES.put(LedgerEntryType.BUY, List.of(
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.ACCOUNT,
                                        OwnerKind.ACCOUNT),
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.PORTFOLIO,
                                        OwnerKind.PORTFOLIO)));
        PRIMARY_RULES.put(LedgerEntryType.SELL, List.of(
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.ACCOUNT,
                                        OwnerKind.ACCOUNT),
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.PORTFOLIO,
                                        OwnerKind.PORTFOLIO)));

        PRIMARY_RULES.put(LedgerEntryType.DELIVERY_INBOUND, List.of(
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.DELIVERY_INBOUND,
                                        OwnerKind.PORTFOLIO)));
        PRIMARY_RULES.put(LedgerEntryType.DELIVERY_OUTBOUND, List.of(
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.DELIVERY_OUTBOUND,
                                        OwnerKind.PORTFOLIO)));

        PRIMARY_RULES.put(LedgerEntryType.CASH_TRANSFER, List.of(
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.SOURCE_ACCOUNT,
                                        OwnerKind.ACCOUNT),
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.TARGET_ACCOUNT,
                                        OwnerKind.ACCOUNT)));
        PRIMARY_RULES.put(LedgerEntryType.SECURITY_TRANSFER, List.of(
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.SOURCE_PORTFOLIO,
                                        OwnerKind.PORTFOLIO),
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.TARGET_PORTFOLIO,
                                        OwnerKind.PORTFOLIO)));

        UNIT_RULES.put(LedgerPostingType.FEE, new UnitRule(LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE,
                        LedgerPostingUnitRole.FEE));
        UNIT_RULES.put(LedgerPostingType.TAX, new UnitRule(LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX,
                        LedgerPostingUnitRole.TAX));
        UNIT_RULES.put(LedgerPostingType.GROSS_VALUE, new UnitRule(LedgerPostingType.GROSS_VALUE,
                        LedgerPostingSemanticRole.GROSS_VALUE, LedgerPostingUnitRole.GROSS_VALUE));
    }

    private LedgerRoleRules()
    {
    }

    public static List<PrimaryRule> primaryRules(LedgerEntryType entryType)
    {
        return PRIMARY_RULES.getOrDefault(Objects.requireNonNull(entryType), List.of());
    }

    /**
     * Returns the single primary rule for the given posting type, or empty if
     * the entry type does not use the posting type as a primary or uses it for
     * more than one primary (transfers) - then the direction is required to
     * disambiguate via {@link #primaryRule(LedgerEntryType, LedgerPostingType, LedgerPostingDirection)}.
     */
    public static Optional<PrimaryRule> primaryRule(LedgerEntryType entryType, LedgerPostingType postingType)
    {
        var matches = primaryRules(entryType).stream() //
                        .filter(rule -> rule.postingType() == postingType) //
                        .toList();

        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public static Optional<PrimaryRule> primaryRule(LedgerEntryType entryType, LedgerPostingType postingType,
                    LedgerPostingDirection direction)
    {
        return primaryRules(entryType).stream() //
                        .filter(rule -> rule.postingType() == postingType) //
                        .filter(rule -> rule.direction() == direction) //
                        .findFirst();
    }

    /**
     * Returns true if the entry type uses the posting type for more than one
     * primary posting, so the direction cannot be derived and must be stated.
     */
    public static boolean isDirectionRequired(LedgerEntryType entryType, LedgerPostingType postingType)
    {
        return primaryRules(entryType).stream() //
                        .filter(rule -> rule.postingType() == postingType) //
                        .count() > 1;
    }

    public static Optional<UnitRule> unitRule(LedgerPostingType postingType)
    {
        return Optional.ofNullable(UNIT_RULES.get(Objects.requireNonNull(postingType)));
    }

    private static void cashOnly(LedgerEntryType entryType)
    {
        accountOnly(entryType, LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH);
    }

    private static void accountOnly(LedgerEntryType entryType, LedgerPostingType postingType,
                    LedgerPostingSemanticRole semanticRole)
    {
        PRIMARY_RULES.put(entryType, List.of(new PrimaryRule(postingType, semanticRole,
                        LedgerPostingDirection.NEUTRAL, LedgerProjectionRole.ACCOUNT, OwnerKind.ACCOUNT)));
    }
}
