package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.datatransfer.actions.MarkNonImportableAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ReviewExtractedItemsPage extends AbstractWizardPage implements ImportAction.Context
{

    private static final String IMPORT_TARGET = "import-target"; //$NON-NLS-1$
    private static final String IMPORT_TARGET_PORTFOLIO = IMPORT_TARGET + "-portfolio-"; //$NON-NLS-1$
    private static final String IMPORT_TARGET_ACCOUNT = IMPORT_TARGET + "-account-"; //$NON-NLS-1$

    /**
     * If embedded into the CSV import, the first page can change the parsing
     * result and transactions must be extracted before every page. If embedded
     * into the PDF or XML import wizard, do not extract transactions again.
     */
    private boolean doExtractBeforeEveryPageDisplay = false;

    private TableViewer tableViewer;
    private TableViewer errorTableViewer;

    private ComboViewer primaryPortfolio;
    private Label lblSecondaryPortfolio;
    private ComboViewer secondaryPortfolio;
    private ComboViewer primaryAccount;
    private Label lblSecondaryAccount;
    private ComboViewer secondaryAccount;
    private Button cbConvertToDelivery;
    private Button cbRemoveDividends;

    private final Client client;
    private final Extractor extractor;
    private final IPreferenceStore preferences;
    private List<Extractor.InputFile> files;
    private Account account;
    private Portfolio portfolio;

    private List<ExtractedEntry> allEntries = new ArrayList<>();

    private List<Exception> extractionErrors = new ArrayList<>();

    public ReviewExtractedItemsPage(Client client, Extractor extractor, IPreferenceStore preferences,
                    List<Extractor.InputFile> files, String pageId)
    {
        super(pageId);

        this.client = client;
        this.extractor = extractor;
        this.preferences = preferences;
        this.files = files;

        setTitle(extractor.getLabel());
        setDescription(Messages.PDFImportWizardDescription);
    }

    public ReviewExtractedItemsPage(Client client, Extractor extractor, IPreferenceStore preferences,
                    List<Extractor.InputFile> files)
    {
        this(client, extractor, preferences, files, extractor.getLabel());
    }

    public void setDoExtractBeforeEveryPageDisplay(boolean doExtractBeforeEveryPageDisplay)
    {
        this.doExtractBeforeEveryPageDisplay = doExtractBeforeEveryPageDisplay;
    }

    public List<ExtractedEntry> getEntries()
    {
        return allEntries;
    }

    @Override
    public Portfolio getPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) primaryPortfolio.getSelection()).getFirstElement();
    }

    @Override
    public Portfolio getSecondaryPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) secondaryPortfolio.getSelection()).getFirstElement();
    }

    @Override
    public Account getAccount()
    {
        return (Account) ((IStructuredSelection) primaryAccount.getSelection()).getFirstElement();
    }

    @Override
    public Account getSecondaryAccount()
    {
        return (Account) ((IStructuredSelection) secondaryAccount.getSelection()).getFirstElement();
    }

    public boolean doConvertToDelivery()
    {
        return cbConvertToDelivery.getSelection();
    }

    public boolean doRemoveDividends()
    {
        return cbRemoveDividends.getSelection();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Composite targetContainer = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).applyTo(targetContainer);

        Label lblPrimaryAccount = new Label(targetContainer, SWT.NONE);
        lblPrimaryAccount.setText(Messages.ColumnAccount);
        Combo cmbAccount = new Combo(targetContainer, SWT.READ_ONLY);
        primaryAccount = new ComboViewer(cmbAccount);
        primaryAccount.setContentProvider(ArrayContentProvider.getInstance());
        primaryAccount.setInput(client.getActiveAccounts());
        primaryAccount.addSelectionChangedListener(e -> checkEntriesAndRefresh(allEntries));

        lblSecondaryAccount = new Label(targetContainer, SWT.NONE);
        lblSecondaryAccount.setText(Messages.LabelTransferTo);
        lblSecondaryAccount.setVisible(false);
        Combo cmbAccountTarget = new Combo(targetContainer, SWT.READ_ONLY);
        secondaryAccount = new ComboViewer(cmbAccountTarget);
        secondaryAccount.setContentProvider(ArrayContentProvider.getInstance());
        secondaryAccount.setInput(client.getActiveAccounts());
        secondaryAccount.getControl().setVisible(false);

        Label lblPrimaryPortfolio = new Label(targetContainer, SWT.NONE);
        lblPrimaryPortfolio.setText(Messages.ColumnPortfolio);
        Combo cmbPortfolio = new Combo(targetContainer, SWT.READ_ONLY);
        primaryPortfolio = new ComboViewer(cmbPortfolio);
        primaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        primaryPortfolio.setInput(client.getActivePortfolios());
        primaryPortfolio.addSelectionChangedListener(e -> checkEntriesAndRefresh(allEntries));

        lblSecondaryPortfolio = new Label(targetContainer, SWT.NONE);
        lblSecondaryPortfolio.setText(Messages.LabelTransferTo);
        lblSecondaryPortfolio.setVisible(false);
        Combo cmbPortfolioTarget = new Combo(targetContainer, SWT.READ_ONLY);
        secondaryPortfolio = new ComboViewer(cmbPortfolioTarget);
        secondaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        secondaryPortfolio.setInput(client.getActivePortfolios());
        secondaryPortfolio.getControl().setVisible(false);

        preselectDropDowns();

        cbConvertToDelivery = new Button(container, SWT.CHECK);
        cbConvertToDelivery.setText(Messages.LabelConvertBuySellIntoDeliveryTransactions);

        cbRemoveDividends = new Button(container, SWT.CHECK);
        cbRemoveDividends.setText(Messages.LabelRemoveDividends);

        Composite compositeTable = new Composite(container, SWT.NONE);
        Composite errorTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        FormDataFactory.startingWith(targetContainer) //
                        .top(new FormAttachment(0, 0)).left(new FormAttachment(0, 0)).right(new FormAttachment(100, 0))
                        .thenBelow(cbConvertToDelivery) //
                        .thenRight(cbRemoveDividends);

        FormDataFactory.startingWith(cbConvertToDelivery) //
                        .thenBelow(compositeTable).right(targetContainer).bottom(new FormAttachment(80, 0)) //
                        .thenBelow(errorTable).right(targetContainer).bottom(new FormAttachment(100, 0));

        //
        // table & columns
        //

        TableColumnLayout layout = new TableColumnLayout();
        compositeTable.setLayout(layout);

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(tableViewer);

        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumns(tableViewer, layout);
        attachContextMenu(table);

        layout = new TableColumnLayout();
        errorTable.setLayout(layout);
        errorTableViewer = new TableViewer(errorTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        CopyPasteSupport.enableFor(errorTableViewer);
        errorTableViewer.setContentProvider(ArrayContentProvider.getInstance());

        table = errorTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        addColumnsExceptionTable(errorTableViewer, layout);
    }

    private void preselectDropDowns()
    {
        // idea: generally one type of document (i.e. from the same bank) will
        // be imported into the same account

        List<Account> activeAccounts = client.getActiveAccounts();
        if (!activeAccounts.isEmpty())
        {
            String uuid = account != null ? account.getUUID()
                            : preferences.getString(IMPORT_TARGET_ACCOUNT + extractor.getLabel());

            // do not trigger selection listener (-> do not user #setSelection)
            primaryAccount.getCombo().select(IntStream.range(0, activeAccounts.size())
                            .filter(i -> activeAccounts.get(i).getUUID().equals(uuid)).findAny().orElse(0));
            secondaryAccount.getCombo().select(0);
        }

        List<Portfolio> activePortfolios = client.getActivePortfolios();
        if (!activePortfolios.isEmpty())
        {
            String uuid = portfolio != null ? portfolio.getUUID()
                            : preferences.getString(IMPORT_TARGET_PORTFOLIO + extractor.getLabel());
            // do not trigger selection listener (-> do not user #setSelection)
            primaryPortfolio.getCombo().select(IntStream.range(0, activePortfolios.size())
                            .filter(i -> activePortfolios.get(i).getUUID().equals(uuid)).findAny().orElse(0));
            secondaryPortfolio.getCombo().select(0);
        }
    }

    private void addColumnsExceptionTable(TableViewer viewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnErrorMessages);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Exception e = (Exception) element;
                String text = e.getMessage();
                return text == null || text.isEmpty() ? e.getClass().getName() : text;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100, true));
    }

    private void addColumns(TableViewer viewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnStatus);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public Image getImage(ExtractedEntry entry)
            {
                Images image = null;
                switch (entry.getMaxCode())
                {
                    case WARNING:
                        image = Images.WARNING;
                        break;
                    case ERROR:
                        image = Images.ERROR;
                        break;
                    case OK:
                    default:
                }
                return image != null ? image.image() : null;
            }

            @Override
            public String getToolTipText(Object entry)
            {
                List<String> messages = new ArrayList<String>();
                ((ExtractedEntry) entry).getStatus() //
                                .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                                .forEach(s -> {
                                    if (s.getMessage() != null)
                                        messages.add(s.getMessage());
                                });
                String message = null;
                for (String m : messages)
                {
                    if (message != null && message.length() > 0)
                        message = message.concat("\n").concat(m); //$NON-NLS-1$
                    else
                        message = m;
                }
                return message;
            }

            @Override
            public String getText(ExtractedEntry entry)
            {
                return ""; //$NON-NLS-1$
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(22, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                LocalDateTime date = entry.getItem().getDate();
                return date != null ? Values.DateTime.format(date) : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return entry.getItem().getTypeInformation();
            }

            @Override
            public String getToolTipText(Object entry)
            {
                List<String> messages = new ArrayList<String>();
                ((ExtractedEntry) entry).getStatus() //
                                .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                                .forEach(s -> {
                                    if (s.getMessage() != null)
                                        messages.add(s.getMessage());
                                });
                String message = null;
                for (String m : messages)
                {
                    if (message != null && message.length() > 0)
                        message = message.concat("\n").concat(m); //$NON-NLS-1$
                    else
                        message = m;
                }
                return message;
            }

            @Override
            public Image getImage(ExtractedEntry entry)
            {
                Annotated subject = entry.getItem().getSubject();
                if (subject instanceof AccountTransaction)
                    return Images.ACCOUNT.image();
                else if (subject instanceof PortfolioTransaction)
                    return Images.PORTFOLIO.image();
                else if (subject instanceof Security)
                    return Images.SECURITY.image();
                else if (subject instanceof BuySellEntry)
                    return Images.PORTFOLIO.image();
                else if (subject instanceof AccountTransferEntry)
                    return Images.ACCOUNT.image();
                else if (subject instanceof PortfolioTransferEntry)
                    return Images.PORTFOLIO.image();
                else
                    return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Money amount = entry.getItem().getAmount();
                return amount != null ? Values.Money.format(amount) : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return Values.Share.formatNonZero(entry.getItem().getShares());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Security security = entry.getItem().getSecurity();
                return security != null ? security.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(250, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnAccount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Account account = entry.getItem().getAccountPrimary();
                return account != null ? account.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnOffsetAccount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Account account = entry.getItem().getAccountSecondary();
                return account != null ? account.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnPortfolio);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Portfolio portfolio = entry.getItem().getPortfolioPrimary();
                return portfolio != null ? portfolio.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnOffsetPortfolio);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Portfolio portfolio = entry.getItem().getPortfolioSecondary();
                return portfolio != null ? portfolio.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));
    }

    private void attachContextMenu(final Table table)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(this::showContextMenu);

        final Menu contextMenu = menuMgr.createContextMenu(table.getShell());
        table.setMenu(contextMenu);

        table.addDisposeListener(e -> {
            if (contextMenu != null && !contextMenu.isDisposed())
                contextMenu.dispose();
        });
    }

    private void showContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();

        if (selection.isEmpty())
            return;

        boolean atLeastOneImported = false;
        boolean atLeastOneNotImported = false;

        for (Object element : selection.toList())
        {
            ExtractedEntry entry = (ExtractedEntry) element;

            // an entry will be imported if has a status code OK *or* if it is
            // marked as to be imported by the user
            atLeastOneImported = atLeastOneImported || entry.isImported();

            // an entry will not be imported if it marked as not to be
            // imported *or* if it has a WARNING code (e.g. is a duplicate)
            atLeastOneNotImported = atLeastOneNotImported
                            || (!entry.isImported() && (entry.getMaxCode() != Code.ERROR));
        }

        // provide a hint to the user why the entry is struck out
        if (selection.size() == 1)
        {
            ExtractedEntry entry = (ExtractedEntry) selection.getFirstElement();
            entry.getStatus() //
                            .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                            .forEach(s -> {
                                Images image = s.getCode() == ImportAction.Status.Code.WARNING ? //
                                                Images.WARNING : Images.ERROR;
                                manager.add(new LabelOnly(s.getMessage(), image.descriptor()));
                            });
        }

        if (atLeastOneImported)
        {
            manager.add(new SimpleAction(Messages.LabelDoNotImport, a -> {
                for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                    ((ExtractedEntry) element).setImported(false);

                tableViewer.refresh();
            }));
        }

        if (atLeastOneNotImported)
        {
            manager.add(new SimpleAction(Messages.LabelDoImport, a -> {
                for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                    ((ExtractedEntry) element).setImported(true);

                tableViewer.refresh();
            }));
        }

        manager.add(new Separator());

        showApplyToAllItemsMenu(manager, Messages.ColumnAccount, client::getAccounts,
                        (item, account) -> item.setAccountPrimary(account));
        showApplyToAllItemsMenu(manager, Messages.ColumnOffsetAccount, client::getAccounts,
                        (item, account) -> item.setAccountSecondary(account));

        showApplyToAllItemsMenu(manager, Messages.ColumnPortfolio, client::getPortfolios,
                        (item, portfolio) -> item.setPortfolioPrimary(portfolio));
        showApplyToAllItemsMenu(manager, Messages.ColumnOffsetPortfolio, client::getPortfolios,
                        (item, portfolio) -> item.setPortfolioSecondary(portfolio));
    }

    private <T extends Named> void showApplyToAllItemsMenu(IMenuManager parent, String label, Supplier<List<T>> options,
                    BiConsumer<Extractor.Item, T> applier)
    {
        IMenuManager manager = new MenuManager(label);
        parent.add(manager);

        for (T subject : options.get())
        {
            manager.add(new SimpleAction(subject.getName(), a -> {
                for (Object element : tableViewer.getStructuredSelection().toList())
                    applier.accept(((ExtractedEntry) element).getItem(), subject);

                checkEntriesAndRefresh(allEntries);
            }));
        }
    }

    @Override
    public void beforePage()
    {
        setTitle(extractor.getLabel());

        if (!doExtractBeforeEveryPageDisplay
                        && (!allEntries.isEmpty() || errorTableViewer.getTable().getItemCount() > 0))
            return;

        runExtractionJob();
    }

    private void runExtractionJob()
    {
        allEntries.clear();
        tableViewer.setInput(allEntries);
        errorTableViewer.setInput(Collections.emptyList());

        if (extractor == null)
        {
            setResults(Collections.emptyList(), files.stream().map(f -> new UnsupportedOperationException(f.getName()))
                            .collect(Collectors.toList()));
            return;
        }

        try
        {
            new AbstractClientJob(client, extractor.getLabel())
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    monitor.beginTask(Messages.PDFImportWizardMsgExtracting, files.size());
                    final List<Exception> errors = new ArrayList<>();

                    try
                    {

                        List<ExtractedEntry> entries = extractor //
                                        .extract(files, errors).stream() //
                                        .map(ExtractedEntry::new) //
                                        .collect(Collectors.toList());

                        // Logging them is not a bad idea if the whole method
                        // fails
                        PortfolioPlugin.log(errors);

                        Display.getDefault().asyncExec(() -> setResults(entries, errors));
                    }
                    catch (Exception e)
                    {
                        throw new UnsupportedOperationException(e);
                    }

                    return Status.OK_STATUS;
                }
            }.schedule();
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public void afterPage()
    {
        preferences.setValue(IMPORT_TARGET_ACCOUNT + extractor.getLabel(), getAccount().getUUID());
        preferences.setValue(IMPORT_TARGET_PORTFOLIO + extractor.getLabel(), getPortfolio().getUUID());
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    private void setResults(List<ExtractedEntry> entries, List<Exception> errors)
    {
        checkEntries(entries);

        allEntries.addAll(entries);
        tableViewer.setInput(allEntries);
        extractionErrors.addAll(errors);
        errorTableViewer.setInput(extractionErrors);

        for (ExtractedEntry entry : entries)
        {
            if (entry.getItem() instanceof Extractor.AccountTransferItem)
            {
                lblSecondaryAccount.setVisible(true);
                secondaryAccount.getControl().setVisible(true);
            }
            else if (entry.getItem() instanceof Extractor.PortfolioTransferItem)
            {
                lblSecondaryPortfolio.setVisible(true);
                secondaryPortfolio.getControl().setVisible(true);
            }
        }
    }

    private void checkEntriesAndRefresh(List<ExtractedEntry> entries)
    {
        checkEntries(entries);
        tableViewer.refresh();
    }

    private void checkEntries(List<ExtractedEntry> entries)
    {
        List<ImportAction> actions = new ArrayList<>();
        actions.add(new CheckValidTypesAction());
        actions.add(new CheckSecurityRelatedValuesAction());
        actions.add(new DetectDuplicatesAction(client));
        actions.add(new CheckCurrenciesAction());
        actions.add(new CheckForexGrossValueAction());
        actions.add(new MarkNonImportableAction());

        List<Exception> allErrors = new ArrayList<>(extractionErrors);

        for (ExtractedEntry entry : entries)
        {
            entry.clearStatus();
            for (ImportAction action : actions)
            {
                ImportAction.Status actionStatus = entry.getItem().apply(action, this);
                entry.addStatus(actionStatus);
                if (actionStatus.getCode() == ImportAction.Status.Code.ERROR)
                    allErrors.add(new IOException(
                                    entry.getItem().getSubject().getNote() + ": " + actionStatus.getMessage())); //$NON-NLS-1$

            }
        }

        errorTableViewer.setInput(allErrors);
    }

    abstract static class FormattedLabelProvider extends StyledCellLabelProvider // NOSONAR
    {
        private static Styler strikeoutStyler = new Styler()
        {
            @Override
            public void applyStyles(TextStyle textStyle)
            {
                textStyle.strikeout = true;
            }
        };

        public String getText(ExtractedEntry element) // NOSONAR
        {
            return null;
        }

        public Image getImage(ExtractedEntry element) // NOSONAR
        {
            return null;
        }

        @Override
        public void update(ViewerCell cell)
        {
            ExtractedEntry entry = (ExtractedEntry) cell.getElement();
            String text = getText(entry);
            if (text == null)
                text = ""; //$NON-NLS-1$

            boolean strikeout = !entry.isImported();
            StyledString styledString = new StyledString(text, strikeout ? strikeoutStyler : null);

            cell.setText(styledString.toString());
            cell.setStyleRanges(styledString.getStyleRanges());
            cell.setImage(getImage(entry));

            super.update(cell);
        }
    }
}
