package name.abuchen.portfolio.model.ledger.nativeentry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingTypeDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingTypeDefinitionRegistry;
import name.abuchen.portfolio.money.Money;

/**
 * Assembles ledger-native entries from declarative native-entry input.
 * This is internal infrastructure for ledger-native transactions. It is not a completed UI
 * or reporting workflow by itself.
 *
 * <p>
 * The assembler input is not persisted as a separate configuration object. It uses the
 * static Ledger configuration model to create persisted Ledger entries, postings,
 * parameters, and projection refs.
 * </p>
 */
public final class LedgerNativeEntryAssembler
{
    private final Client client;
    private final Function<LedgerEntryType, Optional<LedgerEntryDefinition>> entryDefinitionLookup;
    private final Function<LedgerPostingType, Optional<LedgerPostingTypeDefinition>> postingDefinitionLookup;

    private LedgerNativeEntryAssembler(Client client,
                    Function<LedgerEntryType, Optional<LedgerEntryDefinition>> entryDefinitionLookup,
                    Function<LedgerPostingType, Optional<LedgerPostingTypeDefinition>> postingDefinitionLookup)
    {
        this.client = Objects.requireNonNull(client);
        this.entryDefinitionLookup = Objects.requireNonNull(entryDefinitionLookup);
        this.postingDefinitionLookup = Objects.requireNonNull(postingDefinitionLookup);
    }

    LedgerNativeEntryAssembler(Client client,
                    Function<LedgerEntryType, Optional<LedgerEntryDefinition>> entryDefinitionLookup)
    {
        this(client, entryDefinitionLookup, LedgerPostingTypeDefinitionRegistry::lookup);
    }

    public static LedgerNativeEntryAssembler forClient(Client client)
    {
        return new LedgerNativeEntryAssembler(client, LedgerEntryDefinitionRegistry::lookup,
                        LedgerPostingTypeDefinitionRegistry::lookup);
    }

    public EntryBuilder forType(LedgerEntryType entryType)
    {
        Objects.requireNonNull(entryType);

        if (!entryType.isLedgerNativeTargeted())
            throw issue(LedgerNativeEntryAssemblyIssue.ENTRY_TYPE_NOT_NATIVE,
                            entryType + " is a standard transaction family. " //$NON-NLS-1$
                                            + "Use LedgerTransactionCreator for standard transaction families"); //$NON-NLS-1$

        var definition = entryDefinitionLookup.apply(entryType).orElseThrow(
                        () -> issue(LedgerNativeEntryAssemblyIssue.ENTRY_DEFINITION_MISSING,
                                        "Missing LedgerEntryDefinition for " + entryType)); //$NON-NLS-1$

        return new EntryBuilder(client, definition, postingDefinitionLookup);
    }

    public EntryBuilder spinOff()
    {
        return forType(LedgerEntryType.SPIN_OFF);
    }

    static LedgerNativeEntryAssemblyException issue(LedgerNativeEntryAssemblyIssue issue, String message)
    {
        return new LedgerNativeEntryAssemblyException(issue, message);
    }

    public static final class EntryBuilder
    {
        private final Client client;
        private final LedgerEntryDefinition definition;
        private final Function<LedgerPostingType, Optional<LedgerPostingTypeDefinition>> postingDefinitionLookup;
        private final List<NativeSecurityLeg> securityLegs = new ArrayList<>();
        private final List<NativeFee> fees = new ArrayList<>();
        private final List<NativeTax> taxes = new ArrayList<>();
        private NativeEntryMetadata metadata;
        private NativeCorporateActionEvent event;
        private NativeCashCompensation cashCompensation;

        private EntryBuilder(Client client, LedgerEntryDefinition definition,
                        Function<LedgerPostingType, Optional<LedgerPostingTypeDefinition>> postingDefinitionLookup)
        {
            this.client = client;
            this.definition = definition;
            this.postingDefinitionLookup = postingDefinitionLookup;
        }

        public EntryBuilder metadata(NativeEntryMetadata metadata)
        {
            this.metadata = Objects.requireNonNull(metadata);
            return this;
        }

        public EntryBuilder event(NativeCorporateActionEvent event)
        {
            this.event = Objects.requireNonNull(event);
            return this;
        }

        public EntryBuilder securityLeg(NativeSecurityLeg leg)
        {
            securityLegs.add(Objects.requireNonNull(leg));
            return this;
        }

        public EntryBuilder cashCompensation(NativeCashCompensation compensation)
        {
            this.cashCompensation = Objects.requireNonNull(compensation);
            return this;
        }

        public EntryBuilder fee(NativeFee fee)
        {
            fees.add(Objects.requireNonNull(fee));
            return this;
        }

        public EntryBuilder tax(NativeTax tax)
        {
            taxes.add(Objects.requireNonNull(tax));
            return this;
        }

        public LedgerNativeEntryBuildResult buildDetached()
        {
            var entry = new LedgerEntry();
            entry.setType(definition.getEntryType());

            applyMetadata(entry);
            applyEvent(entry);

            for (var leg : securityLegs)
                addSecurityLeg(entry, leg);

            LedgerPosting compensationPosting = null;
            if (cashCompensation != null)
                compensationPosting = addCashCompensation(entry, cashCompensation);

            for (var fee : fees)
                addFee(entry, fee);

            for (var tax : taxes)
                addTax(entry, tax);

            if (compensationPosting != null)
                addCashCompensationProjection(entry, cashCompensation, compensationPosting);

            var validationResult = validateDetached(entry);

            if (!validationResult.isOK())
                throw issue(LedgerNativeEntryAssemblyIssue.STRUCTURAL_VALIDATION_FAILED,
                                validationResult.format());

            var definitionValidationResult = LedgerNativeEntryDefinitionValidator.validate(entry);

            if (!definitionValidationResult.isOK())
                throw issue(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED,
                                definitionValidationResult.format());

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
                throw issue(LedgerNativeEntryAssemblyIssue.STRUCTURAL_VALIDATION_FAILED,
                                validationResult.format());

            return new LedgerNativeEntryBuildResult(liveEntry, validationResult);
        }

        private void applyMetadata(LedgerEntry entry)
        {
            if (metadata == null)
                throw issue(LedgerNativeEntryAssemblyIssue.REQUIRED_VALUE_MISSING,
                                "Native entry metadata is required"); //$NON-NLS-1$

            entry.setDateTime(metadata.getDateTime());
            entry.setNote(metadata.getNote());
            entry.setSource(metadata.getSource());
        }

        private void applyEvent(LedgerEntry entry)
        {
            if (event == null)
                return;

            for (var parameter : event.getParameters())
                addEntryParameter(entry, parameter);
        }

        private void addSecurityLeg(LedgerEntry entry, NativeSecurityLeg leg)
        {
            assertPostingTypeAllowed(leg.getPostingType());

            var posting = new LedgerPosting();
            posting.setType(leg.getPostingType());
            posting.setPortfolio(leg.getPortfolio());
            posting.setSecurity(leg.getSecurity());
            posting.setShares(leg.getShares());
            applyMoney(posting, leg.getAmount());

            if (leg.getLegCode() != null)
                addPostingParameter(posting, leg.getPostingType(),
                                new NativeParameterValue(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getLegCode()));

            for (var parameter : leg.getParameters())
                addPostingParameter(posting, leg.getPostingType(), parameter);

            entry.addPosting(posting);

            if (leg.getProjectionRole() != null)
                addPortfolioProjection(entry, leg.getProjectionRole(), leg.getPortfolio(), posting);
        }

        private LedgerPosting addCashCompensation(LedgerEntry entry, NativeCashCompensation compensation)
        {
            assertPostingTypeAllowed(LedgerPostingType.CASH_COMPENSATION);

            var posting = new LedgerPosting();
            posting.setType(LedgerPostingType.CASH_COMPENSATION);
            posting.setAccount(compensation.getAccount());
            applyMoney(posting, compensation.getAmount());

            if (compensation.getAccount() != null)
                addPostingParameter(posting, LedgerPostingType.CASH_COMPENSATION,
                                new NativeParameterValue(LedgerParameterType.CASH_ACCOUNT,
                                                compensation.getAccount()));

            if (compensation.getAmount() != null)
                addPostingParameter(posting, LedgerPostingType.CASH_COMPENSATION,
                                new NativeParameterValue(LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                                                compensation.getAmount()));

            addPostingParameter(posting, LedgerPostingType.CASH_COMPENSATION,
                            new NativeParameterValue(LedgerParameterType.CORPORATE_ACTION_LEG,
                                            CorporateActionLeg.CASH_COMPENSATION.getCode()));

            for (var parameter : compensation.getParameters())
                addPostingParameter(posting, LedgerPostingType.CASH_COMPENSATION, parameter);

            entry.addPosting(posting);

            return posting;
        }

        private void addCashCompensationProjection(LedgerEntry entry, NativeCashCompensation compensation,
                        LedgerPosting posting)
        {
            addProjection(entry, ProjectionIntent.account(LedgerProjectionRole.CASH_COMPENSATION,
                            compensation.getAccount(), posting, posting));
        }

        private void addFee(LedgerEntry entry, NativeFee fee)
        {
            assertPostingTypeAllowed(LedgerPostingType.FEE);

            var posting = new LedgerPosting();
            posting.setType(LedgerPostingType.FEE);
            posting.setAccount(fee.getAccount());
            applyMoney(posting, fee.getAmount());
            addPostingParameter(posting, LedgerPostingType.FEE,
                            new NativeParameterValue(LedgerParameterType.CORPORATE_ACTION_LEG,
                                            CorporateActionLeg.FEE.getCode()));

            for (var parameter : fee.getParameters())
                addPostingParameter(posting, LedgerPostingType.FEE, parameter);

            entry.addPosting(posting);
        }

        private void addTax(LedgerEntry entry, NativeTax tax)
        {
            assertPostingTypeAllowed(LedgerPostingType.TAX);

            var posting = new LedgerPosting();
            posting.setType(LedgerPostingType.TAX);
            posting.setAccount(tax.getAccount());
            applyMoney(posting, tax.getAmount());
            addPostingParameter(posting, LedgerPostingType.TAX,
                            new NativeParameterValue(LedgerParameterType.CORPORATE_ACTION_LEG,
                                            CorporateActionLeg.TAX.getCode()));

            for (var parameter : tax.getParameters())
                addPostingParameter(posting, LedgerPostingType.TAX, parameter);

            entry.addPosting(posting);
        }

        private void applyMoney(LedgerPosting posting, Money amount)
        {
            if (amount == null)
                throw issue(LedgerNativeEntryAssemblyIssue.REQUIRED_VALUE_MISSING,
                                posting.getType() + " posting amount is required"); //$NON-NLS-1$

            posting.setAmount(amount.getAmount());
            posting.setCurrency(amount.getCurrencyCode());
        }

        private void addPortfolioProjection(LedgerEntry entry, LedgerProjectionRole role, Portfolio portfolio,
                        LedgerPosting posting)
        {
            addProjection(entry, ProjectionIntent.portfolio(role, portfolio, posting));
        }

        private void addProjection(LedgerEntry entry, ProjectionIntent intent)
        {
            assertProjectionRoleAllowed(intent.role());

            if (intent.primaryPosting() == null)
                throw issue(LedgerNativeEntryAssemblyIssue.PROJECTION_TARGET_MISSING,
                                "Projection target posting is required for " + intent.role()); //$NON-NLS-1$

            var projection = new LedgerProjectionRef();
            projection.setRole(intent.role());

            if (intent.account() != null)
                projection.setAccount(intent.account());

            if (intent.portfolio() != null)
                projection.setPortfolio(intent.portfolio());

            projection.setPrimaryPosting(intent.primaryPosting());

            if (intent.postingGroup() != null)
                projection.setPostingGroup(intent.postingGroup());

            entry.addProjectionRef(projection);
        }

        private void addEntryParameter(LedgerEntry entry, NativeParameterValue parameter)
        {
            if (!definition.getEntryParameterTypes().contains(parameter.getType()))
                throw issue(LedgerNativeEntryAssemblyIssue.PARAMETER_NOT_IN_ENTRY_DEFINITION,
                                parameter.getType() + " is not an entry parameter for " //$NON-NLS-1$
                                                + definition.getEntryType());

            entry.addParameter(parameter(parameter.getType(), parameter.getValue()));
        }

        private void addPostingParameter(LedgerPosting posting, LedgerPostingType postingType,
                        NativeParameterValue parameter)
        {
            if (!definition.getPostingParameterTypes().contains(parameter.getType()))
                throw issue(LedgerNativeEntryAssemblyIssue.PARAMETER_NOT_IN_POSTING_DEFINITION,
                                parameter.getType() + " is not a posting parameter for " //$NON-NLS-1$
                                                + definition.getEntryType());

            var postingDefinition = postingDefinitionLookup.apply(postingType)
                            .orElseThrow(() -> issue(
                                            LedgerNativeEntryAssemblyIssue.PARAMETER_NOT_IN_POSTING_TYPE_DEFINITION,
                                            "Missing LedgerPostingTypeDefinition for " + postingType)); //$NON-NLS-1$

            if (!postingDefinition.supportsParameterType(parameter.getType()))
                throw issue(LedgerNativeEntryAssemblyIssue.PARAMETER_NOT_IN_POSTING_TYPE_DEFINITION,
                                parameter.getType() + " is not meaningful for " + postingType); //$NON-NLS-1$

            posting.addParameter(parameter(parameter.getType(), parameter.getValue()));
        }

        private LedgerParameter<?> parameter(LedgerParameterType type, Object value)
        {
            Objects.requireNonNull(type);

            if (!type.supportsValue(value))
                throw issue(LedgerNativeEntryAssemblyIssue.VALUE_KIND_MISMATCH,
                                type + " requires " + type.getExpectedValueKind() + " backed by " //$NON-NLS-1$ //$NON-NLS-2$
                                                + type.getExpectedJavaType().getSimpleName());

            if (type.hasCodeDomain() && !type.supportsCode((String) value))
                throw issue(LedgerNativeEntryAssemblyIssue.PARAMETER_CODE_NOT_ALLOWED,
                                value + " is not allowed for " + type + "; allowed: " //$NON-NLS-1$ //$NON-NLS-2$
                                                + type.getCodeDomain().getAllowedCodes());

            return switch (type.getExpectedValueKind())
            {
                case STRING -> LedgerParameter.ofString(type, (String) value);
                case DECIMAL -> LedgerParameter.ofDecimal(type, (BigDecimal) value);
                case LONG -> LedgerParameter.ofLong(type, ((Long) value).longValue());
                case MONEY -> LedgerParameter.ofMoney(type, (Money) value);
                case SECURITY -> LedgerParameter.ofSecurity(type, (Security) value);
                case ACCOUNT -> LedgerParameter.ofAccount(type, (Account) value);
                case PORTFOLIO -> LedgerParameter.ofPortfolio(type, (Portfolio) value);
                case BOOLEAN -> LedgerParameter.ofBoolean(type, (Boolean) value);
                case LOCAL_DATE -> LedgerParameter.ofLocalDate(type, (LocalDate) value);
                case LOCAL_DATE_TIME -> LedgerParameter.ofLocalDateTime(type, (LocalDateTime) value);
            };
        }

        private void assertPostingTypeAllowed(LedgerPostingType postingType)
        {
            if (!definition.getPostingTypes().contains(postingType))
                throw issue(LedgerNativeEntryAssemblyIssue.POSTING_TYPE_NOT_IN_ENTRY_DEFINITION,
                                postingType + " is not a posting type for " + definition.getEntryType()); //$NON-NLS-1$
        }

        private void assertProjectionRoleAllowed(LedgerProjectionRole role)
        {
            if (!definition.getProjectionRoles().contains(role))
                throw issue(LedgerNativeEntryAssemblyIssue.PROJECTION_ROLE_NOT_IN_ENTRY_DEFINITION,
                                role + " is not a projection role for " + definition.getEntryType()); //$NON-NLS-1$
        }

        private LedgerStructuralValidator.ValidationResult validateDetached(LedgerEntry entry)
        {
            var candidate = new Ledger();

            client.getLedger().getEntries().forEach(candidate::addEntry);
            candidate.addEntry(entry);

            return LedgerStructuralValidator.validate(candidate);
        }

        private record ProjectionIntent(LedgerProjectionRole role, Account account, Portfolio portfolio,
                        LedgerPosting primaryPosting, LedgerPosting postingGroup)
        {
            private ProjectionIntent
            {
                Objects.requireNonNull(role);
            }

            private static ProjectionIntent portfolio(LedgerProjectionRole role, Portfolio portfolio,
                            LedgerPosting primaryPosting)
            {
                return new ProjectionIntent(role, null, portfolio, primaryPosting, null);
            }

            private static ProjectionIntent account(LedgerProjectionRole role, Account account,
                            LedgerPosting primaryPosting, LedgerPosting postingGroup)
            {
                return new ProjectionIntent(role, account, null, primaryPosting, postingGroup);
            }
        }
    }
}
