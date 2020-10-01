package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants.Preferences;

public class FormattingPreferencePage extends FieldEditorPreferencePage
{
    public FormattingPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleFormatting);
    }

    @Override
    protected void createFieldEditors()
    {
        IntegerFieldEditor sharesPrecisionEditor = new IntegerFieldEditor(
                        Preferences.FORMAT_SHARES_DIGITS, Messages.PrefLabelSharesDigits, getFieldEditorParent(), 1);
        sharesPrecisionEditor.setValidRange(0, 6);
        addField(sharesPrecisionEditor);
    }
}
