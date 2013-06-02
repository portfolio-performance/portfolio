package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.ui.util.PieChart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */class PieChartViewer
{
    private TaxonomyModel model;

    public PieChartViewer(TaxonomyModel model)
    {
        this.model = model;
    }

    public Control createContainer(Composite container, TaxonomyNodeRenderer renderer)
    {
        PieChart pieChart = new PieChart(container, SWT.NONE);

        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (TaxonomyNode child : model.getRootNode().getChildren())
        {
            slices.add(new PieChart.Slice(child.getActual(), //
                            child.getName(), //
                            renderer.getColorFor(child)));
        }

        pieChart.setSlices(slices);
        pieChart.redraw();

        return pieChart;
    }
}
