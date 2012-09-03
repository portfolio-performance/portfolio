package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

public class IndustryClassificationPage extends AbstractWizardPage
{
    public static final String PAGE_NAME = "industryClassification"; //$NON-NLS-1$

    private final IndustryClassification taxonomy = new IndustryClassification();
    private Security security;
    private Category classification;

    private TreeViewer tree;

    private PatternFilter filterPattern;

    protected IndustryClassificationPage(Security security)
    {
        super(PAGE_NAME);
        setTitle(taxonomy.getRootCategory().getLabel());
        setDescription(Messages.EditWizardIndustryClassificationDescription);

        this.security = security;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayoutFactory.fillDefaults().applyTo(container);
        setControl(container);

        filterPattern = new PatternFilter();

        FilteredTree t = new FilteredTree(container, SWT.BORDER, filterPattern, true);
        tree = t.getViewer();
        tree.setContentProvider(new ClassificationContentProvider());
        tree.setLabelProvider(new ClassificationLabelProvider());
        tree.setInput(new IndustryClassification().getRootCategory());

        tree.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                classification = (Category) ((IStructuredSelection) event.getSelection()).getFirstElement();
                setMessage(classification != null ? classification.getDescription() : null);
            }
        });
    }

    @Override
    public void beforePage()
    {
        classification = taxonomy.getCategoryById(security.getIndustryClassification());

        if (classification != null)
            tree.setSelection(new StructuredSelection(classification), true);
    }

    @Override
    public void afterPage()
    {
        security.setIndustryClassification(classification != null ? classification.getId() : null);
    }

    @Override
    public IWizardPage getNextPage()
    {
        return getWizard().getPage(SecurityMasterDataPage.PAGE_NAME);
    }

    private static class ClassificationLabelProvider extends LabelProvider
    {
        @Override
        public String getText(Object element)
        {
            return ((Category) element).getLabel();
        }

        @Override
        public Image getImage(Object element)
        {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        }
    }

    private static class ClassificationContentProvider implements ITreeContentProvider
    {
        private Category root;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            root = (Category) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return root.getChildren().toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((Category) parentElement).getChildren().toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return ((Category) element).getParent();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((Category) element).getChildren().isEmpty();
        }

        @Override
        public void dispose()
        {}
    }
}
