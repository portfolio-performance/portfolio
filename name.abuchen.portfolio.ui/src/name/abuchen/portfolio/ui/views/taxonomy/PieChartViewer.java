package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.ui.util.PieChart;
import name.abuchen.portfolio.ui.util.PieChart.Slice;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */class PieChartViewer extends AbstractChartPage
{
    private PieChart pieChart;

    public PieChartViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public Control createControl(Composite container)
    {
        pieChart = new PieChart(container, SWT.NONE);

        updatePieSlices();

        return pieChart;
    }

    private void updatePieSlices()
    {
        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (TaxonomyNode child : getModel().getRootNode().getChildren())
        {
            if (getModel().isUnassignedCategoryInChartsExcluded() && child.isUnassignedCategory())
                continue;

            slices.add(new PieChart.Slice(child.getActual(), //
                            child.getName(), //
                            getRenderer().getColorFor(child)));
        }

        Collections.sort(slices, new Slice.ByValue());

        pieChart.setSlices(slices);
        pieChart.redraw();
    }

    @Override
    public void beforePage()
    {}

    @Override
    public void afterPage()
    {}

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        updatePieSlices();
    }

    @Override
    public void onConfigChanged()
    {
        updatePieSlices();
    }
}
