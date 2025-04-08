package name.abuchen.portfolio.ui.views.taxonomy;

import java.text.MessageFormat;
import java.time.LocalDate;

import jakarta.inject.Inject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.FunctionalBooleanEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;

public class ReBalancingViewer extends AbstractNodeTreeViewer
{
    @Inject
    public ReBalancingViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(view, model, renderer);
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

        Column column = new Column("used-for-rebalancing", Messages.ColumnUsedForRebalancing, SWT.RIGHT, 40); //$NON-NLS-1$
        column.setDescription(Messages.ColumnUsedForRebalancing_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ""; //$NON-NLS-1$
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                if (investmentVehicle == null)
                    return null;

                return getModel().getTaxonomy().isUsedForRebalancing(investmentVehicle) ? Images.CHECK.image() : null;
            }
        });
        new FunctionalBooleanEditingSupport(
                        // read function
                        element -> {
                            TaxonomyNode node = (TaxonomyNode) element;
                            InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                            if (investmentVehicle != null && !node.getParent().isUnassignedCategory())
                                return getModel().getTaxonomy().isUsedForRebalancing(investmentVehicle);
                            return false;
                        },

                        // write function
                        (element, value) -> {
                            TaxonomyNode node = (TaxonomyNode) element;
                            InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                            if (investmentVehicle != null && !node.getParent().isUnassignedCategory())
                            {
                                getModel().getTaxonomy().setUsedForRebalancing(investmentVehicle, value);
                            }
                        }).addListener(this::onModified).attachTo(column);
        support.addColumn(column);

        column = new Column("targetvalue", Messages.ColumnTargetValue, SWT.RIGHT, 100); //$NON-NLS-1$
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

        // Column which shows percentage of the target for this asset class in
        // relationship to total assets
        column = new Column("toBePctOfTotal", Messages.ColumnToBePctOfTotal, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnToBePctOfTotal_MenuLabel);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                // Divide to-be amount for this asset class by amount of total
                // assets (root of asset class tree)
                if (node.getTarget() == null)
                    return null;

                long nodeTarget = node.getTarget().getAmount();
                long totalTarget = node.getRoot().getTarget().getAmount();

                if (totalTarget == 0)
                    return Values.Percent.format(0d);
                else
                    return Values.Percent.format(nodeTarget / (double) totalTarget);
            }
        });
        column.setVisible(false);
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
                return node.getActual().isGreaterOrEqualTo(node.getTarget()) ? Colors.theme().greenForeground()
                                : Colors.theme().redForeground();
            }
        });
        support.addColumn(column);

        column = new Column("delta%indicator", Messages.ColumnDeltaPercentIndicator, SWT.LEFT, 60); //$NON-NLS-1$

        column.setLabelProvider(new DeltaPercentageIndicatorLabelProvider(getNodeViewer().getControl(),
                        getModel().getClient(), element -> (TaxonomyNode) element));
        support.addColumn(column);

        column = new Column("delta%relative", Messages.ColumnDeltaPercentRelative, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setDescription(Messages.ColumnDeltaPercentRelative_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.getTarget() == null)
                    return null;

                return Values.Percent.format(calculateRelativeDelta(node));
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.getTarget() == null)
                    return null;
                return calculateRelativeDelta(node) >= 0 ? Colors.theme().greenForeground()
                                : Colors.theme().redForeground();
            }

            private double calculateRelativeDelta(TaxonomyNode node)
            {
                long actual = node.getActual().getAmount();
                long base = node.getParent() == null ? node.getActual().getAmount()
                                : node.getParent().getActual().getAmount();
                double weightPercent = node.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT;
                double actualPercent = (base != 0) ? (double) actual / base : weightPercent;

                return actualPercent - weightPercent;
            }
        });
        column.setVisible(false);
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
                return node.getActual().isGreaterOrEqualTo(node.getTarget()) ? Colors.theme().greenForeground()
                                : Colors.theme().redForeground();
            }
        });
        support.addColumn(column);

        // Column which shows delta between to-be percentage of total and as-is
        // percentage of total
        column = new Column("deltaPctOfTotal", Messages.ColumnDeltaPctOfTotal, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDeltaPctOfTotal_MenuLabel);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            public Double getValue(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                // Divide as-is and to-be amount for this asset class by amount
                // of total assets and calculate delta
                // (root of asset class tree)
                if (node.getTarget() == null)
                    return null;

                long nodeActual = node.getActual().getAmount();
                long nodeTarget = node.getTarget().getAmount();
                long totalActual = node.getRoot().getActual().getAmount();
                long totalTarget = node.getRoot().getTarget().getAmount();

                if (totalTarget == 0)
                    return 0d;
                else
                    return (nodeActual / (double) totalActual) - (nodeTarget / (double) totalTarget);
            }

            @Override
            public String getText(Object element)
            {
                Double value = getValue(element);

                if (value == null)
                    return null;
                else
                    return Values.Percent.format(value);
            }

            @Override
            public Color getForeground(Object element)
            {
                Double value = getValue(element);

                if (value == null)
                    return null;
                else
                    return Double.compare(value, 0d) < 0 ? Colors.theme().redForeground()
                                    : Colors.theme().greenForeground();
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("rebalanceAmount", Messages.ColumnRebalanceAmount, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                // no delta shares for unassigned securities
                if (node.getParent() != null && node.getParent().isUnassignedCategory())
                    return null;

                InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                if (investmentVehicle == null || !getModel().getTaxonomy().isUsedForRebalancing(investmentVehicle))
                    return null;

                Money rebalancingAmount = getModel().getRebalancingSolution().getMoney(investmentVehicle);
                return Values.Money.format(rebalancingAmount);
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                if (investmentVehicle == null || !getModel().getTaxonomy().isUsedForRebalancing(investmentVehicle))
                    return null;

                if (!getModel().getRebalancingSolution().isExact(investmentVehicle))
                    return Colors.theme().redBackground();
                if (getModel().getRebalancingSolution().isAmbigous(investmentVehicle))
                    return Colors.theme().warningBackground();
                return null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                if (investmentVehicle == null)
                    return null;

                if (!node.isPrimary())
                    return Colors.theme().grayForeground();
                return null;
            }

            @Override
            public String getToolTipText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
                if (investmentVehicle == null || !getModel().getTaxonomy().isUsedForRebalancing(investmentVehicle))
                    return null;

                if (!getModel().getRebalancingSolution().isExact(investmentVehicle))
                    return Messages.RebalanceInexactTooltip;
                if (getModel().getRebalancingSolution().isAmbigous(investmentVehicle))
                    return Messages.RebalanceAmbiguousTooltip;
                return null;
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
                if (security == null || security.getCurrencyCode() == null
                                || !getModel().getTaxonomy().isUsedForRebalancing(security))
                    return null;

                String priceCurrency = security.getCurrencyCode();
                long price = security.getSecurityPrice(LocalDate.now()).getValue();
                if (price == 0L)
                    return Values.Share.format(0L);

                Money rebalancingAmount = getModel().getRebalancingSolution().getMoney(security);
                String deltaCurrency = rebalancingAmount.getCurrencyCode();
                long delta = rebalancingAmount.getAmount();

                // if currency of the data (here: deltaCurrency) does not match
                // the currency of the security (here: priceCurrency), convert
                // delta in order to know how many shares need to bought or sold
                if (!deltaCurrency.equals(priceCurrency))
                {
                    delta = getModel().getCurrencyConverter().with(priceCurrency)
                                    .convert(LocalDate.now(), Money.of(deltaCurrency, delta)).getAmount();
                }

                long shares = Math.round(delta * Values.Share.divider() * Values.Quote.dividerToMoney() / price);
                return Values.Share.format(shares);
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null || security.getCurrencyCode() == null
                                || !getModel().getTaxonomy().isUsedForRebalancing(security))
                    return null;

                if (!getModel().getRebalancingSolution().isExact(security))
                    return Colors.theme().redBackground();
                if (getModel().getRebalancingSolution().isAmbigous(security))
                    return Colors.theme().warningBackground();
                return null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null || security.getCurrencyCode() == null
                                || !getModel().getTaxonomy().isUsedForRebalancing(security))
                    return null;

                if (!node.isPrimary())
                    return Colors.theme().grayForeground();
                return null;
            }

            @Override
            public String getToolTipText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null || security.getCurrencyCode() == null
                                || !getModel().getTaxonomy().isUsedForRebalancing(security))
                    return null;

                if (!getModel().getRebalancingSolution().isExact(security))
                    return Messages.RebalanceInexactTooltip;
                if (getModel().getRebalancingSolution().isAmbigous(security))
                    return Messages.RebalanceAmbiguousTooltip;
                return null;
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
                                ? Display.getDefault().getSystemColor(SWT.COLOR_BLACK)
                                : null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() && getModel().hasWeightError(node) ? Colors.theme().warningBackground()
                                : null;
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
    public void configMenuAboutToShow(IMenuManager manager)
    {
        super.configMenuAboutToShow(manager);

        manager.add(new Separator());

        RebalancingColoringRule rule = new RebalancingColoringRule(getModel().getClient());
        manager.add(new SimpleAction(MessageFormat.format(Messages.MenuConfigureRebalancingIndicator, //
                        rule.getAbsoluteThreshold(), rule.getRelativeThreshold()),
                        a -> new EditRebalancingColoringRuleDialog(ActiveShell.get(), rule).open()));
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
