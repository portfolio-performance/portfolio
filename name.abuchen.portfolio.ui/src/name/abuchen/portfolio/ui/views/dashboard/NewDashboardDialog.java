package name.abuchen.portfolio.ui.views.dashboard;

import java.util.UUID;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.views.dashboard.DefaultLabels.TemplateLabel;

public class NewDashboardDialog extends Dialog
{
    enum Template
    {
        EMTPY(Messages.LabelEmptyDashboard), INDICATORS(Messages.LabelKeyIndicators), EARNINGS(Messages.LabelEarnings);

        private String label;

        private Template(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }
    }

    private Template selectedTemplate = Template.EMTPY;
    private String selectedName = Messages.LabelDashboard;

    protected NewDashboardDialog(Shell parentShell)
    {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.ConfigurationNew);
    }

    @Override
    protected final Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayout(new FormLayout());

        Label label = new Label(editArea, SWT.NONE);
        label.setText(Messages.ColumnName);

        Text name = new Text(editArea, SWT.BORDER);
        name.setText(selectedName);
        name.addModifyListener(e -> selectedName = name.getText());

        Group group = new Group(editArea, SWT.SHADOW_IN);
        group.setText(Messages.LabelTemplate);
        group.setLayout(new RowLayout(SWT.VERTICAL));

        // create radio buttons
        for (Template template : Template.values())
        {
            Button button = new Button(group, SWT.RADIO);
            button.setText(template.getLabel());
            button.setSelection(template == selectedTemplate);
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> selectedTemplate = template));
        }

        FormDataFactory.startingWith(label).thenRight(name).width(150);
        FormDataFactory.startingWith(label).thenBelow(group, 10).right(name);

        return composite;
    }

    public Dashboard createDashboard()
    {
        Dashboard newDashboard = new Dashboard(UUID.randomUUID().toString());
        if (selectedName.equals(TemplateLabel.DASHBOARD.getText()))
        {
            DefaultLabels.setDefaultName(newDashboard);
        }
        else
        {
            newDashboard.setName(selectedName);
            DefaultLabels.clear(newDashboard);
        }

        switch (selectedTemplate)
        {
            case EMTPY:
                buildEmptyDashboard(newDashboard);
                break;
            case INDICATORS:
                buildIndicatorDashboard(newDashboard);
                break;
            case EARNINGS:
                buildEarningsDashboard(newDashboard);
                break;
            default:
                throw new IllegalArgumentException("unsupported template " + selectedTemplate); //$NON-NLS-1$
        }

        return newDashboard;
    }

    /* package */ static void buildEmptyDashboard(Dashboard newDashboard)
    {
        // add two columns + explanatory note

        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        addWidget(column, TemplateLabel.DASHBOARD_USE);

        newDashboard.getColumns().add(new Dashboard.Column());
    }

    /* package */ static void buildIndicatorDashboard(Dashboard newDashboard)
    {
        newDashboard.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "L1Y0"); //$NON-NLS-1$

        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        addWidget(column, TemplateLabel.KEY_INDICATORS);
        addWidget(column, WidgetFactory.TTWROR);
        addWidget(column, WidgetFactory.IRR);
        addWidget(column, WidgetFactory.ABSOLUTE_CHANGE);
        addWidget(column, WidgetFactory.DELTA);

        addWidget(column, TemplateLabel.LAST_DAY);
        var widget = addWidget(column, WidgetFactory.TTWROR);
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "T1"); //$NON-NLS-1$
        widget = addWidget(column, WidgetFactory.ABSOLUTE_CHANGE);
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "T1"); //$NON-NLS-1$

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        addWidget(column, TemplateLabel.RISK_INDICATORS);
        addWidget(column, WidgetFactory.MAXDRAWDOWN);
        addWidget(column, WidgetFactory.MAXDRAWDOWNDURATION);
        addWidget(column, WidgetFactory.VOLATILITY);
        addWidget(column, WidgetFactory.SEMIVOLATILITY);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        addWidget(column, TemplateLabel.CALCULATION);
        addWidget(column, WidgetFactory.CALCULATION);
    }

    /* package */ static void buildEarningsDashboard(Dashboard newDashboard)
    {
        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        addWidget(column, TemplateLabel.EARNINGS);
        addWidget(column, WidgetFactory.EARNINGS_PER_YEAR_CHART);
        addWidget(column, WidgetFactory.EARNINGS_PER_MONTH_CHART);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        addWidget(column, WidgetFactory.EARNINGS);
    }

    /**
     * Adds a widget labeled with the default label of the widget type.
     */
    private static Dashboard.Widget addWidget(Dashboard.Column column, WidgetFactory widgetType)
    {
        var widget = new Dashboard.Widget();
        widget.setType(widgetType.name());
        widget.setLabel(widgetType.getLabel());
        DefaultLabels.markAsDefault(widget);
        column.getWidgets().add(widget);
        return widget;
    }

    /**
     * Adds a widget labeled with the given template label.
     */
    private static Dashboard.Widget addWidget(Dashboard.Column column, TemplateLabel label)
    {
        var widget = new Dashboard.Widget();
        widget.setType(label.getWidgetType());
        DefaultLabels.setLabel(widget, label);
        column.getWidgets().add(widget);
        return widget;
    }

}
