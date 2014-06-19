package name.abuchen.portfolio.ui.views.taxonomy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
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
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);
        this.taxonomy = (Taxonomy) parameter;
        this.model = new TaxonomyModel(getClient(), taxonomy);

        this.identifier = TaxonomyView.class.getSimpleName() + "-VIEW-" + taxonomy.getId(); //$NON-NLS-1$

        this.taxonomy.addPropertyChangeListener(this);
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
        addView(toolBar, Messages.LabelViewTaxonomyDefinition, PortfolioPlugin.IMG_VIEW_TABLE, 0);
        addView(toolBar, Messages.LabelViewReBalancing, PortfolioPlugin.IMG_VIEW_REBALANCING, 1);
        addView(toolBar, Messages.LabelViewPieChart, PortfolioPlugin.IMG_VIEW_PIECHART, 2);
        addView(toolBar, Messages.LabelViewTreeMap, PortfolioPlugin.IMG_VIEW_TREEMAP, 3);
        addView(toolBar, Messages.LabelViewStackedChart, PortfolioPlugin.IMG_VIEW_STACKEDCHART, 4);
        addConfigButton(toolBar);
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action config = new Action()
        {
            @Override
            public void run()
            {
                StackLayout layout = (StackLayout) container.getLayout();
                if (layout.topControl != null)
                    ((Page) layout.topControl.getData()).showConfigMenu(getActiveShell());
            }
        };
        config.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_CONFIG));
        config.setToolTipText(Messages.MenuShowHideColumns);

        new ActionContributionItem(config).fill(toolBar, -1);
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

        Page[] pages = new Page[] { new DefinitionViewer(model, renderer), //
                        new ReBalancingViewer(model, renderer), //
                        new PieChartViewer(model, renderer), //
                        new TreeMapViewer(model, renderer), //
                        new StackedChartViewer(getPart(), model, renderer) };

        for (Page page : pages)
        {
            Control control = page.createControl(container);
            control.setData(page);
        }

        activateView(getPart().getPreferenceStore().getInt(identifier));

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
            if (layout.topControl != null)
                ((Page) layout.topControl.getData()).afterPage();

            ((Page) children[index].getData()).beforePage();

            layout.topControl = children[index];
            container.layout();

            getPart().getPreferenceStore().setValue(identifier, index);
        }
    }
}
