package name.abuchen.portfolio.ui.dialogs.transactions;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

public class OpenDialogAction extends Action
{
    private AbstractFinanceView owner;

    private Class<? extends AbstractTransactionDialog> type;
    private Consumer<? extends AbstractTransactionDialog> prepare;

    private Object[] parameters;
    private Account account;
    private Portfolio portfolio;
    private Security security;

    public OpenDialogAction(AbstractFinanceView owner, String label)
    {
        super(label);
        this.owner = owner;
    }

    public OpenDialogAction type(Class<? extends AbstractTransactionDialog> type)
    {
        this.type = type;
        this.prepare = null;
        return this;
    }

    public <D extends AbstractTransactionDialog> OpenDialogAction type(Class<D> type, Consumer<D> prepare)
    {
        this.type = type;
        this.prepare = prepare;
        return this;
    }

    public OpenDialogAction parameters(Object... parameters)
    {
        this.parameters = parameters;
        return this;
    }

    public OpenDialogAction with(Account account)
    {
        this.account = account;
        return this;
    }

    public OpenDialogAction with(Portfolio portfolio)
    {
        this.portfolio = portfolio;
        return this;
    }

    public OpenDialogAction with(Security security)
    {
        this.security = security;
        return this;
    }

    public void addTo(IMenuManager manager)
    {
        manager.add(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run()
    {
        Objects.requireNonNull(type);

        // The action can still be executed - triggered by a context menu, a key
        // binding or an entry of the command palette - after the owning view
        // has already been disposed, for example if the user switched to
        // another view in the meantime. Creating the dialog would then use a
        // disposed eclipse context while opening it would parent the dialog to
        // a disposed shell, both of which end in an internal error (#620).
        if (owner.isDisposed())
        {
            PortfolioPlugin.log(MessageFormat.format( // NOSONAR
                            "Skipped opening transaction dialog of type {0}: view {1} is already disposed", //$NON-NLS-1$
                            type.getSimpleName(), owner.getClass().getSimpleName()));
            return;
        }

        Object activeShell = owner.getContext().get(IServiceConstants.ACTIVE_SHELL);
        if (activeShell instanceof Shell shell && shell.isDisposed())
        {
            PortfolioPlugin.log(MessageFormat.format( // NOSONAR
                            "Skipped opening transaction dialog of type {0}: parent shell is already disposed", //$NON-NLS-1$
                            type.getSimpleName()));
            return;
        }

        AbstractTransactionDialog dialog = owner.make(type, parameters);

        if (prepare != null)
            ((Consumer<AbstractTransactionDialog>) prepare).accept(dialog);

        if (account != null)
            dialog.setAccount(account);
        if (portfolio != null)
            dialog.setPortfolio(portfolio);
        if (security != null)
            dialog.setSecurity(security);

        dialog.open();

        // the view might have been disposed while the (modal) dialog was open.
        // The dirty flag belongs to the editor and not to the view, therefore
        // it is always set.
        if (dialog.hasAtLeastOneSuccessfulEdit())
        {
            owner.markDirty();
        }
    }
}
