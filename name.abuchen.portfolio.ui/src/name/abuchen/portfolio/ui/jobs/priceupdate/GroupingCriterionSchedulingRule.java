package name.abuchen.portfolio.ui.jobs.priceupdate;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Ensure that multiple jobs running tasks against one site are not running in
 * parallel. Background: rate limiting is done on per site basis.
 */
/* package */ class GroupingCriterionSchedulingRule implements ISchedulingRule
{
    private final String criterion;

    GroupingCriterionSchedulingRule(String criterion)
    {
        this.criterion = criterion;
    }

    @Override
    public boolean contains(ISchedulingRule rule)
    {
        return isConflicting(rule);
    }

    @Override
    public boolean isConflicting(ISchedulingRule rule)
    {
        return rule instanceof GroupingCriterionSchedulingRule groupingCriterion
                        && groupingCriterion.criterion.equals(this.criterion);
    }
}
