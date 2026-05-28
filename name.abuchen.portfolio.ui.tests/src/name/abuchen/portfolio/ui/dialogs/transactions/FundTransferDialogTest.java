package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

public class FundTransferDialogTest
{
    @Test
    public void testDialogExposesFundTransferWorkflowHooks() throws NoSuchMethodException
    {
        assertThat(AbstractTransactionDialog.class.isAssignableFrom(FundTransferDialog.class), is(true));

        assertThat(FundTransferDialog.class.getMethod("setEntry", FundTransferEntry.class).getReturnType(),
                        is(Void.TYPE));
        assertThat(FundTransferDialog.class.getMethod("presetEntry", FundTransferEntry.class).getReturnType(),
                        is(Void.TYPE));
        assertThat(FundTransferDialog.class.getMethod("setSecurity", Security.class).getReturnType(), is(Void.TYPE));
        assertThat(FundTransferDialog.class.getMethod("setPortfolio", Portfolio.class).getReturnType(), is(Void.TYPE));
    }
}
