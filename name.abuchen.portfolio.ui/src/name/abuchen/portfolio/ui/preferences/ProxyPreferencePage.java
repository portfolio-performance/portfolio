package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class ProxyPreferencePage extends FieldEditorPreferencePage
{

    public ProxyPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleProxy);
        setDescription(Messages.PrefDescriptionProxy);
    }

    @Override
    public void createFieldEditors()
    {
        addField(new StringFieldEditor(UIConstants.Preferences.PROXY_HOST, //
                        Messages.PrefLabelProxyHost, getFieldEditorParent()));
        addField(new IntegerFieldEditor(UIConstants.Preferences.PROXY_PORT, //
                        Messages.PrefLabelProxyPort, getFieldEditorParent()));
    }
}
