package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.junit.Test;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

/**
 * The action must not create or open a dialog if the owning view has already
 * been disposed (for example when the user switches the view while the action
 * is still triggered through a menu or the command palette).
 */
@SuppressWarnings("nls")
public class OpenDialogActionTest
{
    /**
     * A view without a view control: it behaves like a view that has never been
     * shown or that has already been disposed.
     */
    private static class ViewWithoutControl extends AbstractFinanceView
    {
        @Override
        protected String getDefaultTitle()
        {
            return "test view";
        }

        @Override
        protected Control createBody(Composite parent)
        {
            return null;
        }
    }

    @Test
    public void viewWithoutViewControlIsDisposed()
    {
        assertThat(new ViewWithoutControl().isDisposed(), is(true));
    }

    @Test
    public void runIsSkippedIfViewIsDisposed()
    {
        var action = new OpenDialogAction(new ViewWithoutControl(), "new transaction") //
                        .type(AccountTransactionDialog.class);

        // before the guard was added, the action tried to instantiate the
        // dialog through the (already disposed) eclipse context of the view and
        // failed with an exception
        action.run();
    }
}
