package name.abuchen.portfolio.ui.views.taxonomy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;

public class TaxonomyView extends AbstractFinanceView implements PropertyChangeListener
{
    /** preference key: store last active view as index */
    private String identifierView;
    /** preference key: include unassigned category in charts */
    private String identifierUnassigned;
    /** preference key: order by taxonomy in stack chart */
    private String identifierOrderByTaxonomy;
    /** preference key: node expansion state in definition viewer */
    private String expansionStateDefinition;
    /** preference key: node expansion state in rebalancing viewer */
    private String expansionStateReblancing;

    private TaxonomyModel model;
    private Taxonomy taxonomy;
    private ClientFilterDropDown clientFilter;

    private Composite container;
    private List<Action> viewActions = new ArrayList<>();

    @Override
    protected String getDefaultTitle()
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
        this.expansionStateDefinition = TaxonomyView.class.getSimpleName() + "-EXPANSION-DEFINITION-" //$NON-NLS-1$
                        + taxonomy.getId();
        this.expansionStateReblancing = TaxonomyView.class.getSimpleName() + "-EXPANSION-REBALANCE-" //$NON-NLS-1$
                        + taxonomy.getId();

        this.model = make(TaxonomyModel.class, taxonomy);
        this.model.setExcludeUnassignedCategoryInCharts(part.getPreferenceStore().getBoolean(identifierUnassigned));
        this.model.setOrderByTaxonomyInStackChart(part.getPreferenceStore().getBoolean(identifierOrderByTaxonomy));
        this.model.setExpansionStateDefinition(part.getPreferenceStore().getString(expansionStateDefinition));
        this.model.setExpansionStateRebalancing(part.getPreferenceStore().getString(expansionStateReblancing));

        this.taxonomy.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        updateTitle(taxonomy.getName());
    }

    @Override
    public void dispose()
    {
        taxonomy.removePropertyChangeListener(this);

        Control[] children = container.getChildren();
        for (Control control : children)
        {
            Page page = (Page) control.getData();
            page.dispose();
        }

        // store preferences *after* disposing pages -> allow pages to update
        // the model
        getPreferenceStore().setValue(identifierUnassigned, model.isUnassignedCategoryInChartsExcluded());
        getPreferenceStore().setValue(identifierOrderByTaxonomy, model.isOrderByTaxonomyInStackChart());
        getPreferenceStore().setValue(expansionStateDefinition, model.getExpansionStateDefinition());
        getPreferenceStore().setValue(expansionStateReblancing, model.getExpansionStateRebalancing());

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
        addFilterButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addFilterButton(ToolBar toolBar)
    {
        this.clientFilter = new ClientFilterDropDown(toolBar, getClient(), getPreferenceStore(), filter -> {
            Client filteredClient = filter.filter(getClient());
            ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, model.getCurrencyConverter(),
                            LocalDate.now());
            model.setClientSnapshot(snapshot);
            model.fireTaxonomyModelChange(model.getVirtualRootNode());
        });
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
        Client filteredClient = this.clientFilter.getSelectedFilter().filter(getClient());
        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, model.getCurrencyConverter(), LocalDate.now());
        model.setClientSnapshot(snapshot);
        model.fireTaxonomyModelChange(model.getVirtualRootNode());
    }

    private void addView(final ToolBar toolBar, String label, Images image, final int index)
    {
        Action showDefinition = new Action(label, Action.AS_CHECK_BOX)
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
        viewActions.add(showDefinition);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), parent);

        TaxonomyNodeRenderer renderer = new TaxonomyNodeRenderer(resources);

        container = new Composite(parent, SWT.NONE);
        StackLayout layout = new StackLayout();
        container.setLayout(layout);

        Page[] pages = new Page[] { make(DefinitionViewer.class, model, renderer), //
                        make(ReBalancingViewer.class, model, renderer), //
                        make(PieChartViewer.class, model, renderer), //
                        make(TreeMapViewer.class, model, renderer), //
                        make(StackedChartViewer.class, model, renderer) };

        for (Page page : pages)
        {
            Control control = page.createControl(container);
            control.setData(page);
        }

        activateView(getPart().getPreferenceStore().getInt(identifierView));

        model.addDirtyListener(this::markDirty);

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

            for (int ii = 0; ii < viewActions.size(); ii++)
                viewActions.get(ii).setChecked(index == ii);

            getPart().getPreferenceStore().setValue(identifierView, index);
        }
    }
}
