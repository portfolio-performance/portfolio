package name.abuchen.portfolio.ui.dialogs;

import java.util.Calendar;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class ReportingPeriodDialog extends Dialog
{
    private final ReportingPeriod template;
    private ReportingPeriod result;

    private Button radioLast;
    private Spinner years;
    private Spinner months;

    private Button radioFromXtoY;
    private DateTime dateFrom;
    private DateTime dateTo;

    private Button radioSinceX;
    private DateTime dateSince;

    public ReportingPeriodDialog(Shell parentShell, ReportingPeriod template)
    {
        super(parentShell);
        this.template = template != null ? template : new ReportingPeriod.LastX(1, 0);
    }

    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LabelReportInterval);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        editArea.setLayout(new FormLayout());

        radioLast = new Button(editArea, SWT.RADIO);
        Label lblLast = new Label(editArea, SWT.NONE);
        lblLast.setText(Messages.LabelReportingDialogLast);
        years = new Spinner(editArea, SWT.BORDER);
        years.setMinimum(0);
        Label lblYears = new Label(editArea, SWT.NONE);
        lblYears.setText(Messages.LabelReportingDialogYears);
        months = new Spinner(editArea, SWT.BORDER);
        months.setMinimum(0);
        months.setMaximum(11);
        Label lblMonths = new Label(editArea, SWT.NONE);
        lblMonths.setText(Messages.LabelReportingDialogMonths);

        radioFromXtoY = new Button(editArea, SWT.RADIO);
        Label lblFrom = new Label(editArea, SWT.NONE);
        lblFrom.setText(Messages.LabelReportingDialogFrom);
        dateFrom = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
        Label lblTo = new Label(editArea, SWT.NONE);
        lblTo.setText(Messages.LabelReportingDialogUntil);
        dateTo = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);

        radioSinceX = new Button(editArea, SWT.RADIO);
        Label lblSince = new Label(editArea, SWT.NONE);
        lblSince.setText(Messages.LabelReportingDialogSince);
        dateSince = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);

        //
        // form layout
        //

        FormData data = new FormData();
        radioLast.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(radioLast, 5);
        data.top = new FormAttachment(radioLast, 0, SWT.CENTER);
        lblLast.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(lblLast, 5);
        data.top = new FormAttachment(radioLast, 0, SWT.CENTER);
        years.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(years, 2);
        data.top = new FormAttachment(radioLast, 0, SWT.CENTER);
        lblYears.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(lblYears, 10);
        data.top = new FormAttachment(radioLast, 0, SWT.CENTER);
        months.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(months, 2);
        data.top = new FormAttachment(radioLast, 0, SWT.CENTER);
        lblMonths.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(radioLast, 30);
        radioFromXtoY.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(radioFromXtoY, 5);
        data.top = new FormAttachment(radioFromXtoY, 0, SWT.CENTER);
        lblFrom.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(lblFrom, 5);
        data.top = new FormAttachment(lblFrom, -3, SWT.TOP);
        dateFrom.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(dateFrom, 5);
        data.top = new FormAttachment(radioFromXtoY, 0, SWT.CENTER);
        lblTo.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(lblTo, 5);
        data.top = new FormAttachment(lblTo, -3, SWT.TOP);
        dateTo.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(radioFromXtoY, 30);
        radioSinceX.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(radioSinceX, 5);
        data.top = new FormAttachment(radioSinceX, 0, SWT.CENTER);
        lblSince.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(lblSince, 5);
        data.top = new FormAttachment(lblSince, -3, SWT.TOP);
        dateSince.setLayoutData(data);

        //
        // wiring
        //

        presetFromTemplate();

        listen(radioLast, years, months);
        listen(radioFromXtoY, dateFrom, dateTo);
        listen(radioSinceX, dateSince);

        return composite;
    }

    private void listen(final Button radio, Control... controls)
    {
        for (Control c : controls)
        {
            c.addListener(SWT.Selection, new Listener()
            {
                @Override
                public void handleEvent(Event event)
                {
                    radioLast.setSelection(false);
                    radioFromXtoY.setSelection(false);
                    radioSinceX.setSelection(false);

                    radio.setSelection(true);
                }
            });
        }
    }

    private void presetFromTemplate()
    {
        if (template instanceof ReportingPeriod.LastX)
            radioLast.setSelection(true);
        else if (template instanceof ReportingPeriod.FromXtoY)
            radioFromXtoY.setSelection(true);
        else if (template instanceof ReportingPeriod.SinceX)
            radioSinceX.setSelection(true);
        else
            throw new RuntimeException();

        Calendar cal = Calendar.getInstance();
        cal.setTime(template.getStartDate());
        dateFrom.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dateSince.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(template.getEndDate());
        dateTo.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        int m = Dates.monthsBetween(template.getStartDate(), template.getEndDate());
        years.setSelection(m / 12);
        months.setSelection(m % 12);
    }

    @Override
    protected void okPressed()
    {
        if (radioLast.getSelection())
        {
            result = new ReportingPeriod.LastX(years.getSelection(), months.getSelection());
        }
        else if (radioFromXtoY.getSelection())
        {
            result = new ReportingPeriod.FromXtoY( //
                            Dates.date(dateFrom.getYear(), dateFrom.getMonth(), dateFrom.getDay()), //
                            Dates.date(dateTo.getYear(), dateTo.getMonth(), dateTo.getDay()));
        }
        else if (radioSinceX.getSelection())
        {
            result = new ReportingPeriod.SinceX( //
                            Dates.date(dateSince.getYear(), dateSince.getMonth(), dateSince.getDay()));
        }
        else
        {
            throw new RuntimeException();
        }

        super.okPressed();
    }

    public ReportingPeriod getReportingPeriod()
    {
        return result;
    }

}
