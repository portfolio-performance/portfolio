package name.abuchen.portfolio.ui.preferences;

import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInput.BACKUP_MODE;

public class BackupsPreferencePage extends FieldEditorPreferencePage
{
    private ComboFieldEditor backupModeEditor;
    private DirectoryFieldEditor backupFolderAbsoluteEditor;
    private StringFieldEditor backupFolderRelativeEditor;

    public BackupsPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleBackup);
        setDescription(Messages.PrefMsgBackup);
    }

    @Override
    protected void createFieldEditors()
    {
        addField(new IntegerFieldEditor(UIConstants.Preferences.AUTO_SAVE_FILE, Messages.PrefAutoSaveFrequency,
                        getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, //
                        Messages.PrefCreateBackupBeforeSaving, getFieldEditorParent()));

        backupModeEditor = createBackupModeSelect(getFieldEditorParent());
        addField(backupModeEditor);

        backupFolderAbsoluteEditor = new DirectoryFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE,
                        "Absoluter Backup Ordner", getFieldEditorParent());
        addField(backupFolderAbsoluteEditor);

        String relativeEditorMessage = MessageFormat.format("Relativer Backup Order (z.B. {0})",
                        Paths.get(".", "back").toString()); //$NON-NLS-1$ //$NON-NLS-2$
        backupFolderRelativeEditor = new StringFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE,
                        relativeEditorMessage, getFieldEditorParent());
        addField(backupFolderRelativeEditor);
    }

    protected static ComboFieldEditor createBackupModeSelect(Composite parent)
    {
        BACKUP_MODE[] backupModes = BACKUP_MODE.values();
        String[][] entryNamesAndValues = new String[backupModes.length][2];

        for (int i = 0; i < backupModes.length; i++)
        {
            entryNamesAndValues[i][0] = backupModes[i].getTitle();
            entryNamesAndValues[i][1] = backupModes[i].getPreferenceValue();
        }

        return new ComboFieldEditor(UIConstants.Preferences.BACKUP_MODE, "Backup mode", entryNamesAndValues, parent);
    }

    @Override
    protected void initialize()
    {
        super.initialize();

        updateBackupFolderEditors(BACKUP_MODE.byValue(
                        backupModeEditor.getPreferenceStore().getString(backupModeEditor.getPreferenceName())));

        String relativeFolder = backupFolderRelativeEditor.getStringValue();
        if (relativeFolder == null || relativeFolder.isBlank())
        {
            backupFolderRelativeEditor.setStringValue(ClientInput.DEFAULT_RELATIVE_BACKUP_FOLDER);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource().equals(backupModeEditor))
        {
            updateBackupFolderEditors(BACKUP_MODE.byValue((String) event.getNewValue()));
        }
    }

    private void updateBackupFolderEditors(BACKUP_MODE mode)
    {
        if (mode == null)
        {
            mode = BACKUP_MODE.getDefault();
        }

        switch (mode)
        {
            case NEXT_TO_FILE:
                backupFolderAbsoluteEditor.setEnabled(false, getFieldEditorParent());
                backupFolderRelativeEditor.setEnabled(false, getFieldEditorParent());
                return;
            case ABSOLUTE_FOLDER:
                backupFolderAbsoluteEditor.setEnabled(true, getFieldEditorParent());
                backupFolderRelativeEditor.setEnabled(false, getFieldEditorParent());
                return;
            case RELATIVE_FOLDER:
                backupFolderAbsoluteEditor.setEnabled(false, getFieldEditorParent());
                backupFolderRelativeEditor.setEnabled(true, getFieldEditorParent());
                return;
            default:
                throw new RuntimeException("Unsupported backup mode: " + mode.name()); //$NON-NLS-1$
        }
    }
}
