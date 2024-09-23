package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;

import name.abuchen.portfolio.math.AllTimeHigh;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ParameterizedColumnLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ReportingPeriodColumnOptions;
import name.abuchen.portfolio.util.Interval;

public class DistanceFromAllTimeHighColumn extends Column
{
    private static final class QuoteReportingPeriodLabelProvider extends ParameterizedColumnLabelProvider
    {
        private BiFunction<Object, ReportingPeriod, AllTimeHigh> valueProvider;

        public QuoteReportingPeriodLabelProvider(BiFunction<Object, ReportingPeriod, AllTimeHigh> valueProvider)
        {
            this.valueProvider = valueProvider;
        }

        @Override
        public String getText(Object e)
        {
            var ath = valueProvider.apply(e, (ReportingPeriod) getOption());
            if (ath == null)
                return null;
            Double value = ath.getDistance();
            if (value == null)
                return null;

            return String.format("%,.2f %%", value * 100); //$NON-NLS-1$
        }

        @Override
        public String getToolTipText(Object e)
        {
            var ath = valueProvider.apply(e, (ReportingPeriod) getOption());
            if (ath == null || ath.getValue() == null)
                return null;

            return String.format("ATH: %s (%s)", //$NON-NLS-1$
                            Values.Quote.format(ath.getValue()), Values.Date.format(ath.getDate()));
        }
    }

    public DistanceFromAllTimeHighColumn(Supplier<LocalDate> dateProvider, List<ReportingPeriod> options)
    {
        super("distance-from-ath", Messages.ColumnQuoteDistanceFromAthPercent, SWT.RIGHT, 80); //$NON-NLS-1$

        BiFunction<Object, ReportingPeriod, AllTimeHigh> valueProvider = (element, option) -> {
            Interval interval = option.toInterval(dateProvider.get());

            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            return new AllTimeHigh(security, interval);
        };

        this.setOptions(new ReportingPeriodColumnOptions(Messages.ColumnQuoteDistanceFromAthPercent_Option, options));
        this.setDescription(Messages.ColumnQuoteDistanceFromAthPercent_Description);
        this.setLabelProvider(() -> new QuoteReportingPeriodLabelProvider(valueProvider));
        this.setVisible(false);
        this.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            ReportingPeriod option = (ReportingPeriod) ColumnViewerSorter.SortingContext.getColumnOption();

            var ath1 = valueProvider.apply(o1, option);
            Double v1 = ath1 == null ? null : ath1.getDistance();
            var ath2 = valueProvider.apply(o2, option);
            Double v2 = ath2 == null ? null : ath2.getDistance();

            if (v1 == null && v2 == null)
                return 0;
            else if (v1 == null)
                return -1;
            else if (v2 == null)
                return 1;

            return Double.compare(v1.doubleValue(), v2.doubleValue());
        }));
    }
}
