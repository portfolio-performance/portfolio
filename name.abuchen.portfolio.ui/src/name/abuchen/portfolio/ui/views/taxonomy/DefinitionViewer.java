package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ValueEditingSupport;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;

/* package */class DefinitionViewer extends AbstractNodeTreeViewer
{

    public DefinitionViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    protected void addColumns(ShowHideColumnHelper support)
    {
        addDimensionColumn(support);

        Column column = new Column("weigth", Messages.ColumnWeight, SWT.RIGHT, 70); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() ? Values.Weight.format(node.getWeight()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() && getModel().hasWeightError(node) ? Display.getDefault().getSystemColor(
                                SWT.COLOR_INFO_FOREGROUND) : null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() && getModel().hasWeightError(node) ? Display.getDefault().getSystemColor(
                                SWT.COLOR_INFO_BACKGROUND) : null;
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() && getModel().hasWeightError(node) ? PortfolioPlugin
                                .image(PortfolioPlugin.IMG_QUICKFIX) : null;
            }

        });
        new ValueEditingSupport(TaxonomyNode.class, "weight", Values.Weight) //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                if (((TaxonomyNode) element).isUnassignedCategory())
                    return false;
                if (((TaxonomyNode) element).isClassification())
                    return false;
                return super.canEdit(element);
            }
        }.addListener(new ModificationListener()
        {
            @Override
            public void onModified(Object element, Object newValue, Object oldValue)
            {
                onWeightModified(element, newValue, oldValue);
            }
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("color", Messages.ColumnColor, SWT.LEFT, 60); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.isClassification())
                    return getRenderer().getColorFor((TaxonomyNode) element);
                else
                    return null;
            }
        });
        support.addColumn(column);

        addActualColumns(support);

        addAdditionalColumns(support);
    }

    @Override
    protected void fillContextMenu(IMenuManager manager)
    {
        super.fillContextMenu(manager);

        final TaxonomyNode node = (TaxonomyNode) ((IStructuredSelection) getNodeViewer().getSelection())
                        .getFirstElement();

        if (node == null || node.isUnassignedCategory())
            return;

        if (!node.isClassification())
            return;

        MenuManager color = new MenuManager(Messages.ColumnColor);

        color.add(new Action(Messages.MenuTaxonomyColorEdit)
        {
            @Override
            public void run()
            {
                doEditColor(node);
            }
        });

        color.add(new Action(Messages.MenuTaxonomyColorRandomPalette)
        {
            @Override
            public void run()
            {
                doAutoAssignColors(node);
            }
        });

        color.add(new Action(Messages.MenuTaxonomyColorCascadeToChildren)
        {
            @Override
            public void run()
            {
                doCascadeColorsDown(node);
            }
        });

        manager.appendToGroup(MENU_GROUP_DEFAULT_ACTIONS, color);
    }

    private void doEditColor(TaxonomyNode node)
    {
        RGB oldColor = Colors.toRGB(node.getClassification().getColor());

        ColorDialog colorDialog = new ColorDialog(getNodeViewer().getControl().getShell());
        colorDialog.setRGB(oldColor);
        RGB newColor = colorDialog.open();

        if (newColor != null && !newColor.equals(oldColor))
        {
            node.getClassification().setColor(Colors.toHex(newColor));
            getModel().fireTaxonomyModelChange(node);
        }
    }

    private void doAutoAssignColors(TaxonomyNode node)
    {
        node.getClassification().assignRandomColors();

        getModel().fireTaxonomyModelChange(null);
        getNodeViewer().getTree().redraw(); // avoids artifacts around cell
    }

    protected void doCascadeColorsDown(TaxonomyNode node)
    {
        node.getClassification().cascadeColorDown();

        getModel().fireTaxonomyModelChange(null);
        getNodeViewer().getTree().redraw(); // avoids artifacts around cell
    }
}
