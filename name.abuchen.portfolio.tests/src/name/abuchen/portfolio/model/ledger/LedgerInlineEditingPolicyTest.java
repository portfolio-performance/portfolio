package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingField;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingPolicy;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;

@SuppressWarnings("nls")
public class LedgerInlineEditingPolicyTest
{
    @Test
    public void testSuppliedInlineEditingMatrixRows()
    {
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.DEPOSIT, LedgerProjectionRole.ACCOUNT,
                        LedgerInlineEditingField.SOURCE), is(false));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.DEPOSIT, LedgerProjectionRole.ACCOUNT,
                        LedgerInlineEditingField.TRANSACTION_SOURCE), is(false));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.DEPOSIT, LedgerProjectionRole.ACCOUNT,
                        LedgerInlineEditingField.DATE), is(true));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.DIVIDENDS, LedgerProjectionRole.ACCOUNT,
                        LedgerInlineEditingField.EX_DATE), is(true));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.DIVIDENDS, LedgerProjectionRole.ACCOUNT,
                        LedgerInlineEditingField.SHARES), is(true));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.FEES, LedgerProjectionRole.ACCOUNT,
                        LedgerInlineEditingField.TYPE), is(false));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.BUY, LedgerProjectionRole.PORTFOLIO,
                        LedgerInlineEditingField.SHARES), is(false));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.DELIVERY_INBOUND,
                        LedgerProjectionRole.DELIVERY_INBOUND, LedgerInlineEditingField.TYPE), is(true));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.SPIN_OFF,
                        LedgerProjectionRole.DELIVERY_INBOUND, LedgerInlineEditingField.DATE), is(false));
        assertThat(LedgerInlineEditingPolicy.isEditable(LedgerEntryType.SPIN_OFF,
                        LedgerProjectionRole.CASH_COMPENSATION, LedgerInlineEditingField.SOURCE), is(false));
    }
}
