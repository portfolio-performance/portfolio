package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class GeneralPreferencePage extends FieldEditorPreferencePage
{

    public GeneralPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleGeneral);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new IntegerFieldEditor(UIConstants.Preferences.AUTO_SAVE_FILE,
                        Messages.PrefAutoSaveFrequency, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, //
                        Messages.PrefCreateBackupBeforeSaving, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, //
                        Messages.PrefUpdateQuotesAfterFileOpen, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.STORE_SETTINGS_NEXT_TO_FILE, //
                        Messages.PrefStoreSettingsNextToFile, getFieldEditorParent()));

        createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(), //
                        Messages.PrefLabelNote, Messages.PrefNoteStoreSettingsNextToFile);
    }
}
