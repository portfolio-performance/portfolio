package name.abuchen.portfolio.ui.views.taxonomy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.TaxonomyModelChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class TaxonomyView extends AbstractFinanceView implements PropertyChangeListener
{
    private String identifier;

    private TaxonomyModel model;
    private Taxonomy taxonomy;

    private Composite container;

    @Override
    protected String getTitle()
    {
        return taxonomy.getName();
    }

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);
        this.taxonomy = (Taxonomy) parameter;
        this.model = new TaxonomyModel(getClient(), taxonomy);

        this.identifier = TaxonomyView.class.getSimpleName() + "-VIEW-" + taxonomy.getId(); //$NON-NLS-1$

        this.taxonomy.addPropertyChangeListener(this); //$NON-NLS-1$
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        updateTitle();
    }

    @Override
    public void dispose()
    {
        taxonomy.removePropertyChangeListener(this);
        super.dispose();
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        addView(toolBar, "Definition", PortfolioPlugin.IMG_VIEW_TABLE, 0);
        addView(toolBar, "Allocation", PortfolioPlugin.IMG_VIEW_REBALANCING, 1);
        addView(toolBar, "Pie Chart", PortfolioPlugin.IMG_VIEW_PIECHART, 2);
        addView(toolBar, "Tree Map", PortfolioPlugin.IMG_VIEW_TREEMAP, 3);
    }

    @Override
    public void notifyModelUpdated()
    {
        model.fireTaxonomyModelChange(model.getRootNode());
    }

    private void addView(final ToolBar toolBar, String label, String image, final int index)
    {
        Action showDefinition = new Action()
        {
            @Override
            public void run()
            {
                activateView(index);
            }
        };
        showDefinition.setImageDescriptor(PortfolioPlugin.descriptor(image));
        showDefinition.setToolTipText(label);
        new ActionContributionItem(showDefinition).fill(toolBar, -1);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), parent);

        TaxonomyNodeRenderer renderer = new TaxonomyNodeRenderer(resources);

        container = new Composite(parent, SWT.NONE);
        StackLayout layout = new StackLayout();
        container.setLayout(layout);

        DefinitionViewer definition = new DefinitionViewer(model, renderer);
        definition.createContainer(container);

        AbstractNodeTreeViewer allocation = new ReBalancingViewer(model, renderer);
        allocation.createContainer(container);

        PieChartViewer pie = new PieChartViewer(model, renderer);
        pie.createContainer(container);

        TreeMapViewer tree = new TreeMapViewer(model, renderer);
        tree.createContainer(container);

        int index = getClientEditor().getPreferenceStore().getInt(identifier);
        activateView(index);

        model.addListener(new TaxonomyModelChangeListener()
        {
            @Override
            public void nodeChange(TaxonomyNode node)
            {
                markDirty();
            }
        });

        return container;
    }

    private void activateView(final int index)
    {
        StackLayout layout = (StackLayout) container.getLayout();
        Control[] children = container.getChildren();

        if (index >= 0 && index < children.length)
        {
            layout.topControl = children[index];
            container.layout();

            getClientEditor().getPreferenceStore().setValue(identifier, index);
        }
    }
}
