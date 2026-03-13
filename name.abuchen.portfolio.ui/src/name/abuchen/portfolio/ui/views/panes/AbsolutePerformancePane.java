package name.abuchen.portfolio.ui.views.panes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.views.AbsolutePerformancePieChart;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;

public class AbsolutePerformancePane implements InformationPanePage
{
    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private AbstractFinanceView view;

    private Security security;
    private Portfolio portfolio;
    private Account account;
    private Classification classification;

    private IPieChart chart;


    @Override
    public String getLabel()
    {
        return Messages.LabelAbsolutePerformanceChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        var cache = view.make(DataSeriesCache.class);

        chart = new AbsolutePerformancePieChart(client, preferences, cache);
        chart.createControl(container);
        return container;
    }

    @Override
    public void setInput(Object input)
    {
        this.security = Adaptor.adapt(Security.class, input);
        this.portfolio = Adaptor.adapt(Portfolio.class, input);
        this.account = Adaptor.adapt(Account.class, input);
        this.classification = Adaptor.adapt(Classification.class, input);

        CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        ClientFilter clientFilter = null;

        if (security != null)
        {
            clientFilter = new ClientSecurityFilter(security);
        }

        if (portfolio != null)
        {
            clientFilter = new PortfolioClientFilter(portfolio);
        }

        if (account != null)
        {
            clientFilter = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(account));
        }

        if (classification != null)
        {
            clientFilter = new ClientClassificationFilter(classification);
        }

        ClientSnapshot snapshot = null;
        if (clientFilter != null)
        {
            snapshot = ClientSnapshot.create(clientFilter.filter(client), converter, LocalDate.now());
        }
        else
        {
            snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        }

        chart.refresh(snapshot);
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (security != null)
            setInput(security);
        if (portfolio != null)
            setInput(portfolio);
        if (account != null)
            setInput(account);
        if (classification != null)
            setInput(classification);
    }
}
