package name.abuchen.portfolio.ui.views.panes;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.views.SecuritiesChart;

public class SecurityPriceChartPane implements InformationPanePage
{
    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    private IStylingEngine stylingEngine;

    private Security security;
    private SecuritiesChart chart;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        chart = new SecuritiesChart(parent, client, new CurrencyConverterImpl(factory, client.getBaseCurrency()));

        stylingEngine.style(chart.getControl());

        return chart.getControl();
    }

    @Override
    public void setInput(Object input)
    {
        security = Adaptor.adapt(Security.class, input);
        chart.updateChart(security);
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (security != null)
            setInput(security);
    }
}
