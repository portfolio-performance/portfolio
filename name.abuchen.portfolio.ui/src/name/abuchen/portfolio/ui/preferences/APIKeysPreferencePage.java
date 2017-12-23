package name.abuchen.portfolio.ui.preferences;

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

public class APIKeysPreferencePage extends FieldEditorPreferencePage
{

    public APIKeysPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleAlphaVantage);
        setDescription(Messages.PrefDescriptionAlphaVantage);
    }

    public void createFieldEditors()
    {
        Link link = new Link(getFieldEditorParent(), SWT.NONE);
        link.setText("<a>https://www.alphavantage.co/support/#api-key</a>"); //$NON-NLS-1$
        link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(final SelectionEvent event)
            {
                DesktopAPI.browse("https://www.alphavantage.co/support/#api-key"); //$NON-NLS-1$
            }
        });

        addField(new StringFieldEditor(UIConstants.Preferences.ALPHAVANTAGE_API_KEY, //
                        Messages.PrefAlphaVantageAPIKey, getFieldEditorParent()));
    }
}
