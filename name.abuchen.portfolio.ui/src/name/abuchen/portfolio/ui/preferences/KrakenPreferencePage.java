package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Link;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class KrakenPreferencePage extends FieldEditorPreferencePage
{

    public KrakenPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleKraken);
        setDescription(Messages.PrefDescriptionKraken);
    }

    public void createFieldEditors()
    {
        final String krakenApiSettingsUrl = "https://www.kraken.com/u/settings/api";
        Link link = new Link(getFieldEditorParent(), SWT.NONE);
        link.setText("<a>" + krakenApiSettingsUrl + "</a>"); //$NON-NLS-1$
        link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(final SelectionEvent event)
            {
                DesktopAPI.browse(krakenApiSettingsUrl); // $NON-NLS-1$
            }
        });

        addField(new StringFieldEditor(UIConstants.Preferences.KRAKEN_API_KEY, Messages.PrefKrakenAPIKey,
                        getFieldEditorParent()));

        addField(new StringFieldEditor(UIConstants.Preferences.KRAKEN_PRIVATE_KEY, Messages.PrefKrakenPrivateKey,
                        getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.KRAKEN_AUTO_IMPORT_ENABLED, //
                        Messages.PrefLabelKrakenAutoImport, getFieldEditorParent()));

    }
}
