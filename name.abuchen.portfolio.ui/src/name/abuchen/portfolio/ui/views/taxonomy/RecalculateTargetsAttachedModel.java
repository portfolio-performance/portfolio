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
                Money target = Money.of(parent.getCurrencyCode(), Math.round(
                                parent.getAmount() * node.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT));
                node.setTarget(target);
            }
        });
    }
}
