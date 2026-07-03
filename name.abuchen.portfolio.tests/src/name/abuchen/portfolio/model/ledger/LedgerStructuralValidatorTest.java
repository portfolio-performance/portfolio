package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator.IssueCode;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests structural validation for ledger entries.
 */
@SuppressWarnings("nls")
public class LedgerStructuralValidatorTest
{
    @Test
    public void testEmptyLedgerIsValid()
    {
        assertOK(LedgerStructuralValidator.validate(new Ledger()));
    }

    @Test
    public void testSimpleValidLedgerEntryPassesValidation()
    {
        assertOK(LedgerStructuralValidator.validate(createStandardLedger()));
    }

    @Test
    public void testValidFixedShapeSemanticEntriesPassValidation()
    {
        for (var type : new LedgerEntryType[] { LedgerEntryType.DEPOSIT, LedgerEntryType.REMOVAL,
                        LedgerEntryType.INTEREST, LedgerEntryType.INTEREST_CHARGE, LedgerEntryType.DIVIDENDS,
                        LedgerEntryType.FEES, LedgerEntryType.FEES_REFUND, LedgerEntryType.TAXES,
                        LedgerEntryType.TAX_REFUND, LedgerEntryType.BUY, LedgerEntryType.SELL,
                        LedgerEntryType.DELIVERY_INBOUND, LedgerEntryType.DELIVERY_OUTBOUND,
                        LedgerEntryType.CASH_TRANSFER, LedgerEntryType.SECURITY_TRANSFER })
            assertOK(LedgerStructuralValidator.validate(ledger(entry(type))));
    }

    @Test
    public void testMissingTransferSourceIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.removePosting(posting(entry, LedgerPostingDirection.OUTBOUND));

        assertIssue(entry, IssueCode.SEMANTIC_SOURCE_REQUIRED);
    }

    @Test
    public void testMissingTransferTargetIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.removePosting(posting(entry, LedgerPostingDirection.INBOUND));

        assertIssue(entry, IssueCode.SEMANTIC_TARGET_REQUIRED);
    }

    @Test
    public void testDuplicateTransferSourceIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.addPosting(cashPosting("duplicate-source", LedgerPostingDirection.OUTBOUND)); //$NON-NLS-1$

        assertIssue(entry, IssueCode.SEMANTIC_SOURCE_AMBIGUOUS);
    }

    @Test
    public void testDuplicateTransferTargetIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.addPosting(cashPosting("duplicate-target", LedgerPostingDirection.INBOUND)); //$NON-NLS-1$

        assertIssue(entry, IssueCode.SEMANTIC_TARGET_AMBIGUOUS);
    }

    @Test
    public void testMissingPrimarySemanticRoleIsRejected()
    {
        var entry = entry(LedgerEntryType.BUY);
        posting(entry, LedgerPostingSemanticRole.CASH).setSemanticRole(null);

        assertIssue(entry, IssueCode.SEMANTIC_PRIMARY_REQUIRED);
    }

    @Test
    public void testDuplicatePrimaryPostingsAreRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.addPosting(cashPosting("duplicate-cash", LedgerPostingDirection.NEUTRAL)); //$NON-NLS-1$

        assertIssue(entry, IssueCode.SEMANTIC_PRIMARY_AMBIGUOUS);
    }

    @Test
    public void testGroupedUnitWithoutGroupKeyIsRejectedWhenEntryHasMultiplePrimaries()
    {
        var entry = entry(LedgerEntryType.BUY);
        posting(entry, LedgerPostingSemanticRole.CASH).setGroupKey(LedgerProjectionRole.ACCOUNT.name());
        posting(entry, LedgerPostingSemanticRole.SECURITY).setGroupKey(LedgerProjectionRole.PORTFOLIO.name());
        var fee = unitPosting(LedgerPostingType.FEE, LedgerPostingUnitRole.FEE, null);
        entry.addPosting(fee);

        assertIssue(entry, IssueCode.SEMANTIC_UNIT_GROUP_REQUIRED);
    }

    @Test
    public void testUnitWithUnknownGroupKeyIsRejected()
    {
        var entry = entry(LedgerEntryType.BUY);
        var fee = unitPosting(LedgerPostingType.FEE, LedgerPostingUnitRole.FEE, "missing-group"); //$NON-NLS-1$
        entry.addPosting(fee);

        assertIssue(entry, IssueCode.SEMANTIC_UNIT_GROUP_AMBIGUOUS);
    }

    @Test
    public void testRepeatedUnitWithoutLocalKeyIsRejected()
    {
        var entry = entry(LedgerEntryType.BUY);
        entry.addPosting(unitPosting(LedgerPostingType.FEE, LedgerPostingUnitRole.FEE, "account")); //$NON-NLS-1$
        entry.addPosting(unitPosting(LedgerPostingType.FEE, LedgerPostingUnitRole.FEE, "account")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.SEMANTIC_LOCAL_KEY_REQUIRED);
    }

    @Test
    public void testOptionalLocalKeyIsNotRequiredForUniqueUnit()
    {
        var entry = entry(LedgerEntryType.BUY);
        entry.addPosting(unitPosting(LedgerPostingType.FEE, LedgerPostingUnitRole.FEE, "account")); //$NON-NLS-1$

        assertOK(LedgerStructuralValidator.validate(ledger(entry)));
    }

    @Test
    public void testCorporateActionLegCompletenessIsValidated()
    {
        assertOK(LedgerStructuralValidator.validate(ledger(nativeEntry(LedgerEntryType.CORPORATE_ACTION))));
    }

    @Test
    public void testSpinOffAllowsNoCorporateActionPrimaryLegs()
    {
        var entry = new LedgerEntry();
        entry.setType(LedgerEntryType.CORPORATE_ACTION);
        entry.setDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF));

        assertOK(LedgerStructuralValidator.validate(ledger(entry)));
    }

    @Test
    public void testDuplicateCorporateActionLegWithoutLocalKeyIsRejected()
    {
        var entry = nativeEntry(LedgerEntryType.CORPORATE_ACTION);
        var duplicate = securityPosting("duplicate-target", LedgerPostingDirection.INBOUND, //$NON-NLS-1$
                        CorporateActionLeg.TARGET_SECURITY);
        duplicate.setLocalKey(null);
        entry.addPosting(duplicate);

        assertIssue(entry, IssueCode.SEMANTIC_LOCAL_KEY_REQUIRED);
    }

    @Test
    public void testSpinOffAcceptsRepeatedTargetLegsWithDistinctLocalKeys()
    {
        var entry = nativeEntry(LedgerEntryType.CORPORATE_ACTION);
        var target = posting(entry, CorporateActionLeg.TARGET_SECURITY);
        var duplicate = securityPosting("target-2", LedgerPostingDirection.INBOUND, //$NON-NLS-1$
                        CorporateActionLeg.TARGET_SECURITY);

        target.setLocalKey("target-1"); //$NON-NLS-1$
        target.setGroupKey("main"); //$NON-NLS-1$
        duplicate.setGroupKey("main"); //$NON-NLS-1$
        entry.addPosting(duplicate);

        assertOK(LedgerStructuralValidator.validate(ledger(entry)));
    }

    @Test
    public void testSpinOffRejectsRepeatedTargetLegsWithDuplicateLocalKey()
    {
        var entry = nativeEntry(LedgerEntryType.CORPORATE_ACTION);
        var target = posting(entry, CorporateActionLeg.TARGET_SECURITY);
        var duplicate = securityPosting("target-1", LedgerPostingDirection.INBOUND, //$NON-NLS-1$
                        CorporateActionLeg.TARGET_SECURITY);

        target.setLocalKey("target-1"); //$NON-NLS-1$
        entry.addPosting(duplicate);

        assertIssue(entry, IssueCode.SEMANTIC_TARGET_AMBIGUOUS);
    }

    @Test
    public void testSpinOffWithoutTargetLegIsAccepted()
    {
        var entry = nativeEntry(LedgerEntryType.CORPORATE_ACTION);
        entry.removePosting(posting(entry, CorporateActionLeg.TARGET_SECURITY));

        assertOK(LedgerStructuralValidator.validate(ledger(entry)));
    }

    @Test
    public void testSpinOffAcceptsRepeatedCashCompensationWithDistinctLocalKeys()
    {
        var entry = nativeEntry(LedgerEntryType.CORPORATE_ACTION);

        entry.addPosting(cashCompensationPosting("cash-1")); //$NON-NLS-1$
        entry.addPosting(cashCompensationPosting("cash-2")); //$NON-NLS-1$

        assertOK(LedgerStructuralValidator.validate(ledger(entry)));
    }

    @Test
    public void testSpinOffRejectsRepeatedCashCompensationWithDuplicateLocalKey()
    {
        var entry = nativeEntry(LedgerEntryType.CORPORATE_ACTION);

        entry.addPosting(cashCompensationPosting("cash-1")); //$NON-NLS-1$
        entry.addPosting(cashCompensationPosting("cash-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.SEMANTIC_PRIMARY_AMBIGUOUS);
    }

    private Ledger createStandardLedger()
    {
        return ledger(entry(LedgerEntryType.DEPOSIT));
    }

    private Ledger ledger(LedgerEntry entry)
    {
        var ledger = new Ledger();
        ledger.addEntry(entry);

        return ledger;
    }

    private LedgerEntry entry(LedgerEntryType type)
    {
        var entry = new LedgerEntry();
        entry.setType(type);
        entry.setDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));

        switch (type)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, DIVIDENDS ->
            {
                var posting = cashPosting("account", LedgerPostingDirection.NEUTRAL); //$NON-NLS-1$
                if (type == LedgerEntryType.DIVIDENDS)
                    posting.setSecurity(security());
                entry.addPosting(posting);
            }
            case FEES, FEES_REFUND -> entry.addPosting(accountPosting("fee", LedgerPostingType.FEE, //$NON-NLS-1$
                            LedgerPostingSemanticRole.FEE, LedgerPostingDirection.NEUTRAL));
            case TAXES, TAX_REFUND -> entry.addPosting(accountPosting("tax", LedgerPostingType.TAX, //$NON-NLS-1$
                            LedgerPostingSemanticRole.TAX, LedgerPostingDirection.NEUTRAL));
            case BUY -> {
                entry.addPosting(cashPosting("account", LedgerPostingDirection.OUTBOUND)); //$NON-NLS-1$
                entry.addPosting(securityPosting("portfolio", LedgerPostingDirection.INBOUND)); //$NON-NLS-1$
            }
            case SELL -> {
                entry.addPosting(cashPosting("account", LedgerPostingDirection.INBOUND)); //$NON-NLS-1$
                entry.addPosting(securityPosting("portfolio", LedgerPostingDirection.OUTBOUND)); //$NON-NLS-1$
            }
            case DELIVERY_INBOUND -> entry.addPosting(securityPosting("delivery-in", LedgerPostingDirection.INBOUND)); //$NON-NLS-1$
            case DELIVERY_OUTBOUND -> entry.addPosting(securityPosting("delivery-out", LedgerPostingDirection.OUTBOUND)); //$NON-NLS-1$
            case CASH_TRANSFER -> {
                entry.addPosting(cashPosting("source", LedgerPostingDirection.OUTBOUND)); //$NON-NLS-1$
                entry.addPosting(cashPosting("target", LedgerPostingDirection.INBOUND)); //$NON-NLS-1$
            }
            case SECURITY_TRANSFER -> {
                entry.addPosting(securityPosting("source", LedgerPostingDirection.OUTBOUND)); //$NON-NLS-1$
                entry.addPosting(securityPosting("target", LedgerPostingDirection.INBOUND)); //$NON-NLS-1$
            }
            default -> throw new IllegalArgumentException(type.name());
        }

        return entry;
    }

    private LedgerEntry nativeEntry(LedgerEntryType type)
    {
        var entry = new LedgerEntry();
        entry.setType(type);
        entry.setDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));

        switch (type)
        {
            case CORPORATE_ACTION -> {
                entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_KIND,
                                CorporateActionKind.SPIN_OFF));
                entry.addPosting(securityPosting(LedgerProjectionRole.OLD_SECURITY_LEG.name(),
                                LedgerPostingDirection.OUTBOUND, CorporateActionLeg.SOURCE_SECURITY));
                entry.addPosting(securityPosting(LedgerProjectionRole.NEW_SECURITY_LEG.name(),
                                LedgerPostingDirection.INBOUND,
                                CorporateActionLeg.TARGET_SECURITY));
            }
            default -> throw new IllegalArgumentException(type.name());
        }

        return entry;
    }

    private LedgerPosting cashPosting(String localKey, LedgerPostingDirection direction)
    {
        return accountPosting(localKey, LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH, direction);
    }

    private LedgerPosting accountPosting(String localKey, LedgerPostingType type, LedgerPostingSemanticRole role,
                    LedgerPostingDirection direction)
    {
        var posting = new LedgerPosting();
        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setAccount(account());
        markPrimary(posting, role, direction, localKey);

        return posting;
    }

    private LedgerPosting securityPosting(String localKey, LedgerPostingDirection direction)
    {
        var posting = new LedgerPosting();
        posting.setType(LedgerPostingType.SECURITY);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setPortfolio(portfolio());
        posting.setSecurity(security());
        posting.setShares(Values.Share.factorize(10));
        markPrimary(posting, LedgerPostingSemanticRole.SECURITY, direction, localKey);

        return posting;
    }

    private LedgerPosting securityPosting(String localKey, LedgerPostingDirection direction, CorporateActionLeg leg)
    {
        var posting = securityPosting(localKey, direction);
        posting.setCorporateActionLeg(leg);
        posting.setGroupKey(localKey);
        return posting;
    }

    private LedgerPosting cashCompensationPosting(String localKey)
    {
        var posting = accountPosting(localKey, LedgerPostingType.CASH_COMPENSATION,
                        LedgerPostingSemanticRole.CASH_COMPENSATION, LedgerPostingDirection.NEUTRAL);
        posting.setCorporateActionLeg(CorporateActionLeg.CASH_COMPENSATION);
        posting.setGroupKey(localKey);
        return posting;
    }

    private LedgerPosting rightPosting(String localKey, CorporateActionLeg leg)
    {
        var posting = securityPosting(localKey, LedgerPostingDirection.INBOUND, leg);
        posting.setType(LedgerPostingType.RIGHT);
        posting.setSemanticRole(LedgerPostingSemanticRole.RIGHT);
        return posting;
    }

    private LedgerPosting bondPosting(String localKey, LedgerPostingDirection direction, CorporateActionLeg leg)
    {
        var posting = securityPosting(localKey, direction, leg);
        posting.setType(LedgerPostingType.BOND);
        posting.setSemanticRole(LedgerPostingSemanticRole.BOND);
        return posting;
    }

    private LedgerPosting unitPosting(LedgerPostingType type, LedgerPostingUnitRole unitRole, String groupKey)
    {
        var posting = new LedgerPosting();
        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(type == LedgerPostingType.FEE ? LedgerPostingSemanticRole.FEE
                        : LedgerPostingSemanticRole.TAX);
        posting.setUnitRole(unitRole);
        posting.setGroupKey(groupKey);

        return posting;
    }

    private void markPrimary(LedgerPosting posting, LedgerPostingSemanticRole role, LedgerPostingDirection direction,
                    String localKey)
    {
        posting.setSemanticRole(role);
        posting.setDirection(direction);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setLocalKey(localKey);
        posting.setGroupKey(localKey);
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingDirection direction)
    {
        return entry.getPostings().stream().filter(posting -> posting.getDirection() == direction).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingSemanticRole role)
    {
        return entry.getPostings().stream().filter(posting -> posting.getSemanticRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, CorporateActionLeg leg)
    {
        return entry.getPostings().stream().filter(posting -> posting.getCorporateActionLeg() == leg).findFirst()
                        .orElseThrow();
    }

    private Account account()
    {
        var account = new Account();
        account.setName("Cash");
        return account;
    }

    private Portfolio portfolio()
    {
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio");
        return portfolio;
    }

    private Security security()
    {
        return new Security("Security", CurrencyUnit.EUR);
    }

    private void assertOK(LedgerStructuralValidator.ValidationResult result)
    {
        assertTrue(result.format(), result.isOK());
    }

    private void assertIssue(LedgerEntry entry, IssueCode code)
    {
        var result = LedgerStructuralValidator.validate(ledger(entry));

        assertThat(result.format(), result.hasIssue(code), is(true));
    }
}
