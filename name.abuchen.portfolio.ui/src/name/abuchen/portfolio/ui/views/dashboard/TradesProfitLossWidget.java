package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.TaxesAndFees;
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
        CostMethod costMethod = getCostMethod();

        Money profitLoss = calculateProfitLoss(trades, costMethod);

        this.indicator.setText(
                        MessageFormat.format(profitLoss.isNegative() ? "<negative>{0}</negative>" : "<positive>{0}</positive>", //$NON-NLS-1$ //$NON-NLS-2$
                                        Values.Money.format(profitLoss, getClient().getBaseCurrency())));
    }

    /* package */ CostMethod getCostMethod()
    {
        return getDashboardData().getGlobalCostMethod();
    }

    /* package */ Money calculateProfitLoss(List<Trade> trades, CostMethod costMethod)
    {
        return trades.stream().map(trade -> trade.getProfitLoss(costMethod, TaxesAndFees.INCLUDED))
                        .collect(MoneyCollectors.sum(getDashboardData().getCurrencyConverter().getTermCurrency()));
    }
}
