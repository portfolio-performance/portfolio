package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.util.CellEditorFactory;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

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
                return node.isClassification() ? getRenderer().getColorFor((TaxonomyNode) element) : null;
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

}
