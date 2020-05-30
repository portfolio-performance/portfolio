package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class UpdatePreferencePage extends FieldEditorPreferencePage
{

    public UpdatePreferencePage()
    {
        super(GRID);
        
        setTitle(Messages.PrefTitle);
        setDescription(Messages.PrefMsgConfigureUpdates);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new StringFieldEditor(UIConstants.Preferences.UPDATE_SITE, //
                        Messages.PrefUpdateSite, getFieldEditorParent()));
        addField(new BooleanFieldEditor(UIConstants.Preferences.AUTO_UPDATE, //
                        Messages.PrefCheckOnStartup, getFieldEditorParent()));
    }
}
