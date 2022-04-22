package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class BackupsPreferencePage extends FieldEditorPreferencePage
{
    public BackupsPreferencePage()
    {
        super(GRID);

        setTitle("Backup"); // TODO Messages...
        setDescription("Backup bla bla"); // TODO Messages...
    }

    @Override
    protected void createFieldEditors()
    {
        addField(new IntegerFieldEditor(UIConstants.Preferences.AUTO_SAVE_FILE, Messages.PrefAutoSaveFrequency,
                        getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, //
                        Messages.PrefCreateBackupBeforeSaving, getFieldEditorParent()));

        addField(new StringFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE,  "REL", getFieldEditorParent())); // TODO Messages...
        addField(new StringFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE,  "ABS", getFieldEditorParent())); // TODO Messages...
    }
}
