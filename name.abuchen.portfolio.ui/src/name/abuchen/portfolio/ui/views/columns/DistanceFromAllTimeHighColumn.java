package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;

import name.abuchen.portfolio.math.AllTimeHigh;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.OptionLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ReportingPeriodColumnOptions;
import name.abuchen.portfolio.util.Interval;

public class DistanceFromAllTimeHighColumn extends Column
{
    private static final class QuoteReportingPeriodLabelProvider extends OptionLabelProvider<ReportingPeriod>
    {
        private BiFunction<Object, ReportingPeriod, Double> valueProvider;

        public QuoteReportingPeriodLabelProvider(BiFunction<Object, ReportingPeriod, Double> valueProvider)
        {
            this.valueProvider = valueProvider;
        }

        @Override
        public String getText(Object e, ReportingPeriod option)
        {
            Double value = valueProvider.apply(e, option);
            if (value == null)
                return null;

            return String.format("%,.2f %%", value * 100); //$NON-NLS-1$
        }
    }

    public DistanceFromAllTimeHighColumn(Supplier<LocalDate> dateProvider, List<ReportingPeriod> options)
    {
        super("distance-from-ath", Messages.ColumnQuoteDistanceFromAthPercent, SWT.RIGHT, 80); //$NON-NLS-1$

        BiFunction<Object, ReportingPeriod, Double> valueProvider = (element, option) -> {
            Interval interval = option.toInterval(dateProvider.get());

            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            return new AllTimeHigh(security, interval).getDistance();
        };

        this.setOptions(new ReportingPeriodColumnOptions(Messages.ColumnQuoteDistanceFromAthPercent_Option, options));
        this.setDescription(Messages.ColumnQuoteDistanceFromAthPercent_Description);
        this.setLabelProvider(new QuoteReportingPeriodLabelProvider(valueProvider));
        this.setVisible(false);
        this.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            ReportingPeriod option = (ReportingPeriod) ColumnViewerSorter.SortingContext.getColumnOption();

            Double v1 = valueProvider.apply(o1, option);
            Double v2 = valueProvider.apply(o2, option);

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
