package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerParameterRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingGroupRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerProjectionRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirement;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirementGroup;

/**
 * Describes Ledger configuration for entries, postings, parameters, or native shapes.
 * This metadata is used by Ledger validation and assembly infrastructure. It is not a
 * normal transaction-editing API.
 *
 * <p>
 * This definition is not persisted as a standalone object. It describes how persisted entry
 * type ids, posting type codes, parameter type codes, and projection roles are expected to fit
 * together for validation and native entry assembly.
 * </p>
 */
public final class LedgerEntryDefinition
{
    private final LedgerEntryType entryType;
    private final LedgerNativeEntryShape nativeShape;
    private final Set<LedgerPostingRule> postingRules;
    private final Set<LedgerParameterRule> entryParameterRules;
    private final Set<LedgerParameterRule> postingParameterRules;
    private final Set<LedgerProjectionRule> projectionRules;
    private final Set<LedgerPostingGroupRule> postingGroupRules;
    private final Set<LedgerRequirementGroup> alternativeRequirementGroups;
    private final Set<LedgerLegDefinition> legDefinitions;
    private final Set<LedgerPostingType> postingTypes;
    private final Set<LedgerParameterType> entryParameterTypes;
    private final Set<LedgerParameterType> postingParameterTypes;
    private final Set<LedgerProjectionRole> projectionRoles;
    private final LedgerReportingClass reportingClass;
    private final LedgerPerformanceTreatment performanceTreatment;
    private final Set<LedgerDownstreamResult> downstreamResultsNotPersisted;

    private LedgerEntryDefinition(LedgerEntryType entryType, LedgerNativeEntryShape nativeShape,
                    Set<LedgerPostingRule> postingRules, Set<LedgerParameterRule> entryParameterRules,
                    Set<LedgerParameterRule> postingParameterRules, Set<LedgerProjectionRule> projectionRules,
                    Set<LedgerPostingGroupRule> postingGroupRules,
                    Set<LedgerRequirementGroup> alternativeRequirementGroups,
                    Set<LedgerLegDefinition> legDefinitions,
                    LedgerReportingClass reportingClass, LedgerPerformanceTreatment performanceTreatment,
                    Set<LedgerDownstreamResult> downstreamResultsNotPersisted)
    {
        this.entryType = Objects.requireNonNull(entryType);
        this.nativeShape = Objects.requireNonNull(nativeShape);
        this.postingRules = copyRuleSet(postingRules);
        this.entryParameterRules = copyRuleSet(entryParameterRules);
        this.postingParameterRules = copyRuleSet(postingParameterRules);
        this.projectionRules = copyRuleSet(projectionRules);
        this.postingGroupRules = copyRuleSet(postingGroupRules);
        this.alternativeRequirementGroups = copyRuleSet(alternativeRequirementGroups);
        this.legDefinitions = copyLegDefinitions(legDefinitions);
        this.postingTypes = postingTypesFrom(this.postingRules);
        this.entryParameterTypes = parameterTypesFrom(this.entryParameterRules);
        this.postingParameterTypes = postingParameterTypesFrom(this.postingParameterRules, this.postingRules);
        this.projectionRoles = projectionRolesFrom(this.projectionRules);
        this.reportingClass = Objects.requireNonNull(reportingClass);
        this.performanceTreatment = Objects.requireNonNull(performanceTreatment);
        this.downstreamResultsNotPersisted = copyOf(LedgerDownstreamResult.class, downstreamResultsNotPersisted);
    }

    static LedgerEntryDefinition of(LedgerEntryType entryType, LedgerNativeEntryShape nativeShape,
                    Set<LedgerPostingType> postingTypes, Set<LedgerParameterType> entryParameterTypes,
                    Set<LedgerParameterType> postingParameterTypes, Set<LedgerProjectionRole> projectionRoles,
                    LedgerReportingClass reportingClass, LedgerPerformanceTreatment performanceTreatment,
                    Set<LedgerDownstreamResult> downstreamResultsNotPersisted)
    {
        return new LedgerEntryDefinition(entryType, nativeShape, optionalPostingRules(postingTypes),
                        optionalParameterRules(entryParameterTypes), optionalParameterRules(postingParameterTypes),
                        optionalProjectionRules(projectionRoles), Set.of(), Set.of(), Set.of(), reportingClass,
                        performanceTreatment, downstreamResultsNotPersisted);
    }

    static LedgerEntryDefinition of(LedgerEntryType entryType, LedgerNativeEntryShape nativeShape,
                    Set<LedgerPostingRule> postingRules, Set<LedgerParameterRule> entryParameterRules,
                    Set<LedgerParameterRule> postingParameterRules, Set<LedgerProjectionRule> projectionRules,
                    Set<LedgerPostingGroupRule> postingGroupRules,
                    Set<LedgerRequirementGroup> alternativeRequirementGroups, LedgerReportingClass reportingClass,
                    LedgerPerformanceTreatment performanceTreatment,
                    Set<LedgerDownstreamResult> downstreamResultsNotPersisted)
    {
        return new LedgerEntryDefinition(entryType, nativeShape, postingRules, entryParameterRules,
                        postingParameterRules, projectionRules, postingGroupRules, alternativeRequirementGroups, Set.of(),
                        reportingClass, performanceTreatment, downstreamResultsNotPersisted);
    }

    static LedgerEntryDefinition of(LedgerEntryType entryType, LedgerNativeEntryShape nativeShape,
                    Set<LedgerPostingRule> postingRules, Set<LedgerParameterRule> entryParameterRules,
                    Set<LedgerParameterRule> postingParameterRules, Set<LedgerProjectionRule> projectionRules,
                    Set<LedgerPostingGroupRule> postingGroupRules,
                    Set<LedgerRequirementGroup> alternativeRequirementGroups, Set<LedgerLegDefinition> legDefinitions,
                    LedgerReportingClass reportingClass, LedgerPerformanceTreatment performanceTreatment,
                    Set<LedgerDownstreamResult> downstreamResultsNotPersisted)
    {
        return new LedgerEntryDefinition(entryType, nativeShape, postingRules, entryParameterRules,
                        postingParameterRules, projectionRules, postingGroupRules, alternativeRequirementGroups,
                        legDefinitions, reportingClass, performanceTreatment, downstreamResultsNotPersisted);
    }

    public LedgerEntryType getEntryType()
    {
        return entryType;
    }

    public LedgerNativeEntryShape getNativeShape()
    {
        return nativeShape;
    }

    public Set<LedgerPostingType> getPostingTypes()
    {
        return postingTypes;
    }

    public Set<LedgerPostingRule> getPostingRules()
    {
        return postingRules;
    }

    public Set<LedgerPostingRule> getRequiredPostingRules()
    {
        return filterPostingRules(postingRules, LedgerRequirement.REQUIRED);
    }

    public Set<LedgerPostingRule> getOptionalPostingRules()
    {
        return filterPostingRules(postingRules, LedgerRequirement.OPTIONAL);
    }

    public Set<LedgerParameterType> getEntryParameterTypes()
    {
        return entryParameterTypes;
    }

    public Set<LedgerParameterRule> getEntryParameterRules()
    {
        return entryParameterRules;
    }

    public Set<LedgerParameterRule> getRequiredEntryParameterRules()
    {
        return filterParameterRules(entryParameterRules, LedgerRequirement.REQUIRED);
    }

    public Set<LedgerParameterRule> getOptionalEntryParameterRules()
    {
        return filterParameterRules(entryParameterRules, LedgerRequirement.OPTIONAL);
    }

    public Set<LedgerParameterType> getPostingParameterTypes()
    {
        return postingParameterTypes;
    }

    public Set<LedgerParameterRule> getPostingParameterRules()
    {
        return postingParameterRules;
    }

    public Set<LedgerParameterRule> getRequiredPostingParameterRules()
    {
        return filterParameterRules(postingParameterRules, LedgerRequirement.REQUIRED);
    }

    public Set<LedgerParameterRule> getOptionalPostingParameterRules()
    {
        return filterParameterRules(postingParameterRules, LedgerRequirement.OPTIONAL);
    }

    public Set<LedgerParameterType> getRepeatableParameterTypes()
    {
        var values = EnumSet.noneOf(LedgerParameterType.class);

        for (var rule : entryParameterRules)
            if (rule.isRepeatable())
                values.add(rule.getParameterType());

        for (var rule : postingParameterRules)
            if (rule.isRepeatable())
                values.add(rule.getParameterType());

        for (var rule : postingRules)
            values.addAll(rule.getRepeatableParameterTypes());

        return Collections.unmodifiableSet(values);
    }

    public Set<LedgerProjectionRole> getProjectionRoles()
    {
        return projectionRoles;
    }

    public Set<LedgerProjectionRule> getProjectionRules()
    {
        return projectionRules;
    }

    public Set<LedgerProjectionRule> getRequiredProjectionRules()
    {
        return filterProjectionRules(projectionRules, LedgerRequirement.REQUIRED);
    }

    public Set<LedgerProjectionRule> getOptionalProjectionRules()
    {
        return filterProjectionRules(projectionRules, LedgerRequirement.OPTIONAL);
    }

    public Set<LedgerPostingGroupRule> getPostingGroupRules()
    {
        return postingGroupRules;
    }

    public Set<LedgerRequirementGroup> getAlternativeRequirementGroups()
    {
        return alternativeRequirementGroups;
    }

    public Set<LedgerLegDefinition> getLegDefinitions()
    {
        return legDefinitions;
    }

    public Optional<LedgerLegDefinition> getLegDefinition(LedgerLegRole role)
    {
        return legDefinitions.stream().filter(definition -> definition.getRole() == role).findFirst();
    }

    public LedgerReportingClass getReportingClass()
    {
        return reportingClass;
    }

    public LedgerPerformanceTreatment getPerformanceTreatment()
    {
        return performanceTreatment;
    }

    public Set<LedgerDownstreamResult> getDownstreamResultsNotPersisted()
    {
        return downstreamResultsNotPersisted;
    }

    private static <E extends Enum<E>> Set<E> copyOf(Class<E> type, Set<E> values)
    {
        Objects.requireNonNull(values);

        var copy = EnumSet.noneOf(type);

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }

    private static <T> Set<T> copyRuleSet(Set<T> values)
    {
        Objects.requireNonNull(values);

        var copy = new LinkedHashSet<T>();

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }

    private static Set<LedgerLegDefinition> copyLegDefinitions(Set<LedgerLegDefinition> values)
    {
        Objects.requireNonNull(values);

        var roles = EnumSet.noneOf(LedgerLegRole.class);
        var copy = new LinkedHashSet<LedgerLegDefinition>();

        for (var value : values)
        {
            var definition = Objects.requireNonNull(value);

            if (!roles.add(definition.getRole()))
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_015
                                .message("Duplicate Ledger leg role: " + definition.getRole())); //$NON-NLS-1$

            copy.add(definition);
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Set<LedgerPostingType> postingTypesFrom(Set<LedgerPostingRule> rules)
    {
        var values = EnumSet.noneOf(LedgerPostingType.class);

        for (var rule : rules)
            values.add(rule.getPostingType());

        return Collections.unmodifiableSet(values);
    }

    private static Set<LedgerParameterType> parameterTypesFrom(Set<LedgerParameterRule> rules)
    {
        var values = EnumSet.noneOf(LedgerParameterType.class);

        for (var rule : rules)
            values.add(rule.getParameterType());

        return Collections.unmodifiableSet(values);
    }

    private static Set<LedgerParameterType> postingParameterTypesFrom(Set<LedgerParameterRule> parameterRules,
                    Set<LedgerPostingRule> postingRules)
    {
        var values = EnumSet.noneOf(LedgerParameterType.class);
        values.addAll(parameterTypesFrom(parameterRules));

        for (var rule : postingRules)
        {
            values.addAll(rule.getRequiredParameterTypes());
            values.addAll(rule.getOptionalParameterTypes());
        }

        return Collections.unmodifiableSet(values);
    }

    private static Set<LedgerProjectionRole> projectionRolesFrom(Set<LedgerProjectionRule> rules)
    {
        var values = EnumSet.noneOf(LedgerProjectionRole.class);

        for (var rule : rules)
            values.add(rule.getRole());

        return Collections.unmodifiableSet(values);
    }

    private static Set<LedgerPostingRule> optionalPostingRules(Set<LedgerPostingType> postingTypes)
    {
        Objects.requireNonNull(postingTypes);

        var rules = new LinkedHashSet<LedgerPostingRule>();

        for (var postingType : postingTypes)
            rules.add(LedgerPostingRule.optional(postingType, EnumSet.noneOf(LedgerParameterType.class),
                            EnumSet.noneOf(LedgerParameterType.class)));

        return Collections.unmodifiableSet(rules);
    }

    private static Set<LedgerParameterRule> optionalParameterRules(Set<LedgerParameterType> parameterTypes)
    {
        Objects.requireNonNull(parameterTypes);

        var rules = new LinkedHashSet<LedgerParameterRule>();

        for (var parameterType : parameterTypes)
            rules.add(LedgerParameterRule.optional(parameterType));

        return Collections.unmodifiableSet(rules);
    }

    private static Set<LedgerProjectionRule> optionalProjectionRules(Set<LedgerProjectionRole> projectionRoles)
    {
        Objects.requireNonNull(projectionRoles);

        var rules = new LinkedHashSet<LedgerProjectionRule>();

        for (var projectionRole : projectionRoles)
            rules.add(LedgerProjectionRule.optional(projectionRole, true, false));

        return Collections.unmodifiableSet(rules);
    }

    private static Set<LedgerPostingRule> filterPostingRules(Set<LedgerPostingRule> rules,
                    LedgerRequirement requirement)
    {
        var filtered = new LinkedHashSet<LedgerPostingRule>();

        for (var rule : rules)
        {
            if (rule.getRequirement() == requirement)
                filtered.add(rule);
        }

        return Collections.unmodifiableSet(filtered);
    }

    private static Set<LedgerParameterRule> filterParameterRules(Set<LedgerParameterRule> rules,
                    LedgerRequirement requirement)
    {
        var filtered = new LinkedHashSet<LedgerParameterRule>();

        for (var rule : rules)
        {
            if (rule.getRequirement() == requirement)
                filtered.add(rule);
        }

        return Collections.unmodifiableSet(filtered);
    }

    private static Set<LedgerProjectionRule> filterProjectionRules(Set<LedgerProjectionRule> rules,
                    LedgerRequirement requirement)
    {
        var filtered = new LinkedHashSet<LedgerProjectionRule>();

        for (var rule : rules)
        {
            if (rule.getRequirement() == requirement)
                filtered.add(rule);
        }

        return Collections.unmodifiableSet(filtered);
    }
}
