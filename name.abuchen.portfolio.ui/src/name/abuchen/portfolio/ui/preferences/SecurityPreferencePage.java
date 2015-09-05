package name.abuchen.portfolio.ui.preferences;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

public class SecurityPreferencePage extends FieldEditorPreferencePage
{

    public SecurityPreferencePage()
    {
        super(GRID);
        
        setTitle(Messages.PrefTitleSecurity);
        setDescription(Messages.PrefMsgConfigureSecurity);
    }

    public void createFieldEditors()
    {
        addField(new BooleanFieldEditor(PortfolioPlugin.Preferences.CRYPTO_USE_EXPERIMENTAL, //
                        Messages.PrefCryptUseExperimental, getFieldEditorParent()));
    }
}
