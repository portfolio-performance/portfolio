package name.abuchen.portfolio.ui.views.taxonomy;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.swtchart.ISeries;
import org.swtchart.Range;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.Aggregation.Period;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.StackedTimelineChart;
import name.abuchen.portfolio.util.Interval;

public class StackedChartViewer extends AbstractChartPage
{
    private static class VehicleBuilder
    {
        private List<Integer> weights = new ArrayList<>();
        private List<SeriesBuilder> series = new ArrayList<>();

        public void add(int weight, SeriesBuilder series)
        {
            this.weights.add(weight);
            this.series.add(series);
        }

        public void book(int index, AssetPosition pos)
        {
            long value = pos.getValuation().getAmount();

            for (int ii = 0; ii < weights.size(); ii++)
                series.get(ii).book(index, value * weights.get(ii) / Classification.ONE_HUNDRED_PERCENT);
        }

    }

    private static class SeriesBuilder implements Comparable<SeriesBuilder>
    {
        private TaxonomyNode node;
        private long[] values;
        private boolean hasValues = false;

        public SeriesBuilder(TaxonomyNode node, int size)
        {
            this.node = node;
            this.values = new long[size];
        }

        public boolean hasValues()
        {
            return hasValues;
        }

        public void book(int index, long l)
        {
            // stacked charts cannot handle negative values.
            // Therefore we ignore them here, which in turn means that the other
            // data series will stack up to over 100% in the chart
            if (l < 0)
                return;

            hasValues = true;
            values[index] += l;
        }

        public double[] getValues(long[] totals)
        {
            double[] answer = new double[values.length];
            for (int ii = 0; ii < answer.length; ii++)
            {
                if (totals[ii] == 0)
                    answer[ii] = 0d;
                else
                    answer[ii] = values[ii] / (double) totals[ii];
            }
            return answer;
        }

        @Override
        public int compareTo(SeriesBuilder other)
        {
            long l1 = values[values.length - 1];
            long l2 = other.values[other.values.length - 1];
            return Long.compare(l1, l2) * -1;
        }
    }

    private StackedTimelineChart chart;
    private boolean isVisible = false;
    private boolean isDirty = true;

    private List<LocalDate> dates;

    @Inject
    public StackedChartViewer(PortfolioPart part, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);

        Interval interval = part.getReportingPeriods().get(0).toInterval(LocalDate.now());

        Period weekly = Aggregation.Period.WEEKLY;

        final LocalDate start = interval.getStart();
        final LocalDate now = LocalDate.now();
        final LocalDate end = interval.getEnd().isAfter(now) ? now : interval.getEnd();
        LocalDate current = weekly.getStartDateFor(start);

        dates = new ArrayList<>();
        while (current.isBefore(end))
        {
            dates.add(current);
            current = current.plus(weekly.getPeriod());
        }
        dates.add(end);
    }

    @Override
    public Control createControl(Composite container)
    {
        Composite composite = new Composite(container, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        composite.setLayout(new FillLayout());

        chart = new StackedTimelineChart(composite, dates);
        chart.getTitle().setVisible(false);

        chart.getLegend().setPosition(SWT.BOTTOM);
        chart.getLegend().setVisible(true);

        chart.getAxisSet().getYAxis(0).getTick().setFormat(new DecimalFormat("#0.0%")); //$NON-NLS-1$

        return composite;
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        super.configMenuAboutToShow(manager);
        Action action = new SimpleAction(Messages.LabelOrderByTaxonomy, a -> {
            getModel().setOrderByTaxonomyInStackChart(!getModel().isOrderByTaxonomyInStackChart());
            onConfigChanged();
        });
        action.setChecked(getModel().isOrderByTaxonomyInStackChart());
        manager.add(action);
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        onConfigChanged();
    }

    @Override
    public void onConfigChanged()
    {
        isDirty = true;

        if (isVisible)
            asyncUpdateChart();
    }

    @Override
    public void beforePage()
    {
        isVisible = true;

        if (isDirty)
            asyncUpdateChart();
    }

    @Override
    public void afterPage()
    {
        isVisible = false;
    }

    private void asyncUpdateChart()
    {
        new Job(Messages.JobLabelUpdateStackedLineChart)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                updateChart();

                return Status.OK_STATUS;
            }

        }.schedule();
    }

    private void updateChart()
    {
        final Map<InvestmentVehicle, VehicleBuilder> vehicle2builder = new HashMap<>();
        final Map<TaxonomyNode, SeriesBuilder> node2series = new LinkedHashMap<>();

        getModel().visitAll(node -> {
            if (node.isClassification())
            {
                node2series.put(node, new SeriesBuilder(node, dates.size()));
            }
            else
            {
                InvestmentVehicle vehicle = node.getAssignment().getInvestmentVehicle();

                VehicleBuilder builder = vehicle2builder.get(vehicle);
                if (builder == null)
                {
                    builder = new VehicleBuilder();
                    vehicle2builder.put(vehicle, builder);
                }

                builder.add(node.getWeight(), node2series.get(node.getParent()));
            }
        });

        final long[] totals = new long[dates.size()];

        int index = 0;
        for (LocalDate current : dates)
        {
            ClientSnapshot snapshot = ClientSnapshot.create(getModel().getFilteredClient(),
                            getModel().getCurrencyConverter(), current);
            totals[index] = snapshot.getMonetaryAssets().getAmount();

            Map<InvestmentVehicle, AssetPosition> p = snapshot.getPositionsByVehicle();

            for (Map.Entry<InvestmentVehicle, VehicleBuilder> entry : vehicle2builder.entrySet())
            {
                AssetPosition pos = p.get(entry.getKey());
                if (pos != null)
                    entry.getValue().book(index, pos);
            }

            index++;
        }

        // if the unassigned category is excluded, reduce the total values
        if (getModel().isUnassignedCategoryInChartsExcluded())
        {
            SeriesBuilder unassigned = node2series.get(getModel().getUnassignedNode());
            for (int ii = 0; ii < totals.length; ii++)
                totals[ii] -= unassigned.values[ii];
        }

        Stream<SeriesBuilder> seriesStream = node2series.values().stream().filter(SeriesBuilder::hasValues);
        if (getModel().isUnassignedCategoryInChartsExcluded())
            seriesStream = seriesStream.filter(s -> !s.node.isUnassignedCategory());

        List<SeriesBuilder> series = seriesStream.collect(Collectors.toList());

        if (getModel().isOrderByTaxonomyInStackChart())
        {
            // reverse because chart is stacked bottom-up
            Collections.reverse(series);
        }
        else
        {
            Collections.sort(series);
        }

        Display.getDefault().asyncExec(() -> rebuildChartSeries(totals, series));
    }

    private void rebuildChartSeries(long[] totals, List<SeriesBuilder> series)
    {
        if (chart.isDisposed())
            return;

        try
        {
            chart.suspendUpdate(true);
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            for (SeriesBuilder serie : series)
            {
                chart.addSeries(serie.node.getClassification().getPathName(false), //
                                serie.getValues(totals), //
                                getRenderer().getColorFor(serie.node));
            }

            chart.getAxisSet().adjustRange();
            chart.getAxisSet().getYAxis(0).setRange(new Range(-0.01, 1.01));
        }
        finally
        {
            chart.suspendUpdate(false);
            chart.redraw();
        }

        isDirty = false;
    }
}
