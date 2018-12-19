package name.abuchen.portfolio.ui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.TradeCalendarCode;

public class CalendarPreferencePage extends FieldEditorPreferencePage
{
    private String[] getLabelAndValue(TradeCalendarCode tradeCalendarCode)
    {
        return new String[] { tradeCalendarCode.getDisplayName(), tradeCalendarCode.getCalendarCode() };
    }

    public CalendarPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleCalendar);
        setDescription(Messages.PrefMsgCalendar);
    }

    @Override
    public void createFieldEditors()
    {
        List<TradeCalendarCode> calendar = new ArrayList<>();
        calendar.addAll(TradeCalendarCode.getAvailableCalendar().stream().sorted().collect(Collectors.toList()));
        String[][] entryNamesAndValues = new String[calendar.size()][2];
        int i = 0;
        for (TradeCalendarCode cal : calendar)
        {
            entryNamesAndValues[i] = getLabelAndValue(cal);
            i++;
        }
        addField(new ComboFieldEditor(UIConstants.Preferences.CALENDAR, Messages.PrefUpdateSite, entryNamesAndValues,
                        getFieldEditorParent()));
    }
}
