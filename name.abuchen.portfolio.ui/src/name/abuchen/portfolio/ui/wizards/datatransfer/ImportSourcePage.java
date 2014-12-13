package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.text.MessageFormat;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

public class ImportSourcePage extends AbstractWizardPage
{
    private Security security;

    private Text source;

    protected ImportSourcePage(Security security)
    {
        super("source"); //$NON-NLS-1$
        setTitle(Messages.ImportWizardPasteSourceTitle);
        setDescription(MessageFormat.format(Messages.ImportWizardPasteSourceDescription, security.getName()));

        this.security = security;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Link explanation = new Link(container, SWT.NONE);
        FormData fd_text = new FormData();
        fd_text.top = new FormAttachment(0);
        fd_text.left = new FormAttachment(0);
        explanation.setLayoutData(fd_text);
        explanation.setText(MessageFormat.format(Messages.ImportWizardPasteSourceExplanation, security.getIsin()));

        source = new Text(container, SWT.MULTI | SWT.BORDER);
        FormData fd_source = new FormData();
        fd_source.top = new FormAttachment(explanation, 5);
        fd_source.left = new FormAttachment(0);
        fd_source.right = new FormAttachment(100, 0);
        fd_source.bottom = new FormAttachment(100, 0);
        source.setLayoutData(fd_source);
        final String initialText = Messages.ImportWizardPasteSourcePasteHere;
        source.setText(initialText);
        source.setSelection(0, source.getText().length());
        source.setFocus();

        // wiring

        setPageComplete(false);

        explanation.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent event)
            {
                DesktopAPI.browse(event.text);
            }
        });

        source.addModifyListener(new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent event)
            {
                setPageComplete(!initialText.equals(source.getText()) && source.getText().length() > 0);
            }
        });
    }

    public String getSourceText()
    {
        return source.getText();
    }
}
