package name.abuchen.portfolio.model.ledger.configuration.rule;

import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Names semantic movement predicates that may satisfy a corporate action primary
 * movement requirement.
 */
public enum LedgerPrimaryMovement
{
    SOURCE_SECURITY,
    TARGET_SECURITY,
    CASH,
    PRINCIPAL_REDEMPTION,
    ACCRUED_INTEREST,
    FEE,
    TAX;

    public boolean matches(LedgerPosting posting)
    {
        return switch (this)
        {
            case SOURCE_SECURITY -> posting.getType() == LedgerPostingType.SECURITY
                            && posting.getDirection() == LedgerPostingDirection.OUTBOUND
                            && posting.getCorporateActionLeg() == CorporateActionLeg.SOURCE_SECURITY;
            case TARGET_SECURITY -> posting.getType() == LedgerPostingType.SECURITY
                            && posting.getDirection() == LedgerPostingDirection.INBOUND
                            && posting.getCorporateActionLeg() == CorporateActionLeg.TARGET_SECURITY;
            case CASH -> posting.getType() == LedgerPostingType.CASH
                            || posting.getType() == LedgerPostingType.CASH_COMPENSATION;
            case PRINCIPAL_REDEMPTION -> posting.getType() == LedgerPostingType.PRINCIPAL_REDEMPTION
                            && posting.getCorporateActionLeg() == CorporateActionLeg.PRINCIPAL;
            case ACCRUED_INTEREST -> posting.getType() == LedgerPostingType.ACCRUED_INTEREST
                            && posting.getCorporateActionLeg() == CorporateActionLeg.ACCRUED_INTEREST;
            case FEE -> posting.getType() == LedgerPostingType.FEE
                            && posting.getCorporateActionLeg() == CorporateActionLeg.FEE;
            case TAX -> posting.getType() == LedgerPostingType.TAX
                            && posting.getCorporateActionLeg() == CorporateActionLeg.TAX;
        };
    }
}
