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
        newDashboard.setName(selectedName);

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

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.DESCRIPTION.name());
        widget.setLabel(Messages.DescriptionDashboardUse);
        column.getWidgets().add(widget);

        newDashboard.getColumns().add(new Dashboard.Column());
    }

    /* package */ static void buildIndicatorDashboard(Dashboard newDashboard)
    {
        newDashboard.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "L1Y0"); //$NON-NLS-1$

        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelKeyIndicators);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.TTWROR.name());
        widget.setLabel(WidgetFactory.TTWROR.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.IRR.name());
        widget.setLabel(WidgetFactory.IRR.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.ABSOLUTE_CHANGE.name());
        widget.setLabel(WidgetFactory.ABSOLUTE_CHANGE.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.DELTA.name());
        widget.setLabel(WidgetFactory.DELTA.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelTTWROROneDay);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.TTWROR.name());
        widget.setLabel(WidgetFactory.TTWROR.getLabel());
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "T1"); //$NON-NLS-1$
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.ABSOLUTE_CHANGE.name());
        widget.setLabel(WidgetFactory.ABSOLUTE_CHANGE.getLabel());
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "T1"); //$NON-NLS-1$
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelRiskIndicators);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWN.name());
        widget.setLabel(WidgetFactory.MAXDRAWDOWN.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWNDURATION.name());
        widget.setLabel(WidgetFactory.MAXDRAWDOWNDURATION.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.VOLATILITY.name());
        widget.setLabel(WidgetFactory.VOLATILITY.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.SEMIVOLATILITY.name());
        widget.setLabel(WidgetFactory.SEMIVOLATILITY.getLabel());
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.PerformanceTabCalculation);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CALCULATION.name());
        widget.setLabel(WidgetFactory.CALCULATION.getLabel());
        column.getWidgets().add(widget);
    }

    /* package */ static void buildEarningsDashboard(Dashboard newDashboard)
    {
        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelEarnings);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.EARNINGS_PER_YEAR_CHART.name());
        widget.setLabel(WidgetFactory.EARNINGS_PER_YEAR_CHART.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.EARNINGS_PER_MONTH_CHART.name());
        widget.setLabel(WidgetFactory.EARNINGS_PER_MONTH_CHART.getLabel());
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.EARNINGS.name());
        widget.setLabel(WidgetFactory.EARNINGS.getLabel());
        column.getWidgets().add(widget);
    }

}
