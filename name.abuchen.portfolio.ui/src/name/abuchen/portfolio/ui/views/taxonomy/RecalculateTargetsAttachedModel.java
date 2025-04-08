package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.Money;

public class RecalculateTargetsAttachedModel implements TaxonomyModel.AttachedModel
{
    @Override
    public void recalculate(TaxonomyModel model)
    {
        TaxonomyNode virtualRootNode = model.getVirtualRootNode();
        TaxonomyNode unassignedNode = model.getUnassignedNode();

        virtualRootNode.setTarget(virtualRootNode.getActual().subtract(unassignedNode.getActual()));

        model.visitAll(node -> {
            if (node.isClassification() && !node.isRoot())
            {
                Money parent = node.getParent().getTarget();
                Money parentAssignmentDirectChilds = node.getParent().getChildren().stream()
                                .filter(child -> child.isAssignment())
                                .map(assignment -> assignment.getActual())
                                .reduce(Money.of(parent.getCurrencyCode(), 0), Money::add);
                Money target = Money.of(parent.getCurrencyCode(),
                                Math.round(parent.subtract(parentAssignmentDirectChilds).getAmount() * node.getWeight()
                                                / (double) Classification.ONE_HUNDRED_PERCENT));
                node.setTarget(target);
            }
        });
    }
}
