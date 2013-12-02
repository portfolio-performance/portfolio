package name.abuchen.portfolio.ui.views.taxonomy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.Aggregation.Period;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.StackedTimelineChart;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.NodeVisitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.joda.time.DateMidnight;
import org.joda.time.Interval;
import org.swtchart.ISeries;
import org.swtchart.Range;

public class StackedChartViewer extends Page
{
    private static class VehicleBuilder
    {
        private List<Integer> weights = new ArrayList<Integer>();
        private List<SeriesBuilder> series = new ArrayList<SeriesBuilder>();

        public void add(int weight, SeriesBuilder series)
        {
            this.weights.add(weight);
            this.series.add(series);
        }

        public void book(int index, AssetPosition pos)
        {
            long value = pos.getValuation();

            for (int ii = 0; ii < weights.size(); ii++)
                series.get(ii).book(index, value * weights.get(ii) / Classification.ONE_HUNDRED_PERCENT);
        }

    }

    private static class SeriesBuilder
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
            hasValues = true;
            values[index] += l;
        }

        public double[] getValues(long[] totals)
        {
            double[] answer = new double[values.length];
            for (int ii = 0; ii < answer.length; ii++)
            {
                if (totals[ii] == 0d)
                    answer[ii] = 0d;
                else
                    answer[ii] = values[ii] / (double) totals[ii];
            }
            return answer;
        }
    }

    private StackedTimelineChart chart;
    private boolean isVisible = false;
    private boolean isDirty = true;

    private List<DateMidnight> dates;

    public StackedChartViewer(ClientEditor editor, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);

        Interval interval = editor.loadReportingPeriods().getFirst().toInterval();

        Period weekly = Aggregation.Period.WEEKLY;

        final DateMidnight start = interval.getStart().toDateMidnight();
        final DateMidnight end = interval.getEnd().toDateMidnight();
        DateMidnight current = weekly.getStartDateFor(start);

        dates = new ArrayList<DateMidnight>();
        while (current.isBefore(end))
        {
            dates.add(current);
            current = current.plus(weekly.getPeriod());
        }
        dates.add(end);
    }

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
    public void nodeChange(TaxonomyNode node)
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
        final Map<InvestmentVehicle, VehicleBuilder> vehicle2builder = new HashMap<InvestmentVehicle, VehicleBuilder>();
        final Map<TaxonomyNode, SeriesBuilder> node2series = new HashMap<TaxonomyNode, SeriesBuilder>();

        getModel().visitAll(new NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
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
            }
        });

        final long[] totals = new long[dates.size()];

        int index = 0;
        for (DateMidnight current : dates)
        {
            ClientSnapshot snapshot = ClientSnapshot.create(getModel().getClient(), current.toDate());
            totals[index] = snapshot.getAssets();

            Map<InvestmentVehicle, AssetPosition> p = snapshot.getPositionsByVehicle();

            for (Map.Entry<InvestmentVehicle, VehicleBuilder> entry : vehicle2builder.entrySet())
            {
                AssetPosition pos = p.get(entry.getKey());
                if (pos != null)
                    entry.getValue().book(index, pos);
            }

            index++;
        }

        final List<SeriesBuilder> series = new ArrayList<SeriesBuilder>();
        for (SeriesBuilder s : node2series.values())
        {
            if (s.hasValues())
                series.add(s);
        }

        Collections.sort(series, new Comparator<SeriesBuilder>()
        {
            @Override
            public int compare(SeriesBuilder o1, SeriesBuilder o2)
            {
                long l1 = o1.values[o1.values.length - 1];
                long l2 = o2.values[o2.values.length - 1];
                return l1 > l2 ? -1 : l1 < l2 ? 1 : 0;
            }
        });

        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
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
        });
    }

}
