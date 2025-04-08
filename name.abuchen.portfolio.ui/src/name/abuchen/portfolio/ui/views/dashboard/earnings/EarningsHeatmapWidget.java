package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.inject.Inject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.AbstractMonhtlyHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.HeatmapModel;
import name.abuchen.portfolio.ui.views.payments.PaymentsView;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewInput;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel;
import name.abuchen.portfolio.util.Interval;

public class EarningsHeatmapWidget extends AbstractMonhtlyHeatmapWidget
{
    @Inject
    private PortfolioPart part;

    public EarningsHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfigAfter(ClientFilterConfig.class, new EarningTypeConfig(this));
        addConfigAfter(EarningTypeConfig.class, new GrossNetTypeConfig(this));
    }

    @Override
    protected void linkActivated()
    {
        int startYear = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()).getStart()
                        .getYear();
        String clientFilterId = get(ClientFilterConfig.class).getSelectedItem().getId();

        EarningType earningsType = get(EarningTypeConfig.class).getValue();
        PaymentsViewModel.Mode mode = earningsType.getPaymentsViewModelMode();
        GrossNetType grossNetType = get(GrossNetTypeConfig.class).getValue();

        part.activateView(PaymentsView.class,
                        new PaymentsViewInput(0 /* monthly table */, startYear, Optional.of(clientFilterId), mode,
                                        grossNetType == GrossNetType.GROSS, false));
    }

    @Override
    protected void processTransactions(int startYear, Interval interval, HeatmapModel<Long> model,
                    Client filteredClient)
    {
        CurrencyConverter converter = getDashboardData().getCurrencyConverter();
        EarningType type = get(EarningTypeConfig.class).getValue();
        GrossNetType grossNet = get(GrossNetTypeConfig.class).getValue();

        filteredClient.getAccounts().stream() //
                        .flatMap(a -> a.getTransactions().stream()) //
                        .filter(type::isIncluded) //
                        .filter(t -> interval.contains(t.getDateTime())).forEach(t -> {
                            int row = t.getDateTime().getYear() - startYear;
                            int col = t.getDateTime().getMonth().getValue() - 1;

                            Long value = converter.convert(t.getDateTime(), grossNet.getValue(t)).getAmount();
                            if (t.getType().isDebit())
                                value = -value;

                            Long oldValue = model.getRow(row).getData(col);

                            model.getRow(row).setData(col, oldValue + value);
                        });
    }

}
