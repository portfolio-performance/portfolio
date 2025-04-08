package name.abuchen.portfolio.ui.preferences;

import name.abuchen.portfolio.ui.Messages;

public enum BackupMode
{
    NEXT_TO_FILE(Messages.LabelBackupModeNextToFile), //
    ABSOLUTE_FOLDER(Messages.LabelBackupModeAbsoluteDirectory), //
    RELATIVE_FOLDER(Messages.LabelBackupModeRelativeDirectory);

    private String title;

    private BackupMode(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public static BackupMode getDefault()
    {
        return NEXT_TO_FILE;
    }
}
