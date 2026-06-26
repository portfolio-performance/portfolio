package name.abuchen.portfolio.model.ledger;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator.IssueCode;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

/**
 * Tests structural validation for ledger entries.
 * These tests make sure malformed ledger facts are rejected before they can become persisted transaction truth.
 */
@SuppressWarnings("nls")
public class LedgerStructuralValidatorTest
{
    /**
     * Checks the ledger rule scenario: empty ledger is valid.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEmptyLedgerIsValid()
    {
        assertOK(LedgerStructuralValidator.validate(new Ledger()));
    }

    /**
     * Checks the ledger rule scenario: simple valid ledger entry passes validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSimpleValidLedgerEntryPassesValidation()
    {
        assertOK(LedgerStructuralValidator.validate(createStandardLedger()));
    }

    /**
     * Checks the ledger rule scenario: validation issue formats code and message.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testValidationIssueFormatsCodeAndMessage()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setCurrency(null);

        var issue = LedgerStructuralValidator.validate(ledger).getIssues().get(0);

        assertTrue(issue.format(), issue.format().startsWith("[POSTING_CURRENCY_REQUIRED] "));
        assertTrue(issue.format(), issue.format().contains("[LEDGER-STRUCT-014] "));
        assertTrue(issue.format(), issue.format().contains("\n  Entry:\n"));
        assertTrue(issue.format(), issue.format().contains("\n    Currency: <missing>"));
        assertTrue(issue.format(), !issue.format().contains("{"));
        assertTrue(issue.toString(), issue.toString().contains("POSTING_CURRENCY_REQUIRED"));
    }

    /**
     * Checks the ledger rule scenario: validation result formats issues deterministically.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testValidationResultFormatsIssuesDeterministically()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);
        var posting = entry.getPostings().get(0);

        entry.setDateTime(null);
        posting.setCurrency(null);

        var result = LedgerStructuralValidator.validate(ledger);
        var format = result.format();

        assertTrue(format, format.startsWith("[ENTRY_DATE_TIME_REQUIRED] "));
        assertTrue(format, format.contains("[LEDGER-STRUCT-008] "));
        assertTrue(format, format.contains("\n\n[POSTING_CURRENCY_REQUIRED] "));
        assertTrue(format, format.contains("[LEDGER-STRUCT-014] "));
        assertTrue(format, format.contains("\n  Entry:\n"));
        assertTrue(format, format.contains("\n  Posting:\n"));
        assertTrue(result.toString(), result.toString().contains("POSTING_CURRENCY_REQUIRED"));
    }

    /**
     * Checks the ledger rule scenario: creator exception uses formatted diagnostics.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testCreatorExceptionUsesFormattedDiagnostics()
    {
        var client = new Client();
        var account = account();
        var ledger = createStandardLedger();

        ledger.getEntries().get(0).getPostings().get(0).setCurrency(null);
        client.addAccount(account);
        client.getLedger().addEntry(ledger.getEntries().get(0));

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerTransactionCreator(client).createDeposit(
                                        LedgerTransactionMetadata.of(LocalDateTime.of(2026, 1, 3, 0, 0)),
                                        LedgerAccountCashLeg.of(account, Money.of(CurrencyUnit.EUR, 1L))));

        assertTrue(exception.getMessage(), exception.getMessage().contains("[POSTING_CURRENCY_REQUIRED] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("[LEDGER-STRUCT-014] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("\n  Posting:\n"));
    }

    /**
     * Checks the ledger rule scenario: duplicate entry uuidis reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDuplicateEntryUUIDIsReported()
    {
        var ledger = new Ledger();

        ledger.addEntry(validEntry("entry-1", LedgerEntryType.DEPOSIT));
        ledger.addEntry(validEntry("entry-1", LedgerEntryType.REMOVAL));

        assertIssue(ledger, IssueCode.DUPLICATE_ENTRY_UUID);
    }

    /**
     * Checks the ledger rule scenario: duplicate posting uuidis reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDuplicatePostingUUIDIsReported()
    {
        var ledger = new Ledger();
        var entry1 = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var entry2 = validEntry("entry-2", LedgerEntryType.REMOVAL);

        entry1.addPosting(validPosting("posting-1"));
        entry2.addPosting(validPosting("posting-1"));
        ledger.addEntry(entry1);
        ledger.addEntry(entry2);

        assertIssue(ledger, IssueCode.DUPLICATE_POSTING_UUID);

        var format = findIssue(ledger, IssueCode.DUPLICATE_POSTING_UUID).format();

        assertTrue(format, format.contains("\n  Duplicate:\n"));
        assertTrue(format, format.contains("DuplicateUUID: posting-1"));
        assertTrue(format, format.contains("ObjectType: LedgerPosting"));
        assertTrue(format, format.contains("OccurrenceCount: 2"));
        assertTrue(format, format.contains("UUID: entry-2"));
    }

    /**
     * Checks the ledger rule scenario: duplicate projection ref uuidis reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDuplicateProjectionRefUUIDIsReported()
    {
        var ledger = new Ledger();
        var entry1 = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var entry2 = validEntry("entry-2", LedgerEntryType.REMOVAL);

        entry1.addProjectionRef(accountProjection("projection-1"));
        entry2.addProjectionRef(accountProjection("projection-1"));
        ledger.addEntry(entry1);
        ledger.addEntry(entry2);

        assertIssue(ledger, IssueCode.DUPLICATE_PROJECTION_REF_UUID);
    }

    /**
     * Checks the ledger rule scenario: missing required entry fields are reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testMissingRequiredEntryFieldsAreReported() throws ReflectiveOperationException
    {
        var ledger = new Ledger();
        var entry = new LedgerEntry("entry-1");

        setField(entry, "uuid", null);
        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);

        assertTrue(result.hasIssue(IssueCode.ENTRY_UUID_REQUIRED));
        assertTrue(result.hasIssue(IssueCode.ENTRY_TYPE_REQUIRED));
        assertTrue(result.hasIssue(IssueCode.ENTRY_DATE_TIME_REQUIRED));
    }

    /**
     * Checks the ledger rule scenario: missing required posting fields are reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testMissingRequiredPostingFieldsAreReported() throws ReflectiveOperationException
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var posting = new LedgerPosting("posting-1");

        setField(posting, "uuid", null);
        entry.addPosting(posting);
        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);

        assertTrue(result.hasIssue(IssueCode.POSTING_UUID_REQUIRED));
        assertTrue(result.hasIssue(IssueCode.POSTING_TYPE_REQUIRED));
    }

    /**
     * Checks the ledger rule scenario: missing required projection fields are reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testMissingRequiredProjectionFieldsAreReported() throws ReflectiveOperationException
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var projection = new LedgerProjectionRef("projection-1");

        setField(projection, "uuid", null);
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);

        assertTrue(result.hasIssue(IssueCode.PROJECTION_REF_UUID_REQUIRED));
        assertTrue(result.hasIssue(IssueCode.PROJECTION_REF_ROLE_REQUIRED));
    }

    /**
     * Checks the ledger rule scenario: invalid owner role combination is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testInvalidOwnerRoleCombinationIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var projection = new LedgerProjectionRef("projection-1");

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setPortfolio(new Portfolio());
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);

        assertTrue(result.hasIssue(IssueCode.PROJECTION_REF_ACCOUNT_REQUIRED));
        assertTrue(result.hasIssue(IssueCode.PROJECTION_REF_PORTFOLIO_NOT_ALLOWED));

        var format = findIssue(ledger, IssueCode.PROJECTION_REF_ACCOUNT_REQUIRED).format();

        assertTrue(format, format.contains("\n  Projection:\n"));
        assertTrue(format, format.contains("UUID: projection-1"));
        assertTrue(format, format.contains("Role: ACCOUNT"));
        assertTrue(format, format.contains("Portfolio: "));
    }

    /**
     * Checks the ledger rule scenario: portfolio role requires portfolio ownership.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testPortfolioRoleRequiresPortfolioOwnership()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var projection = new LedgerProjectionRef("projection-1");

        projection.setRole(LedgerProjectionRole.DELIVERY_INBOUND);
        projection.setAccount(new Account());
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);

        assertTrue(result.hasIssue(IssueCode.PROJECTION_REF_PORTFOLIO_REQUIRED));
        assertTrue(result.hasIssue(IssueCode.PROJECTION_REF_ACCOUNT_NOT_ALLOWED));
    }

    /**
     * Checks the ledger rule scenario: spin off without targeted projection reference is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSpinOffWithoutTargetedProjectionReferenceIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);

        entry.addPosting(validPosting("posting-1"));
        entry.addProjectionRef(portfolioProjection("projection-1"));
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.TARGETING_REF_REQUIRED);
    }

    /**
     * Checks the ledger rule scenario: invalid primary posting uuidis reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testInvalidPrimaryPostingUUIDIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.setPrimaryPostingUUID("missing-posting");
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.PRIMARY_POSTING_REF_NOT_FOUND);
    }

    /**
     * Checks the ledger rule scenario: fixed-shape primary posting uuid must target the same entry.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testFixedShapeInvalidPrimaryPostingUUIDIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);
        var projection = accountProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.setPrimaryPostingUUID("missing-posting");
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.PRIMARY_POSTING_REF_NOT_FOUND);
    }

    /**
     * Checks the ledger rule scenario: primary posting uuidmust target posting inside same entry.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testPrimaryPostingUUIDMustTargetPostingInsideSameEntry()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var otherEntry = validEntry("entry-2", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.setPrimaryPostingUUID("posting-2");
        entry.addProjectionRef(projection);
        otherEntry.addPosting(validPosting("posting-2"));
        ledger.addEntry(entry);
        ledger.addEntry(otherEntry);

        assertIssue(ledger, IssueCode.PRIMARY_POSTING_REF_NOT_FOUND);
    }

    /**
     * Checks the ledger rule scenario: valid primary posting uuidpasses validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testValidPrimaryPostingUUIDPassesValidation()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.setPrimaryPostingUUID("posting-1");
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: primary projection membership can satisfy targeting.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testPrimaryMembershipPassesValidation()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.addMembership("posting-1", ProjectionMembershipRole.PRIMARY);
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: projection membership target must resolve inside same entry.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testInvalidPrimaryMembershipPostingUUIDIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.addMembership("missing-posting", ProjectionMembershipRole.PRIMARY);
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.PROJECTION_MEMBERSHIP_REF_NOT_FOUND);
    }

    /**
     * Checks the ledger rule scenario: primary membership and scalar fallback must not conflict.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testPrimaryMembershipAndScalarPrimaryConflictIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        entry.addPosting(validPosting("posting-2"));
        projection.setPrimaryPostingUUID("posting-1");
        projection.addMembership("posting-2", ProjectionMembershipRole.PRIMARY);
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.PROJECTION_PRIMARY_TARGET_CONFLICT);
    }

    /**
     * Checks the ledger rule scenario: group membership and scalar fallback must not conflict.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testGroupMembershipAndScalarPostingGroupConflictIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        entry.addPosting(validPosting("posting-2"));
        projection.setPrimaryPostingUUID("posting-1");
        projection.setPostingGroupUUID("posting-1");
        projection.addMembership("posting-2", ProjectionMembershipRole.GROUP_ANCHOR);
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.PROJECTION_GROUP_TARGET_CONFLICT);
    }

    /**
     * Checks the ledger rule scenario: ex-date with local date time passes validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testExDateWithLocalDateTimePassesValidation()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setSecurity(new Security("Security", CurrencyUnit.EUR));
        posting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2020, 9, 28, 0, 0)));

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: ex-date with wrong value kind is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testExDateWithWrongValueKindIsReported()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.addParameter(LedgerParameter.unchecked(LedgerParameterType.EX_DATE, ValueKind.STRING,
                        "2020-09-28"));

        assertIssue(ledger, IssueCode.EX_DATE_VALUE_KIND_REQUIRED);
    }

    /**
     * Checks the ledger rule scenario: ex-date without posting security is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testExDateWithoutPostingSecurityIsReported()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2020, 9, 28, 0, 0)));

        assertIssue(ledger, IssueCode.EX_DATE_SECURITY_REQUIRED);
    }

    /**
     * Checks the ledger rule scenario: parameter value kind mismatch is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testParameterValueKindMismatchIsReported()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.addParameter(LedgerParameter.unchecked(LedgerParameterType.FEE_REASON,
                        ValueKind.STRING, BigDecimal.ONE));

        assertIssue(ledger, IssueCode.PARAMETER_VALUE_KIND_MISMATCH);
    }

    /**
     * Checks the ledger rule scenario: parameter type value kind mismatch is reported for every configured type.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testParameterTypeValueKindMismatchIsReportedForEveryConfiguredType()
    {
        for (var type : LedgerParameterType.values())
        {
            var ledger = createStandardLedger();
            var posting = ledger.getEntries().get(0).getPostings().get(0);
            var wrongValueKind = wrongValueKind(type);

            posting.setSecurity(new Security("Security", CurrencyUnit.EUR));
            posting.addParameter(LedgerParameter.unchecked(type, wrongValueKind, valueFor(wrongValueKind)));

            assertIssue(ledger, type == LedgerParameterType.EX_DATE ? IssueCode.EX_DATE_VALUE_KIND_REQUIRED
                            : IssueCode.PARAMETER_VALUE_KIND_MISMATCH);
        }
    }

    /**
     * Checks the ledger rule scenario: supported parameter types pass validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSupportedParameterTypesPassValidation()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Security", CurrencyUnit.EUR);

        posting.setSecurity(security);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.FEE_REASON,
                        FeeReason.BROKER_FEE.getCode()));
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.TAX_REASON,
                        TaxReason.WITHHOLDING_TAX.getCode()));
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF.getCode()));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR,
                        BigDecimal.ONE));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_DENOMINATOR,
                        BigDecimal.valueOf(2)));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.CONVERSION_RATIO,
                        BigDecimal.valueOf(3)));
        posting.addParameter(LedgerParameter.ofMoney(LedgerParameterType.NOMINAL_VALUE,
                        Money.of(CurrencyUnit.EUR, 100L)));
        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY, security));
        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY, security));
        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.RIGHT_SECURITY, security));
        posting.addParameter(LedgerParameter.ofAccount(LedgerParameterType.SOURCE_ACCOUNT, account));
        posting.addParameter(LedgerParameter.ofPortfolio(LedgerParameterType.SOURCE_PORTFOLIO,
                        portfolio));
        posting.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.RECORD_DATE,
                        LocalDate.of(2026, 1, 2)));
        posting.addParameter(LedgerParameter.ofBoolean(LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        Boolean.TRUE));
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.SOURCE_SECURITY.getCode()));
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CASH_COMPENSATION_KIND,
                        CashCompensationKind.CASH_IN_LIEU.getCode()));
        posting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2020, 9, 28, 0, 0)));

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: controlled ledger parameter codes pass validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testControlledLedgerParameterCodesPassValidation()
    {
        for (var type : LedgerParameterType.values())
        {
            if (!type.hasCodeDomain())
                continue;

            var ledger = createStandardLedger();
            var posting = ledger.getEntries().get(0).getPostings().get(0);
            var code = type.getCodeDomain().getAllowedCodes().get(0);

            posting.addParameter(LedgerParameter.ofString(type, code));

            assertOK(LedgerStructuralValidator.validate(ledger));
        }
    }

    /**
     * Checks the ledger rule scenario: unknown controlled ledger parameter code is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testUnknownControlledLedgerParameterCodeIsReported()
    {
        for (var type : LedgerParameterType.values())
        {
            if (!type.hasCodeDomain())
                continue;

            var ledger = createStandardLedger();
            var posting = ledger.getEntries().get(0).getPostings().get(0);

            posting.addParameter(LedgerParameter.ofString(type, "UNKNOWN_CODE"));

            assertIssue(ledger, IssueCode.PARAMETER_CODE_NOT_ALLOWED);
        }
    }

    /**
     * Checks the ledger rule scenario: free string ledger parameter still accepts arbitrary value.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testFreeStringLedgerParameterStillAcceptsArbitraryValue()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE,
                        "external reference #42"));

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: entry level parameter passes generic validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEntryLevelParameterPassesGenericValidation()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);

        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF.getCode()));
        entry.addParameter(LedgerParameter.ofBoolean(LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        Boolean.TRUE));

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: entry level parameter value kind mismatch is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEntryLevelParameterValueKindMismatchIsReported()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);

        entry.addParameter(LedgerParameter.unchecked(LedgerParameterType.RECORD_DATE, ValueKind.STRING,
                        "2026-01-02"));

        assertIssue(ledger, IssueCode.PARAMETER_VALUE_KIND_MISMATCH);
    }

    /**
     * Checks the ledger rule scenario: entry level parameter java value mismatch is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEntryLevelParameterJavaValueMismatchIsReported()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);

        entry.addParameter(LedgerParameter.unchecked(LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        ValueKind.BOOLEAN, "true"));

        assertIssue(ledger, IssueCode.PARAMETER_VALUE_KIND_MISMATCH);
    }

    /**
     * Checks the ledger rule scenario: entry level controlled parameter unknown code is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEntryLevelControlledParameterUnknownCodeIsReported()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);

        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                        "UNKNOWN_CODE"));

        assertIssue(ledger, IssueCode.PARAMETER_CODE_NOT_ALLOWED);
    }

    /**
     * Checks the ledger rule scenario: entry level ex-date does not use posting security rule.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEntryLevelExDateDoesNotUsePostingSecurityRule()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);

        entry.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2020, 9, 28, 0, 0)));

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: fixed income money posting types pass generic currency validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testFixedIncomeMoneyPostingTypesPassGenericCurrencyValidation()
    {
        for (var type : new LedgerPostingType[] { LedgerPostingType.ACCRUED_INTEREST,
                        LedgerPostingType.PRINCIPAL_REDEMPTION })
        {
            var ledger = createStandardLedger();
            var posting = ledger.getEntries().get(0).getPostings().get(0);

            posting.setType(type);

            assertOK(LedgerStructuralValidator.validate(ledger));
        }
    }

    /**
     * Checks the ledger rule scenario: fixed income money posting types require currency.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testFixedIncomeMoneyPostingTypesRequireCurrency()
    {
        for (var type : new LedgerPostingType[] { LedgerPostingType.ACCRUED_INTEREST,
                        LedgerPostingType.PRINCIPAL_REDEMPTION })
        {
            var ledger = createStandardLedger();
            var posting = ledger.getEntries().get(0).getPostings().get(0);

            posting.setType(type);
            posting.setCurrency(null);

            assertIssue(ledger, IssueCode.POSTING_CURRENCY_REQUIRED);
        }
    }

    /**
     * Checks the ledger rule scenario: posting type currency policy drives generic validation.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testPostingTypeCurrencyPolicyDrivesGenericValidation()
    {
        for (var type : LedgerPostingType.values())
        {
            var ledger = createStandardLedger();
            var posting = ledger.getEntries().get(0).getPostings().get(0);

            posting.setType(type);
            posting.setCurrency(null);
            posting.setSecurity(new Security("Security", CurrencyUnit.EUR));

            var result = LedgerStructuralValidator.validate(ledger);

            if (type.requiresCurrency())
                assertTrue(type.name(), result.hasIssue(IssueCode.POSTING_CURRENCY_REQUIRED));
            else
                assertTrue(type.name(), !result.hasIssue(IssueCode.POSTING_CURRENCY_REQUIRED));
        }
    }

    /**
     * Checks the ledger rule scenario: negative amount facts are rejected for standard types.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testNegativeAmountFactsAreRejectedForStandardTypes()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setAmount(-1L);

        assertIssue(ledger, IssueCode.SIGNED_FACT_NOT_ALLOWED);
    }

    /**
     * Checks the ledger rule scenario: negative share facts are rejected for standard types.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testNegativeShareFactsAreRejectedForStandardTypes()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setShares(-1L);

        assertIssue(ledger, IssueCode.SIGNED_FACT_NOT_ALLOWED);
    }

    /**
     * Checks the ledger rule scenario: missing currency on money bearing posting is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testMissingCurrencyOnMoneyBearingPostingIsReported()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setCurrency(null);

        assertIssue(ledger, IssueCode.POSTING_CURRENCY_REQUIRED);

        var format = findIssue(ledger, IssueCode.POSTING_CURRENCY_REQUIRED).format();

        assertTrue(format, format.contains("\n  Entry:\n"));
        assertTrue(format, format.contains("UUID: entry-1"));
        assertTrue(format, format.contains("Type: DEPOSIT"));
        assertTrue(format, format.contains("\n  Posting:\n"));
        assertTrue(format, format.contains("UUID: posting-1"));
        assertTrue(format, format.contains("Type: CASH"));
        assertTrue(format, format.contains("Currency: <missing>"));
    }

    /**
     * Checks the ledger rule scenario: security posting without security is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSecurityPostingWithoutSecurityIsReported()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setType(LedgerPostingType.SECURITY);

        assertIssue(ledger, IssueCode.POSTING_SECURITY_REQUIRED);

        var format = findIssue(ledger, IssueCode.POSTING_SECURITY_REQUIRED).format();

        assertTrue(format, format.contains("UUID: entry-1"));
        assertTrue(format, format.contains("UUID: posting-1"));
        assertTrue(format, format.contains("Type: SECURITY"));
        assertTrue(format, format.contains("Security: <missing>"));
    }

    /**
     * Checks the ledger rule scenario: dividend entry without security is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDividendEntryWithoutSecurityIsReported()
    {
        var ledger = createStandardLedger();
        var entry = ledger.getEntries().get(0);

        entry.setType(LedgerEntryType.DIVIDENDS);

        assertIssue(ledger, IssueCode.DIVIDEND_SECURITY_REQUIRED);
    }

    /**
     * Checks the ledger rule scenario: negative exchange rate is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testNegativeExchangeRateIsReported()
    {
        var ledger = createStandardLedger();
        var posting = ledger.getEntries().get(0).getPostings().get(0);

        posting.setExchangeRate(BigDecimal.valueOf(-1));

        assertIssue(ledger, IssueCode.POSTING_EXCHANGE_RATE_POSITIVE);
        assertTrue(findIssue(ledger, IssueCode.POSTING_EXCHANGE_RATE_POSITIVE).getMessage()
                        .contains(LedgerDiagnosticCode.LEDGER_FOREX_002.prefix()));
    }

    /**
     * Checks the ledger rule scenario: fixed shape missing required projection role is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testFixedShapeMissingRequiredProjectionRoleIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.BUY);

        entry.addPosting(validPosting("posting-1"));
        entry.addProjectionRef(accountProjection("projection-1"));
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.FIXED_SHAPE_PROJECTION_ROLE_REQUIRED);
    }

    /**
     * Checks the ledger rule scenario: fixed shape unexpected projection role is reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testFixedShapeUnexpectedProjectionRoleIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);

        entry.addPosting(validPosting("posting-1"));
        entry.addProjectionRef(portfolioProjection("projection-1"));
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.FIXED_SHAPE_PROJECTION_ROLE_NOT_ALLOWED);
    }

    /**
     * Checks the ledger rule scenario: signed posting facts are accepted for spin off.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSignedPostingFactsAreAcceptedForSpinOff()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var posting = validPosting("posting-1");
        var projection = portfolioProjection("projection-1");

        posting.setShares(-1L);
        entry.addPosting(posting);
        projection.setPrimaryPostingUUID(posting.getUUID());
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: signed posting facts are accepted for other ledger native targeted types.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSignedPostingFactsAreAcceptedForOtherLedgerNativeTargetedTypes()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.STOCK_DIVIDEND);
        var posting = validPosting("posting-1");
        var projection = portfolioProjection("projection-1");

        posting.setShares(-1L);
        entry.addPosting(posting);
        projection.setPrimaryPostingUUID(posting.getUUID());
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertOK(LedgerStructuralValidator.validate(ledger));
    }

    /**
     * Checks the ledger rule scenario: invalid posting group uuidis reported.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testInvalidPostingGroupUUIDIsReported()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.SPIN_OFF);
        var projection = portfolioProjection("projection-1");

        entry.addPosting(validPosting("posting-1"));
        projection.setPrimaryPostingUUID("posting-1");
        projection.setPostingGroupUUID("group-1");
        entry.addProjectionRef(projection);
        ledger.addEntry(entry);

        assertIssue(ledger, IssueCode.POSTING_GROUP_REF_NOT_FOUND);
    }

    private Ledger createStandardLedger()
    {
        var ledger = new Ledger();
        var entry = validEntry("entry-1", LedgerEntryType.DEPOSIT);

        entry.addPosting(validPosting("posting-1"));
        entry.addProjectionRef(accountProjection("projection-1"));
        ledger.addEntry(entry);

        return ledger;
    }

    private LedgerEntry validEntry(String uuid, LedgerEntryType type)
    {
        var entry = new LedgerEntry(uuid);

        entry.setType(type);
        entry.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));

        return entry;
    }

    private LedgerPosting validPosting(String uuid)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(LedgerPostingType.CASH);
        posting.setAmount(100L);
        posting.setCurrency(CurrencyUnit.EUR);

        return posting;
    }

    private LedgerProjectionRef accountProjection(String uuid)
    {
        var projection = new LedgerProjectionRef(uuid);

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(new Account());

        return projection;
    }

    private Account account()
    {
        var account = new Account();

        account.setCurrencyCode(CurrencyUnit.EUR);

        return account;
    }

    private LedgerProjectionRef portfolioProjection(String uuid)
    {
        var projection = new LedgerProjectionRef(uuid);

        projection.setRole(LedgerProjectionRole.DELIVERY_INBOUND);
        projection.setPortfolio(new Portfolio());

        return projection;
    }

    private void assertIssue(Ledger ledger, IssueCode code)
    {
        assertTrue(LedgerStructuralValidator.validate(ledger).hasIssue(code));
    }

    private LedgerStructuralValidator.ValidationIssue findIssue(Ledger ledger, IssueCode code)
    {
        return LedgerStructuralValidator.validate(ledger).getIssues().stream() //
                        .filter(issue -> issue.getCode() == code) //
                        .findFirst() //
                        .orElseThrow();
    }

    private void assertOK(LedgerStructuralValidator.ValidationResult result)
    {
        assertTrue(result.getIssues().toString(), result.isOK());
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException
    {
        Field field = target.getClass().getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }

    private ValueKind wrongValueKind(LedgerParameterType type)
    {
        for (var valueKind : ValueKind.values())
            if (!type.supportsValueKind(valueKind))
                return valueKind;

        throw new AssertionError(type);
    }

    private Object valueFor(ValueKind valueKind)
    {
        return switch (valueKind)
        {
            case STRING -> "value";
            case DECIMAL -> BigDecimal.ONE;
            case LONG -> Long.valueOf(1);
            case MONEY -> Money.of(CurrencyUnit.EUR, 1);
            case SECURITY -> new Security("Security", CurrencyUnit.EUR);
            case ACCOUNT -> new Account();
            case PORTFOLIO -> new Portfolio();
            case BOOLEAN -> Boolean.TRUE;
            case LOCAL_DATE -> LocalDate.of(2020, 9, 28);
            case LOCAL_DATE_TIME -> LocalDateTime.of(2020, 9, 28, 0, 0);
        };
    }
}
