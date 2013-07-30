package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.Random;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.Colors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
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
    protected void addColumns(TreeColumnLayout layout)
    {
        addDimensionColumn(layout);

        TreeViewerColumn column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText("Weight");
        column.getColumn().setWidth(70);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(70));
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

        column = new TreeViewerColumn(getNodeViewer(), SWT.LEFT);
        column.getColumn().setText("Color");
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
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
                if (node.isClassification() && !node.isRoot())
                    return getRenderer().getColorFor((TaxonomyNode) element);
                else
                    return null;
            }
        });

        new CellEditorFactory(getNodeViewer(), TaxonomyNode.class) //
                        .notify(new NodeModificationListener(this)
                        {
                            @Override
                            public boolean canModify(Object element, String property)
                            {
                                if ("weight".equals(property) && ((TaxonomyNode) element).isClassification()) //$NON-NLS-1$
                                    return false;

                                return super.canModify(element, property);
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .decimal("weight", Values.Weight) // //$NON-NLS-1$
                        .readonly("color") //$NON-NLS-1$
                        .apply();
    }

    @Override
    protected void fillContextMenu(IMenuManager manager)
    {
        super.fillContextMenu(manager);

        final TaxonomyNode node = (TaxonomyNode) ((IStructuredSelection) getNodeViewer().getSelection())
                        .getFirstElement();

        if (node != null && node.isClassification())
        {
            MenuManager color = new MenuManager("Color");

            if (!node.isRoot())
            {
                color.add(new Action("Edit...")
                {
                    @Override
                    public void run()
                    {
                        doEditColor(node);
                    }
                });
            }

            color.add(new Action("Random palette to children")
            {
                @Override
                public void run()
                {
                    doAutoAssignColors(node);
                }
            });

            if (!node.isRoot())
            {
                color.add(new Action("Cascade color to children")
                {
                    @Override
                    public void run()
                    {
                        doCascadeColorsDown(node);
                    }
                });
            }

            manager.appendToGroup(MENU_GROUP_DEFAULT_ACTIONS, color);
        }

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
        int size = node.getClassification().getChildren().size();

        Random random = new Random();

        float hue = random.nextFloat() * 360f;
        float saturation = (random.nextFloat() * 0.5f) + 0.3f;
        float brightness = (random.nextFloat() * 0.4f) + 0.5f;

        float[][] hsb = new float[size][];
        float step = 360f / (float) size;
        for (int ii = 0; ii < size; ii++)
            hsb[ii] = new float[] { (hue + (step * ii)) % 360f, saturation, brightness };

        int index = 0;

        for (TaxonomyNode child : node.getChildren())
        {
            if (!child.isClassification() || child.isUnassignedCategory())
                continue;

            RGB rgb = new RGB(hsb[index][0], hsb[index][1], hsb[index][2]);
            child.getClassification().setColor(Colors.toHex(rgb));

            cascade(child, hsb[index]);

            index++;
        }

        getModel().fireTaxonomyModelChange(null);
        getNodeViewer().getTree().redraw(); // avoids artifacts around cell
    }

    protected void doCascadeColorsDown(TaxonomyNode node)
    {
        float[] hsb = Colors.toRGB(node.getClassification().getColor()).getHSB();

        cascade(node, hsb);

        getModel().fireTaxonomyModelChange(null);
        getNodeViewer().getTree().redraw(); // avoids artifacts around cell
    }

    private void cascade(TaxonomyNode node, float[] hsb)
    {
        float[] childColor = new float[3];
        childColor[0] = hsb[0];
        childColor[1] = Math.max(0f, hsb[1] - 0.1f);
        childColor[2] = Math.min(1f, hsb[2] + 0.1f);

        for (TaxonomyNode child : node.getChildren())
        {
            if (!child.isClassification())
                continue;

            child.getClassification().setColor(Colors.toHex(childColor));

            cascade(child, childColor);
        }
    }

}
