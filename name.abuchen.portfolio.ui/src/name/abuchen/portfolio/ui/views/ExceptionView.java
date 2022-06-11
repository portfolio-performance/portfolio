package name.abuchen.portfolio.ui.views;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

public class ExceptionView extends AbstractFinanceView
{
    private Exception exception;

    @Override
    protected String getDefaultTitle()
    {
        if (exception != null)
            return exception.getMessage();
        else
            return Messages.LabelError;
    }

    @Inject
    public void init(@Named(UIConstants.Parameter.VIEW_PARAMETER) Exception parameter)
    {
        this.exception = parameter;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        String text = createText();

        Composite container = new Composite(parent, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        WidgetFactory.button(SWT.PUSH) //
                        .text(Messages.LabelCopyToClipboard) //
                        .font(JFaceResources.getDialogFont()) //
                        .onSelect(event -> {
                            Clipboard cb = new Clipboard(Display.getCurrent());
                            TextTransfer textTransfer = TextTransfer.getInstance();
                            cb.setContents(new Object[] { "```\n" + text + "\n```" }, // //$NON-NLS-1$ //$NON-NLS-2$
                                            new Transfer[] { textTransfer });
                        }).create(container);

        Text entryText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
        entryText.setText(text);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(entryText);
        return container;
    }

    private String createText()
    {
        if (exception == null)
            return ""; //$NON-NLS-1$

        StringBuilder msg = new StringBuilder();

        if (exception.getMessage() != null)
            msg.append(exception.getMessage()).append("\n\n"); //$NON-NLS-1$

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        msg.append(sw.toString());

        return msg.toString();
    }

}
