package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingField;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingPolicy;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingPatch;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class ExDateEditingSupport extends ColumnEditingSupport
{
    private static final DateTimeFormatter[] dateFormatters = new DateTimeFormatter[] {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT), //
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG), //
                    DateTimeFormatter.ofPattern("d.M.yyyy"), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                    DateTimeFormatter.ISO_DATE };

    private final Client client;

    public ExDateEditingSupport()
    {
        this(null);
    }

    public ExDateEditingSupport(Client client)
    {
        this.client = client;
    }

    @Override
    public boolean canEdit(Object element)
    {
        if (isLedgerNativeTargetedProjection(element))
            return false;
        if (!LedgerInlineEditingPolicy.isEditable(element, LedgerInlineEditingField.EX_DATE))
            return false;

        var tx = Adaptor.adapt(AccountTransaction.class, element);
        if (tx == null || tx.getSecurity() == null)
            return false;

        return canUpdateLedgerBackedDividend(tx) || !isLedgerBackedAccountTransaction(tx);
    }

    @Override
    public Object getValue(Object element)
    {
        var tx = Adaptor.adapt(AccountTransaction.class, element);
        var exDate = tx.getExDate();
        return exDate != null ? Values.Date.format(exDate.toLocalDate()) : ""; //$NON-NLS-1$
    }

    @Override
    public void setValue(Object element, Object value)
    {
        var tx = Adaptor.adapt(AccountTransaction.class, element);

        var oldValue = tx.getExDate();

        var inputValue = ((String) value).trim();

        // ex-date is an optional value, therefore we remove the ex-date if not
        // input is given
        if (inputValue.isEmpty())
        {
            if (oldValue != null)
            {
                updateExDate(tx, null);
                notify(element, null, oldValue);
            }
            return;
        }

        LocalDateTime newValue = null;
        for (DateTimeFormatter formatter : dateFormatters)
        {
            try
            {
                newValue = LocalDate.parse(inputValue, formatter).atStartOfDay();
                break;
            }
            catch (DateTimeParseException ignore)
            {
                // continue with next formatter
            }
        }

        // there is text input, but it cannot be parsed as a local date -> throw
        // an error
        if (newValue == null)
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value));

        if (!newValue.equals(oldValue))
        {
            updateExDate(tx, newValue);
            notify(element, newValue, oldValue);
        }
    }

    private void updateExDate(AccountTransaction transaction, LocalDateTime exDate)
    {
        if (!LedgerInlineEditingPolicy.isEditable(transaction, LedgerInlineEditingField.EX_DATE))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_009
                                            .message(Messages.LedgerExDateEditingSupportNoSafeEditorPolicyBlocked));

        if (updateLedgerBackedDividend(transaction, exDate))
            return;

        if (isLedgerBackedAccountTransaction(transaction))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_010
                                            .message(Messages.LedgerExDateEditingSupportNoSafeEditorLedgerBacked));

        transaction.setExDate(exDate);
    }

    private boolean updateLedgerBackedDividend(AccountTransaction transaction, LocalDateTime exDate)
    {
        if (client == null)
            return false;

        if (!canUpdateLedgerBackedDividend(transaction))
            return false;

        new LedgerDividendTransactionCreator(client).update(transaction, ownerOf(transaction), transaction.getType(),
                        transaction.getDateTime(), transaction.getAmount(), transaction.getCurrencyCode(),
                        transaction.getSecurity(), transaction.getShares(), exDate, null,
                        LedgerUnitPostingPatch.none(), transaction.getNote(), transaction.getSource());
        return true;
    }

    private boolean canUpdateLedgerBackedDividend(AccountTransaction transaction)
    {
        return client != null && new LedgerDividendTransactionCreator(client).canUpdate(transaction);
    }

    private Account ownerOf(AccountTransaction transaction)
    {
        return client.getAccounts().stream().filter(account -> account.getTransactions().contains(transaction))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        Messages.LedgerExDateEditingSupportDividendOwnerNotFound));
    }

    private boolean isLedgerBackedAccountTransaction(AccountTransaction transaction)
    {
        if (client == null)
            return false;

        if (new LedgerAccountOnlyTransactionCreator(client).canUpdate(transaction))
            return true;

        if (transaction.getCrossEntry() instanceof BuySellEntry entry
                        && new LedgerBuySellTransactionCreator(client).isLedgerBacked(entry))
            return true;

        return transaction.getCrossEntry() instanceof AccountTransferEntry entry
                        && new LedgerAccountTransferTransactionCreator(client).isLedgerBacked(entry);
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        ((Text) textEditor.getControl()).setTextLimit(20);
        return textEditor;
    }
}
