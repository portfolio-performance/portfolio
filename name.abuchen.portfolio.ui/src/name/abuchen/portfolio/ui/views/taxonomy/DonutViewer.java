package name.abuchen.portfolio.ui.views.taxonomy;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.IPieChart;

/* package */class DonutViewer extends AbstractChartPage
{
    private AbstractFinanceView view;

    private IPieChart chart;

    /**
     * True while the page is shown in the taxonomy view.
     */
    private boolean isVisible = false;

    /**
     * True if the model changed while the page was not shown.
     */
    private boolean isDirty = false;

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
    public void configMenuAboutToShow(IMenuManager manager)
    {
        super.configMenuAboutToShow(manager);

        Action action = new SimpleAction(Messages.LabelIncludeSecuritiesInPieChart, a -> {
            getModel().setExcludeSecuritiesInPieChart(!getModel().isSecuritiesInPieChartExcluded());
            getModel().fireTaxonomyModelChange(getModel().getVirtualRootNode());
            chart.refresh(null);
        });
        action.setChecked(!getModel().isSecuritiesInPieChartExcluded());
        manager.add(action);
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
        Control control = chart.createControl(container);

        // the chart has just been built with the current data
        isDirty = false;

        return control;
    }

    @Override
    public void beforePage()
    {
        isVisible = true;

        if (isDirty)
            refreshChart();
    }

    @Override
    public void afterPage()
    {
        isVisible = false;
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        // rebuild only the chart which is shown. A hidden chart is rebuilt
        // when the page is shown again (see #beforePage).
        if (isVisible)
            refreshChart();
        else
            isDirty = true;
    }

    private void refreshChart()
    {
        // chart is null while the control has not yet been created lazily
        if (chart != null)
            chart.refresh(null);

        isDirty = false;
    }
}
