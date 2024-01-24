package name.abuchen.portfolio.ui.views.taxonomy;

import jakarta.inject.Inject;

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
            chart = new TaxonomyDonutSWT(this, view);
        }
        else
        {
            chart = new TaxonomyDonutBrowser(make(EmbeddedBrowser.class), this, view);
        }
        return chart.createControl(container);
    }

    @Override
    public void beforePage() // NOSONAR
    {
    }

    @Override
    public void afterPage() // NOSONAR
    {
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        chart.refresh(null);
    }
}
