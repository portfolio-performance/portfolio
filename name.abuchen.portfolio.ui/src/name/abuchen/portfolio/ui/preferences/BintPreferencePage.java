package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Link;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class BintPreferencePage extends FieldEditorPreferencePage
{

    public BintPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleBintEe);
        setDescription(Messages.PrefDescriptionBintEe);
        setImageDescriptor(Images.BINTEE_LOGO.descriptor());
    }

    @Override
    public void createFieldEditors()
    {
        Link link = new Link(getFieldEditorParent(), SWT.NONE);
        link.setText("<a>eMail: pp@bint.ee</a>"); //$NON-NLS-1$
        link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(final SelectionEvent event)
            {
                DesktopAPI.browse("mailto:pp@bint.ee"); //$NON-NLS-1$
            }
        });

        addField(new StringFieldEditor(UIConstants.Preferences.BINT_EE_API_KEY, //
                        Messages.PrefBintEeAPIKey, getFieldEditorParent()));
    }
}
