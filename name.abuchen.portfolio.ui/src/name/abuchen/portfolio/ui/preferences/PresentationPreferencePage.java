package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class PresentationPreferencePage extends FieldEditorPreferencePage
{

    public PresentationPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitlePresentation);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.USE_INDIRECT_QUOTATION, //
                        Messages.PrefLabelUseIndirectQuotation, getFieldEditorParent()));

        createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(), //
                        Messages.PrefLabelNote, Messages.PrefNoteIndirectQuotation);
    }
}
