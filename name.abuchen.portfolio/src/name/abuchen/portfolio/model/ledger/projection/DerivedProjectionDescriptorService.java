package name.abuchen.portfolio.model.ledger.projection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegProjection;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Derives runtime projection descriptors from Ledger entry type and posting
 * semantics. The descriptors are runtime-only compatibility view metadata;
 * they are not persisted Ledger facts.
 */
public final class DerivedProjectionDescriptorService
{
    public Result derive(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        var descriptors = new ArrayList<DerivedProjectionDescriptor>();
        var diagnostics = new ArrayList<Diagnostic>();

        if (entry.getType() == null)
        {
            diagnostics.add(Diagnostic.missing(entry, null, "Ledger entry type is required")); //$NON-NLS-1$
            return new Result(descriptors, diagnostics);
        }

        corporateAction(entry, descriptors, diagnostics);

        return new Result(descriptors, diagnostics);
    }

    private void corporateAction(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors,
                    List<Diagnostic> diagnostics)
    {
        var kind = CorporateActionKind.fromEntry(entry);

        if (kind.filter(CorporateActionKind.SPIN_OFF::equals).isPresent())
        {
            spinOff(entry, descriptors, diagnostics);
            return;
        }

        if (kind.filter(this::isSecurityInCorporateAction).isPresent())
        {
            securityInCorporateAction(entry, descriptors, diagnostics);
            return;
        }

        if (kind.filter(this::isFixedIncomeRedemptionCorporateAction).isPresent())
        {
            fixedIncomeRedemptionCorporateAction(entry, descriptors, diagnostics);
            return;
        }

        if (kind.filter(this::isSecurityReorganizationCorporateAction).isPresent())
        {
            securityReorganizationCorporateAction(entry, descriptors, diagnostics);
            return;
        }

        if (kind.filter(this::isOpenMovementCorporateAction).isPresent())
        {
            openMovementCorporateAction(entry, descriptors, diagnostics);
            return;
        }

        if (kind.filter(k -> k == CorporateActionKind.CASH_DISTRIBUTION || k == CorporateActionKind.COUPON_PAYMENT)
                        .isPresent())
            cashOrientedCorporateAction(entry, descriptors, diagnostics);
    }

    private boolean isSecurityInCorporateAction(CorporateActionKind kind)
    {
        return switch (kind)
        {
            case STOCK_DIVIDEND, BONUS_ISSUE, RIGHTS_DISTRIBUTION, PIK_INTEREST -> true;
            default -> false;
        };
    }

    private boolean isSecurityReorganizationCorporateAction(CorporateActionKind kind)
    {
        return switch (kind)
        {
            case CONVERSION, EXCHANGE -> true;
            default -> false;
        };
    }

    private boolean isOpenMovementCorporateAction(CorporateActionKind kind)
    {
        return switch (kind)
        {
            case DEFAULTED_INTEREST, RESTRUCTURING, DEFAULT -> true;
            default -> false;
        };
    }

    private boolean isFixedIncomeRedemptionCorporateAction(CorporateActionKind kind)
    {
        return switch (kind)
        {
            case MATURITY, PARTIAL_REDEMPTION, CALL, PUT -> true;
            default -> false;
        };
    }

    private void spinOff(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors, List<Diagnostic> diagnostics)
    {
        repeatedPortfolio(entry, LedgerProjectionRole.OLD_SECURITY_LEG,
                        primary().and(corporateLeg(CorporateActionLeg.SOURCE_SECURITY))
                                        .and(localKey(LedgerProjectionRole.OLD_SECURITY_LEG)),
                        primary().and(corporateLeg(CorporateActionLeg.SOURCE_SECURITY)), true, diagnostics)
                                        .forEach(descriptors::add);
        optionalPortfolio(entry, LedgerProjectionRole.DELIVERY_INBOUND,
                        primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY))
                                        .and(localKey(LedgerProjectionRole.DELIVERY_INBOUND)),
                        diagnostics).ifPresent(descriptors::add);
        repeatedPortfolio(entry, LedgerProjectionRole.NEW_SECURITY_LEG,
                        primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY))
                                        .and(localKey(LedgerProjectionRole.NEW_SECURITY_LEG)),
                        primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY))
                                        .and(localKey(LedgerProjectionRole.DELIVERY_INBOUND).negate()),
                        true, diagnostics).forEach(descriptors::add);
        repeatedAccount(entry, LedgerProjectionRole.CASH_COMPENSATION,
                        cashCompensationPreferredSelector(), cashCompensationRepeatedSelector(), true, diagnostics)
                        .forEach(descriptors::add);
    }

    private void securityInCorporateAction(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors,
                    List<Diagnostic> diagnostics)
    {
        repeatedPortfolio(entry, LedgerProjectionRole.NEW_SECURITY_LEG,
                        securityInPreferredSelector(), securityInRepeatedSelector(), false, diagnostics)
                        .forEach(descriptors::add);
        repeatedAccount(entry, LedgerProjectionRole.CASH_COMPENSATION,
                        cashCompensationPreferredSelector(), cashCompensationRepeatedSelector(), true, diagnostics)
                        .forEach(descriptors::add);
    }

    private void cashOrientedCorporateAction(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors,
                    List<Diagnostic> diagnostics)
    {
        repeatedAccount(entry, LedgerProjectionRole.ACCOUNT, cashPreferredSelector(), cashRepeatedSelector(), true,
                        diagnostics).forEach(descriptors::add);
    }

    private void fixedIncomeRedemptionCorporateAction(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors,
                    List<Diagnostic> diagnostics)
    {
        repeatedPortfolio(entry, LedgerProjectionRole.DELIVERY_OUTBOUND, sourceSecurityPreferredSelector(),
                        sourceSecurityRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
        repeatedAccount(entry, LedgerProjectionRole.ACCOUNT, cashPreferredSelector(), cashRepeatedSelector(), true,
                        diagnostics).forEach(descriptors::add);
    }

    private void securityReorganizationCorporateAction(LedgerEntry entry,
                    List<DerivedProjectionDescriptor> descriptors, List<Diagnostic> diagnostics)
    {
        repeatedPortfolio(entry, LedgerProjectionRole.DELIVERY_OUTBOUND, sourceSecurityPreferredSelector(),
                        sourceSecurityRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
        repeatedPortfolio(entry, LedgerProjectionRole.NEW_SECURITY_LEG, securityInPreferredSelector(),
                        securityInRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
        repeatedAccount(entry, LedgerProjectionRole.CASH_COMPENSATION, cashCompensationPreferredSelector(),
                        cashCompensationRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
    }

    private void openMovementCorporateAction(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors,
                    List<Diagnostic> diagnostics)
    {
        repeatedPortfolio(entry, LedgerProjectionRole.DELIVERY_OUTBOUND, sourceSecurityPreferredSelector(),
                        sourceSecurityRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
        repeatedPortfolio(entry, LedgerProjectionRole.NEW_SECURITY_LEG, securityInPreferredSelector(),
                        securityInRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
        repeatedAccount(entry, LedgerProjectionRole.ACCOUNT, openAccountPreferredSelector(),
                        openAccountRepeatedSelector(), true, diagnostics).forEach(descriptors::add);
    }

    private Predicate<LedgerPosting> securityInPreferredSelector()
    {
        return primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY)
                        .or(corporateLeg(CorporateActionLeg.RIGHT_SECURITY)))
                        .and(localKey(LedgerProjectionRole.NEW_SECURITY_LEG));
    }

    private Predicate<LedgerPosting> securityInRepeatedSelector()
    {
        return primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY)
                        .or(corporateLeg(CorporateActionLeg.RIGHT_SECURITY)));
    }

    private Predicate<LedgerPosting> cashCompensationPreferredSelector()
    {
        return primary().and(corporateLeg(CorporateActionLeg.CASH_COMPENSATION))
                        .and(localKey(LedgerProjectionRole.CASH_COMPENSATION));
    }

    private Predicate<LedgerPosting> cashCompensationRepeatedSelector()
    {
        return primary().and(corporateLeg(CorporateActionLeg.CASH_COMPENSATION));
    }

    private Predicate<LedgerPosting> sourceSecurityPreferredSelector()
    {
        return primary().and(corporateLeg(CorporateActionLeg.SOURCE_SECURITY))
                        .and(localKey(LedgerProjectionRole.DELIVERY_OUTBOUND));
    }

    private Predicate<LedgerPosting> sourceSecurityRepeatedSelector()
    {
        return primary().and(corporateLeg(CorporateActionLeg.SOURCE_SECURITY));
    }

    private Predicate<LedgerPosting> cashPreferredSelector()
    {
        return primary().and(postingType(LedgerPostingType.CASH)).and(localKey(LedgerProjectionRole.ACCOUNT));
    }

    private Predicate<LedgerPosting> cashRepeatedSelector()
    {
        return primary().and(postingType(LedgerPostingType.CASH)).and(semantic(LedgerPostingSemanticRole.CASH));
    }

    private Predicate<LedgerPosting> openAccountPreferredSelector()
    {
        return cashPreferredSelector().or(standaloneFeeTaxSelector().and(localKey(LedgerProjectionRole.ACCOUNT)));
    }

    private Predicate<LedgerPosting> openAccountRepeatedSelector()
    {
        return cashRepeatedSelector().or(standaloneFeeTaxSelector());
    }

    private Predicate<LedgerPosting> standaloneFeeTaxSelector()
    {
        return posting -> (posting.getType() == LedgerPostingType.FEE || posting.getType() == LedgerPostingType.TAX)
                        && isBlank(posting.getGroupKey());
    }

    private java.util.Optional<DerivedProjectionDescriptor> optionalPortfolio(LedgerEntry entry,
                    LedgerProjectionRole role, Predicate<LedgerPosting> selector, List<Diagnostic> diagnostics)
    {
        return descriptor(entry, role, DerivedProjectionViewKind.PORTFOLIO, selector, true, diagnostics);
    }

    private List<DerivedProjectionDescriptor> repeatedAccount(LedgerEntry entry, LedgerProjectionRole role,
                    Predicate<LedgerPosting> preferredSelector, Predicate<LedgerPosting> repeatedSelector,
                    boolean optional, List<Diagnostic> diagnostics)
    {
        return repeated(entry, role, DerivedProjectionViewKind.ACCOUNT, preferredSelector, repeatedSelector, optional,
                        diagnostics);
    }

    private List<DerivedProjectionDescriptor> repeatedPortfolio(LedgerEntry entry, LedgerProjectionRole role,
                    Predicate<LedgerPosting> preferredSelector, Predicate<LedgerPosting> repeatedSelector,
                    boolean optional, List<Diagnostic> diagnostics)
    {
        return repeated(entry, role, DerivedProjectionViewKind.PORTFOLIO, preferredSelector, repeatedSelector, optional,
                        diagnostics);
    }

    private List<DerivedProjectionDescriptor> repeated(LedgerEntry entry, LedgerProjectionRole role,
                    DerivedProjectionViewKind viewKind, Predicate<LedgerPosting> preferredSelector,
                    Predicate<LedgerPosting> repeatedSelector, boolean optional, List<Diagnostic> diagnostics)
    {
        var preferredMatches = matches(entry, preferredSelector);

        if (!preferredMatches.isEmpty())
        {
            var extraMatches = matches(entry, repeatedSelector.and(preferredSelector.negate()));

            if (!extraMatches.isEmpty())
            {
                diagnostics.add(Diagnostic.ambiguous(entry, role,
                                "Ambiguous semantic primary postings; use distinct localKey values", //$NON-NLS-1$
                                extraMatches));
                return List.of();
            }

            return descriptor(entry, role, viewKind, preferredMatches, optional, diagnostics).stream().toList();
        }

        var repeatedMatches = matches(entry, repeatedSelector);

        if (repeatedMatches.isEmpty())
        {
            if (!optional)
                diagnostics.add(Diagnostic.missing(entry, role, "Missing semantic primary posting")); //$NON-NLS-1$
            return List.of();
        }

        return repeatedDescriptors(entry, role, viewKind, repeatedMatches, diagnostics);
    }

    private java.util.Optional<DerivedProjectionDescriptor> descriptor(LedgerEntry entry, LedgerProjectionRole role,
                    DerivedProjectionViewKind viewKind, Predicate<LedgerPosting> selector, boolean optional,
                    List<Diagnostic> diagnostics)
    {
        return descriptor(entry, role, viewKind, matches(entry, selector), optional, diagnostics);
    }

    private java.util.Optional<DerivedProjectionDescriptor> descriptor(LedgerEntry entry, LedgerProjectionRole role,
                    DerivedProjectionViewKind viewKind, List<LedgerPosting> matches, boolean optional,
                    List<Diagnostic> diagnostics)
    {
        if (matches.isEmpty())
        {
            if (!optional)
                diagnostics.add(Diagnostic.missing(entry, role, "Missing semantic primary posting")); //$NON-NLS-1$
            return java.util.Optional.empty();
        }

        if (matches.size() > 1)
        {
            diagnostics.add(Diagnostic.ambiguous(entry, role, "Ambiguous semantic primary postings", matches)); //$NON-NLS-1$
            return java.util.Optional.empty();
        }

        var primary = matches.get(0);
        var account = primary.getAccount();
        var portfolio = primary.getPortfolio();

        if (viewKind == DerivedProjectionViewKind.ACCOUNT && account == null)
        {
            diagnostics.add(Diagnostic.missing(entry, role, "Semantic account owner is missing")); //$NON-NLS-1$
            return java.util.Optional.empty();
        }

        if (viewKind == DerivedProjectionViewKind.PORTFOLIO && portfolio == null)
        {
            diagnostics.add(Diagnostic.missing(entry, role, "Semantic portfolio owner is missing")); //$NON-NLS-1$
            return java.util.Optional.empty();
        }

        var projection = projection(entry, role, viewKind, primary);

        if (projection.isEmpty())
        {
            diagnostics.add(Diagnostic.missing(entry, role, "Projection definition is missing")); //$NON-NLS-1$
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new DerivedProjectionDescriptor(entry, role, viewKind, projection.get(), account,
                        portfolio, primary, unitPostings(entry, primary), primarySelector(role),
                        unitSelector(primary)));
    }

    private List<DerivedProjectionDescriptor> repeatedDescriptors(LedgerEntry entry, LedgerProjectionRole role,
                    DerivedProjectionViewKind viewKind, List<LedgerPosting> matches, List<Diagnostic> diagnostics)
    {
        var invalid = false;
        var localKeys = new HashSet<String>();

        for (var posting : matches)
        {
            if (isBlank(posting.getLocalKey()))
            {
                diagnostics.add(Diagnostic.missingInstanceKey(entry, role,
                                "Repeated semantic primary posting requires a localKey")); //$NON-NLS-1$
                invalid = true;
            }
            else if (!localKeys.add(posting.getLocalKey()))
            {
                diagnostics.add(Diagnostic.duplicateInstanceKey(entry, role,
                                "Repeated semantic primary postings require distinct localKey values", //$NON-NLS-1$
                                posting));
                invalid = true;
            }
        }

        if (invalid)
            return List.of();

        var descriptors = new ArrayList<DerivedProjectionDescriptor>();

        for (var primary : matches)
        {
            var account = primary.getAccount();
            var portfolio = primary.getPortfolio();

            if (viewKind == DerivedProjectionViewKind.ACCOUNT && account == null)
            {
                diagnostics.add(Diagnostic.missing(entry, role, "Semantic account owner is missing")); //$NON-NLS-1$
                invalid = true;
                continue;
            }

            if (viewKind == DerivedProjectionViewKind.PORTFOLIO && portfolio == null)
            {
                diagnostics.add(Diagnostic.missing(entry, role, "Semantic portfolio owner is missing")); //$NON-NLS-1$
                invalid = true;
                continue;
            }

            var projection = projection(entry, role, viewKind, primary);

            if (projection.isEmpty())
            {
                diagnostics.add(Diagnostic.missing(entry, role, "Projection definition is missing")); //$NON-NLS-1$
                invalid = true;
                continue;
            }

            descriptors.add(new DerivedProjectionDescriptor(entry, role, viewKind, projection.get(), account,
                            portfolio, primary, unitPostings(entry, primary), primary.getLocalKey(),
                            primarySelector(role, primary.getLocalKey()), unitSelector(primary)));
        }

        if (invalid)
            return List.of();

        return List.copyOf(descriptors);
    }

    private List<LedgerPosting> matches(LedgerEntry entry, Predicate<LedgerPosting> selector)
    {
        return entry.getPostings().stream().filter(selector).toList();
    }

    private java.util.Optional<LedgerLegProjection> projection(LedgerEntry entry, LedgerProjectionRole role,
                    DerivedProjectionViewKind viewKind, LedgerPosting primary)
    {
        return LedgerEntryDefinitionRegistry.lookup(entry).stream() //
                        .flatMap(definition -> definition.getLegDefinitions().stream()) //
                        .filter(leg -> postingMatchesLeg(entry.getType(), primary, leg)) //
                        .filter(leg -> leg.getProjection().isProjecting()) //
                        .filter(leg -> leg.getProjectionRole().filter(role::equals).isPresent()
                                        || matchesViewKind(leg.getProjection(), viewKind))
                        .map(LedgerLegDefinition::getProjection) //
                        .findFirst();
    }

    private boolean matchesViewKind(LedgerLegProjection projection, DerivedProjectionViewKind viewKind)
    {
        return (viewKind == DerivedProjectionViewKind.ACCOUNT && projection.isAccountProjection())
                        || (viewKind == DerivedProjectionViewKind.PORTFOLIO && projection.isPortfolioProjection());
    }

    private boolean postingMatchesLeg(LedgerEntryType entryType, LedgerPosting posting, LedgerLegDefinition leg)
    {
        if (posting.getType() != leg.getPostingType())
            return false;

        var expectedLegCode = expectedCorporateActionLegCode(entryType, leg.getRole());

        return expectedLegCode.isEmpty() || corporateActionLeg(posting) == expectedLegCode.get();
    }

    private java.util.Optional<CorporateActionLeg> expectedCorporateActionLegCode(LedgerEntryType entryType,
                    LedgerLegRole role)
    {
        if (entryType == LedgerEntryType.CORPORATE_ACTION)
        {
            if (role == LedgerLegRole.SOURCE_SECURITY_LEG)
                return java.util.Optional.of(CorporateActionLeg.SOURCE_SECURITY);
            if (role == LedgerLegRole.TARGET_SECURITY_LEG)
                return java.util.Optional.of(CorporateActionLeg.TARGET_SECURITY);
            if (role == LedgerLegRole.SECURITY_CONTEXT_LEG)
                return java.util.Optional.of(CorporateActionLeg.SECURITY_CONTEXT);
            if (role == LedgerLegRole.RECEIVED_SECURITY_LEG)
                return java.util.Optional.of(CorporateActionLeg.TARGET_SECURITY);
            if (role == LedgerLegRole.DISTRIBUTED_SECURITY_LEG)
                return java.util.Optional.of(CorporateActionLeg.DISTRIBUTED_SECURITY);
            if (role == LedgerLegRole.DISTRIBUTED_RIGHT_LEG)
                return java.util.Optional.of(CorporateActionLeg.RIGHT_SECURITY);
            if (role == LedgerLegRole.SOURCE_BOND_LEG)
                return java.util.Optional.of(CorporateActionLeg.SOURCE_SECURITY);
            if (role == LedgerLegRole.PRINCIPAL_REDEMPTION_LEG)
                return java.util.Optional.of(CorporateActionLeg.PRINCIPAL);
        }

        if (role == LedgerLegRole.CASH_COMPENSATION_LEG)
            return java.util.Optional.of(CorporateActionLeg.CASH_COMPENSATION);
        if (role == LedgerLegRole.FEE_LEG)
            return java.util.Optional.of(CorporateActionLeg.FEE);
        if (role == LedgerLegRole.TAX_LEG)
            return java.util.Optional.of(CorporateActionLeg.TAX);
        if (role == LedgerLegRole.ACCRUED_INTEREST_LEG)
            return java.util.Optional.of(CorporateActionLeg.ACCRUED_INTEREST);

        return java.util.Optional.empty();
    }

    private List<LedgerPosting> unitPostings(LedgerEntry entry, LedgerPosting primary)
    {
        if (unitPosting(primary))
            return List.of();

        return entry.getPostings().stream() //
                        .filter(posting -> posting != primary) //
                        .filter(this::unitPosting) //
                        .filter(posting -> primary.getGroupKey() == null
                                        || Objects.equals(primary.getGroupKey(), posting.getGroupKey()))
                        .toList();
    }

    private String primarySelector(LedgerProjectionRole role)
    {
        return "semantic primary for " + role; //$NON-NLS-1$
    }

    private String primarySelector(LedgerProjectionRole role, String semanticInstanceKey)
    {
        return primarySelector(role) + " instance " + semanticInstanceKey; //$NON-NLS-1$
    }

    private String unitSelector(LedgerPosting primary)
    {
        return primary.getGroupKey() == null ? "semantic unit role" //$NON-NLS-1$
                        : "semantic unit role in group " + primary.getGroupKey(); //$NON-NLS-1$
    }

    private Predicate<LedgerPosting> primary()
    {
        return posting -> posting.getUnitRole() == LedgerPostingUnitRole.PRIMARY;
    }

    private Predicate<LedgerPosting> semantic(LedgerPostingSemanticRole role)
    {
        return posting -> posting.getSemanticRole() == role;
    }

    private Predicate<LedgerPosting> postingType(LedgerPostingType type)
    {
        return posting -> posting.getType() == type;
    }

    private Predicate<LedgerPosting> corporateLeg(CorporateActionLeg leg)
    {
        return posting -> posting.getCorporateActionLeg() == leg;
    }

    private Predicate<LedgerPosting> localKey(LedgerProjectionRole role)
    {
        return posting -> role.name().equals(posting.getLocalKey());
    }

    private boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private boolean unitPosting(LedgerPosting posting)
    {
        if (posting.getUnitRole() != null)
            return posting.getUnitRole() != LedgerPostingUnitRole.PRIMARY;

        return switch (posting.getType())
        {
            case FEE, TAX, GROSS_VALUE, FOREX -> true;
            default -> false;
        };
    }

    private CorporateActionLeg corporateActionLeg(LedgerPosting posting)
    {
        if (posting.getCorporateActionLeg() != null)
            return posting.getCorporateActionLeg();

        var code = parameterString(posting, LedgerParameterType.CORPORATE_ACTION_LEG);
        if (code == null)
            return null;

        for (var leg : CorporateActionLeg.values())
            if (leg.getCode().equals(code))
                return leg;

        return null;
    }

    private String parameterString(LedgerPosting posting, LedgerParameterType type)
    {
        return posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == type) //
                        .filter(parameter -> parameter.getValueKind() == LedgerParameter.ValueKind.STRING) //
                        .map(LedgerParameter::getValue) //
                        .map(String.class::cast) //
                        .findFirst().orElse(null);
    }

    public static final class Result
    {
        private final List<DerivedProjectionDescriptor> descriptors;
        private final List<Diagnostic> diagnostics;

        private Result(List<DerivedProjectionDescriptor> descriptors, List<Diagnostic> diagnostics)
        {
            this.descriptors = List.copyOf(descriptors);
            this.diagnostics = List.copyOf(diagnostics);
        }

        public boolean isOK()
        {
            return diagnostics.isEmpty();
        }

        public List<DerivedProjectionDescriptor> getDescriptors()
        {
            return Collections.unmodifiableList(descriptors);
        }

        public List<Diagnostic> getDiagnostics()
        {
            return Collections.unmodifiableList(diagnostics);
        }

        public String formatDiagnostics()
        {
            return diagnostics.stream().map(Diagnostic::format).collect(Collectors.joining("\n")); //$NON-NLS-1$
        }
    }

    public static final class Diagnostic
    {
        public enum IssueCode
        {
            MISSING_SEMANTIC_PRIMARY,
            AMBIGUOUS_SEMANTIC_PRIMARY,
            MISSING_SEMANTIC_INSTANCE_KEY,
            DUPLICATE_SEMANTIC_INSTANCE_KEY
        }

        private final IssueCode code;
        private final LedgerEntry entry;
        private final LedgerProjectionRole role;
        private final String message;
        private final List<String> postingUUIDs;

        private Diagnostic(IssueCode code, LedgerEntry entry, LedgerProjectionRole role, String message,
                        List<String> postingUUIDs)
        {
            this.code = Objects.requireNonNull(code);
            this.entry = Objects.requireNonNull(entry);
            this.role = role;
            this.message = Objects.requireNonNull(message);
            this.postingUUIDs = List.copyOf(postingUUIDs);
        }

        private static Diagnostic missing(LedgerEntry entry, LedgerProjectionRole role, String message)
        {
            return new Diagnostic(IssueCode.MISSING_SEMANTIC_PRIMARY, entry, role, message, List.of());
        }

        private static Diagnostic ambiguous(LedgerEntry entry, LedgerProjectionRole role, String message,
                        List<LedgerPosting> postings)
        {
            return new Diagnostic(IssueCode.AMBIGUOUS_SEMANTIC_PRIMARY, entry, role, message,
                            postings.stream().map(LedgerPosting::getUUID).toList());
        }

        private static Diagnostic missingInstanceKey(LedgerEntry entry, LedgerProjectionRole role, String message)
        {
            return new Diagnostic(IssueCode.MISSING_SEMANTIC_INSTANCE_KEY, entry, role, message, List.of());
        }

        private static Diagnostic duplicateInstanceKey(LedgerEntry entry, LedgerProjectionRole role, String message,
                        LedgerPosting posting)
        {
            return new Diagnostic(IssueCode.DUPLICATE_SEMANTIC_INSTANCE_KEY, entry, role, message,
                            List.of(posting.getUUID()));
        }

        public IssueCode getCode()
        {
            return code;
        }

        public LedgerEntry getEntry()
        {
            return entry;
        }

        public LedgerProjectionRole getRole()
        {
            return role;
        }

        public String getMessage()
        {
            return message;
        }

        public List<String> getPostingUUIDs()
        {
            return Collections.unmodifiableList(postingUUIDs);
        }

        public String format()
        {
            return "[" + code + "] entry=" + entry.getUUID() + " role=" + role + " " + message //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                            + (postingUUIDs.isEmpty() ? "" : " postings=" + postingUUIDs); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
