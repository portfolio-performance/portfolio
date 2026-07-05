package name.abuchen.portfolio.model.ledger.nativeentry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.CorporateActionBasisAllocation;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisMethod;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisStatus;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.Money;

/**
 * Contributor-facing fluent builder for native Ledger Corporate Action entries.
 * It writes the persisted Ledger truth directly and delegates business validation
 * to the native definition and structural validators.
 */
public final class LedgerCorporateActionBuilder
{
    private final Client client;
    private final List<Consumer<LedgerEntry>> postingWriters = new ArrayList<>();
    private final List<CorporateActionBasisAllocation> basisAllocations = new ArrayList<>();
    private CorporateActionKind kind;
    private LocalDateTime dateTime;
    private String note;
    private String source;
    private LocalDate effectiveDate;
    private LocalDate paymentDate;
    private LocalDate settlementDate;
    private CorporateActionBasisStatus basisStatus;
    private CorporateActionBasisMethod basisMethod;

    LedgerCorporateActionBuilder(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public LedgerCorporateActionBuilder kind(CorporateActionKind kind)
    {
        this.kind = Objects.requireNonNull(kind);
        return this;
    }

    public LedgerCorporateActionBuilder date(LocalDateTime dateTime)
    {
        this.dateTime = Objects.requireNonNull(dateTime);
        return this;
    }

    public LedgerCorporateActionBuilder note(String note)
    {
        this.note = note;
        return this;
    }

    public LedgerCorporateActionBuilder source(String source)
    {
        this.source = source;
        return this;
    }

    public LedgerCorporateActionBuilder effectiveDate(LocalDate effectiveDate)
    {
        this.effectiveDate = Objects.requireNonNull(effectiveDate);
        return this;
    }

    public LedgerCorporateActionBuilder paymentDate(LocalDate paymentDate)
    {
        this.paymentDate = Objects.requireNonNull(paymentDate);
        return this;
    }

    public LedgerCorporateActionBuilder settlementDate(LocalDate settlementDate)
    {
        this.settlementDate = Objects.requireNonNull(settlementDate);
        return this;
    }

    public LedgerCorporateActionBuilder securityContext(String localKey, Portfolio portfolio, Security security)
    {
        return securityContext(localKey, localKey, portfolio, security);
    }

    public LedgerCorporateActionBuilder securityContext(String localKey, String groupKey, Portfolio portfolio,
                    Security security)
    {
        postingWriters.add(entry -> entry.addPosting(securityPosting(LedgerLegRole.SECURITY_CONTEXT_LEG,
                        CorporateActionLeg.SECURITY_CONTEXT, LedgerPostingDirection.NEUTRAL, localKey, groupKey,
                        portfolio, security, 0L)));
        return this;
    }

    public LedgerCorporateActionBuilder securityIn(String localKey, Portfolio portfolio, Security security,
                    long shares)
    {
        return securityIn(localKey, localKey, portfolio, security, shares);
    }

    public LedgerCorporateActionBuilder securityIn(String localKey, String groupKey, Portfolio portfolio,
                    Security security, long shares)
    {
        postingWriters.add(entry -> entry.addPosting(securityPosting(LedgerLegRole.TARGET_SECURITY_LEG,
                        CorporateActionLeg.TARGET_SECURITY, LedgerPostingDirection.INBOUND, localKey, groupKey,
                        portfolio, security, shares)));
        return this;
    }

    public LedgerCorporateActionBuilder securityOut(String localKey, Portfolio portfolio, Security security,
                    long shares)
    {
        return securityOut(localKey, localKey, portfolio, security, shares);
    }

    public LedgerCorporateActionBuilder securityOut(String localKey, String groupKey, Portfolio portfolio,
                    Security security, long shares)
    {
        postingWriters.add(entry -> entry.addPosting(securityPosting(LedgerLegRole.SOURCE_SECURITY_LEG,
                        CorporateActionLeg.SOURCE_SECURITY, LedgerPostingDirection.OUTBOUND, localKey, groupKey,
                        portfolio, security, shares)));
        return this;
    }

    public LedgerCorporateActionBuilder cash(String localKey, Account account, Money amount)
    {
        return cash(localKey, localKey, account, amount);
    }

    public LedgerCorporateActionBuilder cash(String localKey, String groupKey, Account account, Money amount)
    {
        postingWriters.add(entry -> entry.addPosting(cashPosting(localKey, groupKey, account, amount)));
        return this;
    }

    public LedgerCorporateActionBuilder fee(String localKey, Account account, Money amount, String groupKey)
    {
        postingWriters.add(entry -> entry.addPosting(unitPosting(LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE,
                        LedgerPostingUnitRole.FEE, CorporateActionLeg.FEE, localKey, groupKey, account, amount)));
        return this;
    }

    public LedgerCorporateActionBuilder tax(String localKey, Account account, Money amount, String groupKey)
    {
        postingWriters.add(entry -> entry.addPosting(unitPosting(LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX,
                        LedgerPostingUnitRole.TAX, CorporateActionLeg.TAX, localKey, groupKey, account, amount)));
        return this;
    }

    public LedgerCorporateActionBuilder principal(String localKey, Account account, Money amount)
    {
        return principal(localKey, localKey, account, amount);
    }

    public LedgerCorporateActionBuilder principal(String localKey, String groupKey, Account account, Money amount)
    {
        postingWriters.add(entry -> entry.addPosting(primaryMoneyPosting(LedgerPostingType.PRINCIPAL_REDEMPTION,
                        LedgerPostingSemanticRole.PRINCIPAL_REDEMPTION, CorporateActionLeg.PRINCIPAL, localKey,
                        groupKey, account, amount, LedgerParameterType.CASH_ACCOUNT)));
        return this;
    }

    public LedgerCorporateActionBuilder accruedInterest(String localKey, Account account, Money amount)
    {
        return accruedInterest(localKey, localKey, account, amount);
    }

    public LedgerCorporateActionBuilder accruedInterest(String localKey, String groupKey, Account account,
                    Money amount)
    {
        postingWriters.add(entry -> entry.addPosting(primaryMoneyPosting(LedgerPostingType.ACCRUED_INTEREST,
                        LedgerPostingSemanticRole.ACCRUED_INTEREST, CorporateActionLeg.ACCRUED_INTEREST, localKey,
                        groupKey, account, amount, null)));
        return this;
    }

    public LedgerCorporateActionBuilder basis(CorporateActionBasisStatus status)
    {
        this.basisStatus = Objects.requireNonNull(status);
        return this;
    }

    public LedgerCorporateActionBuilder basisMethod(CorporateActionBasisMethod method)
    {
        this.basisMethod = Objects.requireNonNull(method);
        return this;
    }

    public LedgerCorporateActionBuilder basisPercentageAllocation(LedgerLegRole targetRole, String targetLocalKey,
                    String targetGroupKey, BigDecimal percent)
    {
        basisAllocations.add(CorporateActionBasisAllocation.percentage(targetRole, targetLocalKey, targetGroupKey,
                        percent));
        return this;
    }

    public LedgerNativeEntryBuildResult buildDetached()
    {
        var entry = assemble();
        validateNativeDefinition(entry);
        var validationResult = validateDetached(entry);

        if (!validationResult.isOK())
            throw LedgerNativeEntryAssembler.issue(LedgerNativeEntryAssemblyIssue.STRUCTURAL_VALIDATION_FAILED,
                            validationResult.format());

        return new LedgerNativeEntryBuildResult(entry, validationResult);
    }

    public LedgerNativeEntryBuildResult buildAndAdd()
    {
        var detached = buildDetached();
        var context = new LedgerMutationContext(client);
        var liveEntry = context.attachEntry(detached.getEntry());

        context.refresh();

        var validationResult = LedgerStructuralValidator.validate(client.getLedger());

        if (!validationResult.isOK())
            throw LedgerNativeEntryAssembler.issue(LedgerNativeEntryAssemblyIssue.STRUCTURAL_VALIDATION_FAILED,
                            validationResult.format());

        return new LedgerNativeEntryBuildResult(liveEntry, validationResult);
    }

    private LedgerEntry assemble()
    {
        if (kind == null)
            throw LedgerNativeEntryAssembler.issue(LedgerNativeEntryAssemblyIssue.REQUIRED_VALUE_MISSING,
                            "Corporate Action kind is required"); //$NON-NLS-1$
        if (dateTime == null)
            throw LedgerNativeEntryAssembler.issue(LedgerNativeEntryAssemblyIssue.REQUIRED_VALUE_MISSING,
                            "Corporate Action date is required"); //$NON-NLS-1$

        LedgerEntryDefinitionRegistry.lookup(LedgerEntryType.CORPORATE_ACTION, kind)
                        .orElseThrow(() -> LedgerNativeEntryAssembler.issue(
                                        LedgerNativeEntryAssemblyIssue.ENTRY_DEFINITION_MISSING,
                                        "Missing LedgerEntryDefinition for " + kind)); //$NON-NLS-1$

        var entry = new LedgerEntry();
        entry.setType(LedgerEntryType.CORPORATE_ACTION);
        entry.setDateTime(dateTime);
        entry.setNote(note);
        entry.setSource(source);
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND, kind.getCode()));
        applyEventDates(entry);

        applyBasis(entry);
        postingWriters.forEach(writer -> writer.accept(entry));

        return entry;
    }

    private void applyEventDates(LedgerEntry entry)
    {
        if (effectiveDate != null)
            entry.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.EFFECTIVE_DATE, effectiveDate));

        if (paymentDate != null)
            entry.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.PAYMENT_DATE, paymentDate));

        if (settlementDate != null)
            entry.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.SETTLEMENT_DATE, settlementDate));
    }

    private void applyBasis(LedgerEntry entry)
    {
        if (basisStatus != null)
            entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_BASIS_STATUS,
                            basisStatus.getCode()));

        if (basisMethod != null)
            entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_BASIS_METHOD,
                            basisMethod.getCode()));

        for (var allocation : basisAllocations)
            entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_BASIS_ALLOCATION,
                            allocation.toParameterValue()));
    }

    private LedgerPosting securityPosting(LedgerLegRole role, CorporateActionLeg leg, LedgerPostingDirection direction,
                    String localKey, String groupKey, Portfolio portfolio, Security security, long shares)
    {
        var posting = primaryPosting(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY, leg, direction,
                        localKey, groupKey);

        posting.setPortfolio(portfolio);
        posting.setSecurity(security);
        posting.setShares(shares);
        posting.setAmount(0L);
        posting.setCurrency(client.getBaseCurrency());
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        if (role == LedgerLegRole.SOURCE_SECURITY_LEG)
        {
            posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY, security));
            addDefaultRatio(posting);
        }
        else if (role == LedgerLegRole.TARGET_SECURITY_LEG)
        {
            posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY, security));
            addDefaultRatio(posting);
        }

        return posting;
    }

    private void addDefaultRatio(LedgerPosting posting)
    {
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR, BigDecimal.ONE));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_DENOMINATOR, BigDecimal.ONE));
    }

    private LedgerPosting cashPosting(String localKey, String groupKey, Account account, Money amount)
    {
        if (usesCashCompensationLeg())
            return cashCompensationPosting(localKey, groupKey, account, amount);

        return primaryMoneyPosting(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH, null, localKey, groupKey,
                        account, amount, LedgerParameterType.CASH_ACCOUNT);
    }

    private boolean usesCashCompensationLeg()
    {
        return kind == CorporateActionKind.SPIN_OFF || kind == CorporateActionKind.STOCK_DIVIDEND
                        || kind == CorporateActionKind.BONUS_ISSUE || kind == CorporateActionKind.RIGHTS_DISTRIBUTION
                        || kind == CorporateActionKind.PIK_INTEREST || kind == CorporateActionKind.CONVERSION
                        || kind == CorporateActionKind.EXCHANGE;
    }

    private LedgerPosting cashCompensationPosting(String localKey, String groupKey, Account account, Money amount)
    {
        return primaryMoneyPosting(LedgerPostingType.CASH_COMPENSATION,
                        LedgerPostingSemanticRole.CASH_COMPENSATION, CorporateActionLeg.CASH_COMPENSATION, localKey,
                        groupKey, account, amount, LedgerParameterType.CASH_ACCOUNT);
    }

    private LedgerPosting primaryMoneyPosting(LedgerPostingType type, LedgerPostingSemanticRole semanticRole,
                    CorporateActionLeg leg, String localKey, String groupKey, Account account, Money amount,
                    LedgerParameterType accountParameterType)
    {
        var posting = primaryPosting(type, semanticRole, leg, LedgerPostingDirection.NEUTRAL, localKey, groupKey);

        posting.setAccount(account);
        applyMoney(posting, amount);

        if (leg != null)
            posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        if (account != null && accountParameterType != null)
            posting.addParameter(accountParameter(accountParameterType, account));

        if (type == LedgerPostingType.ACCRUED_INTEREST && amount != null)
            posting.addParameter(LedgerParameter.ofMoney(LedgerParameterType.ACCRUED_INTEREST_AMOUNT, amount));

        return posting;
    }

    private LedgerPosting unitPosting(LedgerPostingType type, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingUnitRole unitRole, CorporateActionLeg leg, String localKey, String groupKey,
                    Account account, Money amount)
    {
        var posting = new LedgerPosting();

        posting.setType(type);
        posting.setSemanticRole(semanticRole);
        posting.setUnitRole(unitRole);
        posting.setCorporateActionLeg(leg);
        posting.setLocalKey(localKey);
        posting.setGroupKey(groupKey);
        posting.setAccount(account);
        applyMoney(posting, amount);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        return posting;
    }

    private LedgerPosting primaryPosting(LedgerPostingType type, LedgerPostingSemanticRole semanticRole,
                    CorporateActionLeg leg, LedgerPostingDirection direction, String localKey, String groupKey)
    {
        var posting = new LedgerPosting();

        posting.setType(type);
        posting.setSemanticRole(semanticRole);
        posting.setDirection(direction);
        posting.setCorporateActionLeg(leg);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setLocalKey(localKey);
        posting.setGroupKey(groupKey);

        return posting;
    }

    private void applyMoney(LedgerPosting posting, Money amount)
    {
        if (amount == null)
            throw LedgerNativeEntryAssembler.issue(LedgerNativeEntryAssemblyIssue.REQUIRED_VALUE_MISSING,
                            posting.getType() + " posting amount is required"); //$NON-NLS-1$

        posting.setAmount(amount.getAmount());
        posting.setCurrency(amount.getCurrencyCode());
    }

    private LedgerParameter<Account> accountParameter(LedgerParameterType type, Account account)
    {
        if (type == LedgerParameterType.CASH_ACCOUNT)
            return LedgerParameter.ofAccount(type, account);

        throw new IllegalArgumentException("Unsupported account parameter type: " + type); //$NON-NLS-1$
    }

    private void validateNativeDefinition(LedgerEntry entry)
    {
        var result = LedgerNativeEntryDefinitionValidator.validate(entry);

        if (!result.isOK())
            throw LedgerNativeEntryAssembler.issue(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED,
                            result.format());
    }

    private LedgerStructuralValidator.ValidationResult validateDetached(LedgerEntry entry)
    {
        var candidate = new Ledger();

        client.getLedger().getEntries().forEach(candidate::addEntry);
        candidate.addEntry(entry);

        return LedgerStructuralValidator.validate(candidate);
    }
}
