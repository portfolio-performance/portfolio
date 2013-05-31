package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.PieChart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */class PieChartViewer
{
    private TaxonomyNode model;

    public PieChartViewer(TaxonomyNode model)
    {
        this.model = model;
    }

    public Control createContainer(Composite container)
    {
        PieChart pieChart = new PieChart(container, SWT.NONE);

        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (TaxonomyNode child : model.getChildren())
        {
            slices.add(new PieChart.Slice(child.getActual(), //
                            child.getName(), //
                            Colors.toRGB(child.getColor())));
        }

        pieChart.setSlices(slices);
        pieChart.redraw();

        return pieChart;
    }
}
