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
        setTitle(Messages.PrefTitleQuotes);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, //
                        Messages.PrefUpdateQuotesAfterFileOpen, getFieldEditorParent()));

        addField(new IntegerFieldEditor(UIConstants.Preferences.QUOTES_STALE_AFTER_DAYS_PATH, //
                        Messages.PrefQuotesStaleAfterDays, getFieldEditorParent()));

        createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(), //
                        Messages.PrefLabelNote, Messages.PrefQuotesStaleNote);
    }
}
