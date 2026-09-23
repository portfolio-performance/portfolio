package name.abuchen.portfolio.ui.wizards.datatransfer;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.SkippedItem;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

public class ReviewExtractedItemsPage extends AbstractWizardPage implements ImportAction.Context
{

    private static final String IMPORT_TARGET = "import-target"; //$NON-NLS-1$
    private static final String IMPORT_TARGET_PORTFOLIO = IMPORT_TARGET + "-portfolio-"; //$NON-NLS-1$
    private static final String IMPORT_TARGET_ACCOUNT = IMPORT_TARGET + "-account-"; //$NON-NLS-1$
    private static final String IMPORT_TARGET_SECONDARY_ACCOUNT = IMPORT_TARGET + "-secondary-account-"; //$NON-NLS-1$
    /**
     * Preference for the import wizard to convert "BuySell" transactions to
     * "Delivery" transactions
     */
    private static final String IMPORT_CONVERT_BUYSELL_TO_DELIVERY = "IMPORT_CONVERT_BUYSELL_TO_DELIVERY"; //$NON-NLS-1$

    /**
     * Preference for the import wizard for "Dividends" additional one
     * "Withdrawal" generate
     */
    private static final String IMPORT_REMOVE_DIVIDENDS = "IMPORT_REMOVE_DIVIDENDS"; //$NON-NLS-1$

    /**
     * Preference for the import wizard that the notes are set for the
     * transactions
     */
    private static final String IMPORT_NOTES = "IMPORT_NOTES"; //$NON-NLS-1$

    /**
     * Preference for the import wizard that the columns with taxes and fees
     * are shown
     */
    private static final String IMPORT_SHOW_TAXES_AND_FEES = "IMPORT_SHOW_TAXES_AND_FEES"; //$NON-NLS-1$

    /**
     * Preference for the import wizard that the column with the note is shown
     */
    private static final String IMPORT_SHOW_NOTE = "IMPORT_SHOW_NOTE"; //$NON-NLS-1$

    /**
     * If embedded into the CSV import, the first page can change the parsing
     * result and transactions must be extracted before every page. If embedded
     * into the PDF or XML import wizard, do not extract transactions again.
     */
    private boolean doExtractBeforeEveryPageDisplay = false;

    private ExtractedItemsTable itemsTable;
    private TableViewer errorTableViewer;

    /**
     * the composite holding the dropdowns for primary account
     */
    private Composite primaryContainer;

    /**
     * the composite holding the dropdowns for secondary account and portfolio
     */
    private Composite secondaryContainer;

    /**
     * the composite holding the dropdowns for the primary portfolio and - if
     * needed - the offset portfolio. It is placed between the accounts and the
     * transfers so that it is not read as part of one of the transfer rows.
     */
    private Composite portfolioContainer;

    private ComboViewer primaryPortfolio;
    private ComboViewer secondaryPortfolio;

    /** currency -> source account with label and dropdown */
    private Map<String, Pair<Label, ComboViewer>> primaryAccounts = new HashMap<>();
    /** currency pair of a transfer -> target account with label and dropdown */
    private Map<CurrencyPair, Pair<Label, ComboViewer>> secondaryAccounts = new LinkedHashMap<>();

    private Button cbConvertToDelivery;
    private Button cbRemoveDividends;
    private Button cbImportNotesFromSource;
    private Button cbShowTaxesAndFees;
    private Button cbShowNote;

    /** the row with the import options; options which are not needed are hidden */
    private Composite optionsRow;

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
        if (primaryPortfolio == null || primaryPortfolio.getSelection().isEmpty())
            return null;
        return (Portfolio) primaryPortfolio.getStructuredSelection().getFirstElement();
    }

    @Override
    public Portfolio getSecondaryPortfolio()
    {
        if (secondaryPortfolio == null || secondaryPortfolio.getSelection().isEmpty())
            return null;
        return (Portfolio) secondaryPortfolio.getStructuredSelection().getFirstElement();
    }

    @Override
    public Account getAccount(String currency)
    {
        var pair = primaryAccounts.get(currency);
        if (pair == null)
            return null;

        return (Account) pair.getRight().getStructuredSelection().getFirstElement();
    }

    @Override
    public Account getSecondaryAccount(String currency)
    {
        // the dropdowns exist per currency pair. Without the source currency,
        // use the first dropdown with a matching target currency.
        for (var entry : secondaryAccounts.entrySet())
        {
            if (entry.getKey().target().equals(currency))
                return (Account) entry.getValue().getRight().getStructuredSelection().getFirstElement();
        }

        return null;
    }

    @Override
    public Account getSecondaryAccount(String sourceCurrency, String targetCurrency)
    {
        var pair = secondaryAccounts.get(new CurrencyPair(sourceCurrency, targetCurrency));
        if (pair == null)
            return null;

        return (Account) pair.getRight().getStructuredSelection().getFirstElement();
    }

    public boolean doConvertToDelivery()
    {
        return cbConvertToDelivery.getSelection();
    }

    public boolean doRemoveDividends()
    {
        return cbRemoveDividends.getSelection();
    }

    public boolean doImportNotesFromSource()
    {
        return cbImportNotesFromSource.getSelection();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Composite targetContainer = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(targetContainer);

        primaryContainer = new Composite(targetContainer, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(primaryContainer);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(primaryContainer);

        // portfolio container: portfolio | dropdown | offset portfolio |
        // dropdown
        portfolioContainer = new Composite(targetContainer, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(portfolioContainer);
        GridLayoutFactory.fillDefaults().numColumns(4).applyTo(portfolioContainer);

        // secondary container: "transfer" label | direction | dropdown
        secondaryContainer = new Composite(targetContainer, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(secondaryContainer);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(secondaryContainer);

        // preselect the dropdown even if we do not yet have entries to have a
        // minimum size and avoid flickering in (most) cases where there is only
        // one currency
        populateAccountSelectionContainer(Collections.emptyList());

        optionsRow = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(5).spacing(20, 0).applyTo(optionsRow);

        cbConvertToDelivery = new Button(optionsRow, SWT.CHECK);
        cbConvertToDelivery.setText(Messages.LabelConvertBuySellIntoDeliveryTransactions);
        cbConvertToDelivery.setSelection(
                        preferences.getBoolean(IMPORT_CONVERT_BUYSELL_TO_DELIVERY + extractor.getLabel()));

        cbRemoveDividends = new Button(optionsRow, SWT.CHECK);
        cbRemoveDividends.setText(Messages.LabelRemoveDividends);
        cbRemoveDividends.setSelection(preferences.getBoolean(IMPORT_REMOVE_DIVIDENDS + extractor.getLabel()));

        cbImportNotesFromSource = new Button(optionsRow, SWT.CHECK);
        cbImportNotesFromSource.setText(Messages.LabelImportNotesFromSource);

        // default behavior is to import the notes -> check if the key exists
        // because the boolean value defaults to false
        var hasKey = preferences.contains(IMPORT_NOTES + extractor.getLabel());
        cbImportNotesFromSource.setSelection(!hasKey || preferences.getBoolean(IMPORT_NOTES + extractor.getLabel()));

        cbShowTaxesAndFees = new Button(optionsRow, SWT.CHECK);
        cbShowTaxesAndFees.setText(Messages.LabelShowTaxesAndFees);
        cbShowTaxesAndFees.setSelection(preferences.getBoolean(IMPORT_SHOW_TAXES_AND_FEES + extractor.getLabel()));

        cbShowNote = new Button(optionsRow, SWT.CHECK);
        cbShowNote.setText(Messages.LabelShowNote);
        cbShowNote.setSelection(preferences.getBoolean(IMPORT_SHOW_NOTE + extractor.getLabel()));

        Composite compositeTable = new Composite(container, SWT.NONE);
        Composite errorTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        FormDataFactory.startingWith(targetContainer) //
                        .top(new FormAttachment(0, 0)).left(new FormAttachment(0, 0)).right(new FormAttachment(100, 0))
                        .thenBelow(optionsRow);

        FormDataFactory.startingWith(optionsRow) //
                        .thenBelow(compositeTable).right(targetContainer).bottom(new FormAttachment(80, 0)) //
                        .thenBelow(errorTable).right(targetContainer).bottom(new FormAttachment(100, 0));

        //
        // table & columns
        //

        itemsTable = new ExtractedItemsTable(compositeTable, client, allEntries);
        itemsTable.setContext(this);
        itemsTable.setOnEntriesChanged(() -> checkEntriesAndRefresh(allEntries));

        // show the transactions as they will be imported with the options
        itemsTable.setImportOptions(this::doConvertToDelivery, this::doRemoveDividends);
        itemsTable.setOptionalColumns(cbShowTaxesAndFees::getSelection, cbShowNote::getSelection);
        itemsTable.setOnTypesChanged(this::onTypesChanged);
        cbConvertToDelivery.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> checkEntriesAndRefresh(allEntries)));
        cbRemoveDividends.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> checkEntriesAndRefresh(allEntries)));

        // the optional columns only change the display
        cbShowTaxesAndFees.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> itemsTable.updateColumnVisibility()));
        cbShowNote.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> itemsTable.updateColumnVisibility()));

        TableColumnLayout layout = new TableColumnLayout();
        errorTable.setLayout(layout);
        errorTableViewer = new TableViewer(errorTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(errorTableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(errorTableViewer);
        errorTableViewer.setContentProvider(ArrayContentProvider.getInstance());

        var table = errorTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        addColumnsExceptionTable(errorTableViewer, layout);
    }

    private void preselectDropDowns()
    {
        // idea: generally one type of document (i.e. from the same bank) will
        // be imported into the same account

        for (var entry : primaryAccounts.entrySet())
        {
            var currency = entry.getKey();
            var combo = entry.getValue().getRight();
            var list = (List<?>) combo.getInput();

            // if the dialog is opened with a pre-selected account, use that if
            // the currency matches

            if (account != null && Objects.equals(currency, account.getCurrencyCode()))
            {
                var index = list.indexOf(account);
                if (index >= 0)
                {
                    // do not trigger a selection (do not use #setSelection)
                    combo.getCombo().select(index);
                    continue;
                }
            }

            var uuid = preferences.getString(IMPORT_TARGET_ACCOUNT + extractor.getLabel() + currency);

            // previously, the preferences were stored without currency.
            // Use as fallback for the time being (changed in May 2025)
            if (uuid.isEmpty())
                uuid = preferences.getString(IMPORT_TARGET_ACCOUNT + extractor.getLabel());

            var accountUUID = uuid;
            var index = IntStream.range(0, list.size()) //
                            .filter(i -> ((Account) list.get(i)).getUUID().equals(accountUUID)) //
                            .findAny().orElse(0);
            // do not trigger a selection (do not use #setSelection)
            combo.getCombo().select(index >= 0 ? index : 0);

        }

        for (var entry : secondaryAccounts.entrySet())
        {
            var currencyPair = entry.getKey();
            var combo = entry.getValue().getRight();
            var list = (List<?>) combo.getInput();

            if (list.isEmpty())
                continue;

            // use the target account of the previous import if still available
            var uuid = preferences.getString(
                            IMPORT_TARGET_SECONDARY_ACCOUNT + extractor.getLabel() + currencyPair.getPreferenceKey());
            var index = IntStream.range(0, list.size()) //
                            .filter(i -> ((Account) list.get(i)).getUUID().equals(uuid)) //
                            .findAny().orElse(0);

            // do not trigger a selection (do not use #setSelection)
            combo.getCombo().select(index);
        }

        List<Portfolio> activePortfolios = client.getActivePortfolios();
        if (activePortfolios.isEmpty())
            activePortfolios.addAll(client.getPortfolios());
        if (!activePortfolios.isEmpty())
        {
            String uuid = portfolio != null ? portfolio.getUUID()
                            : preferences.getString(IMPORT_TARGET_PORTFOLIO + extractor.getLabel());
            // do not trigger a selection (do not use #setSelection)
            primaryPortfolio.getCombo().select(IntStream.range(0, activePortfolios.size())
                            .filter(i -> activePortfolios.get(i).getUUID().equals(uuid)).findAny().orElse(0));

            if (secondaryPortfolio != null)
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

            @Override
            public String getToolTipText(Object element)
            {
                return TextUtil.wordwrap(getText(element));
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100, true));
    }

    @Override
    public void beforePage()
    {
        setTitle(extractor.getLabel());

        // apply column widths changed on another page of the wizard
        itemsTable.loadColumnWidths();

        // run the extraction job either if we have to run them every time (in
        // the case of CSV) or the first time around because we do not have any
        // entries nor error messages
        if (doExtractBeforeEveryPageDisplay
                        || (allEntries.isEmpty() && errorTableViewer.getTable().getItemCount() == 0))
        {
            runExtractionJob();

        }
    }

    private void runExtractionJob()
    {
        allEntries.clear();
        itemsTable.getTableViewer().setInput(allEntries);
        itemsTable.updateColumnVisibility();
        errorTableViewer.setInput(Collections.emptyList());

        if (extractor == null)
        {
            setResults(Collections.emptyList(), files.stream().map(f -> new UnsupportedOperationException(f.getName()))
                            .collect(toMutableList()));
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
                        // for PDF documents, the extraction job does not
                        // actually do an extraction because we the extractor
                        // is created in ImportExtractedItemsWizard and just
                        // returns the items

                        // for CSV documents, we actually have to parse the
                        // original file again

                        // in both cases, we have a fresh list to which we can
                        // apply the checks inside setResults again

                        List<ExtractedEntry> entries = extractor //
                                        .extract(files, errors).stream() //
                                        .map(ExtractedEntry::new) //
                                        .toList();

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
        // store the column widths before the next page is shown
        itemsTable.saveColumnWidths();

        for (var entry : primaryAccounts.entrySet())
        {
            var currency = entry.getKey();
            var selectedAccount = entry.getValue().getRight().getStructuredSelection().getFirstElement();
            if (selectedAccount != null)
            {
                preferences.setValue(IMPORT_TARGET_ACCOUNT + extractor.getLabel() + currency,
                                ((Account) selectedAccount).getUUID());
            }
        }

        for (var entry : secondaryAccounts.entrySet())
        {
            var currencyPair = entry.getKey();
            var selectedAccount = entry.getValue().getRight().getStructuredSelection().getFirstElement();
            if (selectedAccount != null)
            {
                preferences.setValue(
                                IMPORT_TARGET_SECONDARY_ACCOUNT + extractor.getLabel() + currencyPair.getPreferenceKey(),
                                ((Account) selectedAccount).getUUID());
            }
        }

        preferences.setValue(IMPORT_TARGET_PORTFOLIO + extractor.getLabel(), getPortfolio().getUUID());

        preferences.setValue(IMPORT_CONVERT_BUYSELL_TO_DELIVERY + extractor.getLabel(), doConvertToDelivery());
        preferences.setValue(IMPORT_REMOVE_DIVIDENDS + extractor.getLabel(), doRemoveDividends());
        preferences.setValue(IMPORT_NOTES + extractor.getLabel(), doImportNotesFromSource());
        preferences.setValue(IMPORT_SHOW_TAXES_AND_FEES + extractor.getLabel(), cbShowTaxesAndFees.getSelection());
        preferences.setValue(IMPORT_SHOW_NOTE + extractor.getLabel(), cbShowNote.getSelection());
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
        allEntries.addAll(entries);
        extractionErrors.addAll(errors);

        setupDependencies(entries);
        populateAccountSelectionContainer(entries);
        checkEntries(entries);

        updateOptionVisibility(allEntries);

        itemsTable.getTableViewer().setInput(allEntries);
        itemsTable.updateColumnVisibility();
    }

    private void populateAccountSelectionContainer(List<ExtractedEntry> entries)
    {
        // first: delete any previously created controls

        var children = primaryContainer.getChildren();
        for (var child : children)
            child.dispose();

        children = secondaryContainer.getChildren();
        for (var child : children)
            child.dispose();

        children = portfolioContainer.getChildren();
        for (var child : children)
            child.dispose();

        primaryPortfolio = null;
        secondaryPortfolio = null;

        primaryAccounts.clear();
        secondaryAccounts.clear();

        // collect available accounts and portfolios

        var accounts = client.getActiveAccounts();
        if (accounts.isEmpty())
            accounts = client.getAccounts();

        var portfolios = client.getActivePortfolios();
        if (portfolios.isEmpty())
            portfolios = client.getPortfolios();

        // second: source container based on used currencies

        var primaryCurrencies = entries.stream().filter(e -> e.getItem().getAmount() != null)
                        .map(e -> e.getItem().getAmount().getCurrencyCode()).collect(Collectors.toSet());

        // if we have currencies at all, create one item to have at least one
        // account in order to avoid flickering when updating the dropdowns
        // later
        if (primaryCurrencies.isEmpty())
            primaryCurrencies.add(client.getBaseCurrency());

        for (String currency : primaryCurrencies)
        {
            var label = new Label(primaryContainer, SWT.NONE);
            label.setText(currency);

            var accountsByCurrency = accounts.stream().filter(a -> a.getCurrencyCode().equals(currency))
                            .sorted(new Account.ByName()).toList();
            if (accountsByCurrency.isEmpty())
            {
                var message = new Label(primaryContainer, SWT.NONE);
                message.setText(MessageFormat.format(Messages.LabelCreateAccountFirst, currency));
            }
            else
            {
                var dropdown = new ComboViewer(primaryContainer, SWT.READ_ONLY);
                dropdown.setContentProvider(ArrayContentProvider.getInstance());
                dropdown.setInput(accountsByCurrency);
                dropdown.addSelectionChangedListener(e -> checkEntriesAndRefresh(allEntries));

                primaryAccounts.put(currency, new Pair<>(label, dropdown));
            }
        }

        var lblPrimaryPortfolio = new Label(portfolioContainer, SWT.NONE);
        lblPrimaryPortfolio.setText(Messages.ColumnPortfolio);
        primaryPortfolio = new ComboViewer(portfolioContainer, SWT.READ_ONLY);
        primaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        primaryPortfolio.setInput(portfolios);
        primaryPortfolio.addSelectionChangedListener(e -> checkEntriesAndRefresh(allEntries));

        // the offset portfolio is placed right next to the portfolio it
        // belongs to

        var needsSecondaryPortfolio = entries.stream()
                        .anyMatch(e -> e.getItem() instanceof Extractor.PortfolioTransferItem);

        if (needsSecondaryPortfolio)
        {
            var label = new Label(portfolioContainer, SWT.NONE);
            label.setText(Messages.ColumnOffsetPortfolio);
            secondaryPortfolio = new ComboViewer(portfolioContainer, SWT.READ_ONLY);
            secondaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
            secondaryPortfolio.setInput(portfolios);
            secondaryPortfolio.addSelectionChangedListener(e -> checkEntriesAndRefresh(allEntries));
        }

        // third: target container with one row per currency pair of the
        // transfers (sorted by source and target currency)

        var currencyPairs = new TreeSet<CurrencyPair>(
                        Comparator.comparing(CurrencyPair::source).thenComparing(CurrencyPair::target));
        for (var e : entries)
        {
            if (e.getItem() instanceof Extractor.AccountTransferItem transfer)
            {
                var transferEntry = (AccountTransferEntry) transfer.getSubject();
                currencyPairs.add(new CurrencyPair(transferEntry.getSourceTransaction().getCurrencyCode(),
                                transferEntry.getTargetTransaction().getCurrencyCode()));
            }
        }

        for (var currencyPair : currencyPairs)
        {
            var currency = currencyPair.target();

            addTransferToCell();

            // show the direction (e.g. "USD → EUR") because the rows are not
            // related to the primary accounts displayed on the same line
            var label = new Label(secondaryContainer, SWT.NONE);
            label.setText(currencyPair.source() + " \u2192 " + currency); //$NON-NLS-1$
            List<Account> accountsByCurrency = accounts.stream().filter(a -> a.getCurrencyCode().equals(currency))
                            .sorted(new Account.ByName()).toList();

            if (accountsByCurrency.isEmpty())
            {
                var message = new Label(secondaryContainer, SWT.NONE);
                message.setText(MessageFormat.format(Messages.LabelCreateAccountFirst, currency));
            }
            else
            {
                var dropdown = new ComboViewer(secondaryContainer, SWT.READ_ONLY);
                dropdown.setContentProvider(ArrayContentProvider.getInstance());
                dropdown.setInput(accountsByCurrency);
                dropdown.addSelectionChangedListener(e -> checkEntriesAndRefresh(allEntries));

                secondaryAccounts.put(currencyPair, new Pair<>(label, dropdown));
            }
        }

        // finally: re-layout

        // The size of the target container often does not change (e.g. the
        // primary side still shows the same currencies). Then SWT
        // does not lay out its children again and the secondary container
        // keeps the size of its initial, empty state, which hides the
        // dropdowns. Therefore lay out all descendants.

        primaryContainer.layout(true);
        portfolioContainer.layout(true);
        secondaryContainer.layout(true);
        primaryContainer.getParent().getParent().layout(true, true);

        preselectDropDowns();
    }

    /**
     * Shows only the options which are relevant for the imported entries: the
     * conversion into deliveries only if there are purchases or sales, the
     * additional removal only if there are dividends. Calculated once after the
     * documents have been read, so that an option does not disappear while the
     * user is working with it.
     */
    private void updateOptionVisibility(List<ExtractedEntry> entries)
    {
        var hasBuySell = entries.stream().anyMatch(e -> e.getItem().getSubject() instanceof BuySellEntry);
        var hasDividends = entries.stream()
                        .anyMatch(e -> e.getItem().getSubject() instanceof AccountTransaction transaction
                                        && transaction.getType() == AccountTransaction.Type.DIVIDENDS);

        setOptionVisible(cbConvertToDelivery, hasBuySell);
        setOptionVisible(cbRemoveDividends, hasDividends);

        optionsRow.layout(true);
        optionsRow.getParent().layout(true, true);
    }

    private void setOptionVisible(Button option, boolean isVisible)
    {
        option.setVisible(isVisible);

        // do not leave a gap for a hidden option
        var data = new GridData();
        data.exclude = !isVisible;
        option.setLayoutData(data);
    }

    /**
     * Called after the user changed the type of entries (e.g. a deposit into
     * a transfer). The entries may need other dropdowns (e.g. for the offset
     * account), therefore the dropdowns are created again while keeping the
     * current selection.
     */
    private void onTypesChanged()
    {
        var primarySelection = new HashMap<String, Object>();
        primaryAccounts.forEach((currency, pair) -> primarySelection.put(currency,
                        pair.getRight().getStructuredSelection().getFirstElement()));

        var secondarySelection = new HashMap<CurrencyPair, Object>();
        secondaryAccounts.forEach((currencyPair, pair) -> secondarySelection.put(currencyPair,
                        pair.getRight().getStructuredSelection().getFirstElement()));

        var portfolioSelection = primaryPortfolio.getStructuredSelection().getFirstElement();
        var secondaryPortfolioSelection = secondaryPortfolio != null
                        ? secondaryPortfolio.getStructuredSelection().getFirstElement()
                        : null;

        populateAccountSelectionContainer(allEntries);

        primaryAccounts.forEach((currency, pair) -> select(pair.getRight(), primarySelection.get(currency)));
        secondaryAccounts.forEach((currencyPair, pair) -> select(pair.getRight(),
                        secondarySelection.get(currencyPair)));
        select(primaryPortfolio, portfolioSelection);
        if (secondaryPortfolio != null)
            select(secondaryPortfolio, secondaryPortfolioSelection);

        checkEntriesAndRefresh(allEntries);
    }

    /**
     * Selects the element if it is available (without triggering a selection
     * event, like the preselection of the dropdowns).
     */
    private void select(ComboViewer combo, Object element)
    {
        if (element == null)
            return;

        var index = ((List<?>) combo.getInput()).indexOf(element);
        if (index >= 0)
            combo.getCombo().select(index);
    }

    /**
     * Adds the first cell of a row in the secondary container: the "transfer"
     * label in the first row, an empty placeholder in all further rows.
     * This keeps the label on the same line as the first target dropdown.
     */
    private void addTransferToCell()
    {
        var label = new Label(secondaryContainer, SWT.NONE);
        if (secondaryContainer.getChildren().length == 1)
            label.setText(Messages.LabelTransfer);
    }

    private void checkEntriesAndRefresh(List<ExtractedEntry> entries)
    {
        checkEntries(entries);
        itemsTable.refresh();
    }

    /**
     * The additional fee and tax entries of a changed type cannot be imported
     * without their transfer. If the transfer has an error, the entries get the
     * same error so that the reason is visible on each of them. (If the user
     * excludes the transfer, the entries are not imported either, see
     * ExtractedEntry#setOwner.)
     */
    private void markAdditionalEntriesOfFailedTransfers(List<ExtractedEntry> entries)
    {
        if (itemsTable == null)
            return;

        for (var entry : entries)
        {
            var errors = entry.getStatus().filter(s -> s.getCode() == ImportAction.Status.Code.ERROR).toList();
            if (errors.isEmpty())
                continue;

            for (var additional : itemsTable.getAdditionalEntries(entry))
                errors.forEach(additional::addStatus);
        }
    }

    /**
     * Setup the securityDependency attribute of the extracted item by
     * collecting all entries which import a new security
     */
    private void setupDependencies(List<ExtractedEntry> entries)
    {
        var security2entry = entries.stream().filter(e -> e.getItem() instanceof Extractor.SecurityItem)
                        .collect(Collectors.toMap(e -> e.getItem().getSecurity(), e -> e));

        for (ExtractedEntry entry : entries)
        {
            if (entry.getItem() instanceof Extractor.SecurityItem)
                continue;

            entry.setSecurityDependency(security2entry.get(entry.getItem().getSecurity()));
        }
    }

    private void checkEntries(List<ExtractedEntry> entries)
    {
        List<ImportAction> actions = new ArrayList<>();
        actions.add(new CheckTransactionDateAction());
        actions.add(new CheckValidTypesAction());
        actions.add(new CheckSecurityRelatedValuesAction());
        actions.add(new DetectDuplicatesAction(client));
        actions.add(new CheckCurrenciesAction());
        actions.add(new CheckForexGrossValueAction());
        actions.add(new CheckTransferSourceAndTargetAction());
        actions.add(new CheckReferenceAccountCurrencyAction(this::doConvertToDelivery, this::getAccount));

        List<Exception> allErrors = new ArrayList<>(extractionErrors);

        for (ExtractedEntry entry : entries)
        {
            entry.clearStatus();

            if (entry.getItem().isFailure())
            {
                entry.addStatus(new ImportAction.Status(Code.ERROR, entry.getItem().getFailureMessage()));
                allErrors.add(new IOException(entry.getItem().getFailureMessage() + ": " + entry.getItem().toString())); //$NON-NLS-1$
            }
            else if (entry.getItem().isSkipped())
            {
                entry.addStatus(new ImportAction.Status(Code.SKIP, ((SkippedItem) entry.getItem()).getSkipReason()));
            }
            else
            {
                for (ImportAction action : actions)
                {
                    try
                    {
                        ImportAction.Status actionStatus = entry.getItem().apply(action, this);
                        entry.addStatus(actionStatus);
                    }
                    catch (Exception e)
                    {
                        // if any of the import action fails to due unexpected
                        // error, we must not abort but mark the item as not
                        // importable and continue

                        entry.addStatus(new ImportAction.Status(ImportAction.Status.Code.ERROR, e.getMessage()));
                        allErrors.add(e);

                        // write to application log as this is most likely than
                        // not a programming error
                        PortfolioPlugin.log(e);
                    }
                }

                entry.getStatus().filter(s -> s.getCode() == ImportAction.Status.Code.ERROR)
                                .forEach(status -> allErrors
                                                .add(new IOException(MessageFormat.format(Messages.LabelColonSeparated,
                                                                status.getMessage(), entry.getItem().toString()))));
            }
        }

        markAdditionalEntriesOfFailedTransfers(entries);

        errorTableViewer.setInput(allErrors);
    }

    /**
     * Currency pair of a transfer from the source to the target currency. The
     * target account is selected per currency pair.
     */
    private record CurrencyPair(String source, String target)
    {
        private String getPreferenceKey()
        {
            return source + "-" + target; //$NON-NLS-1$
        }
    }
}
