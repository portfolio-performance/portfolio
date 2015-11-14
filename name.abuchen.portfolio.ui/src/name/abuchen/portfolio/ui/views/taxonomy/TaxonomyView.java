package name.abuchen.portfolio.ui.views.taxonomy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.TaxonomyModelChangeListener;

public class TaxonomyView extends AbstractFinanceView implements PropertyChangeListener
{
    /** preference key: store last active view as index */
    private String identifierView;
    /** preference key: include unassigned category in charts */
    private String identifierUnassigned;
    /** preference key: order by taxonomy in stack chart */
    private String identifierOrderByTaxonomy;

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

        this.identifierView = TaxonomyView.class.getSimpleName() + "-VIEW-" + taxonomy.getId(); //$NON-NLS-1$
        this.identifierUnassigned = TaxonomyView.class.getSimpleName() + "-UNASSIGNED-" + taxonomy.getId(); //$NON-NLS-1$
        this.identifierOrderByTaxonomy = TaxonomyView.class.getSimpleName() + "-ORDERBYTAXONOMY-" + taxonomy.getId(); //$NON-NLS-1$

        this.model = make(TaxonomyModel.class, taxonomy);
        this.model.setExcludeUnassignedCategoryInCharts(part.getPreferenceStore().getBoolean(identifierUnassigned));
        this.model.setOrderByTaxonomyInStackChart(part.getPreferenceStore().getBoolean(identifierOrderByTaxonomy));

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
        getPreferenceStore().setValue(identifierUnassigned, model.isUnassignedCategoryInChartsExcluded());
        getPreferenceStore().setValue(identifierOrderByTaxonomy, model.isOrderByTaxonomyInStackChart());
        taxonomy.removePropertyChangeListener(this);

        Control[] children = container.getChildren();
        for (Control control : children)
        {
            Page page = (Page) control.getData();
            page.dispose();
        }

        super.dispose();
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        addView(toolBar, Messages.LabelViewTaxonomyDefinition, Images.VIEW_TABLE, 0);
        addView(toolBar, Messages.LabelViewReBalancing, Images.VIEW_REBALANCING, 1);
        addView(toolBar, Messages.LabelViewPieChart, Images.VIEW_PIECHART, 2);
        addView(toolBar, Messages.LabelViewTreeMap, Images.VIEW_TREEMAP, 3);
        addView(toolBar, Messages.LabelViewStackedChart, Images.VIEW_STACKEDCHART, 4);
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
        config.setImageDescriptor(Images.CONFIG.descriptor());
        config.setToolTipText(Messages.MenuShowHideColumns);

        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    public void notifyModelUpdated()
    {
        model.fireTaxonomyModelChange(model.getRootNode());
    }

    private void addView(final ToolBar toolBar, String label, Images image, final int index)
    {
        Action showDefinition = new Action()
        {
            @Override
            public void run()
            {
                activateView(index);
            }
        };
        showDefinition.setImageDescriptor(image.descriptor());
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

        Page[] pages = new Page[] { new DefinitionViewer(getPart(), model, renderer), //
                        new ReBalancingViewer(getPart(), model, renderer), //
                        new PieChartViewer(model, renderer), //
                        new TreeMapViewer(model, renderer), //
                        new StackedChartViewer(getPart(), model, renderer) };

        for (Page page : pages)
        {
            page.setPreferenceStore(getPreferenceStore());
            Control control = page.createControl(container);
            control.setData(page);
        }

        activateView(getPart().getPreferenceStore().getInt(identifierView));

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

            getPart().getPreferenceStore().setValue(identifierView, index);
        }
    }
}
