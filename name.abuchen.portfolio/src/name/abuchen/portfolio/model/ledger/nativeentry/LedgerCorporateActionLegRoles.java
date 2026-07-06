package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.Optional;

import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

final class LedgerCorporateActionLegRoles
{
    private LedgerCorporateActionLegRoles()
    {
    }

    static boolean matches(LedgerPosting posting, LedgerLegRole role)
    {
        return posting.getType() == postingType(role) && posting.getCorporateActionLeg() == corporateActionLeg(role);
    }

    static Optional<LedgerLegRole> roleFor(LedgerPosting posting)
    {
        return switch (posting.getType())
        {
            case SECURITY -> switch (posting.getCorporateActionLeg())
            {
                case SOURCE_SECURITY -> Optional.of(LedgerLegRole.SOURCE_SECURITY_LEG);
                case TARGET_SECURITY -> Optional.of(LedgerLegRole.TARGET_SECURITY_LEG);
                case SECURITY_CONTEXT -> Optional.of(LedgerLegRole.SECURITY_CONTEXT_LEG);
                case DISTRIBUTED_SECURITY -> Optional.of(LedgerLegRole.DISTRIBUTED_SECURITY_LEG);
                default -> Optional.empty();
            };
            case RIGHT -> posting.getCorporateActionLeg() == CorporateActionLeg.RIGHT_SECURITY
                            ? Optional.of(LedgerLegRole.DISTRIBUTED_RIGHT_LEG)
                            : Optional.empty();
            case BOND -> posting.getCorporateActionLeg() == CorporateActionLeg.SOURCE_SECURITY
                            ? Optional.of(LedgerLegRole.SOURCE_BOND_LEG)
                            : Optional.empty();
            case CASH -> posting.getCorporateActionLeg() == null ? Optional.of(LedgerLegRole.CASH_LEG)
                            : Optional.empty();
            case CASH_COMPENSATION -> posting.getCorporateActionLeg() == CorporateActionLeg.CASH_COMPENSATION
                            ? Optional.of(LedgerLegRole.CASH_COMPENSATION_LEG)
                            : Optional.empty();
            case ACCRUED_INTEREST -> posting.getCorporateActionLeg() == CorporateActionLeg.ACCRUED_INTEREST
                            ? Optional.of(LedgerLegRole.ACCRUED_INTEREST_LEG)
                            : Optional.empty();
            case PRINCIPAL_REDEMPTION -> posting.getCorporateActionLeg() == CorporateActionLeg.PRINCIPAL
                            ? Optional.of(LedgerLegRole.PRINCIPAL_REDEMPTION_LEG)
                            : Optional.empty();
            case FEE -> posting.getCorporateActionLeg() == CorporateActionLeg.FEE ? Optional.of(LedgerLegRole.FEE_LEG)
                            : Optional.empty();
            case TAX -> posting.getCorporateActionLeg() == CorporateActionLeg.TAX ? Optional.of(LedgerLegRole.TAX_LEG)
                            : Optional.empty();
            case FOREX -> Optional.of(LedgerLegRole.FOREX_CONTEXT_LEG);
            default -> Optional.empty();
        };
    }

    static LedgerPostingType postingType(LedgerLegRole role)
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

    static CorporateActionLeg corporateActionLeg(LedgerLegRole role)
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
