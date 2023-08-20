package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class ImportWizardPreferencePage extends FieldEditorPreferencePage
{

    public ImportWizardPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleImportWizard);
        setDescription(Messages.PrefDescriptionImportWizard);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.IMPORT_WIZARD_CONVERT_BUYSELL_TO_DELIVERY, //
                        Messages.LabelConvertBuySellIntoDeliveryTransactions, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.IMPORT_WIZARD_REMOVE_DIVIDENDS, //
                        Messages.LabelRemoveDividends, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.IMPORT_WIZARD_NOTES, //
                        Messages.LabelRemoveNotesInTransactions, getFieldEditorParent()));
    }
}