package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;
import name.abuchen.portfolio.util.TextUtil;

public class TradesProfitLossWidget extends AbstractTradesWidget
{
    public TradesProfitLossWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);
    }

    @Override
    public void update(TradeDetailsView.Input input)
    {
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));

        List<Trade> trades = input.getTrades();

        Money profitLoss = trades.stream().map(Trade::getProfitLoss)
                        .collect(MoneyCollectors.sum(getDashboardData().getCurrencyConverter().getTermCurrency()));

        this.indicator.setText(
                        MessageFormat.format(profitLoss.isNegative() ? "<red>{0}</red>" : "<green>{0}</green>", //$NON-NLS-1$ //$NON-NLS-2$
                                        Values.Money.format(profitLoss, getClient().getBaseCurrency())));
    }
}
