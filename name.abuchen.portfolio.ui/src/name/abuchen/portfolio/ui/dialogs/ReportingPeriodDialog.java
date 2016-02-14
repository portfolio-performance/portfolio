package name.abuchen.portfolio.ui.dialogs;

import java.time.Period;
import java.time.Year;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DateTimePicker;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.util.Dates;

public class ReportingPeriodDialog extends Dialog
{
    private final ReportingPeriod template;
    private ReportingPeriod result;

    private Button radioLast;
    private Spinner years;
    private Spinner months;

    private Button radioLastDays;
    private Spinner days;

    private Button radioFromXtoY;
    private DateTimePicker dateFrom;
    private DateTimePicker dateTo;

    private Button radioSinceX;
    private DateTimePicker dateSince;

    private Button radioYearX;
    private Spinner year;

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

        radioLastDays = new Button(editArea, SWT.RADIO);
        Label lblLastDays = new Label(editArea, SWT.NONE);
        lblLastDays.setText(Messages.LabelReportingDialogLast);
        days = new Spinner(editArea, SWT.BORDER);
        days.setMinimum(1);
        days.setMaximum(10000);
        Label lblDays = new Label(editArea, SWT.NONE);
        lblDays.setText(Messages.LabelReportingDialogDays);

        radioFromXtoY = new Button(editArea, SWT.RADIO);
        Label lblFrom = new Label(editArea, SWT.NONE);
        lblFrom.setText(Messages.LabelReportingDialogFrom);
        dateFrom = new DateTimePicker(editArea);
        Label lblTo = new Label(editArea, SWT.NONE);
        lblTo.setText(Messages.LabelReportingDialogUntil);
        dateTo = new DateTimePicker(editArea);

        radioSinceX = new Button(editArea, SWT.RADIO);
        Label lblSince = new Label(editArea, SWT.NONE);
        lblSince.setText(Messages.LabelReportingDialogSince);
        dateSince = new DateTimePicker(editArea);

        radioYearX = new Button(editArea, SWT.RADIO);
        Label lblYearX = new Label(editArea, SWT.NONE);
        lblYearX.setText(Messages.LabelReportingDialogYear);
        year = new Spinner(editArea, SWT.BORDER);
        year.setMinimum(Year.MIN_VALUE);
        year.setMaximum(Year.MAX_VALUE);

        //
        // form layout
        //

        FormDataFactory.startingWith(radioLast).thenRight(lblLast).thenRight(years).thenRight(lblYears)
                        .thenRight(months).thenRight(lblMonths);

        FormDataFactory.startingWith(radioLastDays).top(new FormAttachment(radioLast, 20)).thenRight(lblLastDays)
                        .thenRight(days).thenRight(lblDays);

        FormDataFactory.startingWith(radioFromXtoY).top(new FormAttachment(radioLastDays, 20)).thenRight(lblFrom)
                        .thenRight(dateFrom.getControl()).top(new FormAttachment(lblFrom, -3, SWT.TOP)).thenRight(lblTo)
                        .top(new FormAttachment(lblFrom, 0, SWT.TOP)).thenRight(dateTo.getControl())
                        .top(new FormAttachment(lblFrom, -3, SWT.TOP));

        FormDataFactory.startingWith(radioSinceX).top(new FormAttachment(radioFromXtoY, 20)).thenRight(lblSince)
                        .thenRight(dateSince.getControl()).top(new FormAttachment(lblSince, -3, SWT.TOP));

        FormDataFactory.startingWith(radioYearX).top(new FormAttachment(radioSinceX, 20)).thenRight(lblYearX)
                        .thenRight(year);

        //
        // wiring
        //

        presetFromTemplate();

        listen(radioLast, years, months);
        listen(radioLastDays, days);
        listen(radioFromXtoY, dateFrom.getControl(), dateTo.getControl());
        listen(radioSinceX, dateSince.getControl());
        listen(radioYearX, year);

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
        else if (template instanceof ReportingPeriod.LastXDays)
            radioLastDays.setSelection(true);
        else if (template instanceof ReportingPeriod.FromXtoY)
            radioFromXtoY.setSelection(true);
        else if (template instanceof ReportingPeriod.SinceX)
            radioSinceX.setSelection(true);
        else if (template instanceof ReportingPeriod.YearX)
            radioYearX.setSelection(true);
        else
            throw new RuntimeException();

        dateFrom.setSelection(template.getStartDate());
        dateSince.setSelection(template.getStartDate());

        dateTo.setSelection(template.getEndDate());

        Period p = Period.between(template.getStartDate(), template.getEndDate());
        years.setSelection(p.getYears());
        months.setSelection(p.getMonths());

        days.setSelection(Dates.daysBetween(template.getStartDate(), template.getEndDate()));

        year.setSelection(template.getEndDate().getYear());
    }

    @Override
    protected void okPressed()
    {
        if (radioLast.getSelection())
        {
            result = new ReportingPeriod.LastX(years.getSelection(), months.getSelection());
        }
        else if (radioLastDays.getSelection())
        {
            result = new ReportingPeriod.LastXDays(days.getSelection());
        }
        else if (radioFromXtoY.getSelection())
        {
            // prevent null values which can be set via the CDateTime widget
            if (dateFrom.getSelection() == null || dateTo.getSelection() == null)
                return;

            result = new ReportingPeriod.FromXtoY(dateFrom.getSelection(), dateTo.getSelection());
        }
        else if (radioSinceX.getSelection())
        {
            // prevent null values which can be set via the CDateTime widget
            if (dateSince.getSelection() == null)
                return;

            result = new ReportingPeriod.SinceX(dateSince.getSelection());
        }
        else if (radioYearX.getSelection())
        {
            result = new ReportingPeriod.YearX(year.getSelection());
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
