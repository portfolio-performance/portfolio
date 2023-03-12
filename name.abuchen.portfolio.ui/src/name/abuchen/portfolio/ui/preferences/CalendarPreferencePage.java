package name.abuchen.portfolio.ui.preferences;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.Holiday;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class CalendarPreferencePage extends FieldEditorPreferencePage
{
    private Label infoLabel;
    private int year;
    private String calendar;

    public CalendarPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleCalendar);
        setDescription(Messages.PrefMsgCalendar);
    }

    @Override
    public void createFieldEditors()
    {
        List<TradeCalendar> calendars = TradeCalendarManager.getAvailableCalendar().sorted()
                        .collect(Collectors.toList());
        String[][] entryNamesAndValues = new String[calendars.size()][2];
        int i = 0;
        for (TradeCalendar cal : calendars)
        {
            entryNamesAndValues[i] = getLabelAndValue(cal);
            i++;
        }
        addField(new ComboFieldEditor(UIConstants.Preferences.CALENDAR, Messages.PrefTitleCalendar, entryNamesAndValues,
                        getFieldEditorParent()));

        createInfo(getFieldEditorParent());

    }

    private String[] getLabelAndValue(TradeCalendar calendar)
    {
        return new String[] { calendar.getDescription(), calendar.getCode() };
    }

    protected void createInfo(Composite composite)
    {
        year = LocalDate.now().getYear();
        calendar = TradeCalendarManager.getDefaultInstance().getCode();

        infoLabel = new Label(composite, SWT.WRAP);
        infoLabel.setFont(getFieldEditorParent().getFont());
        GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(infoLabel);

        new Label(composite, SWT.NONE).setText(Messages.LabelYear);

        Spinner yearSpinner = new Spinner(composite, SWT.BORDER);
        yearSpinner.setTextLimit(5);
        yearSpinner.setIncrement(1);
        yearSpinner.setPageIncrement(10);
        // Maximum, minimum, and selection (value) must be set in that order:
        yearSpinner.setMaximum(2150);
        yearSpinner.setMinimum(1850);
        yearSpinner.setSelection(year);
        yearSpinner.addModifyListener(e -> {
            int newYear = yearSpinner.getSelection();
            if (newYear != year)
            {
                year = newYear;
                updateInfoLabel();
            }
        });

        updateInfoLabel();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (event.getProperty().equals(FieldEditor.VALUE))
        {
            String newCode = (String) event.getNewValue();
            if (!newCode.equals(calendar))
            {
                calendar = newCode;
                updateInfoLabel();
            }
        }
    }

    private void updateInfoLabel()
    {
        infoLabel.setText(createHolidayText(calendar, year));
        infoLabel.getParent().getParent().layout(true);
    }

    private static final DateTimeFormatter shortDayOfWeekFormatter = DateTimeFormatter.ofPattern("E"); //$NON-NLS-1$

    private String createHolidayText(String calendarCode, int year)
    {
        TradeCalendar cal = TradeCalendarManager.getInstance(calendarCode);

        if (cal == null)
            return ""; //$NON-NLS-1$

        Collection<Holiday> holidays = cal.getHolidays(year);

        StringBuilder buffer = new StringBuilder();
        holidays.stream().sorted((r, l) -> r.getDate().compareTo(l.getDate()))
                        .forEach(h -> buffer.append(Values.Date.format(h.getDate())).append(" ") //$NON-NLS-1$
                                        .append(h.getLabel())
                                        .append(cal.isWeekend(h.getDate())
                                                        ? " (" + shortDayOfWeekFormatter.format(h.getDate()) + ")" //$NON-NLS-1$ //$NON-NLS-2$
                                                        : "") //$NON-NLS-1$
                                        .append("\n")); //$NON-NLS-1$
        return buffer.toString();
    }
}
