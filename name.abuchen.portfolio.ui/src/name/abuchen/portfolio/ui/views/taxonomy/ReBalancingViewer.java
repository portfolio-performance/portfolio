package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ReBalancingViewer extends AbstractNodeTreeViewer
{
    public ReBalancingViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    protected void addColumns(TreeColumnLayout layout)
    {
        addDimensionColumn(layout);

        TreeViewerColumn column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText("Weight");
        layout.setColumnData(column.getColumn(), new ColumnPixelData(70));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isUnassignedCategory() ? "n/a" : Values.Weight.format(node.getWeight());
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return getModel().hasWeightError(node) ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND)
                                : null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return getModel().hasWeightError(node) ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
                                : null;
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return getModel().hasWeightError(node) ? PortfolioPlugin.image(PortfolioPlugin.IMG_QUICKFIX) : null;
            }
        });

        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnTargetValue);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() ? Values.Amount.format(node.getTarget()) : null;
            }
        });

        addActualColumns(layout);

        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnDeltaPercent);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() ? Values.Percent.format(((double) node.getActual() / (double) node
                                .getTarget()) - 1) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return Display.getCurrent().getSystemColor(
                                node.getActual() >= node.getTarget() ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
            }
        });

        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnDeltaValue);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isClassification() ? Values.Amount.format(node.getActual() - node.getTarget()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return Display.getCurrent().getSystemColor(
                                node.getActual() >= node.getTarget() ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
            }
        });

        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnQuote);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null)
                    return null;

                SecurityPrice price = security.getSecurityPrice(Dates.today());
                return Values.Quote.format(price.getValue());
            }
        });

        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText("Delta Stücke");
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                Security security = node.getBackingSecurity();
                if (security == null)
                    return null;

                long price = security.getSecurityPrice(Dates.today()).getValue();
                long weightedPrice = Math.round(node.getWeight() * price / Classification.ONE_HUNDRED_PERCENT);
                long delta = node.getParent().getTarget() - node.getParent().getActual();
                long shares = weightedPrice == 0 ? 0 : Math.round(delta * Values.Share.factor() / weightedPrice);
                return Values.Share.format(shares);
            }
        });

        new CellEditorFactory(getNodeViewer(), TaxonomyNode.class) //
                        .notify(new NodeModificationListener(this)) //
                        .editable("name") // //$NON-NLS-1$
                        .decimal("weight", Values.Weight) // //$NON-NLS-1$
                        .apply();

    }

    @Override
    protected void fillContextMenu(IMenuManager manager)
    {
        super.fillContextMenu(manager);

        final TaxonomyNode node = (TaxonomyNode) ((IStructuredSelection) getNodeViewer().getSelection())
                        .getFirstElement();

        if (node != null && node.isClassification() && getModel().hasWeightError(node))
        {
            manager.appendToGroup(MENU_GROUP_CUSTOM_ACTIONS, new Action("Fix weights")
            {
                @Override
                public void run()
                {
                    doFixClassificationWeights(node);
                }
            });
        }

    }

    private void doFixClassificationWeights(TaxonomyNode node)
    {
        Classification classification = node.getClassification();

        if (node.isRoot())
        {
            classification.setWeight(Classification.ONE_HUNDRED_PERCENT);
        }
        else
        {
            classification.setWeight(0);
            int weight = Math.max(0, Classification.ONE_HUNDRED_PERCENT
                            - classification.getParent().getChildrenWeight());
            classification.setWeight(weight);
        }
        onTaxnomyNodeEdited(node);
    }

}
