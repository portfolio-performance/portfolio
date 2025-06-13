package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class ExperimentsPreferencePage extends FieldEditorPreferencePage
{

    public ExperimentsPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleExperimentalFeatures);
    }

    @Override
    public void createFieldEditors()
    {
        String[][] features = java.util.Arrays.stream(Experiments.Feature.values())
                        .map(f -> new String[] { f.name(), f.name() }).toArray(String[][]::new);

        addField(new CheckboxGroupFieldEditor(UIConstants.Preferences.EXPERIMENTS,
                        Messages.PrefLabelEnableExperimentalFeatures, features, getFieldEditorParent()));

    }
}
