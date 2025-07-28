package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;
import name.abuchen.portfolio.util.TextUtil;

public class TradesWidget extends AbstractTradesWidget
{
    public class UseSecurityCurrencyConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private boolean useSecurityCurrency = false;

        public UseSecurityCurrencyConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            var code = delegate.getWidget().getConfiguration().get(Dashboard.Config.FLAG_USE_SECURITY_CURRENCY.name());
            this.useSecurityCurrency = code != null && Boolean.parseBoolean(code);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            SimpleAction action = new SimpleAction(Messages.LabelUseSecurityCurrency, a -> {
                this.useSecurityCurrency = !this.useSecurityCurrency;

                delegate.getWidget().getConfiguration().put(Dashboard.Config.FLAG_USE_SECURITY_CURRENCY.name(),
                                String.valueOf(this.useSecurityCurrency));

                delegate.update();
                delegate.getClient().touch();
            });

            action.setChecked(this.useSecurityCurrency);
            manager.add(action);
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelUseSecurityCurrency,
                            this.useSecurityCurrency ? Messages.LabelYes : Messages.LabelNo);
        }

        public boolean useSecurityCurrency()
        {
            return useSecurityCurrency;
        }
    }

    public TradesWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new UseSecurityCurrencyConfig(this));
    }

    @Override
    protected boolean useSecurityCurrency()
    {
        return get(UseSecurityCurrencyConfig.class).useSecurityCurrency();
    }

    @Override
    public void update(TradeDetailsView.Input input)
    {
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));

        List<Trade> trades = input.getTrades();
        long positive = trades.stream().filter(t -> t.getProfitLoss().isPositive()).count();
        String text = MessageFormat.format("{0} <green>↑{1}</green> <red>↓{2}</red>", //$NON-NLS-1$
                        trades.size(), positive, trades.size() - positive);

        this.indicator.setText(text);
    }
}
