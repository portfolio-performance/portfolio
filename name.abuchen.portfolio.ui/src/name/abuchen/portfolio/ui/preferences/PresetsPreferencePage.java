package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.PresetValues;

public class PresetsPreferencePage extends FieldEditorPreferencePage
{
    public PresetsPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PresetsPrefPageTitle);
        setDescription(Messages.PresetsPrefPageDescription);
    }

    @Override
    protected void createFieldEditors()
    {
        String[][] entryNamesAndValues = new String[2][2];
        entryNamesAndValues[0] = new String[] { Messages.PresetsPrefPageStartOfDay,
                        PresetValues.TimePreset.MIDNIGHT.name() };
        entryNamesAndValues[1] = new String[] { Messages.PresetsPrefPageNow, PresetValues.TimePreset.NOW.name() };

        addField(new ComboFieldEditor(UIConstants.Preferences.PRESET_VALUE_TIME, Messages.PresetsPrefPageTime,
                        entryNamesAndValues, getFieldEditorParent()));
    }
}
