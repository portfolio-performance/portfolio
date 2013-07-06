package name.abuchen.portfolio.ui.views.taxonomy;

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

public class TaxonomyView extends AbstractFinanceView
{
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
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        addView(toolBar, "Definition", PortfolioPlugin.IMG_VIEW_TABLE, 0);
        addView(toolBar, "Allocation", PortfolioPlugin.IMG_QUICKFIX, 1);
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
                StackLayout layout = (StackLayout) container.getLayout();
                layout.topControl = container.getChildren()[index];
                container.layout();
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
        layout.topControl = definition.createContainer(container);

        AssetAllocationViewer allocation = new AssetAllocationViewer(model, renderer);
        allocation.createContainer(container);

        PieChartViewer pie = new PieChartViewer(model, renderer);
        pie.createContainer(container);

        TreeMapViewer tree = new TreeMapViewer(model, renderer);
        tree.createContainer(container);

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
}
