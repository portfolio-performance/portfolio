package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.ui.util.PieChart;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.TaxonomyModelChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */class PieChartViewer implements TaxonomyModelChangeListener
{
    private TaxonomyModel model;
    private TaxonomyNodeRenderer renderer;
    private PieChart pieChart;

    public PieChartViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        this.model = model;
        this.renderer = renderer;
        this.model.addListener(this);
    }

    public Control createContainer(Composite container)
    {
        pieChart = new PieChart(container, SWT.NONE);

        updatePieSlices();

        return pieChart;
    }

    private void updatePieSlices()
    {
        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (TaxonomyNode child : model.getRootNode().getChildren())
        {
            slices.add(new PieChart.Slice(child.getActual(), //
                            child.getName(), //
                            renderer.getColorFor(child)));
        }

        pieChart.setSlices(slices);
        pieChart.redraw();
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        updatePieSlices();
    }
}
