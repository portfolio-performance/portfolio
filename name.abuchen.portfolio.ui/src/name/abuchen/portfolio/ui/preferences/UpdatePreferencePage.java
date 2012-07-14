package name.abuchen.portfolio.ui.preferences;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class UpdatePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{

    public UpdatePreferencePage()
    {
        super(GRID);
    }

    public void createFieldEditors()
    {
        addField(new StringFieldEditor(PortfolioPlugin.Preferences.UPDATE_SITE, //
                        "&Update Site:", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PortfolioPlugin.Preferences.AUTO_UPDATE, //
                        "&Check for updates on start", getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench)
    {
        setPreferenceStore(PortfolioPlugin.getDefault().getPreferenceStore());
        setDescription("Configures where to check for updates for the application.");
    }
}
