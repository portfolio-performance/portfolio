package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.TextUtil;

public class PerformanceCalculationWidget extends WidgetDelegate<ClientPerformanceSnapshot>
{
    enum TableLayout
    {
        FULL(Messages.LabelLayoutFull), REDUCED(Messages.LabelLayoutReduced), RELEVANT(Messages.LabelLayoutRelevant);

        private String label;

        private TableLayout(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class LayoutConfig extends EnumBasedConfig<TableLayout>
    {
        public LayoutConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelLayout, TableLayout.class, Dashboard.Config.LAYOUT, Policy.EXACTLY_ONE);
        }
    }

    private Composite container;

    private Label title;
    private Label[] signs;
    private Label[] labels;
    private Label[] values;

    public PerformanceCalculationWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
        addConfig(new LayoutConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).spacing(3, 3).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(title);

        TableLayout layout = get(LayoutConfig.class).getValue();

        createControls(layout);

        return container;
    }

    private void createControls(TableLayout layout)
    {
        // used to detect if we need to re-create the full table
        title.setData(layout);

        int count = ClientPerformanceSnapshot.CategoryType.values().length;

        switch (layout)
        {
            case FULL:
                createTable(count);
                break;
            case REDUCED:
                createTable(5); // show first 2 and last 2 items
                break;
            case RELEVANT:
                createTable(8);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void createTable(int size)
    {
        labels = new Label[size];
        signs = new Label[size];
        values = new Label[size];

        for (int ii = 0; ii < size; ii++)
        {
            signs[ii] = new Label(container, SWT.NONE);
            signs[ii].setBackground(container.getBackground());
            labels[ii] = new Label(container, SWT.NONE);
            labels[ii].setBackground(container.getBackground());
            values[ii] = new Label(container, SWT.RIGHT);
            values[ii].setBackground(container.getBackground());
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(values[ii]);

            if (ii == 0 || ii == size - 1) // first and last line
            {
                signs[ii].setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                labels[ii].setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                values[ii].setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            }

            if (ii == 0 || ii == size - 2) // after first and before last line
            {
                Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
                GridDataFactory.fillDefaults().span(3, 1).grab(true, false).align(SWT.FILL, SWT.BEGINNING)
                                .applyTo(separator);
            }
        }
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<ClientPerformanceSnapshot> getUpdateTask()
    {
        return () -> {
            PerformanceIndex index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
            return index.getClientPerformanceSnapshot().orElseThrow(IllegalArgumentException::new);
        };
    }

    @Override
    public void update(ClientPerformanceSnapshot snapshot)
    {
        TableLayout layout = get(LayoutConfig.class).getValue();

        if (!layout.equals(title.getData()))
        {
            for (Control child : container.getChildren())
                if (!title.equals(child))
                    child.dispose();

            createControls(layout);

            container.getParent().layout(true);
            container.getParent().getParent().layout(true);
        }

        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        switch (layout)
        {
            case FULL:
                filInValues(0, snapshot.getCategories());
                break;
            case REDUCED:
                fillInReducedValues(snapshot);
                break;
            case RELEVANT:
                fillInOnlyRelevantValues(snapshot);
                break;
            default:
                throw new IllegalArgumentException();
        }

        container.layout();
    }

    private void fillInOnlyRelevantValues(final ClientPerformanceSnapshot snapshot)
    {

        List<ClientPerformanceSnapshot.Category> categories = snapshot.getCategories();

        // header
        LocalDate startDate = snapshot.getStartClientSnapshot().getTime();
        String header = MessageFormat.format(Messages.PerformanceRelevantTransactionsHeader,
                        Values.Date.format(startDate));
        labels[0].setText(header);

        for (int i = 1; i < 7; i++)
        {
            ClientPerformanceSnapshot.Category category = categories.get(i);
            signs[i].setText(category.getSign());
            labels[i].setText(category.getLabel());
            values[i].setText(Values.Money.format(category.getValuation(), getClient().getBaseCurrency()));
        }

        // footer
        signs[7].setText("="); //$NON-NLS-1$

        LocalDate endDate = snapshot.getEndClientSnapshot().getTime();
        String footer = MessageFormat.format(Messages.PerformanceRelevantTransactionsFooter,
                        Values.Date.format(endDate));
        labels[7].setText(footer);

        Money totalRelevantTransactions = sumCategoryValuations(
                        snapshot.getValue(CategoryType.INITIAL_VALUE).getCurrencyCode(), categories.subList(1, 6));
        values[7].setText(Values.Money.format(totalRelevantTransactions, getClient().getBaseCurrency()));
    }

    /**
     * @brief Sums up the total currency for the supplied categories
     * @param currencyCode
     *            The currency code of the currency that is summed up
     * @param categories
     *            The categories for which the total value is computed
     * @return
     */
    private Money sumCategoryValuations(String currencyCode, List<ClientPerformanceSnapshot.Category> categories)
    {
        MutableMoney totalMoney = MutableMoney.of(currencyCode);

        for (ClientPerformanceSnapshot.Category category : categories)
        {
            switch (category.getSign())
            {
                case "+": //$NON-NLS-1$
                    totalMoney.add(category.getValuation());
                    break;
                case "-": //$NON-NLS-1$
                    totalMoney.subtract(category.getValuation());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        return totalMoney.toMoney();
    }

    private void filInValues(int startIndex, List<ClientPerformanceSnapshot.Category> categories)
    {
        int ii = startIndex;
        for (ClientPerformanceSnapshot.Category category : categories)
        {
            signs[ii].setText(category.getSign());
            labels[ii].setText(category.getLabel());
            values[ii].setText(Values.Money.format(category.getValuation(), getClient().getBaseCurrency()));

            if (++ii >= labels.length)
                break;
        }
    }

    private void fillInReducedValues(ClientPerformanceSnapshot snapshot)
    {
        List<ClientPerformanceSnapshot.Category> categories = snapshot.getCategories();

        int count = ClientPerformanceSnapshot.CategoryType.values().length;
        int showFirstXItems = 3;
        int showLastXItems = 2;

        Money misc = sumCategoryValuations(snapshot.getValue(CategoryType.INITIAL_VALUE).getCurrencyCode(),
                        categories.subList(showFirstXItems, count - showLastXItems));

        filInValues(0, categories.subList(0, showFirstXItems));

        signs[showFirstXItems].setText("+"); //$NON-NLS-1$
        labels[showFirstXItems].setText(Messages.LabelCategoryOtherMovements);
        values[showFirstXItems].setText(Values.Money.format(misc, getClient().getBaseCurrency()));

        filInValues(showFirstXItems + 1, categories.subList(categories.size() - showLastXItems, categories.size()));
    }
}
