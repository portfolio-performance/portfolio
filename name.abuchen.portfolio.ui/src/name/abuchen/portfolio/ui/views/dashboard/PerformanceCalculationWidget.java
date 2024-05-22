package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Category;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Position;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.LogoManager;
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

    static class PositionToolTip extends ToolTip
    {
        private static final int MAX_NO_OF_ROWS = 10;

        private Client client;
        private Label label;

        public PositionToolTip(Client client, Label label)
        {
            super(label);
            this.client = client;
            this.label = label;
        }

        @Override
        protected Composite createToolTipContentArea(Event event, Composite parent)
        {
            Category category = (Category) label.getData();
            if (category == null)
                return null;

            List<Position> positions = new ArrayList<>(category.getPositions());
            Collections.sort(positions, (r, l) -> l.getValue().compareTo(r.getValue()));

            boolean omitRows = positions.size() > MAX_NO_OF_ROWS + 1;

            Composite table = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(table);

            if (omitRows)
            {
                addRows(table, positions.subList(0, MAX_NO_OF_ROWS / 2));

                Label l = new Label(table, SWT.NONE);
                l.setText("..."); //$NON-NLS-1$
                GridDataFactory.fillDefaults().span(2, 1).applyTo(l);

                Label v = new Label(table, SWT.RIGHT);
                v.setText("..."); //$NON-NLS-1$
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(v);

                addRows(table, positions.subList(positions.size() - (MAX_NO_OF_ROWS / 2), positions.size()));
            }
            else
            {
                addRows(table, positions);
            }

            return table;
        }

        private void addRows(Composite table, List<Position> positions)
        {
            for (Position position : positions)
            {
                Image image = getImage(position);
                if (image != null)
                {
                    Label i = new Label(table, SWT.NONE);
                    i.setImage(image);
                }

                Label l = new Label(table, SWT.NONE);
                l.setText(TextUtil.tooltip(position.getLabel()));
                if (image == null)
                    GridDataFactory.fillDefaults().span(2, 1).applyTo(l);

                Label v = new Label(table, SWT.RIGHT);
                v.setText(Values.Money.format(position.getValue()));
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(v);
            }
        }

        private Image getImage(Position position)
        {
            if (position.getSecurity() == null)
                return null;

            ClientPerformanceSnapshot snapshot = (ClientPerformanceSnapshot) label
                            .getData(ClientPerformanceSnapshot.class.getSimpleName());

            if (snapshot == null)
                return null;

            Security security = position.getSecurity();

            boolean hasHoldings = snapshot.getEndClientSnapshot().getPositionsByVehicle().get(security) != null;

            return LogoManager.instance().getDefaultColumnImage(security, client.getSettings(), !hasHoldings);
        }

        @Override
        protected boolean shouldCreateToolTip(Event event)
        {
            if (!super.shouldCreateToolTip(event))
                return false;

            Category category = (Category) label.getData();
            return category != null && !category.getPositions().isEmpty();
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
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
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
                createTable(6); // show first 3 + other + last 2 items
                break;
            case RELEVANT:
                createTable(8);
                break;
            default:
                throw new IllegalArgumentException("unsupported layout " + layout); //$NON-NLS-1$
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
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(labels[ii]);
            values[ii] = new Label(container, SWT.RIGHT);
            values[ii].setBackground(container.getBackground());
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(values[ii]);

            if (ii > 0 && ii < size - 1)
            {
                new PositionToolTip(getClient(), labels[ii]);
                new PositionToolTip(getClient(), values[ii]);
            }

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
                fillInValues(snapshot, 0, snapshot.getCategories());
                break;
            case REDUCED:
                fillInReducedValues(snapshot);
                break;
            case RELEVANT:
                fillInOnlyRelevantValues(snapshot);
                break;
            default:
                throw new IllegalArgumentException("unsupported layout " + layout); //$NON-NLS-1$
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
            labels[i].setData(category);
            labels[i].setData(ClientPerformanceSnapshot.class.getSimpleName(), snapshot);
            values[i].setText(Values.Money.format(category.getValuation(), getClient().getBaseCurrency()));
            values[i].setData(category);
            values[i].setData(ClientPerformanceSnapshot.class.getSimpleName(), snapshot);
        }

        // footer
        signs[7].setText("="); //$NON-NLS-1$

        LocalDate endDate = snapshot.getEndClientSnapshot().getTime();
        String footer = MessageFormat.format(Messages.PerformanceRelevantTransactionsFooter,
                        Values.Date.format(endDate));
        labels[7].setText(footer);

        Money totalRelevantTransactions = sumCategoryValuations(
                        snapshot.getValue(CategoryType.INITIAL_VALUE).getCurrencyCode(), categories.subList(1, 7));
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
            String sign = category.getSign();
            switch (sign)
            {
                case "+": //$NON-NLS-1$
                    totalMoney.add(category.getValuation());
                    break;
                case "-": //$NON-NLS-1$
                    totalMoney.subtract(category.getValuation());
                    break;
                default:
                    throw new IllegalArgumentException("unsupported sign " + sign); //$NON-NLS-1$
            }
        }

        return totalMoney.toMoney();
    }

    private void fillInValues(ClientPerformanceSnapshot snapshot, int startIndex,
                    List<ClientPerformanceSnapshot.Category> categories)
    {
        int ii = startIndex;
        for (ClientPerformanceSnapshot.Category category : categories)
        {
            signs[ii].setText(category.getSign());
            labels[ii].setText(category.getLabel());
            labels[ii].setData(category);
            labels[ii].setData(ClientPerformanceSnapshot.class.getSimpleName(), snapshot);
            values[ii].setText(Values.Money.format(category.getValuation(), getClient().getBaseCurrency()));
            values[ii].setData(category);
            values[ii].setData(ClientPerformanceSnapshot.class.getSimpleName(), snapshot);

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

        fillInValues(snapshot, 0, categories.subList(0, showFirstXItems));

        signs[showFirstXItems].setText("+"); //$NON-NLS-1$
        labels[showFirstXItems].setText(Messages.LabelCategoryOtherMovements);
        values[showFirstXItems].setText(Values.Money.format(misc, getClient().getBaseCurrency()));

        fillInValues(snapshot, showFirstXItems + 1,
                        categories.subList(categories.size() - showLastXItems, categories.size()));
    }
}
