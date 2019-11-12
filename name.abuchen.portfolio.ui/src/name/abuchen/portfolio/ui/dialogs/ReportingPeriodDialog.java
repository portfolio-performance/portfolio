package name.abuchen.portfolio.ui.dialogs;

import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

public class ReportingPeriodDialog extends Dialog
{
    private final ReportingPeriod template;
    private ReportingPeriod result;

    private Button radioLast;
    private Spinner years;
    private Spinner months;

    private Button radioLastDays;
    private Spinner days;

    private Button radioLastTradingDays;
    private Spinner tradingDays;

    private Button radioFromXtoY;
    private DatePicker dateFrom;
    private DatePicker dateTo;

    private Button radioSinceX;
    private DatePicker dateSince;

    private Button radioYearX;
    private Spinner year;

    private Button radioCurrentMonth;
    private List<Button> radioBtnList;

    public ReportingPeriodDialog(Shell parentShell, ReportingPeriod template)
    {
        super(parentShell);
        this.template = template != null ? template : new ReportingPeriod.LastX(1, 0);
    }

    @Override
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
        radioLast.setText(Messages.LabelReportingDialogLast);
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
        radioLastDays.setText(Messages.LabelReportingDialogLast);
        days = new Spinner(editArea, SWT.BORDER);
        days.setMinimum(1);
        days.setMaximum(10000);
        Label lblDays = new Label(editArea, SWT.NONE);
        lblDays.setText(Messages.LabelReportingDialogDays);

        radioLastTradingDays = new Button(editArea, SWT.RADIO);
        radioLastTradingDays.setText(Messages.LabelReportingDialogLast);
        tradingDays = new Spinner(editArea, SWT.BORDER);
        tradingDays.setMinimum(1);
        tradingDays.setMaximum(10000);
        Label lblTradingDays = new Label(editArea, SWT.NONE);
        lblTradingDays.setText(Messages.LabelReportingDialogTradingDays);

        radioFromXtoY = new Button(editArea, SWT.RADIO);
        radioFromXtoY.setText(Messages.LabelReportingDialogFrom);
        dateFrom = new DatePicker(editArea);
        Label lblTo = new Label(editArea, SWT.NONE);
        lblTo.setText(Messages.LabelReportingDialogUntil);
        dateTo = new DatePicker(editArea);

        radioSinceX = new Button(editArea, SWT.RADIO);
        radioSinceX.setText(Messages.LabelReportingDialogSince);
        dateSince = new DatePicker(editArea);

        radioYearX = new Button(editArea, SWT.RADIO);
        radioYearX.setText(Messages.LabelReportingDialogYear);
        year = new Spinner(editArea, SWT.BORDER);
        year.setMinimum(Year.MIN_VALUE);
        year.setMaximum(Year.MAX_VALUE);

        radioCurrentMonth = new Button(editArea, SWT.RADIO);
        radioCurrentMonth.setText(Messages.LabelCurrentMonth);

        //
        // form layout
        //

        FormDataFactory.startingWith(radioLast).top(new FormAttachment(0, 10)).thenRight(years).thenRight(lblYears)
                        .thenRight(months).thenRight(lblMonths);

        FormDataFactory.startingWith(radioLastDays).top(new FormAttachment(radioLast, 20)).thenRight(days)
                        .thenRight(lblDays);

        FormDataFactory.startingWith(radioLastTradingDays).top(new FormAttachment(radioLastDays, 20))
                        .thenRight(tradingDays).thenRight(lblTradingDays);

        if (Platform.OS_MACOSX.equals(Platform.getOS()))
        {
            // under Mac OS X, the date input fields are not align with the text
            // by default

            FormDataFactory.startingWith(radioFromXtoY).top(new FormAttachment(radioLastTradingDays, 20))
                            .thenRight(dateFrom.getControl()).top(new FormAttachment(radioFromXtoY, -1, SWT.TOP))
                            .thenRight(lblTo).top(new FormAttachment(radioFromXtoY, 2, SWT.TOP))
                            .thenRight(dateTo.getControl()).top(new FormAttachment(radioFromXtoY, -1, SWT.TOP));

            FormDataFactory.startingWith(radioSinceX).top(new FormAttachment(radioFromXtoY, 20))
                            .thenRight(dateSince.getControl()).top(new FormAttachment(radioSinceX, -1, SWT.TOP));
        }
        else
        {
            FormDataFactory.startingWith(radioFromXtoY).top(new FormAttachment(radioLastTradingDays, 20))
                            .thenRight(dateFrom.getControl()).thenRight(lblTo).thenRight(dateTo.getControl());

            FormDataFactory.startingWith(radioSinceX).top(new FormAttachment(radioFromXtoY, 20))
                            .thenRight(dateSince.getControl());
        }

        FormDataFactory.startingWith(radioYearX).top(new FormAttachment(radioSinceX, 20)).thenRight(year);

        FormDataFactory.startingWith(radioCurrentMonth).top(new FormAttachment(radioYearX, 20));

        //
        // wiring
        //

        presetFromTemplate();

        radioBtnList = Arrays.asList(radioLast, radioLastDays, radioLastTradingDays, radioFromXtoY, radioSinceX, radioYearX);
        activateRadioOnChange(radioLast, years, months);
        activateRadioOnChange(radioLastDays, days);
        activateRadioOnChange(radioLastTradingDays, tradingDays);
        activateRadioOnChange(radioFromXtoY, dateFrom.getControl(), dateTo.getControl());
        activateRadioOnChange(radioSinceX, dateSince.getControl());
        activateRadioOnChange(radioYearX, year);

        return composite;
    }
    
    private void deselectSelectedRadioButtons(final Button radio) {
        radioBtnList.stream()
            .filter(btn -> !btn.equals(radio))
            .filter(btn -> btn.getSelection())
            .forEach(btn -> btn.setSelection(false));
    }
    
    private void activateRadioOnChange(final Button radio, Control... controls)
    {
        for (Control c : controls) {
            c.addListener(SWT.Selection, event -> {
                deselectSelectedRadioButtons(radio);
                radio.setSelection(true);
            });
        }
    }

    private void presetFromTemplate()
    {
        if (template instanceof ReportingPeriod.LastX)
            radioLast.setSelection(true);
        else if (template instanceof ReportingPeriod.LastXDays)
            radioLastDays.setSelection(true);
        else if (template instanceof ReportingPeriod.LastXTradingDays)
            radioLastTradingDays.setSelection(true);
        else if (template instanceof ReportingPeriod.FromXtoY)
            radioFromXtoY.setSelection(true);
        else if (template instanceof ReportingPeriod.SinceX)
            radioSinceX.setSelection(true);
        else if (template instanceof ReportingPeriod.YearX)
            radioYearX.setSelection(true);
        else if (template instanceof ReportingPeriod.CurrentMonth)
            radioCurrentMonth.setSelection(true);
        else
            throw new IllegalArgumentException();

        Interval interval = template.toInterval(LocalDate.now());

        dateFrom.setSelection(interval.getStart());
        dateSince.setSelection(interval.getStart());

        dateTo.setSelection(interval.getEnd());

        Period p = Period.between(interval.getStart(), interval.getEnd());
        years.setSelection(p.getYears());
        months.setSelection(p.getMonths());

        days.setSelection(Dates.daysBetween(interval.getStart(), interval.getEnd()));

        tradingDays.setSelection(Dates.tradingDaysBetween(interval.getStart(), interval.getEnd()));

        year.setSelection(interval.getEnd().getYear());
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
        else if (radioLastTradingDays.getSelection())
        {
            result = new ReportingPeriod.LastXTradingDays(tradingDays.getSelection());
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
        else if (radioCurrentMonth.getSelection())
        {
            result = new ReportingPeriod.CurrentMonth();
        }
        else
        {
            throw new IllegalArgumentException();
        }

        super.okPressed();
    }

    public ReportingPeriod getReportingPeriod()
    {
        return result;
    }

}
