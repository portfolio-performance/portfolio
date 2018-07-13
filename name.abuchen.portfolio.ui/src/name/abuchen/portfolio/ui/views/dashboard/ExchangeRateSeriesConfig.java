package name.abuchen.portfolio.ui.views.dashboard;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ExchangeRateSeriesConfig implements WidgetConfig
{
    private WidgetDelegate<?> delegate;
    private List<ExchangeRateTimeSeries> available;
    private ExchangeRateTimeSeries series;

    public ExchangeRateSeriesConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;

        ExchangeRateProviderFactory factory = delegate.getDashboardData().getExchangeRateProviderFactory();
        this.available = factory.getAvailableTimeSeries();
        Collections.sort(this.available, (Comparator<ExchangeRateTimeSeries>) (a, b) -> {
            int c = a.getBaseCurrency().compareTo(b.getBaseCurrency());
            if (c != 0)
                return c;
            return a.getTermCurrency().compareTo(b.getTermCurrency());
        });

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.EXCHANGE_RATE_SERIES.name());
        int slash = code != null ? code.indexOf('/') : -1;
        String base = slash > 0 ? code.substring(0, slash) : null; // NOSONAR
        String term = slash > 0 ? code.substring(slash + 1) : null; // NOSONAR

        this.series = available.stream().filter(t -> t.getBaseCurrency().equals(base))
                        .filter(t -> t.getTermCurrency().equals(term)).findAny()
                        .orElse(factory.getTimeSeries(CurrencyUnit.EUR, CurrencyUnit.USD));
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        MenuManager subMenu = new MenuManager(Messages.LabelExchangeRate);

        available.stream().forEach(ts -> {
            SimpleAction action = new SimpleAction(ts.getLabel(), a -> {
                series = ts;
                String code = ts.getBaseCurrency() + '/' + ts.getTermCurrency();
                delegate.getWidget().getConfiguration().put(Dashboard.Config.EXCHANGE_RATE_SERIES.name(), code);

                delegate.update();
                delegate.markDirty();
            });
            action.setChecked(series.equals(ts));
            subMenu.add(action);
        });

        manager.add(subMenu);
    }

    @Override
    public String getLabel()
    {
        Optional<ExchangeRateProvider> provider = series.getProvider();
        return provider.isPresent() ? series.getLabel() + ' ' + provider.get().getName() : series.getLabel();
    }

    public ExchangeRateTimeSeries getSeries()
    {
        return series;
    }
}
