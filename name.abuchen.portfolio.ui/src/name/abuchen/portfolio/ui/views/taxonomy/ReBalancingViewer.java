package name.abuchen.portfolio.ui.views.taxonomy;

import java.time.LocalDate;

import javax.inject.Inject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;

public class ReBalancingViewer extends AbstractNodeTreeViewer
{
    @Inject
    public ReBalancingViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    protected String readExpansionState()
    {
        return getModel().getExpansionStateRebalancing();
    }

    @Override
    protected void storeExpansionState(String expanded)
    {
        getModel().setExpansionStateRebalancing(expanded);
    }

    @Override
    protected void addColumns(ShowHideColumnHelper support)
    {
        addDimensionColumn(support);

        addDesiredAllocationColumn(support);

        Column column = new Column("targetvalue", Messages.ColumnTargetValue, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() ? Values.Money.format(node.getTarget(), getModel().getCurrencyCode())
                                : null;
            }
        });
        support.addColumn(column);

        addActualColumns(support);

        column = new Column("delta%", Messages.ColumnDeltaPercent, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.getTarget() == null)
                    return null;

                return Values.Percent.format(
                                ((double) node.getActual().getAmount() / (double) node.getTarget().getAmount()) - 1);
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.getTarget() == null)
                    return null;
                return Display.getCurrent().getSystemColor(node.getActual().isGreaterOrEqualThan(node.getTarget())
                                ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
            }
        });
        support.addColumn(column);

        column = new Column("delta", Messages.ColumnDeltaValue, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.getTarget() == null)
                    return null;
                return Values.Money.format(node.getActual().subtract(node.getTarget()), getModel().getCurrencyCode());
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.getTarget() == null)
                    return null;
                return Display.getCurrent().getSystemColor(node.getActual().isGreaterOrEqualThan(node.getTarget())
                                ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
            }
        });
        support.addColumn(column);

        column = new Column("quote", Messages.ColumnQuote, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null || security.getCurrencyCode() == null)
                    return null;

                SecurityPrice price = security.getSecurityPrice(LocalDate.now());
                return Values.Quote.format(security.getCurrencyCode(), price.getValue(), getModel().getCurrencyCode());
            }
        });
        support.addColumn(column);

        column = new Column("shares", Messages.ColumnSharesOwned, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null || security.getCurrencyCode() == null)
                    return null;

                AssetPosition position = getModel().getClientSnapshot().getPositionsByVehicle().get(security);
                if (position == null)
                    return null;
                
                return Math.round(position.getPosition().getShares() * node.getWeight()
                                / (double) Classification.ONE_HUNDRED_PERCENT);
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("deltashares", Messages.ColumnDeltaShares, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                // no delta shares for unassigned securities
                if (node.getParent() != null && node.getParent().isUnassignedCategory())
                    return null;

                Security security = node.getBackingSecurity();
                if (security == null || security.getCurrencyCode() == null)
                    return null;

                String priceCurrency = security.getCurrencyCode();
                long price = security.getSecurityPrice(LocalDate.now()).getValue();
                long weightedPrice = Math.round(node.getWeight() * price / (double) Classification.ONE_HUNDRED_PERCENT);
                if (weightedPrice == 0L)
                    return Values.Share.format(0L);

                String deltaCurrency = node.getActual().getCurrencyCode();
                long delta = node.getParent().getTarget().getAmount() - node.getParent().getActual().getAmount();

                // if currency of the data (here: deltaCurrency) does not match
                // the currency of the security (here: priceCurrency), convert
                // delta in order to know how many shares need to bought or sold
                if (!deltaCurrency.equals(priceCurrency))
                {
                    delta = getModel().getCurrencyConverter().with(priceCurrency)
                                    .convert(LocalDate.now(), Money.of(deltaCurrency, delta)).getAmount();
                }

                long shares = Math
                                .round(delta * Values.Share.divider() * Values.Quote.dividerToMoney() / weightedPrice);
                return Values.Share.format(shares);
            }
        });
        support.addColumn(column);

        addAdditionalColumns(support);
    }

    private void addDesiredAllocationColumn(ShowHideColumnHelper support)
    {
        Column column = new Column("desiredAllocation", Messages.ColumnDesiredAllocation, SWT.RIGHT, 70); //$NON-NLS-1$
        column.setDescription(Messages.ColumnDesiredAllocation_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() ? Values.Weight.format(node.getWeight())
                                : node.isUnassignedCategory() ? Messages.LabelNotAvailable : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() && getModel().hasWeightError(node)
                                ? Display.getDefault().getSystemColor(SWT.COLOR_BLACK) : null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() && getModel().hasWeightError(node) ? getWarningColor() : null;
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() && getModel().hasWeightError(node) ? Images.QUICKFIX.image() : null;
            }
        });
        new ValueEditingSupport(TaxonomyNode.class, "weight", Values.Weight) //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.isAssignment() || node.isUnassignedCategory())
                    return false;
                return super.canEdit(element);
            }

        }.addListener(this::onWeightModified).attachTo(column);
        support.addColumn(column);
    }

    @Override
    protected void fillContextMenu(IMenuManager manager)
    {
        super.fillContextMenu(manager);

        final IStructuredSelection selection = getNodeViewer().getStructuredSelection();
        if (selection.isEmpty() || selection.size() > 1)
            return;

        final TaxonomyNode node = (TaxonomyNode) selection.getFirstElement();
        if (node == null || node.isUnassignedCategory())
            return;

        if (node.isClassification() && getModel().hasWeightError(node))
        {
            manager.appendToGroup(MENU_GROUP_CUSTOM_ACTIONS,
                            new SimpleAction(Messages.MenuTaxonomyWeightFix, a -> doFixClassificationWeights(node)));
        }

    }

    private void doFixClassificationWeights(TaxonomyNode node)
    {
        Classification classification = node.getClassification();

        if (node.isUnassignedCategory())
        {
            classification.setWeight(0);
        }
        else if (node.isRoot() || node.getParent().isRoot())
        {
            classification.setWeight(Classification.ONE_HUNDRED_PERCENT);
        }
        else
        {
            classification.setWeight(0);
            int weight = Math.max(0,
                            Classification.ONE_HUNDRED_PERCENT - classification.getParent().getChildrenWeight());
            classification.setWeight(weight);
        }
        onTaxnomyNodeEdited(node);
    }

}
