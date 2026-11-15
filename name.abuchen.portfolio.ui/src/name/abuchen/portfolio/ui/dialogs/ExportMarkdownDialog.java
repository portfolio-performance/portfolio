package name.abuchen.portfolio.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.markdown.PortfolioMarkdownExporter.TransactionScope;
import name.abuchen.portfolio.ui.Messages;

/**
 * Asks which parts of the portfolio report for AI assistants to include.
 */
public class ExportMarkdownDialog extends Dialog
{
    private static final TransactionScope[] SCOPES = { TransactionScope.LAST_12_MONTHS, TransactionScope.ALL,
                    TransactionScope.NONE };

    private TransactionScope transactionScope = TransactionScope.LAST_12_MONTHS;

    public ExportMarkdownDialog(Shell parentShell)
    {
        super(parentShell);
    }

    public TransactionScope getTransactionScope()
    {
        return transactionScope;
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.ExportMarkdownDialogTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        var composite = (Composite) super.createDialogArea(parent);

        var area = new Composite(composite, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 10).applyTo(area);

        var message = new Label(area, SWT.WRAP);
        message.setText(Messages.ExportMarkdownDialogMessage);
        GridDataFactory.fillDefaults().span(2, 1).hint(450, SWT.DEFAULT).applyTo(message);

        var label = new Label(area, SWT.NONE);
        label.setText(Messages.ExportMarkdownTransactions);

        var combo = new Combo(area, SWT.READ_ONLY | SWT.DROP_DOWN);
        for (var scope : SCOPES)
            combo.add(label(scope));
        combo.select(0);
        combo.addSelectionListener(SelectionListener
                        .widgetSelectedAdapter(e -> transactionScope = SCOPES[Math.max(0, combo.getSelectionIndex())]));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(combo);

        return composite;
    }

    private String label(TransactionScope scope)
    {
        return switch (scope)
        {
            case ALL -> Messages.ExportMarkdownTransactionsAll;
            case LAST_12_MONTHS -> Messages.ExportMarkdownTransactionsLastYear;
            case NONE -> Messages.ExportMarkdownTransactionsNone;
        };
    }
}
