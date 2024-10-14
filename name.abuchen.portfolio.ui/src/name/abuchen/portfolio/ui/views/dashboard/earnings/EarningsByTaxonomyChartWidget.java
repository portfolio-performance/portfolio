package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.LabelConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.TaxonomyConfig;
import name.abuchen.portfolio.ui.views.dashboard.charts.CircularChartWidget;
import name.abuchen.portfolio.util.ColorConversion;
import name.abuchen.portfolio.util.Interval;

record Item(InvestmentVehicle vehicle, long value, int weight)
{
}

record ToolTipItem(Classification classification, long classificationValue, InvestmentVehicle vehicle, long value)
{
}

public class EarningsByTaxonomyChartWidget extends CircularChartWidget<Map<InvestmentVehicle, Item>>
{
    private LocalResourceManager resources;

    public EarningsByTaxonomyChartWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        
        addConfigAfter(LabelConfig.class, new TaxonomyConfig(this));
        addConfigAfter(TaxonomyConfig.class, new ReportingPeriodConfig(this));

        // adding in reverse
        addConfigAfter(ClientFilterConfig.class, new GrossNetTypeConfig(this));
        addConfigAfter(ClientFilterConfig.class, new EarningTypeConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        final Composite control = super.createControl(parent, resources);
        this.resources = new LocalResourceManager(JFaceResources.getResources(), control);
        return control;
    }

    @Override
    protected void configureTooltip(CircularChartToolTip toolTip)
    {
        toolTip.setToolTipBuilder((composite, currentNode) -> {

            final Composite data = new Composite(composite, SWT.NONE);
            data.setLayout(new RowLayout(SWT.VERTICAL));

            Label label = new Label(data, SWT.NONE);
            label.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            var taxonomy = get(TaxonomyConfig.class).getTaxonomy();
            label.setText(taxonomy != null ? taxonomy.getName() : ""); //$NON-NLS-1$

            @SuppressWarnings("unchecked")
            List<Item> skipped = (List<Item>) getChart().getData();

            label = new Label(data, SWT.NONE);
            label.setText(Values.Amount.format(Values.Amount.factorize(currentNode.getParent().getValue())));

            ToolTipItem item = (ToolTipItem) currentNode.getData();
            if (item != null)
            {
                label = new Label(data, SWT.NONE);
                label.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                label.setText(item.classification().getName());

                label = new Label(data, SWT.NONE);
                label.setText(MessageFormat.format("{0}  |  {1}", //$NON-NLS-1$
                                Values.Amount.format(item.classificationValue()),
                                Values.Percent2.format(item.classificationValue() / Values.Amount.divider()
                                                / currentNode.getParent().getValue())));

                label = new Label(data, SWT.NONE);
                label.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                label.setText(item.vehicle().getName());

                label = new Label(data, SWT.NONE);
                label.setText(MessageFormat.format("{0}  |                {1}", //$NON-NLS-1$
                                Values.Amount.format(item.value()),
                                Values.Percent2.format(currentNode.getValue() / currentNode.getParent().getValue())));
            }
            else if (skipped != null && !skipped.isEmpty())
            {
                label = new Label(data, SWT.NONE);
                label.setText(Messages.MsgWarningPieChartWithNegativeValues);

                skipped.forEach(node -> {
                    Label right = new Label(data, SWT.NONE);
                    right.setText(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                                    node.vehicle().getName(), Values.Amount.format(node.value())));
                });
            }
        });

    }

    @Override
    public Supplier<Map<InvestmentVehicle, Item>> getUpdateTask()
    {
        ClientFilter clientFilter = get(ClientFilterConfig.class).getSelectedFilter();
        EarningType earningsType = get(EarningTypeConfig.class).getValue();
        GrossNetType grossNetType = get(GrossNetTypeConfig.class).getValue();
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

        return () -> {

            Map<InvestmentVehicle, Item> result = new HashMap<>();

            CurrencyConverter converter = getDashboardData().getCurrencyConverter();

            Client filteredClient = clientFilter.filter(getClient());

            for (Account account : filteredClient.getAccounts())
            {
                for (AccountTransaction tx : account.getTransactions()) // NOSONAR
                {
                    if (!earningsType.isIncluded(tx))
                        continue;

                    if (!interval.contains(tx.getDateTime()))
                        continue;

                    InvestmentVehicle vehicle = tx.getSecurity() != null ? tx.getSecurity() : account;

                    Money value = (grossNetType == GrossNetType.GROSS ? tx.getGrossValue() : tx.getMonetaryAmount())
                                    .with(converter.at(tx.getDateTime()));

                    var item = result.computeIfAbsent(vehicle, v -> new Item(v, 0, Classification.ONE_HUNDRED_PERCENT));

                    item = new Item(item.vehicle(),
                                    item.value() + (tx.getType().isCredit() ? value.getAmount() : -value.getAmount()),
                                    Classification.ONE_HUNDRED_PERCENT);

                    result.put(vehicle, item);
                }
            }

            return result;
        };
    }

    @Override
    protected void createCircularSeries(Map<InvestmentVehicle, Item> vehicle2money)
    {
        Taxonomy taxonomy = get(TaxonomyConfig.class).getTaxonomy();
        if (taxonomy == null)
            return;

        // pie charts cannot have negative pie slides --> remember the removed
        // items in order to display them in the tool tip
        List<Item> skippedDueToNegativeValue = new ArrayList<>();
        getChart().setData(skippedDueToNegativeValue);

        for (Iterator<Map.Entry<InvestmentVehicle, Item>> it = vehicle2money.entrySet().iterator(); it.hasNext();)
        {
            var entry = it.next();

            if (entry.getValue().value() < 0)
            {
                it.remove();
                skippedDueToNegativeValue.add(entry.getValue());
            }
        }

        Map<String, Color> id2color = new HashMap<>();
        ICircularSeries<?> circularSeries = (ICircularSeries<?>) getChart().getSeriesSet()
                        .createSeries(SeriesType.DOUGHNUT, Messages.LabelEarningsByTaxonomy);
        circularSeries.setSliceColor(getChart().getPlotArea().getBackground());
        Node rootNode = circularSeries.getRootNode();

        for (Classification classification : taxonomy.getRoot().getChildren())
        {
            final List<Assignment> assignments = classification.flattenAssignments().getAssignments();

            List<Item> nodes = new ArrayList<>();
            for (Assignment assignment : assignments)
            {
                Item item = vehicle2money.get(assignment.getInvestmentVehicle());
                if (item == null || item.value() == 0)
                    continue;

                long value = item.value();

                if (assignment.getWeight() < item.weight())
                {
                    value = BigDecimal.valueOf(value) //
                                    .multiply(BigDecimal.valueOf(assignment.getWeight()), Values.MC) //
                                    .divide(BigDecimal.valueOf(item.weight()), Values.MC)
                                    .setScale(0, RoundingMode.HALF_DOWN).longValue();
                }

                vehicle2money.put(assignment.getInvestmentVehicle(), new Item(assignment.getInvestmentVehicle(),
                                item.value() - value, item.weight() - assignment.getWeight()));

                nodes.add(new Item(item.vehicle(), value, assignment.getWeight()));
            }

            Collections.sort(nodes, (a, b) -> Long.compare(b.value(), a.value()));
            long sum = nodes.stream().mapToLong(Item::value).sum();
            for (Item item : nodes)
            {
                String nodeId = classification.getId() + '/' + item.vehicle().getUUID();
                Node n = rootNode.addChild(nodeId, item.value() / Values.Amount.divider());
                n.setData(new ToolTipItem(classification, sum, item.vehicle(), item.value()));
                id2color.put(nodeId, resources.createColor(ColorConversion.hex2RGB(classification.getColor())));
            }
        }

        // create unassigned chart nodes
        Classification unassigned = new Classification(null, Classification.UNASSIGNED_ID,
                        Messages.LabelWithoutClassification);
        long sum = vehicle2money.values().stream().mapToLong(Item::value).sum();
        vehicle2money.values().stream() //
                        .filter(item -> item.value() > 0) //
                        .sorted((a, b) -> Long.compare(b.value(), a.value())) //
                        .forEach(item -> {
                            String nodeId = item.vehicle().getUUID();
                            Node n = rootNode.addChild(nodeId, item.value() / Values.Amount.divider());
                            n.setData(new ToolTipItem(unassigned, sum, item.vehicle(), item.value()));
                            id2color.put(nodeId, Colors.OTHER_CATEGORY);
                        });

        // no earnings found; set an error message
        if (id2color.isEmpty())
        {
            circularSeries.setSeries(new String[] { Messages.MsgWarningPieChartNoValues }, new double[] { 0 });
            circularSeries.setColor(Messages.MsgWarningPieChartNoValues, Colors.LIGHT_GRAY);
        }

        id2color.forEach(circularSeries::setColor);
    }

}
