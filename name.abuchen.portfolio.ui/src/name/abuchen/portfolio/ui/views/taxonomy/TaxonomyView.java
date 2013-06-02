package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.PortfolioPlugin;

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
    private Taxonomy taxonomy;

    private Composite container;

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);
        this.taxonomy = (Taxonomy) parameter;
    }

    @Override
    protected String getTitle()
    {
        return taxonomy.getName();
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        addView(toolBar, "Definition", PortfolioPlugin.IMG_VIEW_TABLE, 0);
        addView(toolBar, "Pie Chart", PortfolioPlugin.IMG_VIEW_PIECHART, 1);
        addView(toolBar, "Tree Map", PortfolioPlugin.IMG_VIEW_TREEMAP, 2);
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

        TaxonomyModel model = new TaxonomyModel(getClient(), taxonomy);
        TaxonomyNodeRenderer renderer = new TaxonomyNodeRenderer(resources);

        container = new Composite(parent, SWT.NONE);
        StackLayout layout = new StackLayout();
        container.setLayout(layout);

        DefinitionViewer definition = new DefinitionViewer(model);
        layout.topControl = definition.createContainer(container, renderer);

        PieChartViewer pie = new PieChartViewer(model);
        pie.createContainer(container, renderer);

        TreeMapViewer tree = new TreeMapViewer(getClient(), model);
        tree.createContainer(container, renderer);

        return container;
    }
}
