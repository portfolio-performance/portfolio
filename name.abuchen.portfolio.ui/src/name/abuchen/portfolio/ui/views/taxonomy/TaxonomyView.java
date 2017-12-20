package name.abuchen.portfolio.ui.views.taxonomy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class TaxonomyView extends AbstractFinanceView implements PropertyChangeListener
{
    private class FilterDropDown extends AbstractDropDown
    {
        private ClientFilterMenu clientFilterMenu;

        public FilterDropDown(ToolBar toolBar, IPreferenceStore preferenceStore)
        {
            super(toolBar, Messages.SecurityFilter, Images.FILTER_OFF.image(), SWT.NONE);

            this.clientFilterMenu = new ClientFilterMenu(getClient(), getPreferenceStore());

            Consumer<ClientFilter> listener = filter -> {
                Client filteredClient = filter.filter(getClient());
                ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, model.getCurrencyConverter(),
                                LocalDateTime.now());
                model.setClientSnapshot(filteredClient, snapshot);
                model.fireTaxonomyModelChange(model.getVirtualRootNode());
            };

            clientFilterMenu.addListener(listener);
            clientFilterMenu.addListener(filter -> updateIcon());

            loadPreselectedFilter(preferenceStore);

            if (clientFilterMenu.hasActiveFilter() || !model.getNodeFilters().isEmpty())
                getToolItem().setImage(Images.FILTER_ON.image());

            // As the taxonomy model is initially calculated in the #init
            // method, we must recalculate the values if an active filter
            // exists.
            clientFilter = clientFilterMenu.getSelectedFilter();
            if (clientFilterMenu.hasActiveFilter())
                listener.accept(clientFilterMenu.getSelectedFilter());
        }

        private void loadPreselectedFilter(IPreferenceStore preferenceStore)
        {
            String prefix = TaxonomyView.class.getSimpleName() + "-" + taxonomy.getId(); //$NON-NLS-1$

            // client filter
            String key = prefix + ClientFilterMenu.PREF_KEY_POSTFIX;

            String selection = preferenceStore.getString(key);
            if (selection != null)
                clientFilterMenu.getAllItems().filter(item -> item.getUUIDs().equals(selection)).findAny()
                                .ifPresent(clientFilterMenu::select);

            clientFilterMenu.addListener(
                            filter -> preferenceStore.putValue(key, clientFilterMenu.getSelectedItem().getUUIDs()));

            // predicates
            if (preferenceStore.getBoolean(prefix + TaxonomyModel.KEY_FILTER_NON_ZERO))
                model.getNodeFilters().add(TaxonomyModel.FILTER_NON_ZERO);

            if (preferenceStore.getBoolean(prefix + TaxonomyModel.KEY_FILTER_NOT_RETIRED))
                model.getNodeFilters().add(TaxonomyModel.FILTER_NOT_RETIRED);

            this.getToolBar().addDisposeListener(e -> {
                preferenceStore.setValue(prefix + TaxonomyModel.KEY_FILTER_NON_ZERO,
                                model.getNodeFilters().contains(TaxonomyModel.FILTER_NON_ZERO));
                preferenceStore.setValue(prefix + TaxonomyModel.KEY_FILTER_NOT_RETIRED,
                                model.getNodeFilters().contains(TaxonomyModel.FILTER_NOT_RETIRED));
            });
        }

        private void updateIcon()
        {
            boolean hasActiveFilter = clientFilterMenu.hasActiveFilter() || !model.getNodeFilters().isEmpty();
            getToolItem().setImage(hasActiveFilter ? Images.FILTER_ON.image() : Images.FILTER_OFF.image());
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            // show the node filter menu only for the tree pages
            getCurrentPage().filter(p -> p instanceof AbstractNodeTreeViewer).ifPresent(p -> {
                manager.add(buildNodeFilterAction(Messages.FilterValuationNonZero, TaxonomyModel.FILTER_NON_ZERO));
                manager.add(buildNodeFilterAction(Messages.FilterNotRetired, TaxonomyModel.FILTER_NOT_RETIRED));
            });

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.MenuChooseClientFilter));
            clientFilterMenu.menuAboutToShow(manager);
        }

        private Action buildNodeFilterAction(String label, Predicate<TaxonomyNode> predicate)
        {
            Action action = new SimpleAction(label, a -> {
                boolean isActive = model.getNodeFilters().contains(predicate);
                if (!isActive)
                    model.getNodeFilters().add(predicate);
                else
                    model.getNodeFilters().remove(predicate);

                model.fireTaxonomyModelChange(model.getVirtualRootNode());
                updateIcon();
            });

            action.setChecked(model.getNodeFilters().contains(predicate));
            return action;
        }
    }

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
    private ClientFilter clientFilter;

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

        new ToolItem(toolBar, SWT.SEPARATOR);

        new FilterDropDown(toolBar, getPreferenceStore());
        addExportButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addExportButton(ToolBar toolBar)
    {
        AbstractDropDown.create(toolBar, Messages.MenuExportData, Images.EXPORT.image(), SWT.NONE,
                        (dropdown, manager) -> getCurrentPage().ifPresent(p -> p.exportMenuAboutToShow(manager)));
    }

    private void addConfigButton(ToolBar toolBar)
    {
        AbstractDropDown.create(toolBar, Messages.MenuShowHideColumns, Images.CONFIG.image(), SWT.NONE,
                        (dropdown, manager) -> getCurrentPage().ifPresent(p -> p.configMenuAboutToShow(manager)));
    }

    private Optional<Page> getCurrentPage()
    {
        StackLayout layout = (StackLayout) container.getLayout();
        return layout.topControl != null ? Optional.of((Page) layout.topControl.getData()) : Optional.empty();
    }

    @Override
    public void notifyModelUpdated()
    {
        Client filteredClient = this.clientFilter.filter(getClient());
        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, model.getCurrencyConverter(), LocalDateTime.now());
        model.setClientSnapshot(filteredClient, snapshot);
        model.fireTaxonomyModelChange(model.getVirtualRootNode());
    }

    private void addView(final ToolBar toolBar, String label, Images image, final int index)
    {
        Action showDefinition = new SimpleAction(label, Action.AS_CHECK_BOX, a -> activateView(index));
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
