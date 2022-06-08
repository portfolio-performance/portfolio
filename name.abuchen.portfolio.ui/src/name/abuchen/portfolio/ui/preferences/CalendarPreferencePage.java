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

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.Holiday;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class CalendarPreferencePage extends FieldEditorPreferencePage
{
    private Label infoLabel;

    public CalendarPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleCalendar);
        setDescription(Messages.PrefMsgCalendar);
    }

    @Override
    public void createFieldEditors()
    {
        List<TradeCalendar> calendar = TradeCalendarManager.getAvailableCalendar().sorted()
                        .collect(Collectors.toList());
        String[][] entryNamesAndValues = new String[calendar.size()][2];
        int i = 0;
        for (TradeCalendar cal : calendar)
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
        int year = LocalDate.now().getYear();
        infoLabel = new Label(composite, SWT.WRAP);
        infoLabel.setText(createHolidayText(TradeCalendarManager.getDefaultInstance().getCode(), year));
        infoLabel.setFont(getFieldEditorParent().getFont());

        GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(infoLabel);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (event.getProperty().equals(FieldEditor.VALUE))
        {
            int year = LocalDate.now().getYear();
            String newCode = (String) event.getNewValue();
            infoLabel.setText(createHolidayText(newCode, year));
            infoLabel.getParent().getParent().layout(true);
        }
    }

    private static final DateTimeFormatter shortDayOfWeekFormatter = DateTimeFormatter.ofPattern("E"); //$NON-NLS-1$

    private String createHolidayText(String calendarCode, int year)
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance(calendarCode);

        if (calendar == null)
            return ""; //$NON-NLS-1$

        Collection<Holiday> holidays = calendar.getHolidays(year);

        StringBuilder buffer = new StringBuilder();
        holidays.stream().sorted((r, l) -> r.getDate().compareTo(l.getDate()))
                        .forEach(h -> buffer.append(Values.Date.format(h.getDate())).append(" ") //$NON-NLS-1$
                                        .append(h.getLabel())
                                        .append(calendar.isWeekend(h.getDate()) ?
                                            " (" + shortDayOfWeekFormatter.format(h.getDate()) + ")" //$NON-NLS-1$ //$NON-NLS-2$
                                            : "") //$NON-NLS-1$
                                        .append("\n")); //$NON-NLS-1$
        return buffer.toString();
    }
}
