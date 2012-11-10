package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ToolBarDropdownMenu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class IndustryClassificationDefinitionView extends AbstractFinanceView
{
    private TableViewer viewer;
    private ToolBarDropdownMenu<IndustryClassification> menu;
    private Action actionUseTaxonomy;

    private IndustryClassification taxonomy;

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);

        taxonomy = clientEditor.getClient().getIndustryTaxonomy();
    }

    @Override
    protected String getTitle()
    {
        return taxonomy.getRootCategory().getLabel();
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        menu = new ToolBarDropdownMenu<IndustryClassification>(toolBar)
        {
            @Override
            protected void itemSelected(IndustryClassification selection)
            {
                viewer.getTable().setRedraw(false);

                try
                {
                    taxonomy = selection;
                    for (TableColumn column : viewer.getTable().getColumns())
                        column.dispose();
                    createColumns(viewer, taxonomy);
                    viewer.setInput(getLeafs(taxonomy.getRootCategory()));
                    actionUseTaxonomy.setEnabled(taxonomy != getClient().getIndustryTaxonomy());
                }
                finally
                {
                    viewer.getTable().setRedraw(true);
                }
            }
        };

        actionUseTaxonomy = new Action(Messages.ChangeTaxonomyAction)
        {
            @Override
            public void run()
            {
                // confirm change if securities are classified
                boolean hasClassification = false;
                for (Security s : getClient().getSecurities())
                {
                    hasClassification = s.getIndustryClassification() != null;
                    if (hasClassification)
                        break;
                }

                if (hasClassification)
                {
                    boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                                    Messages.ChangeTaxonomyTitle, Messages.ChangeTaxonomyRequestConfirmation);

                    if (!confirmed)
                        return;

                }

                getClient().setIndustryTaxonomy(taxonomy);
                actionUseTaxonomy.setEnabled(taxonomy != getClient().getIndustryTaxonomy());
            }
        };
        actionUseTaxonomy.setEnabled(taxonomy != getClient().getIndustryTaxonomy());

        new ActionContributionItem(actionUseTaxonomy).fill(toolBar, -1);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);

        viewer = new TableViewer(composite, SWT.FULL_SELECTION);

        createColumns(viewer, taxonomy);

        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        viewer.setLabelProvider(new IndustryLabelProvider());
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        viewer.setInput(getLeafs(taxonomy.getRootCategory()));

        final Text description = new Text(composite, SWT.WRAP);

        // layout
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 1).margins(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getControl());
        int height = description.getFont().getFontData()[0].getHeight() * 3 + 5;
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, height).applyTo(description);

        // selection event
        viewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                Category category = (Category) ((StructuredSelection) event.getSelection()).getFirstElement();
                description.setText(category != null && category.getDescription() != null ? category.getDescription()
                                : ""); //$NON-NLS-1$
            }
        });

        // add menu items
        for (IndustryClassification t : IndustryClassification.list())
            menu.add(t, t.getName(), null);

        menu.select(taxonomy);

        return composite;
    }

    private void createColumns(TableViewer viewer, IndustryClassification taxonomy)
    {
        for (String label : taxonomy.getLabels())
        {
            TableColumn column = new TableColumn(viewer.getTable(), SWT.None);
            column.setText(label);
            column.setWidth(220);
        }
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

    private class IndustryLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            Category item = getCategory(element, columnIndex);

            return item != null ? item.getLabel() : null;
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            Category item = getCategory(element, columnIndex);

            return item != null ? PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER)
                            : null;
        }

        private Category getCategory(Object element, int columnIndex)
        {
            int size = taxonomy.getLabels().size();

            Category item = (Category) element;
            for (int ii = 0; ii < size - columnIndex - 1 && item != null; ii++)
            {
                Category parent = item.getParent();
                item = parent.getChildren().indexOf(item) == 0 ? parent : null;
            }
            return item;
        }
    }
}
