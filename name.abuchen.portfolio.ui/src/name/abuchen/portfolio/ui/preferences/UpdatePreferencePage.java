package name.abuchen.portfolio.ui.preferences;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

public class UpdatePreferencePage extends FieldEditorPreferencePage
{

    public UpdatePreferencePage()
    {
        super(GRID);
        
        setTitle(Messages.PrefTitle);
        setDescription(Messages.PrefMsgConfigureUpdates);
    }

    public void createFieldEditors()
    {
        addField(new StringFieldEditor(PortfolioPlugin.Preferences.UPDATE_SITE, //
                        Messages.PrefUpdateSite, getFieldEditorParent()));
        addField(new BooleanFieldEditor(PortfolioPlugin.Preferences.AUTO_UPDATE, //
                        Messages.PrefCheckOnStartup, getFieldEditorParent()));
    }
}
