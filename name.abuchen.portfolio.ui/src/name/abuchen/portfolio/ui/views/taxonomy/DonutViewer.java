package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.views.IPieChart;

/* package */class DonutViewer extends AbstractChartPage
{
    private AbstractFinanceView view;

    private IPieChart chart;

    @Inject
    @Preference(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS)
    boolean useSWTCharts;

    @Inject
    public DonutViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.view = view;
    }

    @Override
    public Control createControl(Composite container)
    {
        if (this.useSWTCharts)
        {
            chart = new TaxonomyDonutSWT(this, view, IPieChart.ChartType.DONUT);
        }
        else
        {
            chart = new TaxonomyDonutBrowser(make(EmbeddedBrowser.class), this.view, this);
        }
        return chart.createControl(container);
    }

    @Override
    public void beforePage()
    {
    }

    @Override
    public void afterPage()
    {
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        chart.refresh(null);
    }

    @Override
    public void onConfigChanged()
    {
        chart.refresh(null);
    }
}
