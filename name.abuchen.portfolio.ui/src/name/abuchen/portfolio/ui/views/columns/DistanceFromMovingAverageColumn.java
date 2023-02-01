package name.abuchen.portfolio.ui.views.columns;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.OptionLabelProvider;
import name.abuchen.portfolio.ui.views.SimpleMovingAverage;

public class DistanceFromMovingAverageColumn extends Column
{
    private static final class SmaPeriodColumnOption implements Column.Options<Integer>
    {
        private final List<Integer> options;
        private final String columnLabel;

        public SmaPeriodColumnOption(String columnLabel, List<Integer> smaIntervals)
        {
            this.options = smaIntervals;
            this.columnLabel = columnLabel;
        }

        @Override
        public List<Integer> getOptions()
        {
            return options;
        }

        @Override
        public Integer valueOf(String s)
        {
            return Integer.parseInt(s);
        }

        @Override
        public String toString(Integer option)
        {
            return option.toString();
        }

        @Override
        public String getColumnLabel(Integer option)
        {
            return MessageFormat.format(columnLabel, option);
        }

        @Override
        public String getMenuLabel(Integer option)
        {
            return MessageFormat.format(Messages.LabelXDays, option);
        }

        @Override
        public String getDescription(Integer option)
        {
            return null;
        }

        @Override
        public boolean canCreateNewOptions()
        {
            return false;
        }

        @Override
        public Integer createNewOption(Shell shell)
        {
            return null;
        }
    }

    private static final class SmaPeriodColumnLabelProvider extends OptionLabelProvider<Integer>
    {
        private BiFunction<Object, Integer, Double> valueProvider;

        public SmaPeriodColumnLabelProvider(BiFunction<Object, Integer, Double> valueProvider)
        {
            this.valueProvider = valueProvider;
        }

        @Override
        public String getText(Object e, Integer option)
        {
            Double value = valueProvider.apply(e, option);
            if (value == null)
                return null;

            return String.format("%,.2f %%", value * 100); //$NON-NLS-1$
        }

        @Override
        public Color getForeground(Object e, Integer option)
        {
            Double value = valueProvider.apply(e, option);
            if (value == null)
                return null;

            if (value.doubleValue() < 0)
                return Colors.theme().redForeground();
            else if (value.doubleValue() > 0)
                return Colors.theme().greenForeground();
            else
                return null;
        }

        @Override
        public Image getImage(Object element, Integer option)
        {
            Double value = valueProvider.apply(element, option);
            if (value == null)
                return null;

            if (value.doubleValue() > 0)
                return Images.GREEN_ARROW.image();
            if (value.doubleValue() < 0)
                return Images.RED_ARROW.image();
            return null;
        }
    }

    public DistanceFromMovingAverageColumn(Supplier<LocalDate> dateProvider)
    {
        super("distance-from-sma", Messages.ColumnDistanceFromMovingAverage, SWT.RIGHT, 85); //$NON-NLS-1$

        List<Integer> smaIntervals = Arrays.asList(5, 20, 30, 38, 50, 90, 100, 200);
        BiFunction<Object, Integer, Double> valueProvider = (element, option) -> {

            Security s = Adaptor.adapt(Security.class, element);
            if (s == null)
                return null;

            List<SecurityPrice> prices = s.getLatestNPricesOfDate(dateProvider.get(), option);
            if (prices.size() < option || prices.isEmpty())
                return null;

            Double sma = SimpleMovingAverage.calculateSma(prices);

            return prices.get(prices.size() - 1).getValue() / Values.Quote.divider() / sma - 1;
        };

        setOptions(new SmaPeriodColumnOption(Messages.ColumnDistanceFromMovingAverage_Option, smaIntervals));
        setDescription(Messages.ColumnDistanceFromMovingAverage_Description);
        setLabelProvider(new SmaPeriodColumnLabelProvider(valueProvider));
        setVisible(false);

        ColumnViewerSorter sorter = ColumnViewerSorter.create((o1, o2) -> {
            Integer option = (Integer) ColumnViewerSorter.SortingContext.getColumnOption();

            Double v1 = valueProvider.apply(o1, option);
            Double v2 = valueProvider.apply(o2, option);

            if (v1 == null && v2 == null)
                return 0;
            else if (v1 == null)
                return -1;
            else if (v2 == null)
                return 1;

            return Double.compare(v1.doubleValue(), v2.doubleValue());
        });
        setSorter(sorter);
    }
}
