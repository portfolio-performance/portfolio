package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.math.AllTimeHigh;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.CacheKey;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ParameterizedOwnerDrawLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ReportingPeriodColumnOptions;
import name.abuchen.portfolio.util.Interval;

public class QuoteRangeColumn extends Column implements Column.CacheInvalidationListener
{
    private static final class QuoteReportingPeriodLabelProvider
                    extends ParameterizedOwnerDrawLabelProvider<ReportingPeriod>
    {
        private final BiFunction<Object, ReportingPeriod, AllTimeHigh> valueProvider;

        public QuoteReportingPeriodLabelProvider(BiFunction<Object, ReportingPeriod, AllTimeHigh> valueProvider)
        {
            this.valueProvider = valueProvider;
        }

        @Override
        public String getToolTipText(Object e)
        {
            var range = valueProvider.apply(e, getOption());
            if (range == null || range.getLow() == null || range.getHigh() == null)
                return null;
            Double value = range.getRelLowDistance();

            return String.format("%s -%.2f%% (%s)%n%s +%.2f%% (%s)", //$NON-NLS-1$
                            Values.Quote.format(range.getHigh()), //
                            (1 - value) * 100, //
                            Values.Date.format(range.getHighDate()), //
                            Values.Quote.format(range.getLow()), //
                            value * 100, //
                            Values.Date.format(range.getLowDate()));
        }

        @Override
        public void paint(Event event)
        {
            final int TICK_WIDTH = 3;
            final int BAR_HEIGHT = 16;

            ReportingPeriod option = getOption();
            var range = valueProvider.apply(event.item.getData(), option);
            if (range == null)
                return;
            Double value = range.getRelLowDistance();
            if (value == null)
                return;

            double pos = value;

            // Leave some space in case 2 such columns go side by side
            int width = getTableColumn().getWidth() - 2;
            int yOff = (event.height - BAR_HEIGHT) / 2;

            event.gc.setForeground(Colors.theme().defaultForeground());
            event.gc.setLineWidth(TICK_WIDTH);

            int tickX = (int) (width * pos);
            // When line is drawn, its coordinates are midpoint of its width,
            // but we don't want to see half a line width or something.
            if (tickX < TICK_WIDTH / 2)
                tickX = TICK_WIDTH / 2;
            else if (tickX > (width - 1 - (TICK_WIDTH + 1) / 2))
                tickX = width - 1 - (TICK_WIDTH + 1) / 2;

            event.gc.drawLine(event.x + tickX, event.y + yOff, event.x + tickX, event.y + yOff + BAR_HEIGHT - 1);
        }
    }

    private final Map<CacheKey, AllTimeHigh> cache = new HashMap<>();

    public QuoteRangeColumn(Supplier<LocalDate> dateProvider, List<ReportingPeriod> options)
    {
        super("range-widget", Messages.ColumnQuoteRangeWidget, SWT.RIGHT, 80); //$NON-NLS-1$

        BiFunction<Object, ReportingPeriod, AllTimeHigh> valueProvider = (element, option) -> {
            Interval interval = option.toInterval(dateProvider.get());

            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            return getOrCompute(security, interval);
        };

        this.setOptions(new ReportingPeriodColumnOptions(Messages.ColumnQuoteRangeWidget_Option, options));
        this.setDescription(Messages.ColumnQuoteRangeWidget_Description);
        this.setLabelProvider(() -> new QuoteReportingPeriodLabelProvider(valueProvider));
        this.setVisible(false);
        this.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            ReportingPeriod option = (ReportingPeriod) ColumnViewerSorter.SortingContext.getColumnOption();

            AllTimeHigh range1 = valueProvider.apply(o1, option);
            AllTimeHigh range2 = valueProvider.apply(o2, option);
            Double v1 = range1 != null ? range1.getRelLowDistance() : null;
            Double v2 = range2 != null ? range2.getRelLowDistance() : null;

            if (v1 == null && v2 == null)
                return 0;
            else if (v1 == null)
                return -1;
            else if (v2 == null)
                return 1;

            return Double.compare(v1.doubleValue(), v2.doubleValue());
        }));
    }

    private AllTimeHigh getOrCompute(Security security, Interval interval)
    {
        var cacheKey = new CacheKey(security, interval);
        return cache.computeIfAbsent(cacheKey, key -> new AllTimeHigh(security, interval));
    }

    /**
     * Clears the AllTimeHigh computation cache. This is called when
     * ShowHideColumnHelper is called to clear caches. Keep in mind: this is
     * called per Column object, not per visible column in the table.
     */
    @Override
    public void invalidateCache()
    {
        cache.clear();
    }
}
