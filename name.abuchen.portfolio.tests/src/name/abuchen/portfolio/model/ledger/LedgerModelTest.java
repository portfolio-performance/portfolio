package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.CostAllocationMethod;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.FractionTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterCodeDomain;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.QuotationStyle;
import name.abuchen.portfolio.model.ledger.configuration.RoundingModeCode;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

/**
 * Tests low-level ledger model behavior used by higher-level transaction flows.
 * These tests make sure copying, graph links, and core model fields keep ledger data consistent.
 */
@SuppressWarnings("nls")
public class LedgerModelTest
{
    /**
     * Checks the Ledger-V6 scenario: ledger entry carries identity and minimum fields.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerEntryCarriesIdentityAndMinimumFields()
    {
        var dateTime = LocalDateTime.of(2020, 9, 28, 0, 0);
        var updatedAt = Instant.parse("2026-01-02T03:04:05Z");
        var parameter = LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE, "reference");
        var posting = new LedgerPosting("posting-1");
        var entry = new LedgerEntry("entry-1");

        entry.setType(LedgerEntryType.BUY);
        entry.setDateTime(dateTime);
        entry.setNote("note");
        entry.setSource("source");
        entry.addParameter(parameter);
        entry.addPosting(posting);
        entry.setUpdatedAt(updatedAt);

        assertThat(entry.getUUID(), is("entry-1"));
        assertThat(entry.getType(), is(LedgerEntryType.BUY));
        assertThat(entry.getDateTime(), is(dateTime));
        assertThat(entry.getNote(), is("note"));
        assertThat(entry.getSource(), is("source"));
        assertThat(entry.getUpdatedAt(), is(updatedAt));
        assertThat(entry.getParameters(), is(List.of(parameter)));
        assertThat(entry.getPostings(), is(List.of(posting)));
        assertThrows(UnsupportedOperationException.class, () -> entry.getParameters().add(parameter));
        assertThrows(UnsupportedOperationException.class, () -> entry.getPostings().add(new LedgerPosting()));
        assertTrue(entry.removeParameter(parameter));
        assertTrue(entry.getParameters().isEmpty());
    }

    /**
     * Checks the Ledger-V6 scenario: generated uuids are independent.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testGeneratedUUIDsAreIndependent()
    {
        var entry = new LedgerEntry();
        var otherEntry = new LedgerEntry();
        var posting = new LedgerPosting();

        assertNotEquals(entry.getUUID(), otherEntry.getUUID());
        assertNotEquals(entry.getUUID(), posting.getUUID());
    }

    /**
     * Checks the Ledger-V6 scenario: ledger collects entries without owning client integration.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerCollectsEntriesWithoutOwningClientIntegration()
    {
        var ledger = new Ledger();
        var entry = new LedgerEntry("entry-1");

        ledger.addEntry(entry);

        assertThat(ledger.getEntries(), is(List.of(entry)));
        assertThrows(UnsupportedOperationException.class, () -> ledger.getEntries().add(new LedgerEntry()));
        assertTrue(ledger.removeEntry(entry));
        assertTrue(ledger.getEntries().isEmpty());
    }

    /**
     * Checks the Ledger-V6 scenario: ledger posting carries fields and forex as posting data.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerPostingCarriesFieldsAndForexAsPostingData()
    {
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Siemens", CurrencyUnit.EUR);
        var exchangeRate = BigDecimal.valueOf(1.0875);
        var posting = new LedgerPosting("posting-1");

        posting.setType(LedgerPostingType.SECURITY);
        posting.setAmount(123456L);
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setForexAmount(134259L);
        posting.setForexCurrency(CurrencyUnit.USD);
        posting.setExchangeRate(exchangeRate);
        posting.setSecurity(security);
        posting.setShares(500000L);
        posting.setAccount(account);
        posting.setPortfolio(portfolio);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(LedgerPostingDirection.INBOUND);
        posting.setCorporateActionLeg(CorporateActionLeg.TARGET_SECURITY);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey("security-leg");
        posting.setLocalKey("target");

        assertThat(posting.getUUID(), is("posting-1"));
        assertThat(posting.getType(), is(LedgerPostingType.SECURITY));
        assertThat(posting.getAmount(), is(123456L));
        assertThat(posting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting.getForexAmount(), is(134259L));
        assertThat(posting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting.getExchangeRate(), is(exchangeRate));
        assertSame(security, posting.getSecurity());
        assertThat(posting.getShares(), is(500000L));
        assertSame(account, posting.getAccount());
        assertSame(portfolio, posting.getPortfolio());
        assertThat(posting.getSemanticRole(), is(LedgerPostingSemanticRole.SECURITY));
        assertThat(posting.getDirection(), is(LedgerPostingDirection.INBOUND));
        assertThat(posting.getCorporateActionLeg(), is(CorporateActionLeg.TARGET_SECURITY));
        assertThat(posting.getUnitRole(), is(LedgerPostingUnitRole.PRIMARY));
        assertThat(posting.getGroupKey(), is("security-leg"));
        assertThat(posting.getLocalKey(), is("target"));
    }

    /**
     * Checks the Phase-1 scenario: semantic posting vocabulary is typed and additive.
     * Projection refs remain the active materialization source in this phase.
     */
    @Test
    public void testLedgerPostingSemanticVocabularyIsTypedAndAdditive()
    {
        var posting = new LedgerPosting("posting-1");

        assertThat(posting.getSemanticRole(), is((LedgerPostingSemanticRole) null));
        assertThat(posting.getDirection(), is((LedgerPostingDirection) null));
        assertThat(posting.getCorporateActionLeg(), is((CorporateActionLeg) null));
        assertThat(posting.getUnitRole(), is((LedgerPostingUnitRole) null));
        assertThat(posting.getGroupKey(), is((String) null));
        assertThat(posting.getLocalKey(), is((String) null));

        assertThat(EnumSet.allOf(LedgerPostingSemanticRole.class),
                        is(EnumSet.of(LedgerPostingSemanticRole.CASH, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingSemanticRole.RIGHT, LedgerPostingSemanticRole.BOND,
                                        LedgerPostingSemanticRole.CASH_COMPENSATION,
                                        LedgerPostingSemanticRole.ACCRUED_INTEREST,
                                        LedgerPostingSemanticRole.PRINCIPAL_REDEMPTION,
                                        LedgerPostingSemanticRole.FEE, LedgerPostingSemanticRole.TAX,
                                        LedgerPostingSemanticRole.GROSS_VALUE,
                                        LedgerPostingSemanticRole.FOREX_CONTEXT)));
        assertThat(EnumSet.allOf(LedgerPostingDirection.class),
                        is(EnumSet.of(LedgerPostingDirection.INBOUND, LedgerPostingDirection.OUTBOUND,
                                        LedgerPostingDirection.NEUTRAL)));
        assertThat(EnumSet.allOf(LedgerPostingUnitRole.class),
                        is(EnumSet.of(LedgerPostingUnitRole.PRIMARY, LedgerPostingUnitRole.FEE,
                                        LedgerPostingUnitRole.TAX, LedgerPostingUnitRole.GROSS_VALUE,
                                        LedgerPostingUnitRole.FOREX_CONTEXT)));
    }

    /**
     * Checks the Ledger-V6 scenario: ex-date is local date time ledger parameter.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testExDateIsLocalDateTimeLedgerParameter()
    {
        var exDate = LocalDateTime.of(2020, 9, 28, 0, 0);
        var parameter = LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, exDate);
        var posting = new LedgerPosting();

        posting.addParameter(parameter);

        assertFalse(Arrays.stream(LedgerEntry.class.getDeclaredFields()).anyMatch(f -> "exDate".equals(f.getName())));
        assertThat(posting.getParameters(), is(List.of(parameter)));
        assertThat(parameter.getType(), is(LedgerParameterType.EX_DATE));
        assertThat(parameter.getValueKind(), is(ValueKind.LOCAL_DATE_TIME));
        assertThat(parameter.getValue(), is(exDate));
        assertThrows(UnsupportedOperationException.class, () -> posting.getParameters().clear());
    }

    /**
     * Checks the Ledger-V6 scenario: ledger entry and posting parameters are independent.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerEntryAndPostingParametersAreIndependent()
    {
        var entryParameter = LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF.getCode());
        var postingParameter = LedgerParameter.ofString(LedgerParameterType.FEE_REASON,
                        FeeReason.BROKER_FEE.getCode());
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();

        entry.addParameter(entryParameter);
        posting.addParameter(postingParameter);
        entry.addPosting(posting);

        assertThat(entry.getParameters(), is(List.of(entryParameter)));
        assertThat(posting.getParameters(), is(List.of(postingParameter)));
        assertFalse(entry.getParameters().contains(postingParameter));
        assertFalse(posting.getParameters().contains(entryParameter));
    }

    /**
     * Checks the Ledger-V6 scenario: ledger parameter value kinds are explicit.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerParameterValueKindsAreExplicit()
    {
        var security = new Security("Siemens Energy", CurrencyUnit.EUR);
        var expectedKinds = EnumSet.of(ValueKind.STRING, ValueKind.DECIMAL, ValueKind.LONG, ValueKind.MONEY,
                        ValueKind.SECURITY, ValueKind.ACCOUNT, ValueKind.PORTFOLIO, ValueKind.BOOLEAN,
                        ValueKind.LOCAL_DATE, ValueKind.LOCAL_DATE_TIME);

        assertThat(EnumSet.allOf(ValueKind.class), is(expectedKinds));
        assertValueKindPolicy(ValueKind.STRING, String.class);
        assertValueKindPolicy(ValueKind.DECIMAL, BigDecimal.class);
        assertValueKindPolicy(ValueKind.LONG, Long.class);
        assertValueKindPolicy(ValueKind.MONEY, Money.class);
        assertValueKindPolicy(ValueKind.SECURITY, Security.class);
        assertValueKindPolicy(ValueKind.ACCOUNT, Account.class);
        assertValueKindPolicy(ValueKind.PORTFOLIO, Portfolio.class);
        assertValueKindPolicy(ValueKind.BOOLEAN, Boolean.class);
        assertValueKindPolicy(ValueKind.LOCAL_DATE, LocalDate.class);
        assertValueKindPolicy(ValueKind.LOCAL_DATE_TIME, LocalDateTime.class);

        assertThat(LedgerParameter.ofString(LedgerParameterType.FEE_REASON,
                        FeeReason.BROKER_FEE.getCode()).getValueKind(),
                        is(ValueKind.STRING));
        assertThat(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR, BigDecimal.ONE)
                        .getValueKind(), is(ValueKind.DECIMAL));
        assertSame(security,
                        LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY, security)
                                        .getValue());
        assertThat(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.SOURCE_SECURITY.getCode())
                        .getValueKind(), is(ValueKind.STRING));
        assertThat(LedgerParameter.ofString(LedgerParameterType.CASH_COMPENSATION_KIND,
                        CashCompensationKind.CASH_IN_LIEU.getCode())
                        .getValueKind(), is(ValueKind.STRING));
    }

    /**
     * Checks the Ledger-V6 scenario: ledger parameter type policies are explicit.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerParameterTypePoliciesAreExplicit()
    {
        assertParameterTypePolicy(LedgerParameterType.EX_DATE, LedgerParameterType.Scope.GENERAL,
                        ValueKind.LOCAL_DATE_TIME);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.STRING,
                        LedgerParameterType.CORPORATE_ACTION_LEG,
                        LedgerParameterType.CASH_COMPENSATION_KIND, LedgerParameterType.FEE_REASON,
                        LedgerParameterType.TAX_REASON, LedgerParameterType.CORPORATE_ACTION_KIND,
                        LedgerParameterType.CORPORATE_ACTION_SUBTYPE,
                        LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.EVENT_STAGE,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.ROUNDING_MODE,
                        LedgerParameterType.COST_ALLOCATION_METHOD, LedgerParameterType.QUOTATION_STYLE);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE,
                        LedgerParameterType.RECORD_DATE, LedgerParameterType.PAYMENT_DATE,
                        LedgerParameterType.EFFECTIVE_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.ELECTION_DEADLINE,
                        LedgerParameterType.INTEREST_PERIOD_START,
                        LedgerParameterType.INTEREST_PERIOD_END);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.SECURITY,
                        LedgerParameterType.SOURCE_SECURITY, LedgerParameterType.TARGET_SECURITY,
                        LedgerParameterType.RIGHT_SECURITY);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.ACCOUNT,
                        LedgerParameterType.SOURCE_ACCOUNT, LedgerParameterType.TARGET_ACCOUNT,
                        LedgerParameterType.CASH_ACCOUNT);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.PORTFOLIO,
                        LedgerParameterType.SOURCE_PORTFOLIO, LedgerParameterType.TARGET_PORTFOLIO);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.DECIMAL,
                        LedgerParameterType.RATIO_NUMERATOR, LedgerParameterType.RATIO_DENOMINATOR,
                        LedgerParameterType.FRACTION_QUANTITY, LedgerParameterType.CONVERSION_RATIO,
                        LedgerParameterType.PARTIAL_REDEMPTION_FACTOR,
                        LedgerParameterType.COUPON_RATE, LedgerParameterType.REDEMPTION_PRICE_PERCENT,
                        LedgerParameterType.SOURCE_COST_PERCENT,
                        LedgerParameterType.TARGET_COST_PERCENT,
                        LedgerParameterType.AFFECTED_SOURCE_QUANTITY);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.MONEY,
                        LedgerParameterType.NOMINAL_VALUE, LedgerParameterType.SUBSCRIPTION_PRICE,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                        LedgerParameterType.ACCRUED_INTEREST_AMOUNT);
        assertParameterTypePolicies(LedgerParameterType.Scope.CORPORATE_ACTION, ValueKind.BOOLEAN,
                        LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        LedgerParameterType.TAXABLE_DISTRIBUTION,
                        LedgerParameterType.MANUAL_VALUATION_OVERRIDE,
                        LedgerParameterType.SAME_SECURITY_AS_SOURCE, LedgerParameterType.FRACTION_ROUNDED,
                        LedgerParameterType.RECLAIMABLE_TAX, LedgerParameterType.WITHHOLDING_TAX,
                        LedgerParameterType.TRANSACTION_TAX, LedgerParameterType.STAMP_DUTY);

        assertTrue(LedgerParameterType.EX_DATE.isGeneral());
        assertFalse(LedgerParameterType.EX_DATE.isCorporateAction());
        assertTrue(LedgerParameterType.CORPORATE_ACTION_LEG.isCorporateAction());
        assertFalse(LedgerParameterType.CORPORATE_ACTION_LEG.isGeneral());
    }

    /**
     * Checks the Ledger-V6 scenario: posting type structural policies are explicit.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testPostingTypeStructuralPoliciesAreExplicit()
    {
        assertPostingTypePolicy(LedgerPostingType.CASH, LedgerPostingType.ComponentClass.CASH, true, true, false,
                        false, false, true, false, true);
        assertPostingTypePolicy(LedgerPostingType.SECURITY, LedgerPostingType.ComponentClass.SECURITY, true, true, true,
                        true, true, false, true, true);
        assertPostingTypePolicy(LedgerPostingType.CASH_COMPENSATION, LedgerPostingType.ComponentClass.COMPENSATION,
                        true, true, false, false, false, true, false, true);
        assertPostingTypePolicy(LedgerPostingType.FEE, LedgerPostingType.ComponentClass.FEE, true, true, false, false,
                        false, false, false, true);
        assertPostingTypePolicy(LedgerPostingType.TAX, LedgerPostingType.ComponentClass.TAX, true, true, false, false,
                        false, false, false, true);
        assertPostingTypePolicy(LedgerPostingType.GROSS_VALUE, LedgerPostingType.ComponentClass.GROSS_VALUE, true,
                        true, false, false, false, false, false, true);
        assertPostingTypePolicy(LedgerPostingType.FOREX, LedgerPostingType.ComponentClass.FOREX, false, false, false,
                        false, false, false, false, true);
        assertPostingTypePolicy(LedgerPostingType.RIGHT, LedgerPostingType.ComponentClass.RIGHT, false, false, true,
                        false, true, false, true, false);
        assertPostingTypePolicy(LedgerPostingType.BOND, LedgerPostingType.ComponentClass.BOND, false, false, true,
                        false, true, false, true, false);
        assertPostingTypePolicy(LedgerPostingType.ACCRUED_INTEREST,
                        LedgerPostingType.ComponentClass.ACCRUED_INTEREST, true, true, false, false, false, false,
                        false, true);
        assertPostingTypePolicy(LedgerPostingType.PRINCIPAL_REDEMPTION,
                        LedgerPostingType.ComponentClass.PRINCIPAL_REDEMPTION, true, true, false, false, false, false,
                        false, true);

        var componentClasses = EnumSet.noneOf(LedgerPostingType.ComponentClass.class);

        for (var type : LedgerPostingType.values())
            componentClasses.add(type.getComponentClass());

        assertThat(componentClasses, is(EnumSet.allOf(LedgerPostingType.ComponentClass.class)));
    }

    /**
     * Checks the Ledger-V6 scenario: ledger parameter code domains are explicit.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerParameterCodeDomainsAreExplicit()
    {
        assertCodeDomain(LedgerParameterType.CORPORATE_ACTION_LEG, LedgerParameterCodeDomain.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.SOURCE_SECURITY, CorporateActionLeg.TARGET_SECURITY,
                        CorporateActionLeg.DISTRIBUTED_SECURITY, CorporateActionLeg.RIGHT_SECURITY,
                        CorporateActionLeg.CASH_COMPENSATION, CorporateActionLeg.CASH_IN_LIEU,
                        CorporateActionLeg.FEE, CorporateActionLeg.TAX,
                        CorporateActionLeg.ACCRUED_INTEREST, CorporateActionLeg.PRINCIPAL,
                        CorporateActionLeg.REDEMPTION, CorporateActionLeg.CONVERSION_SOURCE,
                        CorporateActionLeg.CONVERSION_TARGET, CorporateActionLeg.OTHER);
        assertCodeDomain(LedgerParameterType.CORPORATE_ACTION_KIND,
                        LedgerParameterCodeDomain.CORPORATE_ACTION_KIND,
                        CorporateActionKind.STOCK_DIVIDEND, CorporateActionKind.SPIN_OFF,
                        CorporateActionKind.BONUS_ISSUE, CorporateActionKind.RIGHTS_DISTRIBUTION,
                        CorporateActionKind.COUPON_PAYMENT, CorporateActionKind.PIK_INTEREST,
                        CorporateActionKind.DEFAULTED_INTEREST, CorporateActionKind.MATURITY,
                        CorporateActionKind.PARTIAL_REDEMPTION, CorporateActionKind.CALL,
                        CorporateActionKind.PUT, CorporateActionKind.CONVERSION,
                        CorporateActionKind.EXCHANGE, CorporateActionKind.RESTRUCTURING,
                        CorporateActionKind.DEFAULT);
        assertCodeDomain(LedgerParameterType.CORPORATE_ACTION_SUBTYPE,
                        LedgerParameterCodeDomain.CORPORATE_ACTION_SUBTYPE, CorporateActionSubtype.STANDARD,
                        CorporateActionSubtype.OPTIONAL, CorporateActionSubtype.MANDATORY,
                        CorporateActionSubtype.CASH_AND_STOCK, CorporateActionSubtype.OTHER);
        assertCodeDomain(LedgerParameterType.EVENT_STAGE, LedgerParameterCodeDomain.EVENT_STAGE,
                        EventStage.ANNOUNCED, EventStage.RECORD,
                        EventStage.EX_DATE, EventStage.PAYMENT,
                        EventStage.ISSUED, EventStage.EXERCISED,
                        EventStage.SOLD, EventStage.EXPIRED,
                        EventStage.SETTLED, EventStage.OTHER);
        assertCodeDomain(LedgerParameterType.CASH_COMPENSATION_KIND,
                        LedgerParameterCodeDomain.CASH_COMPENSATION_KIND,
                        CashCompensationKind.CASH_IN_LIEU,
                        CashCompensationKind.FRACTIONAL_SHARE_COMPENSATION,
                        CashCompensationKind.ROUNDING_COMPENSATION,
                        CashCompensationKind.OTHER);
        assertCodeDomain(LedgerParameterType.FRACTION_TREATMENT,
                        LedgerParameterCodeDomain.FRACTION_TREATMENT, FractionTreatment.NONE,
                        FractionTreatment.CASH_IN_LIEU, FractionTreatment.ROUND_DOWN,
                        FractionTreatment.ROUND_UP, FractionTreatment.DROP,
                        FractionTreatment.OTHER);
        assertCodeDomain(LedgerParameterType.ROUNDING_MODE, LedgerParameterCodeDomain.ROUNDING_MODE,
                        RoundingModeCode.NONE, RoundingModeCode.FLOOR,
                        RoundingModeCode.CEILING, RoundingModeCode.HALF_UP,
                        RoundingModeCode.HALF_EVEN, RoundingModeCode.OTHER);
        assertCodeDomain(LedgerParameterType.COST_ALLOCATION_METHOD,
                        LedgerParameterCodeDomain.COST_ALLOCATION_METHOD, CostAllocationMethod.NONE,
                        CostAllocationMethod.FMV_RATIO,
                        CostAllocationMethod.MANUAL_PERCENTAGE,
                        CostAllocationMethod.ZERO_COST_TARGET,
                        CostAllocationMethod.CARRY_OVER, CostAllocationMethod.OTHER);
        assertCodeDomain(LedgerParameterType.QUOTATION_STYLE,
                        LedgerParameterCodeDomain.QUOTATION_STYLE, QuotationStyle.UNIT,
                        QuotationStyle.PERCENT, QuotationStyle.NOMINAL,
                        QuotationStyle.OTHER);
        assertCodeDomain(LedgerParameterType.FEE_REASON, LedgerParameterCodeDomain.FEE_REASON,
                        FeeReason.BROKER_FEE, FeeReason.EXCHANGE_FEE,
                        FeeReason.CORPORATE_ACTION_FEE, FeeReason.STAMP_DUTY,
                        FeeReason.OTHER);
        assertCodeDomain(LedgerParameterType.TAX_REASON, LedgerParameterCodeDomain.TAX_REASON,
                        TaxReason.WITHHOLDING_TAX, TaxReason.CAPITAL_GAINS_TAX,
                        TaxReason.TRANSACTION_TAX, TaxReason.STAMP_DUTY,
                        TaxReason.RECLAIMABLE_TAX, TaxReason.OTHER);

        assertFalse(LedgerParameterType.EVENT_REFERENCE.hasCodeDomain());

        for (var type : LedgerParameterType.values())
            if (type.hasCodeDomain())
                assertThat(type.getExpectedValueKind(), is(ValueKind.STRING));

        for (var domain : LedgerParameterCodeDomain.values())
        {
            assertFalse(domain.getAllowedCodes().isEmpty());
            assertThat(domain.getAllowedCodes().stream().distinct().count(), is((long) domain.getAllowedCodes().size()));
            assertTrue(domain.getAllowedCodes().stream().allMatch(code -> code.equals(code.toUpperCase(Locale.ROOT))));
        }
    }

    /**
     * Checks the Ledger-V6 scenario: ledger parameter factories validate parameter type.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerParameterFactoriesValidateParameterType()
    {
        var exDate = LocalDateTime.of(2020, 9, 28, 0, 0);
        var money = Money.of(CurrencyUnit.EUR, 500L);

        assertThat(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, exDate).getValue(),
                        is(exDate));
        assertThat(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.SOURCE_SECURITY.getCode()).getValue(),
                        is(CorporateActionLeg.SOURCE_SECURITY.getCode()));
        assertThat(LedgerParameter.ofString(LedgerParameterType.CASH_COMPENSATION_KIND,
                        CashCompensationKind.CASH_IN_LIEU.getCode()).getValue(),
                        is(CashCompensationKind.CASH_IN_LIEU.getCode()));
        assertThat(LedgerParameter.ofString(LedgerParameterType.FEE_REASON,
                        FeeReason.BROKER_FEE.getCode()).getValue(),
                        is(FeeReason.BROKER_FEE.getCode()));
        assertThat(LedgerParameter.ofLocalDate(LedgerParameterType.RECORD_DATE,
                        LocalDate.of(2026, 1, 2)).getValue(), is(LocalDate.of(2026, 1, 2)));
        assertThat(LedgerParameter.ofBoolean(LedgerParameterType.CASH_IN_LIEU_APPLIED, Boolean.TRUE)
                        .getValue(), is(Boolean.TRUE));
        assertThat(LedgerParameter.ofMoney(LedgerParameterType.NOMINAL_VALUE, money).getValue(),
                        is(money));

        assertFactoryRejects(LedgerParameterType.EX_DATE, ValueKind.MONEY,
                        () -> LedgerParameter.ofMoney(LedgerParameterType.EX_DATE, money));
        assertFactoryRejects(LedgerParameterType.RATIO_NUMERATOR, ValueKind.STRING,
                        () -> LedgerParameter.ofString(LedgerParameterType.RATIO_NUMERATOR, "1"));
        assertFactoryRejects(LedgerParameterType.FEE_REASON, ValueKind.BOOLEAN,
                        () -> LedgerParameter.ofBoolean(LedgerParameterType.FEE_REASON, Boolean.TRUE));
        assertFactoryRejects(LedgerParameterType.EX_DATE, ValueKind.LOCAL_DATE,
                        () -> LedgerParameter.ofLocalDate(LedgerParameterType.EX_DATE,
                                        LocalDate.of(2020, 9, 28)));
        assertFactoryRejects(LedgerParameterType.RECORD_DATE, ValueKind.LOCAL_DATE_TIME,
                        () -> LedgerParameter.ofLocalDateTime(LedgerParameterType.RECORD_DATE, exDate));
        assertFactoryRejects(LedgerParameterType.CASH_IN_LIEU_APPLIED, ValueKind.STRING,
                        () -> LedgerParameter.ofString(LedgerParameterType.CASH_IN_LIEU_APPLIED,
                                        "true"));
    }

    /**
     * Checks the Ledger-V6 scenario: ledger entry type policies separate standard and ledger native shapes.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerEntryTypePoliciesSeparateStandardAndLedgerNativeShapes()
    {
        var corporateActionFamilies = EnumSet.of(LedgerEntryType.CORPORATE_ACTION);
        var standardFamilies = EnumSet.complementOf(corporateActionFamilies);

        standardFamilies.forEach(this::assertStandardLegacyShape);
        corporateActionFamilies.forEach(this::assertLedgerNativeTargetedShape);

        assertTrue(LedgerEntryType.CORPORATE_ACTION.requiresTargetedDerivedDescriptors());
        assertTrue(LedgerEntryType.CORPORATE_ACTION.usesSignedTargetedProjectionFacts());
    }

    /**
     * Checks the Ledger-V6 scenario: ledger entry type does not use positional boolean policy arguments.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerEntryTypeDoesNotUsePositionalBooleanPolicyArguments()
    {
        assertFalse(Arrays.stream(LedgerEntryType.class.getDeclaredFields()).anyMatch(f -> f.getType() == boolean.class));
        assertFalse(Arrays.stream(LedgerEntryType.class.getDeclaredConstructors())
                        .flatMap(c -> Arrays.stream(c.getParameterTypes())).anyMatch(t -> t == boolean.class));
        assertTrue(Arrays.stream(LedgerEntryType.class.getDeclaredConstructors())
                        .flatMap(c -> Arrays.stream(c.getParameterTypes())).anyMatch(t -> t == String.class));
        assertFalse(Arrays.stream(LedgerEntryType.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().contains("Protobuf")));
    }

    /**
     * Checks the Ledger-V6 scenario: ledger posting type does not use positional boolean policy arguments.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testLedgerPostingTypeDoesNotUsePositionalBooleanPolicyArguments()
    {
        assertFalse(Arrays.stream(LedgerPostingType.class.getDeclaredFields())
                        .anyMatch(f -> f.getType() == boolean.class));
        assertFalse(Arrays.stream(LedgerPostingType.class.getDeclaredConstructors())
                        .flatMap(c -> Arrays.stream(c.getParameterTypes())).anyMatch(t -> t == boolean.class));
    }

    /**
     * Checks the Ledger-V6 scenario: enum skeleton contains required posting and projection shapes.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testEnumSkeletonContainsRequiredPostingAndProjectionShapes()
    {
        assertThat(EnumSet.allOf(LedgerPostingType.class),
                        is(EnumSet.of(LedgerPostingType.CASH, LedgerPostingType.SECURITY, LedgerPostingType.FEE,
                                        LedgerPostingType.TAX, LedgerPostingType.GROSS_VALUE,
                                        LedgerPostingType.FOREX, LedgerPostingType.CASH_COMPENSATION,
                                        LedgerPostingType.RIGHT, LedgerPostingType.BOND,
                                        LedgerPostingType.ACCRUED_INTEREST,
                                        LedgerPostingType.PRINCIPAL_REDEMPTION)));
        assertThat(EnumSet.allOf(LedgerProjectionRole.class),
                        is(EnumSet.of(LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO,
                                        LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT,
                                        LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerProjectionRole.TARGET_PORTFOLIO,
                                        LedgerProjectionRole.DELIVERY, LedgerProjectionRole.DELIVERY_INBOUND,
                                        LedgerProjectionRole.DELIVERY_OUTBOUND, LedgerProjectionRole.CASH_COMPENSATION,
                                        LedgerProjectionRole.OLD_SECURITY_LEG, LedgerProjectionRole.NEW_SECURITY_LEG)));
        assertThat(EnumSet.allOf(LedgerParameterType.class),
                        is(EnumSet.of(LedgerParameterType.EX_DATE,
                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                        LedgerParameterType.SOURCE_SECURITY,
                                        LedgerParameterType.TARGET_SECURITY,
                                        LedgerParameterType.RATIO_NUMERATOR,
                                        LedgerParameterType.RATIO_DENOMINATOR,
                                        LedgerParameterType.CASH_COMPENSATION_KIND,
                                        LedgerParameterType.FEE_REASON,
                                        LedgerParameterType.TAX_REASON,
                                        LedgerParameterType.CORPORATE_ACTION_KIND,
                                        LedgerParameterType.CORPORATE_ACTION_SUBTYPE,
                                        LedgerParameterType.EVENT_REFERENCE,
                                        LedgerParameterType.EVENT_STAGE,
                                        LedgerParameterType.RECORD_DATE,
                                        LedgerParameterType.PAYMENT_DATE,
                                        LedgerParameterType.EFFECTIVE_DATE,
                                        LedgerParameterType.SETTLEMENT_DATE,
                                        LedgerParameterType.ELECTION_DEADLINE,
                                        LedgerParameterType.INTEREST_PERIOD_START,
                                        LedgerParameterType.INTEREST_PERIOD_END,
                                        LedgerParameterType.RIGHT_SECURITY,
                                        LedgerParameterType.SOURCE_ACCOUNT,
                                        LedgerParameterType.TARGET_ACCOUNT,
                                        LedgerParameterType.CASH_ACCOUNT,
                                        LedgerParameterType.SOURCE_PORTFOLIO,
                                        LedgerParameterType.TARGET_PORTFOLIO,
                                        LedgerParameterType.FRACTION_QUANTITY,
                                        LedgerParameterType.CONVERSION_RATIO,
                                        LedgerParameterType.PARTIAL_REDEMPTION_FACTOR,
                                        LedgerParameterType.COUPON_RATE,
                                        LedgerParameterType.REDEMPTION_PRICE_PERCENT,
                                        LedgerParameterType.SOURCE_COST_PERCENT,
                                        LedgerParameterType.TARGET_COST_PERCENT,
                                        LedgerParameterType.AFFECTED_SOURCE_QUANTITY,
                                        LedgerParameterType.NOMINAL_VALUE,
                                        LedgerParameterType.SUBSCRIPTION_PRICE,
                                        LedgerParameterType.REFERENCE_PRICE,
                                        LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                                        LedgerParameterType.VALUATION_PRICE,
                                        LedgerParameterType.FAIR_MARKET_VALUE,
                                        LedgerParameterType.ACCRUED_INTEREST_AMOUNT,
                                        LedgerParameterType.FRACTION_TREATMENT,
                                        LedgerParameterType.ROUNDING_MODE,
                                        LedgerParameterType.COST_ALLOCATION_METHOD,
                                        LedgerParameterType.QUOTATION_STYLE,
                                        LedgerParameterType.CASH_IN_LIEU_APPLIED,
                                        LedgerParameterType.TAXABLE_DISTRIBUTION,
                                        LedgerParameterType.MANUAL_VALUATION_OVERRIDE,
                                        LedgerParameterType.SAME_SECURITY_AS_SOURCE,
                                        LedgerParameterType.FRACTION_ROUNDED,
                                        LedgerParameterType.RECLAIMABLE_TAX,
                                        LedgerParameterType.WITHHOLDING_TAX,
                                        LedgerParameterType.TRANSACTION_TAX,
                                        LedgerParameterType.STAMP_DUTY)));
    }

    /**
     * Checks the Ledger-V6 scenario: configurable ledger entry type codes are stable.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testConfigurableLedgerEntryTypeCodesAreStable()
    {
        assertStableCodes(Arrays.stream(LedgerEntryType.values()).map(LedgerEntryType::getCode)
                        .toArray(String[]::new));

        for (LedgerEntryType type : LedgerEntryType.values())
            assertThat(LedgerEntryType.fromCode(type.getCode()), is(type));

        var exception = assertThrows(IllegalArgumentException.class, () -> LedgerEntryType.fromCode("UNKNOWN_CODE"));

        assertTrue(exception.getMessage(), exception.getMessage().contains(LedgerDiagnosticCode.LEDGER_CORE_017.prefix()));
        assertTrue(exception.getMessage(), exception.getMessage().contains("LedgerEntryType"));
        assertTrue(exception.getMessage(), exception.getMessage().contains("UNKNOWN_CODE"));
    }

    /**
     * Checks the Ledger-V6 scenario: configurable ledger posting type codes are stable.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testConfigurableLedgerPostingTypeCodesAreStable()
    {
        assertStableCodes(Arrays.stream(LedgerPostingType.values()).map(LedgerPostingType::getCode)
                        .toArray(String[]::new));

        for (LedgerPostingType type : LedgerPostingType.values())
            assertThat(LedgerPostingType.fromCode(type.getCode()), is(type));

        var exception = assertThrows(IllegalArgumentException.class, () -> LedgerPostingType.fromCode("UNKNOWN_CODE"));

        assertTrue(exception.getMessage(), exception.getMessage().contains(LedgerDiagnosticCode.LEDGER_CORE_022.prefix()));
        assertTrue(exception.getMessage(), exception.getMessage().contains("LedgerPostingType"));
        assertTrue(exception.getMessage(), exception.getMessage().contains("UNKNOWN_CODE"));
    }

    /**
     * Checks the Ledger-V6 scenario: configurable ledger parameter type codes are stable.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testConfigurableLedgerParameterTypeCodesAreStable()
    {
        assertStableCodes(Arrays.stream(LedgerParameterType.values()).map(LedgerParameterType::getCode)
                        .toArray(String[]::new));

        for (LedgerParameterType type : LedgerParameterType.values())
            assertThat(LedgerParameterType.fromCode(type.getCode()), is(type));

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerParameterType.fromCode("UNKNOWN_CODE"));

        assertTrue(exception.getMessage(), exception.getMessage().contains(LedgerDiagnosticCode.LEDGER_CORE_020.prefix()));
        assertTrue(exception.getMessage(), exception.getMessage().contains("LedgerParameterType"));
        assertTrue(exception.getMessage(), exception.getMessage().contains("UNKNOWN_CODE"));
    }

    private void assertStandardLegacyShape(LedgerEntryType type)
    {
        assertTrue(type.isLegacyFixedShape());
        assertFalse(type.isLedgerNativeTargeted());
        assertFalse(type.requiresTargetedDerivedDescriptors());
        assertTrue(type.supportsDerivedDescriptors());
        assertFalse(type.usesSignedTargetedProjectionFacts());
    }

    private void assertLedgerNativeTargetedShape(LedgerEntryType type)
    {
        assertFalse(type.isLegacyFixedShape());
        assertTrue(type.isLedgerNativeTargeted());
        assertTrue(type.requiresTargetedDerivedDescriptors());
        assertTrue(type.supportsDerivedDescriptors());
        assertTrue(type.usesSignedTargetedProjectionFacts());
    }

    private void assertValueKindPolicy(ValueKind valueKind, Class<?> valueType)
    {
        assertThat(valueKind.getValueType(), is(valueType));
        assertTrue(valueKind.supportsValue(valueFor(valueKind)));
        assertFalse(valueKind.supportsValue(null));
        assertFalse(valueKind.supportsValue(new Object()));
    }

    private void assertStableCodes(String[] codes)
    {
        assertThat(Arrays.stream(codes).filter(String::isBlank).count(), is(0L));
        assertThat(Arrays.stream(codes).distinct().count(), is((long) codes.length));
    }

    private void assertParameterTypePolicy(LedgerParameterType type, LedgerParameterType.Scope scope,
                    ValueKind valueKind)
    {
        assertThat(type.getScope(), is(scope));
        assertThat(type.getExpectedValueKind(), is(valueKind));
        assertThat(type.getExpectedValueType(), is(valueKind.getValueType()));
        assertThat(type.getExpectedJavaType(), is(valueKind.getValueType()));
        assertTrue(type.supportsValueKind(valueKind));
        type.requireValueKind(valueKind);
        assertTrue(type.supportsValue(valueFor(valueKind)));
        assertFalse(type.supportsValue(null));
        assertFalse(type.supportsValue(new Object()));
        assertThat(type.isReferenceParameter(),
                        is(valueKind == ValueKind.ACCOUNT || valueKind == ValueKind.PORTFOLIO
                                        || valueKind == ValueKind.SECURITY));
        assertThat(type.isDateParameter(),
                        is(valueKind == ValueKind.LOCAL_DATE || valueKind == ValueKind.LOCAL_DATE_TIME));
        assertThat(type.isBooleanParameter(), is(valueKind == ValueKind.BOOLEAN));

        EnumSet.complementOf(EnumSet.of(valueKind)).forEach(kind -> {
            assertFalse(type.supportsValueKind(kind));
            assertFactoryRejects(type, kind, () -> type.requireValueKind(kind));
        });
    }

    private void assertParameterTypePolicies(LedgerParameterType.Scope scope, ValueKind valueKind,
                    LedgerParameterType... types)
    {
        for (var type : types)
            assertParameterTypePolicy(type, scope, valueKind);
    }

    private void assertCodeDomain(LedgerParameterType type, LedgerParameterCodeDomain domain,
                    LedgerCode... allowedCodes)
    {
        assertTrue(type.hasCodeDomain());
        assertThat(type.getCodeDomain(), is(domain));
        assertThat(domain.getAllowedCodes(), is(Arrays.stream(allowedCodes).map(LedgerCode::getCode).toList()));

        for (var allowedCode : allowedCodes)
        {
            assertTrue(domain.allows(allowedCode.getCode()));
            assertTrue(type.supportsCode(allowedCode.getCode()));
        }

        assertFalse(domain.allows("UNKNOWN_CODE"));
        assertFalse(type.supportsCode("UNKNOWN_CODE"));
        assertTrue(type.isControlledCode());
    }

    private void assertPostingTypePolicy(LedgerPostingType type, LedgerPostingType.ComponentClass componentClass,
                    boolean moneyBearing, boolean currencyRequired, boolean securityBearing, boolean securityRequired,
                    boolean sharesMeaningful, boolean accountReferenceMeaningful,
                    boolean portfolioReferenceMeaningful, boolean forexMeaningful)
    {
        assertThat(type.getComponentClass(), is(componentClass));
        assertThat(type.isMoneyBearing(), is(moneyBearing));
        assertThat(type.requiresCurrency(), is(currencyRequired));
        assertThat(type.isSecurityBearing(), is(securityBearing));
        assertThat(type.requiresSecurity(), is(securityRequired));
        assertThat(type.isSharesMeaningful(), is(sharesMeaningful));
        assertThat(type.isAccountReferenceMeaningful(), is(accountReferenceMeaningful));
        assertThat(type.isPortfolioReferenceMeaningful(), is(portfolioReferenceMeaningful));
        assertThat(type.isForexMeaningful(), is(forexMeaningful));
    }

    private void assertFactoryRejects(LedgerParameterType type, ValueKind valueKind, Runnable runnable)
    {
        var exception = assertThrows(IllegalArgumentException.class, runnable::run);

        assertTrue(exception.getMessage(), exception.getMessage().contains(type.name()));
        assertTrue(exception.getMessage(), exception.getMessage().contains(valueKind.name()));
        assertTrue(exception.getMessage(), exception.getMessage().contains(type.getExpectedValueKind().name()));
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
