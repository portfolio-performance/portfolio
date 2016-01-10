package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class GeneralPreferencePage extends FieldEditorPreferencePage
{

    public GeneralPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleGeneral);
    }

    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, //
                        Messages.PrefCreateBackupBeforeSaving, getFieldEditorParent()));
    }
}
