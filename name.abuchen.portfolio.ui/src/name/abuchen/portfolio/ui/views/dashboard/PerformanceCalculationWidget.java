package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public class PerformanceCalculationWidget extends WidgetDelegate
{
    enum TableLayout
    {
        FULL(Messages.LabelLayoutFull), REDUCED(Messages.LabelLayoutReduced);

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
        public LayoutConfig(WidgetDelegate delegate)
        {
            super(delegate, Messages.LabelLayout, TableLayout.class, Dashboard.Config.LAYOUT);
        }
    }

    private Composite container;
    private DashboardResources resources;

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
        this.resources = resources;

        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).spacing(3, 3).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel());
        GridDataFactory.fillDefaults().span(3, 1).hint(SWT.DEFAULT, title.getFont().getFontData()[0].getHeight() + 10)
                        .grab(true, false).applyTo(title);

        TableLayout layout = get(LayoutConfig.class).getValue();

        createControls(layout);

        return container;
    }

    private void createControls(TableLayout layout)
    {
        // used to detect if we need to re-create the full table
        title.setData(layout);

        switch (layout)
        {
            case FULL:
                createTable(ClientPerformanceSnapshot.CategoryType.values().length);
                break;
            case REDUCED:
                createTable(5);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void createTable(int size)
    {
        Font boldFont = resources.getResourceManager()
                        .createFont(FontDescriptor.createFrom(container.getFont()).setStyle(SWT.BOLD));

        labels = new Label[size];
        signs = new Label[size];
        values = new Label[size];

        for (int ii = 0; ii < size; ii++)
        {
            signs[ii] = new Label(container, SWT.NONE);
            labels[ii] = new Label(container, SWT.NONE);
            values[ii] = new Label(container, SWT.RIGHT);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(values[ii]);

            if (ii == 0 || ii == size - 1) // first and last line
            {
                signs[ii].setFont(boldFont);
                labels[ii].setFont(boldFont);
                values[ii].setFont(boldFont);
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
    public void update()
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

        title.setText(getWidget().getLabel());

        PerformanceIndex index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                        get(ReportingPeriodConfig.class).getReportingPeriod());
        ClientPerformanceSnapshot snapshot = index.getClientPerformanceSnapshot();

        switch (layout)
        {
            case FULL:
                filInValues(0, snapshot.getCategories());
                break;
            case REDUCED:
                fillInReducedValues(snapshot);
                break;
            default:
                throw new IllegalArgumentException();
        }

        container.layout();
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
        MutableMoney misc = MutableMoney.of(snapshot.getValue(CategoryType.INITIAL_VALUE).getCurrencyCode());

        List<ClientPerformanceSnapshot.Category> categories = snapshot.getCategories();

        for (int ii = 2; ii < categories.size() - 2; ii++)
        {
            ClientPerformanceSnapshot.Category category = categories.get(ii);

            switch (category.getSign())
            {
                case "+": //$NON-NLS-1$
                    misc.add(category.getValuation());
                    break;
                case "-": //$NON-NLS-1$
                    misc.subtract(category.getValuation());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        filInValues(0, categories.subList(0, 2));

        signs[2].setText("+"); //$NON-NLS-1$
        labels[2].setText(Messages.LabelCategoryOtherMovements);
        values[2].setText(Values.Money.format(misc.toMoney(), getClient().getBaseCurrency()));

        filInValues(3, categories.subList(categories.size() - 2, categories.size()));
    }
}
