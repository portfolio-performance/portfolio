package name.abuchen.portfolio.ui.views.panes;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.views.PortfolioBalanceChart;

public class PortfolioBalancePane implements InformationPanePage
{

    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private IStylingEngine stylingEngine;

    @Inject
    private ExchangeRateProviderFactory factory;

    private Portfolio portfolio;
    private PortfolioBalanceChart chart;

    @Inject
    @Optional
    public void onDiscreedModeChanged(@UIEventTopic(UIConstants.Event.Global.DISCREET_MODE) Object obj)
    {
        if (chart != null)
            chart.redraw();
    }

    @Override
    public String getLabel()
    {
        return Messages.ClientEditorLabelChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        chart = new PortfolioBalanceChart(container, client);
        stylingEngine.style(chart.getControl());
        stylingEngine.style(chart);

        return container;
    }

    @Override
    public void setInput(Object input)
    {
        portfolio = Adaptor.adapt(Portfolio.class, input);
        chart.updateChart(portfolio, factory);
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (portfolio != null)
            setInput(portfolio);
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        chart.addButtons(toolBar);
    }
}
