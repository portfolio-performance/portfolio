package name.abuchen.portfolio.ui.preferences;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
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

public class BackupsPreferencePage extends FieldEditorPreferencePage
{
    private BooleanFieldEditor backupModeEditor;
    private DirectoryFieldEditor backupFolderAbsoluteEditor;
    private StringFieldEditor backupFolderRelativeEditor;
    private Label relativeFolderInfoLabel;

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

        backupModeEditor = new BooleanFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_MODE,
                        "Backups relativ anstatt absolut erstellen", getFieldEditorParent());

        backupFolderAbsoluteEditor = new DirectoryFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE,
                        "Absoluter Backup Ordner", getFieldEditorParent());

        String relativeEditorMessage = MessageFormat.format("Relativer Backup Order (z.B. {0})",
                        Paths.get(".", "back").toString()); //$NON-NLS-1$ //$NON-NLS-2$
        backupFolderRelativeEditor = new StringFieldEditor(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE,
                        relativeEditorMessage, getFieldEditorParent());

        addField(backupModeEditor);
        addField(backupFolderAbsoluteEditor);
        addField(backupFolderRelativeEditor);

        createRelativeFolderInfo(getFieldEditorParent());
    }

    protected void createRelativeFolderInfo(Composite composite)
    {
        relativeFolderInfoLabel = new Label(composite, SWT.WRAP);
        relativeFolderInfoLabel.setFont(getFieldEditorParent().getFont());

        GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(relativeFolderInfoLabel);
    }

    @Override
    protected void initialize()
    {
        super.initialize();
        setBackupFolderEnabled(backupModeEditor.getBooleanValue());

        if (backupModeEditor.getBooleanValue())
        {
            onRelativeBackupFolderChange(backupFolderRelativeEditor.getStringValue());
        }
        else
        {
            onRelativeBackupFolderChange(null);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource().equals(backupModeEditor))
        {
            setBackupFolderEnabled((Boolean) event.getNewValue());
            onRelativeBackupFolderChange(backupFolderRelativeEditor.getStringValue());
        }
        else if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource().equals(backupFolderRelativeEditor))
        {
            onRelativeBackupFolderChange((String) event.getNewValue());
        }
    }

    private void setBackupFolderEnabled(boolean enabled)
    {
        backupFolderAbsoluteEditor.setEnabled(!enabled, getFieldEditorParent());
        backupFolderRelativeEditor.setEnabled(enabled, getFieldEditorParent());
        relativeFolderInfoLabel.setEnabled(enabled);
    }

    private void onRelativeBackupFolderChange(String newValue)
    {
        if (null == newValue || newValue.trim().equals(""))//$NON-NLS-1$
        {
            relativeFolderInfoLabel.setText("");//$NON-NLS-1$
            return;
        }

        Path currentDataFile = Path.of("C:\\temp\\bla.xml");
        Path currentDataFolder = currentDataFile.getParent();
        Path dest = currentDataFolder.resolve(newValue).normalize();

        String infoText = MessageFormat.format(
                        "Backups für die aktuelle Datendatei {1} werden in den folgenden Ordner gespeichert: {0}", dest,
                        currentDataFile);
        relativeFolderInfoLabel.setText(infoText);
    }
}
