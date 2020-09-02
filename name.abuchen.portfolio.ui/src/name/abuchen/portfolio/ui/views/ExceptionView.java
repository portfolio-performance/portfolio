package name.abuchen.portfolio.ui.views;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
        Composite container = new Composite(parent, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        Text entryText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
        entryText.setText(createText());
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
