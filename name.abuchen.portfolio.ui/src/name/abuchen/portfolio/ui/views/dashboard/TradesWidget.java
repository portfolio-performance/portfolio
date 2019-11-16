package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;
import name.abuchen.portfolio.util.TextUtil;

public class TradesWidget extends AbstractTradesWidget
{
    public TradesWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);
    }

    @Override
    public void update(TradeDetailsView.Input input)
    {
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));

        List<Trade> trades = input.getTrades();
        long positive = trades.stream().filter(t -> t.getIRR() > 0).count();
        String text = MessageFormat.format("{0} <green>↑{1}</green> <red>↓{2}</red>", //$NON-NLS-1$
                        trades.size(), positive, trades.size() - positive);

        this.indicator.setText(text);
    }
}
