package name.abuchen.portfolio.ui.views.panes;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
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
import name.abuchen.portfolio.ui.views.IPieChart;
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

    @Override
    public String getLabel()
    {
        return Messages.ClientEditorLabelHoldings;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());
        chart = new HoldingsPieChartSWT();
        chart.createControl(container);
        return container;
    }

    @Override
    public void setInput(Object input)
    {
        this.portfolio = Adaptor.adapt(Portfolio.class, input);
        CurrencyConverter converter = new CurrencyConverterImpl(factory,
                        client.getBaseCurrency());
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
}
