package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class UpdatePreferencePage extends FieldEditorPreferencePage
{
    private static final String BETA_URL = "https://updates.portfolio-performance.info/portfolio-beta"; //$NON-NLS-1$

    private StringFieldEditor updateSiteField;
    private Button stableButton;
    private Button betaButton;

    public UpdatePreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitle);
        setDescription(Messages.PrefMsgConfigureUpdates);
    }

    @Override
    public void createFieldEditors()
    {
        updateSiteField = new StringFieldEditor(UIConstants.Preferences.UPDATE_SITE, //
                        Messages.PrefUpdateSite, getFieldEditorParent());
        addField(updateSiteField);

        var buttons = new Composite(getFieldEditorParent(), SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        buttons.setLayout(new RowLayout());

        stableButton = new Button(buttons, SWT.PUSH);
        stableButton.setText(Messages.PrefUpdateSiteStableChannel);
        stableButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                getPreferenceStore().setToDefault(UIConstants.Preferences.UPDATE_SITE);
                updateSiteField.load();
                updateButtonStates(stableButton, betaButton);
            }
        });

        betaButton = new Button(buttons, SWT.PUSH);
        betaButton.setText(Messages.PrefUpdateSiteBetaChannel);
        betaButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateSiteField.setStringValue(BETA_URL);
                updateButtonStates(stableButton, betaButton);
            }
        });

        updateSiteField.getTextControl(getFieldEditorParent())
                        .addModifyListener(e -> updateButtonStates(stableButton, betaButton));

        updateButtonStates(stableButton, betaButton);

        addField(new BooleanFieldEditor(UIConstants.Preferences.AUTO_UPDATE, //
                        Messages.PrefCheckOnStartup, getFieldEditorParent()));
    }

    private void updateButtonStates(Button stableButton, Button betaButton)
    {
        var current = updateSiteField.getStringValue();
        var stableUrl = getPreferenceStore().getDefaultString(UIConstants.Preferences.UPDATE_SITE);
        stableButton.setEnabled(!current.equals(stableUrl));
        betaButton.setEnabled(!current.equals(BETA_URL));
    }
}
