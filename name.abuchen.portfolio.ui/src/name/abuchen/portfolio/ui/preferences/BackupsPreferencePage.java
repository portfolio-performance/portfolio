package name.abuchen.portfolio.ui.preferences;

import java.util.regex.Pattern;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;

public class BackupsPreferencePage extends FieldEditorPreferencePage
{
    private ComboFieldEditor backupMode;
    private DirectoryFieldEditor backupFolderAbsolute;
    private StringFieldEditor backupFolderRelative;

    public BackupsPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleBackup);
        setDescription(Messages.PrefMsgBackup);
    }

    @Override
    protected void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, //
                        Messages.PrefCreateBackupBeforeSaving, getFieldEditorParent()));

        backupMode = createBackupModeSelect(getFieldEditorParent());
        addField(backupMode);

        backupFolderAbsolute = new DirectoryFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE,
                        Messages.PrefBackupDirectory, getFieldEditorParent());
        addField(backupFolderAbsolute);

        backupFolderRelative = new StringFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE,
                        Messages.PrefRelativeBackupDirectoryName, getFieldEditorParent())
        {
            @Override
            protected boolean doCheckState()
            {
                return Pattern.matches("[a-zA-Z][a-zA-Z0-9_.-]*", getStringValue()); //$NON-NLS-1$
            }

        };
        addField(backupFolderRelative);

        Label separator = new Label(getFieldEditorParent(), SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().span(3, 1).applyTo(separator);

        addField(new IntegerFieldEditor(UIConstants.Preferences.AUTO_SAVE_FILE, Messages.PrefLabelAutoSaveFrequency,
                        getFieldEditorParent()));

        Label hint = new Label(getFieldEditorParent(), SWT.NONE);
        hint.setText(Messages.PrefAutoSaveFrequency);
        GridDataFactory.fillDefaults().span(3, 1).applyTo(hint);
    }

    protected static ComboFieldEditor createBackupModeSelect(Composite parent)
    {
        BackupMode[] backupModes = BackupMode.values();
        String[][] entryNamesAndValues = new String[backupModes.length][2];

        for (int ii = 0; ii < backupModes.length; ii++)
        {
            entryNamesAndValues[ii][0] = backupModes[ii].getTitle();
            entryNamesAndValues[ii][1] = backupModes[ii].name();
        }

        return new ComboFieldEditor(UIConstants.Preferences.BACKUP_MODE, Messages.PrefBackupLocation,
                        entryNamesAndValues, parent);
    }

    @Override
    protected void initialize()
    {
        super.initialize();

        enableDirectoryPickers(
                        BackupMode.valueOf(backupMode.getPreferenceStore().getString(backupMode.getPreferenceName())));

        String relativeFolder = backupFolderRelative.getStringValue();
        if (relativeFolder == null || relativeFolder.isBlank())
            backupFolderRelative.setStringValue(ClientInput.DEFAULT_RELATIVE_BACKUP_FOLDER);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource().equals(backupMode))
        {
            enableDirectoryPickers(BackupMode.valueOf((String) event.getNewValue()));
        }
    }

    private void enableDirectoryPickers(BackupMode mode)
    {
        if (mode == null)
            mode = BackupMode.getDefault();

        backupFolderAbsolute.setEnabled(mode == BackupMode.ABSOLUTE_FOLDER, getFieldEditorParent());
        backupFolderRelative.setEnabled(mode == BackupMode.RELATIVE_FOLDER, getFieldEditorParent());
    }
}
