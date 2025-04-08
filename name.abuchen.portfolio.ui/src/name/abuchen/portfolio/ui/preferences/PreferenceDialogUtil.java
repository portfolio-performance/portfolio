package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/* package */ class PreferenceDialogUtil
{
    private PreferenceDialogUtil()
    {
    }

    /**
     * Creates a composite with a highlighted Note entry and a message text. In
     * contrast to the default method of the PreferencePage, this is designed to
     * limit the width of the message and word wrap the text.
     */
    /* package */ static void createNoteComposite(Font font, Composite composite, String title, String message)
    {
        Composite messageComposite = new Composite(composite, SWT.NONE);
        messageComposite.setFont(font);

        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(messageComposite);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).hint(400, SWT.DEFAULT).applyTo(messageComposite);

        final Label noteLabel = new Label(messageComposite, SWT.BOLD);
        noteLabel.setText(title);
        noteLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
        noteLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        Label messageLabel = new Label(messageComposite, SWT.WRAP);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(messageLabel);
        messageLabel.setText(message);
        messageLabel.setFont(font);
    }
}
