package name.abuchen.portfolio.model.ledger.projection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
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

        switch (entry.getType())
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND, DIVIDENDS ->
                account(entry, LedgerProjectionRole.ACCOUNT,
                                primary().or(legacyAccountPrimary(entry.getType())), diagnostics)
                                                .ifPresent(descriptors::add);
            case BUY, SELL -> {
                account(entry, LedgerProjectionRole.ACCOUNT,
                                primary().and(semantic(LedgerPostingSemanticRole.CASH))
                                                .or(legacyPrimary(LedgerPostingType.CASH)), diagnostics)
                                                .ifPresent(descriptors::add);
                portfolio(entry, LedgerProjectionRole.PORTFOLIO,
                                primary().and(semantic(LedgerPostingSemanticRole.SECURITY))
                                                .or(legacyPrimary(LedgerPostingType.SECURITY)), diagnostics)
                                                .ifPresent(descriptors::add);
            }
            case DELIVERY_INBOUND -> portfolio(entry, LedgerProjectionRole.DELIVERY_INBOUND,
                            primary().and(semantic(LedgerPostingSemanticRole.SECURITY))
                                            .and(direction(LedgerPostingDirection.INBOUND))
                                            .or(legacyPrimary(LedgerPostingType.SECURITY)),
                            diagnostics).ifPresent(descriptors::add);
            case DELIVERY_OUTBOUND -> portfolio(entry, LedgerProjectionRole.DELIVERY_OUTBOUND,
                            primary().and(semantic(LedgerPostingSemanticRole.SECURITY))
                                            .and(direction(LedgerPostingDirection.OUTBOUND))
                                            .or(legacyPrimary(LedgerPostingType.SECURITY)),
                            diagnostics).ifPresent(descriptors::add);
            case CASH_TRANSFER -> {
                account(entry, LedgerProjectionRole.SOURCE_ACCOUNT,
                                primary().and(direction(LedgerPostingDirection.OUTBOUND)), diagnostics)
                                                .ifPresent(descriptors::add);
                account(entry, LedgerProjectionRole.TARGET_ACCOUNT,
                                primary().and(direction(LedgerPostingDirection.INBOUND)), diagnostics)
                                                .ifPresent(descriptors::add);
            }
            case SECURITY_TRANSFER -> {
                portfolio(entry, LedgerProjectionRole.SOURCE_PORTFOLIO,
                                primary().and(direction(LedgerPostingDirection.OUTBOUND)), diagnostics)
                                                .ifPresent(descriptors::add);
                portfolio(entry, LedgerProjectionRole.TARGET_PORTFOLIO,
                                primary().and(direction(LedgerPostingDirection.INBOUND)), diagnostics)
                                                .ifPresent(descriptors::add);
            }
            case CORPORATE_ACTION -> corporateAction(entry, descriptors, diagnostics);
            default -> diagnostics.add(Diagnostic.missing(entry, null,
                            "No derived projection rule for entry type " + entry.getType())); //$NON-NLS-1$
        }

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

        if (kind.filter(k -> k == CorporateActionKind.CASH_DISTRIBUTION || k == CorporateActionKind.COUPON_PAYMENT)
                        .isPresent())
            cashOrientedCorporateAction(entry, descriptors, diagnostics);
    }

    private void spinOff(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors, List<Diagnostic> diagnostics)
    {
        repeatedPortfolio(entry, LedgerProjectionRole.OLD_SECURITY_LEG,
                        primary().and(corporateLeg(CorporateActionLeg.SOURCE_SECURITY))
                                        .and(localKey(LedgerProjectionRole.OLD_SECURITY_LEG))
                                        .or(legacyCorporateLeg(LedgerPostingType.SECURITY,
                                                        CorporateActionLeg.SOURCE_SECURITY)),
                        primary().and(corporateLeg(CorporateActionLeg.SOURCE_SECURITY)), true, diagnostics)
                                        .forEach(descriptors::add);
        optionalPortfolio(entry, LedgerProjectionRole.DELIVERY_INBOUND,
                        primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY))
                                        .and(localKey(LedgerProjectionRole.DELIVERY_INBOUND))
                                        .or(legacyRetainedSpinOffTarget()),
                        diagnostics).ifPresent(descriptors::add);
        repeatedPortfolio(entry, LedgerProjectionRole.NEW_SECURITY_LEG,
                        primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY))
                                        .and(localKey(LedgerProjectionRole.NEW_SECURITY_LEG))
                                        .or(legacyNewSpinOffTarget()),
                        primary().and(corporateLeg(CorporateActionLeg.TARGET_SECURITY))
                                        .and(localKey(LedgerProjectionRole.DELIVERY_INBOUND).negate()),
                        true, diagnostics).forEach(descriptors::add);
        repeatedAccount(entry, LedgerProjectionRole.CASH_COMPENSATION,
                        primary().and(corporateLeg(CorporateActionLeg.CASH_COMPENSATION))
                                        .and(localKey(LedgerProjectionRole.CASH_COMPENSATION))
                                        .or(legacyCorporateLeg(LedgerPostingType.CASH_COMPENSATION,
                                                        CorporateActionLeg.CASH_COMPENSATION)),
                        primary().and(corporateLeg(CorporateActionLeg.CASH_COMPENSATION)), true, diagnostics)
                        .forEach(descriptors::add);
    }

    private void cashOrientedCorporateAction(LedgerEntry entry, List<DerivedProjectionDescriptor> descriptors,
                    List<Diagnostic> diagnostics)
    {
        repeatedAccount(entry, LedgerProjectionRole.ACCOUNT,
                        primary().and(postingType(LedgerPostingType.CASH)).and(localKey(LedgerProjectionRole.ACCOUNT)),
                        primary().and(postingType(LedgerPostingType.CASH))
                                        .and(semantic(LedgerPostingSemanticRole.CASH)),
                        true, diagnostics).forEach(descriptors::add);
    }

    private java.util.Optional<DerivedProjectionDescriptor> account(LedgerEntry entry, LedgerProjectionRole role,
                    Predicate<LedgerPosting> selector, List<Diagnostic> diagnostics)
    {
        return descriptor(entry, role, DerivedProjectionViewKind.ACCOUNT, selector, false, diagnostics);
    }

    private java.util.Optional<DerivedProjectionDescriptor> portfolio(LedgerEntry entry, LedgerProjectionRole role,
                    Predicate<LedgerPosting> selector, List<Diagnostic> diagnostics)
    {
        return descriptor(entry, role, DerivedProjectionViewKind.PORTFOLIO, selector, false, diagnostics);
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

        return java.util.Optional.of(new DerivedProjectionDescriptor(entry, role, viewKind, account, portfolio,
                        primary, unitPostings(entry, primary), primarySelector(role), unitSelector(primary)));
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

            descriptors.add(new DerivedProjectionDescriptor(entry, role, viewKind, account, portfolio, primary,
                            unitPostings(entry, primary), primary.getLocalKey(),
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

    private List<LedgerPosting> unitPostings(LedgerEntry entry, LedgerPosting primary)
    {
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

    private Predicate<LedgerPosting> direction(LedgerPostingDirection direction)
    {
        return posting -> posting.getDirection() == direction;
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

    private Predicate<LedgerPosting> legacyAccountPrimary(LedgerEntryType entryType)
    {
        return legacyPrimary(switch (entryType)
        {
            case FEES, FEES_REFUND -> LedgerPostingType.FEE;
            case TAXES, TAX_REFUND -> LedgerPostingType.TAX;
            default -> LedgerPostingType.CASH;
        });
    }

    private Predicate<LedgerPosting> legacyPrimary(LedgerPostingType type)
    {
        return posting -> posting.getUnitRole() == null && posting.getType() == type
                        && (posting.getAccount() != null || posting.getPortfolio() != null);
    }

    private Predicate<LedgerPosting> legacyCorporateLeg(LedgerPostingType type, CorporateActionLeg leg)
    {
        return posting -> legacyPrimary(type).test(posting) && corporateActionLeg(posting) == leg;
    }

    private Predicate<LedgerPosting> legacyRetainedSpinOffTarget()
    {
        return legacyCorporateLeg(LedgerPostingType.SECURITY, CorporateActionLeg.TARGET_SECURITY)
                        .and(posting -> posting.getSecurity() != null
                                        && posting.getSecurity() == parameterSecurity(posting,
                                                        LedgerParameterType.SOURCE_SECURITY));
    }

    private Predicate<LedgerPosting> legacyNewSpinOffTarget()
    {
        return legacyCorporateLeg(LedgerPostingType.SECURITY, CorporateActionLeg.TARGET_SECURITY)
                        .and(posting -> posting.getSecurity() != null
                                        && posting.getSecurity() != parameterSecurity(posting,
                                                        LedgerParameterType.SOURCE_SECURITY));
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

    private Security parameterSecurity(LedgerPosting posting, LedgerParameterType type)
    {
        return posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == type) //
                        .filter(parameter -> parameter.getValueKind() == LedgerParameter.ValueKind.SECURITY) //
                        .map(LedgerParameter::getValue) //
                        .map(Security.class::cast) //
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
