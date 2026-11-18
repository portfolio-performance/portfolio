package name.abuchen.portfolio.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.markdown.PortfolioMarkdownExporter.TransactionScope;
import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.ui.Messages;

/**
 * Asks which parts of the portfolio report for AI assistants to include.
 */
public class ExportMarkdownDialog extends Dialog
{
    private static final TransactionScope[] SCOPES = { TransactionScope.LAST_12_MONTHS, TransactionScope.ALL,
                    TransactionScope.NONE };

    private TransactionScope transactionScope = TransactionScope.LAST_12_MONTHS;

    /**
     * Preset with the discreet mode of the application; changing it only
     * affects the export, not the application.
     */
    private boolean discreetMode = DiscreetMode.isActive();

    public ExportMarkdownDialog(Shell parentShell)
    {
        super(parentShell);
    }

    public TransactionScope getTransactionScope()
    {
        return transactionScope;
    }

    public boolean isDiscreetMode()
    {
        return discreetMode;
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
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

        // radio buttons instead of a drop-down: the native drop-down list of a
        // read-only combo is not readable in the dark theme on Windows
        var scopes = new Composite(area, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(scopes);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(scopes);

        for (var scope : SCOPES)
        {
            var button = new Button(scopes, SWT.RADIO);
            button.setText(label(scope));
            button.setSelection(scope == transactionScope);
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                if (button.getSelection())
                    transactionScope = scope;
            }));
        }

        var discreet = new Button(area, SWT.CHECK);
        discreet.setText(Messages.ExportMarkdownDiscreetMode);
        discreet.setSelection(discreetMode);
        discreet.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> discreetMode = discreet.getSelection()));
        GridDataFactory.fillDefaults().span(2, 1).applyTo(discreet);

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
