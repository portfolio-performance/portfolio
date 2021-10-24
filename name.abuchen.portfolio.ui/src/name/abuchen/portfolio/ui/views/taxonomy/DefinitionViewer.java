package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.util.ColorConversion;

/* package */class DefinitionViewer extends AbstractNodeTreeViewer
{

    @Inject
    public DefinitionViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(view, model, renderer);
    }

    @Override
    protected String readExpansionState()
    {
        return getModel().getExpansionStateDefinition();
    }

    @Override
    protected void storeExpansionState(String expanded)
    {
        getModel().setExpansionStateDefinition(expanded);
    }

    @Override
    protected void addColumns(ShowHideColumnHelper support)
    {
        addDimensionColumn(support);

        Column column = new Column("color", Messages.ColumnColor, SWT.LEFT, 60); //$NON-NLS-1$
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

        final IStructuredSelection selection = getNodeViewer().getStructuredSelection();
        if (selection.isEmpty() || selection.size() > 1)
            return;

        final TaxonomyNode node = (TaxonomyNode) selection.getFirstElement();

        if (node == null || node.isUnassignedCategory())
            return;

        if (!node.isClassification())
            return;

        MenuManager color = new MenuManager(Messages.ColumnColor);
        color.add(new SimpleAction(Messages.MenuTaxonomyColorEdit, a -> doEditColor(node)));
        color.add(new SimpleAction(Messages.MenuTaxonomyColorRandomPalette, a -> doAutoAssignColors(node)));
        color.add(new SimpleAction(Messages.MenuTaxonomyColorCascadeToChildren, a -> doCascadeColorsDown(node)));

        manager.appendToGroup(MENU_GROUP_DEFAULT_ACTIONS, color);
    }

    private void doEditColor(TaxonomyNode node)
    {
        RGB oldColor = ColorConversion.hex2RGB(node.getClassification().getColor());

        ColorDialog colorDialog = new ColorDialog(getNodeViewer().getControl().getShell());
        colorDialog.setRGB(oldColor);
        RGB newColor = colorDialog.open();

        if (newColor != null && !newColor.equals(oldColor))
        {
            String hex = Colors.toHex(newColor);
            node.getClassification().setColor(hex);

            // use same color for virtual and classification root nodes
            if (node.getParent().isRoot())
                node.getParent().getClassification().setColor(hex);

            getModel().fireTaxonomyModelChange(node);
            getModel().markDirty();
        }
    }

    private void doAutoAssignColors(TaxonomyNode node)
    {
        node.getClassification().assignRandomColors();

        getModel().fireTaxonomyModelChange(null);
        getNodeViewer().getTree().redraw(); // avoids artifacts around cell
        getModel().markDirty();
    }

    protected void doCascadeColorsDown(TaxonomyNode node)
    {
        node.getClassification().cascadeColorDown();

        getModel().fireTaxonomyModelChange(null);
        getNodeViewer().getTree().redraw(); // avoids artifacts around cell
        getModel().markDirty();
    }
}
