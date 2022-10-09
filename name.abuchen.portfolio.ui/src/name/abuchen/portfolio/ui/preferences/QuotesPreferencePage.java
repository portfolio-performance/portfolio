package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class QuotesPreferencePage extends FieldEditorPreferencePage
{
    public QuotesPreferencePage()
    {
        super(GRID);
        setTitle("Quotes");
    }

    @Override
    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, //
                        Messages.PrefUpdateQuotesAfterFileOpen, getFieldEditorParent()));

        addField(new IntegerFieldEditor(UIConstants.Preferences.QUOTES_STALE_AFTER_DAYS_PATH,
                        "Number of days after a security price is not up-to-date anymore",
                        getFieldEditorParent()));

        createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(), //
                        Messages.PrefLabelNote,
                        "After this amount of days a security price is marked as not up-to-date.\nPlease note that only days with open trade markets are considered\n(depends on the configured calendar for each security)");
    }
}
