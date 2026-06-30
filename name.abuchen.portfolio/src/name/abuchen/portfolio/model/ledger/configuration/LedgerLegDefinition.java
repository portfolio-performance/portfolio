package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Describes one functional leg or component inside a native Ledger entry.
 * This is Java-only configuration metadata. It connects a business leg role to
 * a generic posting type, parameter rules, optional projection expectations,
 * and optional grouping metadata.
 */
public final class LedgerLegDefinition
{
    private final LedgerLegRole role;
    private final LedgerPostingType postingType;
    private final LedgerLegCardinality cardinality;
    private final Set<LedgerParameterType> requiredParameterTypes;
    private final Set<LedgerParameterType> optionalParameterTypes;
    private final LedgerProjectionRole projectionRole;
    private final boolean primaryPostingExpected;
    private final boolean postingGroupExpected;
    private final Set<String> groupNames;
    private final LedgerReportingClass reportingClass;
    private final LedgerPerformanceTreatment performanceTreatment;

    private LedgerLegDefinition(Builder builder)
    {
        this.role = Objects.requireNonNull(builder.role);
        this.postingType = Objects.requireNonNull(builder.postingType);
        this.cardinality = Objects.requireNonNull(builder.cardinality);
        this.requiredParameterTypes = copyParameterTypes(builder.requiredParameterTypes);
        this.optionalParameterTypes = copyParameterTypes(builder.optionalParameterTypes);
        this.projectionRole = builder.projectionRole;
        this.primaryPostingExpected = builder.primaryPostingExpected;
        this.postingGroupExpected = builder.postingGroupExpected;
        this.groupNames = copyGroupNames(builder.groupNames);
        this.reportingClass = Objects.requireNonNull(builder.reportingClass);
        this.performanceTreatment = Objects.requireNonNull(builder.performanceTreatment);
    }

    public static Builder of(LedgerLegRole role, LedgerPostingType postingType, LedgerLegCardinality cardinality)
    {
        return new Builder(role, postingType, cardinality);
    }

    public LedgerLegRole getRole()
    {
        return role;
    }

    public LedgerPostingType getPostingType()
    {
        return postingType;
    }

    public LedgerLegCardinality getCardinality()
    {
        return cardinality;
    }

    public Set<LedgerParameterType> getRequiredParameterTypes()
    {
        return requiredParameterTypes;
    }

    public Set<LedgerParameterType> getOptionalParameterTypes()
    {
        return optionalParameterTypes;
    }

    public Optional<LedgerProjectionRole> getProjectionRole()
    {
        return Optional.ofNullable(projectionRole);
    }

    public boolean isPrimaryPostingExpected()
    {
        return primaryPostingExpected;
    }

    public boolean isPostingGroupExpected()
    {
        return postingGroupExpected;
    }

    public Set<String> getGroupNames()
    {
        return groupNames;
    }

    public LedgerReportingClass getReportingClass()
    {
        return reportingClass;
    }

    public LedgerPerformanceTreatment getPerformanceTreatment()
    {
        return performanceTreatment;
    }

    private static Set<LedgerParameterType> copyParameterTypes(Set<LedgerParameterType> values)
    {
        var copy = EnumSet.noneOf(LedgerParameterType.class);

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> copyGroupNames(Set<String> values)
    {
        var copy = new LinkedHashSet<String>();

        for (var value : values)
        {
            if (value == null || value.isBlank())
                throw new IllegalArgumentException(
                                LedgerDiagnosticCode.LEDGER_CORE_018.message("Ledger leg group name is required")); //$NON-NLS-1$

            copy.add(value);
        }

        return Collections.unmodifiableSet(copy);
    }

    public static final class Builder
    {
        private final LedgerLegRole role;
        private final LedgerPostingType postingType;
        private final LedgerLegCardinality cardinality;
        private final Set<LedgerParameterType> requiredParameterTypes = EnumSet.noneOf(LedgerParameterType.class);
        private final Set<LedgerParameterType> optionalParameterTypes = EnumSet.noneOf(LedgerParameterType.class);
        private LedgerProjectionRole projectionRole;
        private boolean primaryPostingExpected;
        private boolean postingGroupExpected;
        private final Set<String> groupNames = new LinkedHashSet<String>();
        private LedgerReportingClass reportingClass = LedgerReportingClass.UNDEFINED;
        private LedgerPerformanceTreatment performanceTreatment = LedgerPerformanceTreatment.UNDEFINED;

        private Builder(LedgerLegRole role, LedgerPostingType postingType, LedgerLegCardinality cardinality)
        {
            this.role = Objects.requireNonNull(role);
            this.postingType = Objects.requireNonNull(postingType);
            this.cardinality = Objects.requireNonNull(cardinality);
        }

        public Builder requiredParameters(Set<LedgerParameterType> values)
        {
            requiredParameterTypes.addAll(values);
            return this;
        }

        public Builder optionalParameters(Set<LedgerParameterType> values)
        {
            optionalParameterTypes.addAll(values);
            return this;
        }

        public Builder projection(LedgerProjectionRole role, boolean primaryPostingExpected,
                        boolean postingGroupExpected)
        {
            this.projectionRole = Objects.requireNonNull(role);
            this.primaryPostingExpected = primaryPostingExpected;
            this.postingGroupExpected = postingGroupExpected;
            return this;
        }

        public Builder group(String name)
        {
            groupNames.add(name);
            return this;
        }

        public Builder reportingClass(LedgerReportingClass reportingClass)
        {
            this.reportingClass = Objects.requireNonNull(reportingClass);
            return this;
        }

        public Builder performanceTreatment(LedgerPerformanceTreatment performanceTreatment)
        {
            this.performanceTreatment = Objects.requireNonNull(performanceTreatment);
            return this;
        }

        public LedgerLegDefinition build()
        {
            return new LedgerLegDefinition(this);
        }
    }
}
