package name.abuchen.portfolio.ui.dialogs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Messages;

public class DisplayTextDialog extends Dialog
{
    private File source;
    private String text;
    private Text widget;

    public DisplayTextDialog(Shell parentShell, String text)
    {
        this(parentShell, null, text);
    }

    public DisplayTextDialog(Shell parentShell, File source, String text)
    {
        super(parentShell);
        this.source = source;
        this.text = text;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

        Button button = createButton(parent, 9999, Messages.LabelCopyToClipboard, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (widget.isDisposed())
                return;
            Clipboard cb = new Clipboard(Display.getCurrent());
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { widget.getText() }, new Transfer[] { textTransfer });
        }));

        button = createButton(parent, 9998, Messages.LabelSaveInFile, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (widget.isDisposed())
                return;

            FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);

            if (source != null)
            {
                dialog.setFilterPath(source.getAbsolutePath());
                dialog.setFileName(source.getName() + ".txt"); //$NON-NLS-1$
            }
            else
            {
                dialog.setFileName(Messages.LabelUnnamedFile + ".txt"); //$NON-NLS-1$
            }

            dialog.setOverwrite(true);

            String path = dialog.open();
            if (path == null)
                return;

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8))
            {
                writer.write(widget.getText());
                writer.flush();
            }
            catch (IOException x)
            {
                throw new IllegalArgumentException(x);
            }
        }));
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = new Composite(parent, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(600, 200).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        widget = new Text(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
        widget.setText(text);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(widget);
        return container;
    }
}
