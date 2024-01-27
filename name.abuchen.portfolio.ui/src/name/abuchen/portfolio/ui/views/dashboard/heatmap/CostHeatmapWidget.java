package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.payments.PaymentsView;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewInput;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel;
import name.abuchen.portfolio.util.Interval;

public class CostHeatmapWidget extends AbstractMonhtlyHeatmapWidget
{
    private final PaymentsViewModel.Mode mode;

    private final Unit.Type unitType;

    /**
     * Base transactions are to be included fully into the calculation (for
     * example fee and fee_refund) as opposed to other transactions where only
     * the unit sum is included.
     */
    private final Set<AccountTransaction.Type> baseTransactionTypes;

    @Inject
    private PortfolioPart part;

    public CostHeatmapWidget(Widget widget, DashboardData data, PaymentsViewModel.Mode mode)
    {
        super(widget, data);
        this.mode = mode;

        switch (mode)
        {
            case TAXES:
                unitType = Unit.Type.TAX;
                baseTransactionTypes = Set.of(AccountTransaction.Type.TAXES, AccountTransaction.Type.TAX_REFUND);
                break;
            case FEES:
                unitType = Unit.Type.FEE;
                baseTransactionTypes = Set.of(AccountTransaction.Type.FEES, AccountTransaction.Type.FEES_REFUND);
                break;
            default:
                throw new IllegalArgumentException(mode.toString());
        }
    }

    @Override
    protected void linkActivated()
    {
        // there is no direct equivalent between the widget and the payments
        // view because the former uses "reporting periods" and the latter uses
        // "start year" (full year until today)

        // correct for the case that the start day is at the last day of the
        // year because the interval is half-open and excludes the first day

        var startDate = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()).getStart();
        int startYear = startDate.getMonth() == Month.DECEMBER && startDate.getDayOfMonth() == 31
                        ? startDate.getYear() + 1
                        : startDate.getYear();

        String clientFilterId = get(ClientFilterConfig.class).getSelectedItem().getId();

        part.activateView(PaymentsView.class, new PaymentsViewInput(
                        0 /* monthly table */, startYear, Optional.of(clientFilterId), mode, true, false));
    }

    @Override
    protected void processTransactions(int startYear, Interval calcInterval, HeatmapModel<Long> model,
                    Client filteredClient)
    {
        CurrencyConverter converter = getDashboardData().getCurrencyConverter();

        filteredClient.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> calcInterval.contains(t.getDateTime())) //
                        .forEach(t -> {
                            int row = t.getDateTime().getYear() - startYear;
                            int col = t.getDateTime().getMonth().getValue() - 1;
                            long value = t.getUnitSum(unitType).with(converter.at(t.getDateTime())).getAmount();

                            Long oldValue = model.getRow(row).getData(col);
                            model.getRow(row).setData(col, oldValue + value);
                        });

        filteredClient.getAccounts().stream() //
                        .flatMap(a -> a.getTransactions().stream()) //
                        .filter(mode::isAccountTxIncluded) //
                        .filter(t -> calcInterval.contains(t.getDateTime())) //
                        .forEach(t -> {
                            int row = t.getDateTime().getYear() - startYear;
                            int col = t.getDateTime().getMonth().getValue() - 1;

                            long value = 0;
                            if (baseTransactionTypes.contains(t.getType()))
                            {
                                value = t.getMonetaryAmount().with(converter.at(t.getDateTime())).getAmount();
                                if (t.getType().isCredit())
                                    value *= -1;
                            }
                            else
                            {
                                value = t.getUnitSum(unitType).with(converter.at(t.getDateTime())).getAmount();
                            }

                            Long oldValue = model.getRow(row).getData(col);
                            model.getRow(row).setData(col, oldValue + value);
                        });
    }
}
