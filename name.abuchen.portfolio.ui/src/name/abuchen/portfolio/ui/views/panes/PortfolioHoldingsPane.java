package name.abuchen.portfolio.ui.views.panes;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.ui.views.holdings.HoldingsPieChartBrowser;
import name.abuchen.portfolio.ui.views.holdings.HoldingsPieChartSWT;

public class PortfolioHoldingsPane implements InformationPanePage
{
    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    private Portfolio portfolio;
    private IPieChart chart;

    @Inject
    @Preference(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS)
    boolean useSWTCharts;

    @Override
    public String getLabel()
    {
        return Messages.TabPortfolioHoldingsChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        if (this.useSWTCharts)
        {
            chart = new HoldingsPieChartSWT(this);
        }
        else
        {
            chart = new HoldingsPieChartBrowser(make(EmbeddedBrowser.class), this);
        }
        return chart.createControl(parent);
    }

    @Override
    public void setInput(Object input)
    {
        this.portfolio = Adaptor.adapt(Portfolio.class, input);
        CurrencyConverter converter = new CurrencyConverterImpl(factory,
                        portfolio.getReferenceAccount().getCurrencyCode());
        ClientFilter clientFilter = new PortfolioClientFilter(portfolio);
        ClientSnapshot snapshot = ClientSnapshot.create(clientFilter.filter(client), converter, LocalDate.now());
     
        chart.refresh(snapshot);
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (portfolio != null)
            setInput(portfolio);
    }

    @Inject
    private IEclipseContext context;

    public final <T> T make(Class<T> type, Object... parameters)
    {
        if (parameters == null || parameters.length == 0)
            return ContextInjectionFactory.make(type, this.context);

        IEclipseContext c2 = EclipseContextFactory.create();
        for (Object param : parameters)
            c2.set(param.getClass().getName(), param);
        return ContextInjectionFactory.make(type, this.context, c2);
    }
}
