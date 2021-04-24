package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class PortfolioReportPreferencePage extends FieldEditorPreferencePage
{

    public PortfolioReportPreferencePage()
    {
        super(GRID);

        setTitle("Portfolio Report");
        setDescription("Sync data with server portfolio");
    }

    @Override
    public void createFieldEditors()
    {
        Link link = new Link(getFieldEditorParent(), SWT.NONE);
        link.setText("<a>https://app.portfolio-report.net/</a>"); //$NON-NLS-1$
        link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(final SelectionEvent event)
            {
                DesktopAPI.browse("https://app.portfolio-report.net/"); //$NON-NLS-1$
            }
        });

        addField(new StringFieldEditor(UIConstants.Preferences.PORTFOLIO_REPORT_API_URL, //
                        "Portfolio Report API URL", getFieldEditorParent()));

        addField(new StringFieldEditor(UIConstants.Preferences.PORTFOLIO_REPORT_API_KEY, //
                        "Portfolio Report Session Key", getFieldEditorParent()));

        Composite note = createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(), //
                        Messages.PrefLabelNote, "Keep the session key secret!");
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(note);
    }
}
