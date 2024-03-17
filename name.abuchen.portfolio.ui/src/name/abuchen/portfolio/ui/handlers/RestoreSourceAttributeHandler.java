package name.abuchen.portfolio.ui.handlers;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.function.Consumer;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;

public class RestoreSourceAttributeHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, ClientInputFactory clientInputFactory)
    {
        var clientInput = MenuHelper.getActiveClientInput(part);
        if (clientInput.isEmpty())
            return;

        var client = clientInput.get().getClient();
        if (client == null)
            return;

        // ask user to pick another (opened) client

        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(),
                        LabelProvider.createTextProvider(element -> ((ClientInput) element).getLabel()));
        dialog.setTitle(Messages.LabelPickFile);
        dialog.setMessage(Messages.LabelChooseFileToImportFrom);
        dialog.setMultiSelection(false);
        dialog.setElements(clientInputFactory.listOpenClients().stream().filter(c -> c.getClient() != client).toList());

        if (dialog.open() != Window.OK)
            return;

        if (dialog.getResult().length != 1)
            return;

        var otherClient = ((ClientInput) dialog.getResult()[0]).getClient();

        // collect candidates from other other client into a map

        var uuid2tx = new HashMap<String, Transaction>();

        for (Portfolio portfolio : otherClient.getPortfolios())
            for (Transaction tx : portfolio.getTransactions())
                uuid2tx.put(tx.getUUID(), tx);

        for (Account account : otherClient.getAccounts())
            for (Transaction tx : account.getTransactions())
                uuid2tx.put(tx.getUUID(), tx);

        // check transaction of current client for missing source attributes

        int[] count = { 0 };

        Consumer<Transaction> fix = tx -> {
            var crossEntry = tx.getCrossEntry();

            var otherTx = uuid2tx.get(tx.getUUID());
            if (otherTx != null //
                            && otherTx.getSource() != null //
                            && !otherTx.getSource().isEmpty() //
                            && !otherTx.getSource().equals(tx.getSource()))
            {
                crossEntry.setSource(otherTx.getSource());
                count[0]++;
            }
        };

        for (Portfolio portfolio : client.getPortfolios())
            portfolio.getTransactions().forEach(fix);

        for (Account account : client.getAccounts())
            account.getTransactions().forEach(fix);

        if (count[0] > 0)
            client.markDirty();

        MessageDialog.openInformation(shell, Messages.LabelInfo,
                        MessageFormat.format(Messages.MsgUpdatedXEntries, count[0]));
    }
}
