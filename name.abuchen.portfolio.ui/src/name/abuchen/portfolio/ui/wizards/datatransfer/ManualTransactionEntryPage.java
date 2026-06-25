package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.UnrecognizedPDFCache;
import name.abuchen.portfolio.ui.util.swt.PDFViewer;
import name.abuchen.portfolio.ui.dialogs.transactions.AbstractTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

@SuppressWarnings("nls")
public class ManualTransactionEntryPage extends AbstractWizardPage
{
    private static class TransactionType
    {
        final String label;
        final Class<? extends AbstractTransactionDialog> dialogClass;
        final Object typeParam;

        TransactionType(String label, Class<? extends AbstractTransactionDialog> dialogClass, Object typeParam)
        {
            this.label = label;
            this.dialogClass = dialogClass;
            this.typeParam = typeParam;
        }
    }

    // most common transaction types, shown as dedicated buttons
    private static final List<TransactionType> PRIMARY_TYPES = List.of(
                    new TransactionType(PortfolioTransaction.Type.BUY.toString(), SecurityTransactionDialog.class,
                                    PortfolioTransaction.Type.BUY),
                    new TransactionType(PortfolioTransaction.Type.SELL.toString(), SecurityTransactionDialog.class,
                                    PortfolioTransaction.Type.SELL),
                    new TransactionType(AccountTransaction.Type.DIVIDENDS.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.DIVIDENDS));

    // remaining transaction types, offered via the "More" drop-down menu
    private static final List<TransactionType> MORE_TYPES = List.of(
                    new TransactionType(PortfolioTransaction.Type.DELIVERY_INBOUND.toString(),
                                    SecurityTransactionDialog.class, PortfolioTransaction.Type.DELIVERY_INBOUND),
                    new TransactionType(PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString(),
                                    SecurityTransactionDialog.class, PortfolioTransaction.Type.DELIVERY_OUTBOUND),
                    new TransactionType(AccountTransaction.Type.DEPOSIT.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.DEPOSIT),
                    new TransactionType(AccountTransaction.Type.REMOVAL.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.REMOVAL),
                    new TransactionType(AccountTransaction.Type.INTEREST.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.INTEREST),
                    new TransactionType(AccountTransaction.Type.INTEREST_CHARGE.toString(),
                                    AccountTransactionDialog.class, AccountTransaction.Type.INTEREST_CHARGE),
                    new TransactionType(AccountTransaction.Type.FEES.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.FEES),
                    new TransactionType(AccountTransaction.Type.FEES_REFUND.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.FEES_REFUND),
                    new TransactionType(AccountTransaction.Type.TAXES.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.TAXES),
                    new TransactionType(AccountTransaction.Type.TAX_REFUND.toString(), AccountTransactionDialog.class,
                                    AccountTransaction.Type.TAX_REFUND),
                    new TransactionType(name.abuchen.portfolio.Messages.LabelTransferAccount,
                                    AccountTransferDialog.class, null),
                    new TransactionType(name.abuchen.portfolio.Messages.LabelTransferPortfolio,
                                    SecurityTransferDialog.class, null));

    private final PortfolioPart part;
    private final Client client;
    private final List<Security> additionalSecurities;
    private final PDFInputFile inputFile;
    private final Account targetAccount;
    private final Portfolio targetPortfolio;
    private final UnrecognizedPDFCache debugCache;

    private final List<ExtractedEntry> entries = new ArrayList<>();

    private PDFViewer pdfViewer;
    private ExtractedItemsTable itemsTable;
    private Composite buttonRow;
    private boolean editorOpen;
    private boolean capturedForDebug;

    public ManualTransactionEntryPage(PortfolioPart part, Client client, List<Security> additionalSecurities,
                    PDFInputFile inputFile, Account targetAccount, Portfolio targetPortfolio,
                    UnrecognizedPDFCache debugCache)
    {
        super("manual-" + inputFile.getName());

        this.part = part;
        this.client = client;
        this.additionalSecurities = additionalSecurities;
        this.inputFile = inputFile;
        this.targetAccount = targetAccount;
        this.targetPortfolio = targetPortfolio;
        this.debugCache = debugCache;

        setTitle(MessageFormat.format(Messages.PDFImportWizardManualEntryTitle, inputFile.getName()));
        setDescription(Messages.PDFImportWizardManualEntryDescription);
    }

    @Override
    public void createControl(Composite parent)
    {
        var sash = new SashForm(parent, SWT.HORIZONTAL);
        setControl(sash);

        pdfViewer = new PDFViewer(sash, SWT.NONE, inputFile);
        createRightPane(sash);

        sash.setWeights(50, 50);
    }

    @Override
    public void beforePage()
    {
        pdfViewer.initialize();
    }

    @Override
    public void afterPage()
    {
        // free the document and cached page images so that only the currently
        // viewed document stays in memory
        pdfViewer.release();
    }

    private void createRightPane(Composite parent)
    {
        var container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // one button per common transaction type plus a "More" drop-down for
        // the remaining types
        buttonRow = new Composite(container, SWT.NONE);
        buttonRow.setLayout(new GridLayout(PRIMARY_TYPES.size() + 1, false));
        buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (TransactionType type : PRIMARY_TYPES)
        {
            var button = new Button(buttonRow, SWT.PUSH);
            button.setText(type.label);
            button.addListener(SWT.Selection, e -> openDialog(type));
        }

        var moreButton = new Button(buttonRow, SWT.PUSH);
        moreButton.setText(Messages.LabelMore);

        var moreMenu = new Menu(moreButton);
        for (TransactionType type : MORE_TYPES)
        {
            var item = new MenuItem(moreMenu, SWT.PUSH);
            item.setText(type.label);
            item.addListener(SWT.Selection, e -> openDialog(type));
        }

        moreButton.addListener(SWT.Selection, e -> {
            var bounds = moreButton.getBounds();
            moreMenu.setLocation(moreButton.getParent().toDisplay(bounds.x, bounds.y + bounds.height));
            moreMenu.setVisible(true);
        });

        // Transaction table
        var tableComposite = new Composite(container, SWT.NONE);
        tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        itemsTable = new ExtractedItemsTable(tableComposite, client, entries);
        itemsTable.setOnEdit(this::editEntry);
        itemsTable.setOnDelete(selected -> {
            entries.removeAll(selected);
            itemsTable.refresh();
        });

        itemsTable.getTableViewer().setInput(entries);
    }

    private void openDialog(TransactionType type)
    {
        // only one transaction editor may be open at a time (see openModeless)
        if (editorOpen)
            return;

        var session = ShadowSession.create(client, additionalSecurities);

        AbstractTransactionDialog dialog;
        if (type.typeParam != null)
            dialog = part.make(type.dialogClass, getShell(), type.typeParam, session.getClient());
        else
            dialog = part.make(type.dialogClass, getShell(), session.getClient());

        // preselect the wizard's target account/portfolio (mapped to the shadow
        // client)
        var shadowAccount = session.toShadow(targetAccount);
        if (shadowAccount != null)
            dialog.setAccount(shadowAccount);

        var shadowPortfolio = session.toShadow(targetPortfolio);
        if (shadowPortfolio != null)
            dialog.setPortfolio(shadowPortfolio);

        openModeless(dialog, () -> entries.addAll(session.harvest()));
    }

    private void editEntry(ExtractedEntry extractedEntry)
    {
        // only one transaction editor may be open at a time (see openModeless);
        // this also blocks the table's "Edit" context menu while editing
        if (editorOpen)
            return;

        var item = extractedEntry.getItem();
        var subject = item.getSubject();

        var session = ShadowSession.create(client, additionalSecurities);

        AbstractTransactionDialog dialog = null;

        // rebuild the subject in the session's shadow coordinates so the dialog
        // can preselect the account/portfolio; the stored subject references
        // the real client
        if (subject instanceof BuySellEntry)
        {
            var shadowEntry = session.shadowBuySell(item);
            var type = shadowEntry.getPortfolioTransaction().getType();
            dialog = part.make(SecurityTransactionDialog.class, getShell(), type, session.getClient());
            ((SecurityTransactionDialog) dialog).presetBuySellEntry(shadowEntry);
        }
        else if (subject instanceof AccountTransaction at)
        {
            dialog = part.make(AccountTransactionDialog.class, getShell(), at.getType(), session.getClient());
            var shadowAccount = session.toShadow(item.getAccountPrimary());
            if (shadowAccount != null)
                ((AccountTransactionDialog) dialog).presetTransaction(shadowAccount, at);
        }
        else if (subject instanceof PortfolioTransaction pt)
        {
            dialog = part.make(SecurityTransactionDialog.class, getShell(), pt.getType(), session.getClient());
            var shadowPortfolio = session.toShadow(item.getPortfolioPrimary());
            if (shadowPortfolio != null)
                ((SecurityTransactionDialog) dialog)
                                .presetDeliveryTransaction(new TransactionPair<>(shadowPortfolio, pt));
        }
        else if (subject instanceof AccountTransferEntry)
        {
            var shadowEntry = session.shadowAccountTransfer(item);
            dialog = part.make(AccountTransferDialog.class, getShell(), session.getClient());
            ((AccountTransferDialog) dialog).presetEntry(shadowEntry);
        }
        else if (subject instanceof PortfolioTransferEntry)
        {
            var shadowEntry = session.shadowPortfolioTransfer(item);
            dialog = part.make(SecurityTransferDialog.class, getShell(), session.getClient());
            ((SecurityTransferDialog) dialog).presetEntry(shadowEntry);
        }

        if (dialog == null)
            return;

        var theDialog = dialog;
        openModeless(theDialog, () -> {
            // replace the edited entry in place: any additional transactions
            // created via "Save and new" follow right after it
            var index = entries.indexOf(extractedEntry);
            if (index < 0)
            {
                // the entry is no longer in the list (should not happen while
                // the page input is frozen, but guard against it): append the
                // harvested transactions instead of inserting at -1
                entries.addAll(session.harvest());
            }
            else
            {
                entries.remove(extractedEntry);
                entries.addAll(index, session.harvest());
            }
        });
    }

    /**
     * Opens a transaction dialog modeless (so the PDF text view stays
     * selectable / copyable) and non-blocking, freezing the wizard and the
     * page's own type-buttons until the dialog closes. The harvest runs in the
     * dialog shell's dispose listener.
     * <p>
     * The dispose listener must read <em>only</em>
     * {@link AbstractTransactionDialog#hasAtLeastOneSuccessfulEdit()} and the
     * shadow session; the transaction was already committed by
     * {@code model.applyChanges()} before the shell closed, so it must never
     * touch the (now disposed) dialog widgets or its data binding context.
     */
    private void openModeless(AbstractTransactionDialog dialog, Runnable onSuccessfulEdit)
    {
        var importDialog = getImportWizardDialog();

        dialog.setModeless(true);
        dialog.open();

        // Freeze only after open() succeeded: if dialog creation/opening threw,
        // the page was never frozen and there is nothing to undo.
        //
        // Exactly one editor at a time: the flag blocks a second editor from the
        // buttons or the table's "Edit" context menu (see openDialog/editEntry).
        // The wizard chrome is frozen and the type-buttons disabled so the user
        // cannot finish/navigate or start a new transaction mid-edit. The table
        // itself stays usable: deleting or re-assigning other rows is harmless
        // because the target row's index is (re)computed when the editor closes.
        editorOpen = true;
        if (importDialog != null)
            importDialog.setEditing(true);
        setButtonRowEnabled(false);

        dialog.getShell().addDisposeListener(e -> {
            try
            {
                if (dialog.hasAtLeastOneSuccessfulEdit())
                {
                    onSuccessfulEdit.run();
                    if (!capturedForDebug && !entries.isEmpty())
                    {
                        debugCache.add(inputFile.getName(), inputFile.getText(), inputFile.getPDFBoxVersion());
                        capturedForDebug = true;
                    }
                    if (!itemsTable.getTableViewer().getControl().isDisposed())
                        itemsTable.refresh();
                }
            }
            finally
            {
                // unfreeze unless the wizard itself is being torn down
                if (importDialog != null && !importDialog.getShell().isDisposed())
                {
                    importDialog.setEditing(false);
                    getContainer().updateButtons();
                }
                if (!buttonRow.isDisposed())
                    setButtonRowEnabled(true);
                editorOpen = false;
            }
        });
    }

    private ImportWizardDialog getImportWizardDialog()
    {
        var container = getContainer();
        return container instanceof ImportWizardDialog d ? d : null;
    }

    private void setButtonRowEnabled(boolean enabled)
    {
        for (var child : buttonRow.getChildren())
            child.setEnabled(enabled);
    }

    public List<Extractor.Item> getItems()
    {
        return entries.stream().filter(ExtractedEntry::isImported).map(ExtractedEntry::getItem).toList();
    }

    public boolean hasCreatedTransactions()
    {
        return !entries.isEmpty();
    }

}
