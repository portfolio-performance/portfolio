package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class GeneralPreferencePage extends FieldEditorPreferencePage
{

    public GeneralPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleGeneral);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, //
                        Messages.PrefUpdateQuotesAfterFileOpen, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.STORE_SETTINGS_NEXT_TO_FILE, //
                        Messages.PrefStoreSettingsNextToFile, getFieldEditorParent()));

        createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(), //
                        Messages.PrefLabelNote, Messages.PrefNoteStoreSettingsNextToFile);

        addField(new BooleanFieldEditor(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES, //
                        Messages.PrefLabelEnableExperimentalFeatures, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS, //
                        Messages.PrefLabelUseSWTChartLibrary, getFieldEditorParent()));

    }
}
