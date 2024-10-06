package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;

import name.abuchen.portfolio.money.Values;
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
        IntegerFieldEditor sharesPrecisionEditor = new IntegerFieldEditor(Preferences.FORMAT_SHARES_DIGITS,
                        Messages.PrefLabelSharesDigits, getFieldEditorParent(), 1);
        sharesPrecisionEditor.setValidRange(0, Values.Share.precision());
        addField(sharesPrecisionEditor);
        IntegerFieldEditor quotePrecisionEditor = new IntegerFieldEditor(Preferences.FORMAT_CALCULATED_QUOTE_DIGITS,
                        Messages.PrefLabelQuoteDigits, getFieldEditorParent(), 1);
        quotePrecisionEditor.setValidRange(0, Values.Quote.precision());
        addField(quotePrecisionEditor);

        var preferSecuritySymEditor = new BooleanFieldEditor(Preferences.FORMAT_PREFER_SECURITY_SYMBOL,
                        Messages.PrefLabelPreferSecuritySymbol, getFieldEditorParent());
        var control = preferSecuritySymEditor.getDescriptionControl(getFieldEditorParent());
        control.setToolTipText(Messages.PrefLabelPreferSecuritySymbolTooltip);
        addField(preferSecuritySymEditor);
    }
}
