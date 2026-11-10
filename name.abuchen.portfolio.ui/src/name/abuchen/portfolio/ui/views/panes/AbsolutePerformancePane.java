package name.abuchen.portfolio.ui.views.panes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

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
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.views.AbsolutePerformancePieChart;
import name.abuchen.portfolio.util.Interval;

public class AbsolutePerformancePane implements InformationPanePage
{
    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    private Security security;
    private Portfolio portfolio;
    private Account account;
    private Classification classification;
    private Object input;
    private Supplier<LocalDate> snapshotDateSupplier = LocalDate::now;

    private AbsolutePerformancePieChart chart;


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

        chart = new AbsolutePerformancePieChart();
        chart.createControl(container);
        return container;
    }

    @Override
    public void setInput(Object input)
    {
        this.input = input;
        this.security = Adaptor.adapt(Security.class, input);
        this.portfolio = Adaptor.adapt(Portfolio.class, input);
        this.account = Adaptor.adapt(Account.class, input);
        this.classification = Adaptor.adapt(Classification.class, input);

        CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        ClientFilter clientFilter = null;
        String label = Messages.LabelTotalSum;

        if (security != null)
        {
            clientFilter = new ClientSecurityFilter(security);
            label = security.getName();
        }

        if (portfolio != null)
        {
            clientFilter = new PortfolioClientFilter(portfolio);
            label = portfolio.getName();
        }

        if (account != null)
        {
            clientFilter = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(account));
            label = account.getName();
        }

        if (classification != null)
        {
            clientFilter = new ClientClassificationFilter(classification);
            label = classification.getName();
        }

        Client filteredClient = clientFilter != null ? clientFilter.filter(client) : client;
        var period = Interval.of(LocalDate.ofYearDay(1900, 1), getSnapshotDate());
        var snapshot = new ClientPerformanceSnapshot(filteredClient, converter, period);
        chart.refresh(snapshot, label);
    }

    @Override
    public void onRecalculationNeeded()
    {
        setInput(input);
    }

    public void setSnapshotDateSupplier(Supplier<LocalDate> supplier)
    {
        this.snapshotDateSupplier = supplier;
    }

    LocalDate getSnapshotDate()
    {
        return snapshotDateSupplier.get();
    }
}
