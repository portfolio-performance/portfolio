package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class IndustryClassificationDefinitionView extends AbstractFinanceView
{
    private IndustryClassification industryClassification;

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);

        industryClassification = clientEditor.getClient().getIndustryTaxonomy();
    }

    @Override
    protected String getTitle()
    {
        return industryClassification.getRootCategory().getLabel();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);

        TableViewer classification = new TableViewer(composite, SWT.FULL_SELECTION);

        TableColumn column = new TableColumn(classification.getTable(), SWT.None);
        column.setText(Messages.LabelSector);
        column.setWidth(220);

        column = new TableColumn(classification.getTable(), SWT.None);
        column.setText(Messages.LabelIndustryGroup);
        column.setWidth(220);

        column = new TableColumn(classification.getTable(), SWT.None);
        column.setText(Messages.LabelIndustry);
        column.setWidth(220);

        column = new TableColumn(classification.getTable(), SWT.None);
        column.setText(Messages.LabelSubIndustry);
        column.setWidth(220);

        classification.getTable().setHeaderVisible(true);
        classification.getTable().setLinesVisible(true);

        classification.setLabelProvider(new IndustryLabelProvider());
        classification.setContentProvider(ArrayContentProvider.getInstance());

        classification.setInput(getLeafs(industryClassification.getRootCategory()));

        final Text description = new Text(composite, SWT.WRAP);

        // layout
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 1).margins(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(classification.getControl());
        int height = description.getFont().getFontData()[0].getHeight() * 3 + 5;
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, height).applyTo(description);

        // selection event
        classification.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                Category category = (Category) ((StructuredSelection) event.getSelection()).getFirstElement();
                description.setText(category != null ? category.getDescription() : null);
            }
        });

        return composite;
    }

    private List<Category> getLeafs(Category root)
    {
        List<Category> items = new ArrayList<Category>();

        LinkedList<Category> stack = new LinkedList<Category>();
        stack.addAll(root.getChildren());

        while (!stack.isEmpty())
        {
            Category c = stack.removeFirst();

            if (c.getChildren().isEmpty())
                items.add(c);
            else
                stack.addAll(0, c.getChildren());
        }

        return items;
    }

    private static class IndustryLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            Category subIndustry = (Category) element;
            Category industry = getParent(subIndustry);
            Category industryGroup = getParent(industry);
            Category sector = getParent(industryGroup);

            switch (columnIndex)
            {
                case 0:
                    return sector != null ? sector.getLabel() : null;
                case 1:
                    return industryGroup != null ? industryGroup.getLabel() : null;
                case 2:
                    return industry != null ? industry.getLabel() : null;
                case 3:
                    return subIndustry.getLabel();
            }
            return null;
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            Category subIndustry = (Category) element;
            Category industry = getParent(subIndustry);
            Category industryGroup = getParent(industry);
            Category sector = getParent(industryGroup);

            if (columnIndex == 0 && sector == null)
                return null;

            if (columnIndex == 1 && industryGroup == null)
                return null;

            if (columnIndex == 2 && industry == null)
                return null;

            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        }

        private Category getParent(Category category)
        {
            if (category == null)
                return null;
            Category parent = category.getParent();
            return parent.getChildren().indexOf(category) == 0 ? parent : null;
        }
    }
}
