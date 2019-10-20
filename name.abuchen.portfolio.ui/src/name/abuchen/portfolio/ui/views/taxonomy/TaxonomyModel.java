package name.abuchen.portfolio.ui.views.taxonomy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.AssignmentNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.ClassificationNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode.UnassignedContainerNode;

public final class TaxonomyModel
{
    @FunctionalInterface
    public interface NodeVisitor
    {
        void visit(TaxonomyNode node);
    }

    @FunctionalInterface
    public interface TaxonomyModelUpdatedListener
    {
        void nodeChange(TaxonomyNode node);
    }

    @FunctionalInterface
    public interface DirtyListener
    {
        void onModelEdited();
    }

    public interface AttachedModel
    {
        default void setup(TaxonomyModel model)
        {
        }

        void recalculate(TaxonomyModel model);

        default void addColumns(ShowHideColumnHelper columns)
        {
        }
    }

    public static final String KEY_FILTER_NON_ZERO = "-filter-non-zero"; //$NON-NLS-1$
    public static final String KEY_FILTER_NOT_RETIRED = "-filter-not-retired"; //$NON-NLS-1$

    public static final Predicate<TaxonomyNode> FILTER_NON_ZERO = node -> node.isClassification()
                    || !node.getActual().isZero();
    public static final Predicate<TaxonomyNode> FILTER_NOT_RETIRED = node -> node.isClassification()
                    || !node.getAssignment().getInvestmentVehicle().isRetired();

    private final Taxonomy taxonomy;
    private final Client client;
    private final CurrencyConverter converter;

    /**
     * The Client file which was used to create the ClientSnapshot. When
     * filtering, we cannot replace the original client as the filtered client
     * may not contain all securities. But we need the filtered to calculate for
     * example the stacked chart series.
     */
    private Client filteredClient;
    private ClientSnapshot snapshot;

    private TaxonomyNode virtualRootNode;
    private TaxonomyNode classificationRootNode;
    private TaxonomyNode unassignedNode;
    private Map<InvestmentVehicle, Assignment> investmentVehicle2weight = new HashMap<>();

    private boolean excludeUnassignedCategoryInCharts = false;
    private boolean excludeSecuritiesInPieChart = false;
    private boolean orderByTaxonomyInStackChart = false;
    private String expansionStateDefinition;
    private String expansionStateRebalancing;

    private List<Predicate<TaxonomyNode>> nodeFilters = new ArrayList<>();

    private List<AttachedModel> attachedModels = new ArrayList<>();
    private List<TaxonomyModelUpdatedListener> listeners = new ArrayList<>();
    private List<DirtyListener> dirtyListener = new ArrayList<>();

    @Inject
    /* package */ TaxonomyModel(ExchangeRateProviderFactory factory, Client client, Taxonomy taxonomy)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(taxonomy);

        this.taxonomy = taxonomy;
        this.client = client;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());

        this.filteredClient = client;
        this.snapshot = ClientSnapshot.create(client, converter, LocalDate.now());

        this.attachedModels.add(new RecalculateTargetsAttachedModel());

        Classification virtualRoot = new Classification(null, Classification.VIRTUAL_ROOT,
                        Messages.PerformanceChartLabelEntirePortfolio, taxonomy.getRoot().getColor());
        virtualRootNode = new ClassificationNode(null, virtualRoot);

        Classification classificationRoot = taxonomy.getRoot();
        classificationRootNode = new ClassificationNode(virtualRootNode, classificationRoot);
        virtualRootNode.getChildren().add(classificationRootNode);

        LinkedList<TaxonomyNode> stack = new LinkedList<>();
        stack.add(classificationRootNode);

        while (!stack.isEmpty())
        {
            TaxonomyNode m = stack.pop();

            Classification classification = m.getClassification();

            for (Classification c : classification.getChildren())
            {
                TaxonomyNode cm = new ClassificationNode(m, c);
                stack.push(cm);
                m.getChildren().add(cm);
            }

            for (Assignment assignment : classification.getAssignments())
                m.getChildren().add(new AssignmentNode(m, assignment));

            Collections.sort(m.getChildren(), (o1, o2) -> Integer.compare(o1.getRank(), o2.getRank()));
        }

        unassignedNode = new UnassignedContainerNode(virtualRootNode, new Classification(virtualRoot,
                        Classification.UNASSIGNED_ID, Messages.LabelWithoutClassification));
        virtualRootNode.getChildren().add(unassignedNode);

        // add unassigned
        addUnassigned(client);

        // calculate actuals
        visitActuals(snapshot, virtualRootNode);

        // setup attached models
        this.attachedModels.forEach(m -> m.setup(this));

        // calculate targets
        runRecalculations();
    }

    private void addUnassigned(Client client)
    {
        for (Security security : client.getSecurities())
        {
            Assignment assignment = new Assignment(security);
            assignment.setWeight(0);
            investmentVehicle2weight.put(security, assignment);
        }

        for (Account account : client.getAccounts())
        {
            Assignment assignment = new Assignment(account);
            assignment.setWeight(0);
            investmentVehicle2weight.put(account, assignment);
        }

        visitAll(node -> {
            if (!(node instanceof AssignmentNode))
                return;

            Assignment assignment = node.getAssignment();
            Assignment count = investmentVehicle2weight.get(assignment.getInvestmentVehicle());
            count.setWeight(count.getWeight() + assignment.getWeight());
        });

        List<Assignment> unassigned = new ArrayList<>();
        for (Assignment assignment : investmentVehicle2weight.values())
        {
            if (assignment.getWeight() >= Classification.ONE_HUNDRED_PERCENT)
                continue;

            Assignment a = new Assignment(assignment.getInvestmentVehicle());
            a.setWeight(Classification.ONE_HUNDRED_PERCENT - assignment.getWeight());
            unassigned.add(a);

            assignment.setWeight(Classification.ONE_HUNDRED_PERCENT);
        }

        Collections.sort(unassigned, (o1, o2) -> o1.getInvestmentVehicle().toString()
                        .compareToIgnoreCase(o2.getInvestmentVehicle().toString()));

        for (Assignment assignment : unassigned)
            unassignedNode.addChild(assignment);
    }

    private void visitActuals(ClientSnapshot snapshot, TaxonomyNode node)
    {
        MutableMoney actual = MutableMoney.of(snapshot.getCurrencyCode());

        for (TaxonomyNode child : node.getChildren())
        {
            visitActuals(snapshot, child);
            actual.add(child.getActual());
        }

        if (node.isAssignment())
        {
            Assignment assignment = node.getAssignment();

            AssetPosition p = snapshot.getPositionsByVehicle().get(assignment.getInvestmentVehicle());

            if (p != null)
            {
                Money valuation = p.getValuation();
                actual.add(Money.of(valuation.getCurrencyCode(), Math.round(valuation.getAmount()
                                * assignment.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT)));
            }
        }

        node.setActual(actual.toMoney());
    }

    private void runRecalculations()
    {
        this.attachedModels.forEach(m -> m.recalculate(this));
    }

    public boolean isUnassignedCategoryInChartsExcluded()
    {
        return excludeUnassignedCategoryInCharts;
    }

    public void setExcludeUnassignedCategoryInCharts(boolean excludeUnassignedCategoryInCharts)
    {
        this.excludeUnassignedCategoryInCharts = excludeUnassignedCategoryInCharts;
    }

    public boolean isSecuritiesInPieChartExcluded()
    {
        return excludeSecuritiesInPieChart;
    }

    public void setExcludeSecuritiesInPieChart(boolean excludeSecuritiesInPieChart)
    {
        this.excludeSecuritiesInPieChart = excludeSecuritiesInPieChart;
    }

    public boolean isOrderByTaxonomyInStackChart()
    {
        return orderByTaxonomyInStackChart;
    }

    public void setOrderByTaxonomyInStackChart(boolean orderByTaxonomyInStackChart)
    {
        this.orderByTaxonomyInStackChart = orderByTaxonomyInStackChart;
    }

    public String getExpansionStateDefinition()
    {
        return expansionStateDefinition;
    }

    public void setExpansionStateDefinition(String expansionStateDefinition)
    {
        this.expansionStateDefinition = expansionStateDefinition;
    }

    public String getExpansionStateRebalancing()
    {
        return expansionStateRebalancing;
    }

    public void setExpansionStateRebalancing(String expansionStateRebalancing)
    {
        this.expansionStateRebalancing = expansionStateRebalancing;
    }

    public List<Predicate<TaxonomyNode>> getNodeFilters()
    {
        return nodeFilters;
    }

    public Stream<AttachedModel> getAttachedModels()
    {
        return attachedModels.stream();
    }

    public Taxonomy getTaxonomy()
    {
        return taxonomy;
    }

    /**
     * Returns the virtual root node, i.e. the root node that includes the
     * classification and the node with unassigned securities.
     */
    public TaxonomyNode getVirtualRootNode()
    {
        return virtualRootNode;
    }

    /**
     * Returns the root node of classifications, i.e. the part of the tree that
     * includes assigned investment vehicles.
     */
    public TaxonomyNode getClassificationRootNode()
    {
        return classificationRootNode;
    }

    /**
     * Returns the node that holds all unassigned investment vehicles.
     */
    public TaxonomyNode getUnassignedNode()
    {
        return unassignedNode;
    }

    /**
     * Returns the root node that is to be rendered in charts (pie, tree map,
     * stacked chart). It is the whole node tree unless unassigned investment
     * vehicles are not to be included or do not exist.
     */
    public TaxonomyNode getChartRenderingRootNode()
    {
        return isUnassignedCategoryInChartsExcluded() || getUnassignedNode().getActual().isZero()
                        ? getClassificationRootNode()
                        : getVirtualRootNode();
    }

    public Client getClient()
    {
        return client;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public String getCurrencyCode()
    {
        return converter.getTermCurrency();
    }

    public void setClientSnapshot(Client filteredClient, ClientSnapshot newSnapshot)
    {
        this.filteredClient = filteredClient;
        this.snapshot = newSnapshot;
        recalculate();
    }

    public Client getFilteredClient()
    {
        return filteredClient;
    }

    public ClientSnapshot getClientSnapshot()
    {
        return snapshot;
    }

    public void recalculate()
    {
        virtualRootNode.setActual(snapshot.getMonetaryAssets());
        visitActuals(snapshot, virtualRootNode);
        runRecalculations();
    }

    public void visitAll(NodeVisitor visitor)
    {
        virtualRootNode.accept(visitor);
    }

    public void addListener(TaxonomyModelUpdatedListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(TaxonomyModelUpdatedListener listener)
    {
        listeners.remove(listener);
    }

    public void fireTaxonomyModelChange(TaxonomyNode node)
    {
        listeners.forEach(listener -> listener.nodeChange(node));
    }

    public void addDirtyListener(DirtyListener listener)
    {
        dirtyListener.add(listener);
    }

    public void markDirty()
    {
        dirtyListener.forEach(DirtyListener::onModelEdited);
    }

    public int getWeightByInvestmentVehicle(InvestmentVehicle vehicle)
    {
        return investmentVehicle2weight.get(vehicle).getWeight();
    }

    public void setWeightByInvestmentVehicle(InvestmentVehicle vehicle, int weight)
    {
        investmentVehicle2weight.get(vehicle).setWeight(weight);
    }

    public boolean hasWeightError(TaxonomyNode node)
    {
        if (node.isUnassignedCategory() || node.isRoot())
        {
            return false;
        }
        else if (node.isClassification())
        {
            if (node.getParent().isRoot())
                return node.getWeight() != Classification.ONE_HUNDRED_PERCENT;
            else
                return node.getClassification().getParent().getChildrenWeight() != Classification.ONE_HUNDRED_PERCENT;
        }
        else
        {
            // node is assignment
            return getWeightByInvestmentVehicle(
                            node.getAssignment().getInvestmentVehicle()) != Classification.ONE_HUNDRED_PERCENT;
        }
    }
}
