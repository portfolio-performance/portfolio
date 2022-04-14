package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Link;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class EODHistoricalDataPreferencePage extends FieldEditorPreferencePage
{

    public EODHistoricalDataPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleEODHistoricalData);
        setDescription(Messages.PrefDescriptionEODHistoricalData);
        setImageDescriptor(Images.EODHISTORICALDATA_LOGO.descriptor());
    }

    @Override
    public void createFieldEditors()
    {
        Link link = new Link(getFieldEditorParent(), SWT.NONE);
        link.setText("<a>https://eodhistoricaldata.com</a>"); //$NON-NLS-1$
        link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        link.addSelectionListener(SelectionListener
                        .widgetSelectedAdapter(e -> DesktopAPI.browse("https://eodhistoricaldata.com"))); //$NON-NLS-1$

        addField(new StringFieldEditor(UIConstants.Preferences.EOD_HISTORICAL_DATA_API_KEY, //
                        Messages.PrefEODHistoricalDataAPIKey, getFieldEditorParent()));
    }
}
