package name.abuchen.portfolio.ui.dialogs;

import jakarta.inject.Inject;

import org.eclipse.e4.core.services.translation.TranslationService;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.ClientFileType;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.FormDataFactory;

public class PickFileFormatDialog extends Dialog
{
    @Inject
    private TranslationService translationService;

    private ClientFileType fileType = ClientFileType.BINARY_AES256;

    public PickFileFormatDialog(Shell parentShell)
    {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LabelPickFileFormat);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        editArea.setLayout(new FormLayout());

        Button passwordProtected = new Button(editArea, SWT.RADIO);
        passwordProtected.setText(translate("%command.saveAs.passwordProtected")); //$NON-NLS-1$
        passwordProtected.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        passwordProtected.setSelection(true);
        passwordProtected.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> this.fileType = ClientFileType.BINARY_AES256));
        Label passwordProtectedTooltip = new Label(editArea, SWT.WRAP);
        passwordProtectedTooltip.setText(translate("%command.saveAs.passwordProtected.tooltip")); //$NON-NLS-1$

        Button binary = new Button(editArea, SWT.RADIO);
        binary.setText(translate("%command.saveAs.binary")); //$NON-NLS-1$
        binary.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        binary.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> this.fileType = ClientFileType.BINARY));
        Label binaryTooltip = new Label(editArea, SWT.WRAP);
        binaryTooltip.setText(translate("%command.saveAs.binary.tooltip")); //$NON-NLS-1$

        Button xml = new Button(editArea, SWT.RADIO);
        xml.setText(translate("%command.saveAs.xml")); //$NON-NLS-1$
        xml.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        xml.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> this.fileType = ClientFileType.XML));

        Label xmlTooltip = new Label(editArea, SWT.WRAP);
        xmlTooltip.setText(translate("%command.saveAs.xml.tooltip")); //$NON-NLS-1$

        FormDataFactory.startingWith(passwordProtected).top(new FormAttachment(0, 10)).left(new FormAttachment(5))
                        .right(new FormAttachment(100, 5)) //
                        .thenBelow(passwordProtectedTooltip).left(new FormAttachment(15)).width(300)

                        .thenBelow(binary).left(passwordProtected) //
                        .thenBelow(binaryTooltip).left(new FormAttachment(15)).width(300)

                        .thenBelow(xml).left(passwordProtected) //
                        .thenBelow(xmlTooltip).left(new FormAttachment(15)).width(300);

        return composite;

    }

    public ClientFileType getSelectedType()
    {
        return fileType;
    }

    private String translate(String key)
    {
        return translationService.translate(key, "platform:/plugin/name.abuchen.portfolio.bootstrap"); //$NON-NLS-1$
    }
}
