package name.abuchen.portfolio.ui.views.dashboard;

import jakarta.inject.Inject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.views.AllTransactionsView;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.AbstractMonthlyHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.HeatmapModel;
import name.abuchen.portfolio.util.Interval;

public class MonthlyPNTransfersWidget extends AbstractMonthlyHeatmapWidget
{
    @Inject
    private PortfolioPart part;

    public MonthlyPNTransfersWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
    }

    @Override
    protected void linkActivated()
    {
        part.activateView(AllTransactionsView.class, null);
    }

    @Override
    protected void processTransactions(int startYear, Interval interval, HeatmapModel<Long> model,
                    Client filteredClient)
    {
        CurrencyConverter converter = getDashboardData().getCurrencyConverter();

        // Cash account transactions
        filteredClient.getAccounts().stream() //
                        .flatMap(account -> account.getTransactions().stream())
                        .filter(t -> interval.contains(t.getDateTime())) //
                        .forEach(t -> {
                            var row = t.getDateTime().getYear() - startYear;
                            var col = t.getDateTime().getMonth().getValue() - 1;
                            var value = 0L;
                            switch (t.getType())
                            {
                                case DEPOSIT:
                                    value = t.getMonetaryAmount().with(converter.at(t.getDateTime())).getAmount();
                                    break;
                                case REMOVAL:
                                    value = -t.getMonetaryAmount().with(converter.at(t.getDateTime())).getAmount();
                                    break;
                                default:
                                    return;
                            }

                            var oldValue = model.getRow(row).getData(col);
                            model.getRow(row).setData(col, oldValue + value);
                        });

        // Securities account transactions
        filteredClient.getPortfolios().stream() //
                        .flatMap(portfolio -> portfolio.getTransactions().stream())
                        .filter(t -> interval.contains(t.getDateTime())) //
                        .forEach(t -> {
                            var row = t.getDateTime().getYear() - startYear;
                            var col = t.getDateTime().getMonth().getValue() - 1;
                            var value = 0L;
                            switch (t.getType())
                            {
                                case DELIVERY_INBOUND:
                                    value = t.getMonetaryAmount().with(converter.at(t.getDateTime())).getAmount();
                                    break;
                                case DELIVERY_OUTBOUND:
                                    value = -t.getMonetaryAmount().with(converter.at(t.getDateTime())).getAmount();
                                    break;
                                default:
                                    return;
                            }

                            var oldValue = model.getRow(row).getData(col);
                            model.getRow(row).setData(col, oldValue + value);
                        });
    }
}
