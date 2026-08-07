package name.abuchen.portfolio.ui.views.taxonomy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.math.Rebalancer;
import name.abuchen.portfolio.math.Rebalancer.FixedSumRebalancer;
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
import name.abuchen.portfolio.util.TextUtil;

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

    /** key used to persist the sort mode on a classification's data map */
    private static final String KEY_SORT_MODE = "sort-mode"; //$NON-NLS-1$

    private enum SortCriterion
    {
        TYPE, NAME, ACTUAL
    }

    /**
     * Sort mode of a classification. If a mode other than {@link #MANUAL} is
     * active, the children of that classification are kept sorted dynamically,
     * i.e. new and moved items are automatically placed according to the
     * criteria. The mode is stored per classification and persisted with the
     * file.
     */
    public enum SortMode
    {
        MANUAL, //
        TYPE_NAME(SortCriterion.TYPE, SortCriterion.NAME), //
        TYPE_ACTUAL(SortCriterion.TYPE, SortCriterion.ACTUAL), //
        NAME(SortCriterion.NAME), //
        ACTUAL(SortCriterion.ACTUAL);

        private final SortCriterion[] criteria;

        SortMode(SortCriterion... criteria)
        {
            this.criteria = criteria;
        }

        SortCriterion[] getCriteria()
        {
            return criteria;
        }

        /**
         * Whether the order depends on the (actual) value of an item and hence
         * needs to be re-applied when values change.
         */
        boolean isValueBased()
        {
            for (SortCriterion criterion : criteria)
            {
                if (criterion == SortCriterion.ACTUAL)
                    return true;
            }
            return false;
        }

        static SortMode of(String value)
        {
            if (value == null)
                return MANUAL;

            try
            {
                return valueOf(value);
            }
            catch (IllegalArgumentException e)
            {
                return MANUAL;
            }
        }
    }

    private final Taxonomy taxonomy;
    private final Client client;
    private final ExchangeRateProviderFactory factory;
    private CurrencyConverter converter;

    /**
     * The Client file which was used to create the ClientSnapshot. When
     * filtering, we cannot replace the original client as the filtered client
     * may not contain all securities. But we need the filtered to calculate for
     * example the stacked chart series.
     */
    private Client filteredClient;
    private ClientSnapshot snapshot;

    private TaxonomyNode virtualRootNode;
    private TaxonomyNode.ClassificationNode classificationRootNode;
    private TaxonomyNode unassignedNode;
    private Map<InvestmentVehicle, Assignment> investmentVehicle2weight = new HashMap<>();

    private boolean excludeUnassignedCategoryInCharts = false;
    private boolean excludeSecuritiesInPieChart = false;
    private boolean orderByTaxonomyInStackChart = false;
    private String expansionStateDefinition;
    private String expansionStateRebalancing;
    private String coloringStrategy;
    private boolean showGroupHeadingInTreeMap = false;
    private String colorSchemaInTreeMap;

    private List<Predicate<TaxonomyNode>> nodeFilters = new ArrayList<>();
    private Pattern filterPattern;

    private List<AttachedModel> attachedModels = new ArrayList<>();
    private List<TaxonomyModelUpdatedListener> listeners = new ArrayList<>();
    private List<DirtyListener> dirtyListener = new ArrayList<>();

    private Rebalancer.RebalancingSolution rebalancingSolution;

    /**
     * Signed cash flow the user wants to invest (positive) or withdraw
     * (negative) when rebalancing. Transient session state only; never
     * persisted.
     */
    private Money amountToInvest;

    public TaxonomyModel(ExchangeRateProviderFactory factory, Client client, Taxonomy taxonomy)
    {
        this.taxonomy = Objects.requireNonNull(taxonomy);
        this.client = Objects.requireNonNull(client);
        this.factory = Objects.requireNonNull(factory);

        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        this.amountToInvest = Money.of(getCurrencyCode(), 0);

        this.filteredClient = client;
        this.snapshot = ClientSnapshot.create(client, converter, LocalDate.now());

        this.attachedModels.add(new RecalculateTargetsAttachedModel());
        this.attachedModels.add(new ExpectedReturnsAttachedModel());

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

        // re-apply the stored sort mode per classification -> the tree is built
        // in rank order, but ranks can be stale (e.g. values changed while the
        // view was closed), so folders with a sort mode are re-sorted
        applyStoredSort(classificationRootNode, false);
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

        Collections.sort(unassigned, (o1, o2) -> TextUtil.compare(o1.getInvestmentVehicle().toString(),
                        o2.getInvestmentVehicle().toString()));

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
        rebalancingSolution = rebalance(amountToInvest);
    }

    /**
     * Returns the sort mode stored on the given classification node.
     */
    public SortMode getSortMode(TaxonomyNode node)
    {
        if (!node.isClassification())
            return SortMode.MANUAL;

        return SortMode.of((String) node.getClassification().getData(KEY_SORT_MODE));
    }

    /**
     * Stores the sort mode on the given classification node.
     */
    public void setSortMode(TaxonomyNode node, SortMode mode)
    {
        if (!node.isClassification())
            return;

        // do not store the default -> keeps the file clean
        node.getClassification().setData(KEY_SORT_MODE, mode == SortMode.MANUAL ? null : mode.name());
    }

    /**
     * Sets the sort mode on a classification node and – unless
     * {@link SortMode#MANUAL} – sorts its children immediately. When
     * {@code recursive} is set, the mode is applied to all descendant
     * classifications as well. Does not fire an update notification.
     */
    public void setSortMode(TaxonomyNode node, SortMode mode, boolean recursive)
    {
        setSortMode(node, mode);

        if (mode != SortMode.MANUAL)
            sort(node, mode.getCriteria());

        if (recursive)
        {
            for (TaxonomyNode child : node.getChildren())
            {
                if (child.isClassification())
                    setSortMode(child, mode, true);
            }
        }
    }

    /**
     * Re-applies the stored sort mode of every classification to its own
     * children so that newly added, moved or revalued items are placed
     * according to each folder's mode. If {@code valueModesOnly} is set, only
     * value-based modes are re-applied. Does not fire an update notification.
     */
    public void applyStoredSort(TaxonomyNode node, boolean valueModesOnly)
    {
        if (!node.isClassification())
            return;

        SortMode mode = getSortMode(node);
        if (mode != SortMode.MANUAL && (!valueModesOnly || mode.isValueBased()))
            sort(node, mode.getCriteria());

        for (TaxonomyNode child : node.getChildren())
            applyStoredSort(child, valueModesOnly);
    }

    /**
     * Sorts the children of a node according to the given criteria and updates
     * the ranks. Does not fire an update notification.
     */
    private void sort(TaxonomyNode node, SortCriterion... criteria) // NOSONAR
    {
        Collections.sort(node.getChildren(), (node1, node2) -> { // NOSONAR
            // unassigned category always stays at the end of the list
            if (node1.isUnassignedCategory())
                return 1;
            if (node2.isUnassignedCategory())
                return -1;

            for (int ii = 0; ii < criteria.length; ii++)
            {
                switch (criteria[ii])
                {
                    case TYPE:
                        if (node1.isClassification() && !node2.isClassification())
                            return -1;
                        if (!node1.isClassification() && node2.isClassification())
                            return 1;
                        break;
                    case NAME:
                        int cn = TextUtil.compare(node1.getName(), node2.getName());
                        if (cn != 0)
                            return cn;
                        break;
                    case ACTUAL:
                        int ca = node2.getActual().compareTo(node1.getActual());
                        if (ca != 0)
                            return ca;
                        break;
                    default:

                }
            }

            return 0;
        });

        int rank = 0;
        for (TaxonomyNode child : node.getChildren())
            child.setRank(rank++);
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
    
    public void setColoringStrategy(String coloringStrategy)
    {
        this.coloringStrategy = coloringStrategy;
    }
    
    public String getColoringStrategy()
    {
        return coloringStrategy;
    }

    public boolean doShowGroupHeadingInTreeMap()
    {
        return showGroupHeadingInTreeMap;
    }

    public void setShowGroupHeadingInTreeMap(boolean showGroupHeadingInTreeMap)
    {
        this.showGroupHeadingInTreeMap = showGroupHeadingInTreeMap;
    }

    public String getColorSchemaInTreeMap()
    {
        return colorSchemaInTreeMap;
    }

    public void setColorSchemaInTreeMap(String colorSchema)
    {
        this.colorSchemaInTreeMap = colorSchema;
    }

    public List<Predicate<TaxonomyNode>> getNodeFilters()
    {
        return nodeFilters;
    }

    public Pattern getFilterPattern()
    {
        return filterPattern;
    }

    public void setFilterPattern(Pattern filterPattern)
    {
        this.filterPattern = filterPattern;
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

    public void updateClientSnapshot(Client filteredClient)
    {
        if (!filteredClient.getBaseCurrency().equals(converter.getTermCurrency()))
            this.converter = new CurrencyConverterImpl(factory, filteredClient.getBaseCurrency());

        this.filteredClient = filteredClient;
        this.snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());

        recalculate();
        fireTaxonomyModelChange(getVirtualRootNode());
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

        // keep value-sorted folders correctly ordered when actuals change
        applyStoredSort(classificationRootNode, true);
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

    public Rebalancer.RebalancingSolution getRebalancingSolution()
    {
        return rebalancingSolution;
    }

    /**
     * Sets the signed amount to invest (positive) or withdraw (negative) and
     * recomputes the single rebalancing solution for that amount. The amount is
     * transient session state; this must not mark the client dirty.
     */
    public void setAmountToInvest(Money amount)
    {
        this.amountToInvest = amount;
        this.rebalancingSolution = rebalance(amount);
        fireTaxonomyModelChange(getVirtualRootNode());
    }

    private Rebalancer.RebalancingSolution rebalance(Money amount)
    {
        List<InvestmentVehicle> inexactResultsDueToEmptyClassifications = new ArrayList<>();
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(amount);
        collectConstraints(classificationRootNode, rebalancer, Collections.emptyMap(),
                        inexactResultsDueToEmptyClassifications, Money.of(getCurrencyCode(), 0), amount);
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        solution.markAllAsInexact(inexactResultsDueToEmptyClassifications);
        return solution;
    }

    private void collectConstraints(ClassificationNode node, FixedSumRebalancer rebalancer,
                    Map<InvestmentVehicle, Double> investmentVehiclesInParentNodes,
                    List<? super InvestmentVehicle> inexactResultsDueToEmptyClassifications,
                    Money unbalancedSumToHandle, Money amountToInvest)
    {
        double thisWeight = node.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT;
        List<TaxonomyNode> allChildren = node.getChildren();
        List<TaxonomyNode.AssignmentNode> assignmentChildren = allChildren.stream().filter(TaxonomyNode::isAssignment)
                        .map(child -> (TaxonomyNode.AssignmentNode) child).collect(Collectors.toList());
        List<TaxonomyNode.ClassificationNode> classificationChildren = allChildren.stream()
                        .filter(TaxonomyNode::isClassification).map(child -> (TaxonomyNode.ClassificationNode) child)
                        .collect(Collectors.toList());

        Map<InvestmentVehicle, Double> newInvestmentVehiclesInParentNodes = new HashMap<>(
                        investmentVehiclesInParentNodes);
        for (Map.Entry<InvestmentVehicle, Double> entry : newInvestmentVehiclesInParentNodes.entrySet())
            entry.setValue(entry.getValue() * thisWeight);

        for (AssignmentNode assignmentNode : assignmentChildren)
            addToMap(newInvestmentVehiclesInParentNodes, assignmentNode.getAssignment().getInvestmentVehicle(),
                            (double) assignmentNode.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT);

        Money unbalancedSum = unbalancedSumToHandle;
        List<ClassificationNode> childsToRebalance = new ArrayList<>();
        int rebalancableNodesWeight = 0;
        for (ClassificationNode child : classificationChildren)
        {
            if (canBeRebalanced(child))
            {
                childsToRebalance.add(child);
                rebalancableNodesWeight += child.getWeight();
            }
            else
            {
                // This child could not be rebalanced, because it had no
                // assignments that are included in the rebalancing.
                // We try to balance it at least on this level.
                unbalancedSum = unbalancedSum.add(child.getTarget()).subtract(child.getActual());
            }
        }

        for (ClassificationNode child : childsToRebalance)
        {
            // Rebalance the rebalancable classification children. The amount to
            // invest is split by the same weight ratio as the unbalanced sum,
            // but tracked separately so that fully placeable new money does not
            // trip the "inexact" heuristic below.
            double weightRatio = (double) child.getWeight() / (double) rebalancableNodesWeight;
            collectConstraints(child, rebalancer,
                            new HashMap<InvestmentVehicle, Double>(newInvestmentVehiclesInParentNodes),
                            inexactResultsDueToEmptyClassifications, unbalancedSum.multiplyAndRound(weightRatio),
                            amountToInvest.multiplyAndRound(weightRatio));
        }

        if (unbalancedSum.getAmount() != 0)
        {
            // The sum of all unbalancable sub-categories does not add up to
            // zero.
            // => The result in the remaining categories is inexact in any case.
            for (ClassificationNode child : childsToRebalance)
                collectInvestmentVehicles(child, inexactResultsDueToEmptyClassifications);
        }

        if (childsToRebalance.isEmpty())
        {
            // All sub-classifications (if any) have no securities included in
            // the rebalancing.
            // We have to add a constraint for this level
            Map<InvestmentVehicle, Double> equation = new HashMap<>(assignmentChildren.size());
            for (AssignmentNode assignmentNode : assignmentChildren)
            {
                InvestmentVehicle investmentVehicle = assignmentNode.getAssignment().getInvestmentVehicle();
                if (getTaxonomy().isUsedForRebalancing(investmentVehicle))
                    addToMap(equation, investmentVehicle,
                                    assignmentNode.getWeight() / (double) Classification.ONE_HUNDRED_PERCENT);
            }

            // The investment vehicles in parent nodes implicitly lower the
            // weight for this node. We have to take this into account.
            for (Map.Entry<InvestmentVehicle, Double> entry : investmentVehiclesInParentNodes.entrySet())
                addToMap(equation, entry.getKey(), entry.getValue() * thisWeight);

            rebalancer.addConstraint(new Rebalancer.RebalancingConstraint(equation,
                            node.getTarget().subtract(node.getActual()).add(unbalancedSumToHandle).add(amountToInvest)));
        }
    }

    /**
     * @return {@code true}, iff at least one investment vehicle (that is
     *         included in the rebalancing) is contained in this classification.
     */
    private boolean canBeRebalanced(TaxonomyNode node)
    {
        if (node.isAssignment())
        {
            InvestmentVehicle investmentVehicle = node.getAssignment().getInvestmentVehicle();
            return getTaxonomy().isUsedForRebalancing(investmentVehicle);
        }
        else // node.isClassification()
        {
            for (TaxonomyNode child : node.getChildren())
            {
                if (canBeRebalanced(child))
                    return true;
            }
            return false;
        }
    }

    private void collectInvestmentVehicles(TaxonomyNode node, Collection<? super InvestmentVehicle> collection)
    {
        InvestmentVehicle investmentVehicle = node.getBackingInvestmentVehicle();
        if (investmentVehicle != null)
            collection.add(investmentVehicle);
        for (TaxonomyNode child : node.getChildren())
            collectInvestmentVehicles(child, collection);
    }

    // Helper method for building the rebalancer.
    private static void addToMap(Map<InvestmentVehicle, Double> map, InvestmentVehicle investmentVehicle, double weight)
    {
        map.put(investmentVehicle, map.getOrDefault(investmentVehicle, 0d) + weight);
    }
}
