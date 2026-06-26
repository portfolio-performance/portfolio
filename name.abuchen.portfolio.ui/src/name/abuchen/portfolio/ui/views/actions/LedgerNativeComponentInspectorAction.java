package name.abuchen.portfolio.ui.views.actions;

import java.util.Optional;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.ledger.compatibility.LedgerNativeComponentInspectorModel;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.LedgerNativeComponentInspectorDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

/**
 * Opens the read-only Ledger entry inspector for a selected runtime projection.
 * The action is only created when the selected object resolves to a ledger-backed entry.
 */
public final class LedgerNativeComponentInspectorAction extends Action
{
    private final AbstractFinanceView owner;
    private final LedgerNativeComponentInspectorModel model;

    private LedgerNativeComponentInspectorAction(AbstractFinanceView owner, LedgerNativeComponentInspectorModel model)
    {
        super(Messages.LedgerNativeComponentInspectorMenu);
        this.owner = owner;
        this.model = model;
    }

    public static Optional<Action> create(AbstractFinanceView owner, Object transaction)
    {
        return LedgerNativeComponentInspectorModel.from(transaction)
                        .map(model -> new LedgerNativeComponentInspectorAction(owner, model));
    }

    @Override
    public void run()
    {
        new LedgerNativeComponentInspectorDialog(owner.getActiveShell(), model).open();
    }
}
