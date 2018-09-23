package name.abuchen.portfolio.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Messages;

public class DisplayTextDialog extends Dialog
{
    private String entry;
    private Text entryText;

    public DisplayTextDialog(Shell parentShell, String text)
    {
        super(parentShell);
        this.entry = text;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        Button button = createButton(parent, 9999, Messages.LabelCopyToClipboard, false);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (entryText.isDisposed())
                    return;
                Clipboard cb = new Clipboard(Display.getCurrent());
                TextTransfer textTransfer = TextTransfer.getInstance();
                cb.setContents(new Object[] { entryText.getText() }, new Transfer[] { textTransfer });
            }
        });
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = new Composite(parent, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(600, 200).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        entryText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
        entryText.setText(entry);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(entryText);
        return container;
    }
}