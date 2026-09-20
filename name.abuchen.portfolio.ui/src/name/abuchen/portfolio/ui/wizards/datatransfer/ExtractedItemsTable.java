package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
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
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.action.MenuContribution;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

public class ExtractedItemsTable
{
    /**
     * Preference prefix for the column widths. The widths are shared by all
     * tables of this kind (review pages and manual entry pages).
     */
    private static final String PREF_COLUMN_WIDTH = ExtractedItemsTable.class.getSimpleName() + "-column-width-"; //$NON-NLS-1$

    /** columns shown as long as the table is empty */
    private static final Set<String> COLUMNS_OF_EMPTY_TABLE = Set.of("status", "date", "type", "amount"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private final TableViewer tableViewer;
    private final Client client;
    private final List<ExtractedEntry> entries;

    private Runnable onEntriesChanged;
    private Consumer<List<ExtractedEntry>> onDelete;
    private Consumer<ExtractedEntry> onEdit;

    /**
     * Provides the accounts selected in the dropdowns of the wizard page. Used
     * to display the account an item is booked to if no account is set
     * explicitly on the item.
     */
    private ImportAction.Context context;

    /** import option: convert buy/sell transactions into deliveries */
    private BooleanSupplier convertToDelivery = () -> false;

    /** import option: generate an additional removal for dividends */
    private BooleanSupplier removeDividends = () -> false;

    /**
     * called after the user changed the type of entries. If null, the user
     * cannot change the type.
     */
    private Runnable onTypesChanged;

    /** changes the type of entries (e.g. a deposit into a transfer) */
    private final EntryTypeConverter typeConverter;

    private final TableColumnLayout layout;

    /**
     * width of each column when the table is created (stored or default
     * width). Used to show a hidden column again and to detect whether the
     * user has changed the width.
     */
    private final Map<TableColumn, Integer> defaultWidths = new HashMap<>();

    /** stable key of each column to store its width */
    private final Map<TableColumn, String> columnKeys = new HashMap<>();

    /** columns hidden because no entry shows anything in them */
    private final Set<TableColumn> hiddenColumns = new HashSet<>();

    public ExtractedItemsTable(Composite parent, Client client, List<ExtractedEntry> entries)
    {
        this.client = client;
        this.entries = entries;
        this.typeConverter = new EntryTypeConverter(entries);

        layout = new TableColumnLayout();
        parent.setLayout(layout);

        tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(tableViewer);

        var table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumns(layout);
        attachContextMenu();
        activateEditorsOnDoubleClick();

        // hide the columns which are not needed (e.g. an empty table on the
        // manual entry page shows only the basic columns)
        updateColumnVisibility();

        // the dispose event is sent before the columns are released
        table.addDisposeListener(e -> saveColumnWidths());
    }

    public TableViewer getTableViewer()
    {
        return tableViewer;
    }

    public void setOnEntriesChanged(Runnable onEntriesChanged)
    {
        this.onEntriesChanged = onEntriesChanged;
    }

    public void setOnDelete(Consumer<List<ExtractedEntry>> onDelete)
    {
        this.onDelete = onDelete;
    }

    public void setOnEdit(Consumer<ExtractedEntry> onEdit)
    {
        this.onEdit = onEdit;
    }

    public void setContext(ImportAction.Context context)
    {
        this.context = context;
    }

    /**
     * Sets the import options of the wizard page, so that the table shows the
     * transactions as they will be imported. The transactions themselves are
     * converted only when imported (see InsertAction).
     */
    public void setImportOptions(BooleanSupplier convertToDelivery, BooleanSupplier removeDividends)
    {
        this.convertToDelivery = convertToDelivery;
        this.removeDividends = removeDividends;
    }

    /**
     * Allows the user to change the type of entries via the context menu, e.g.
     * a deposit into an inbound transfer. The callback is run afterwards
     * because entries may have been added or removed.
     */
    public void setOnTypesChanged(Runnable onTypesChanged)
    {
        this.onTypesChanged = onTypesChanged;
    }

    /**
     * Returns the additional fee and tax entries created when the type of the
     * entry was changed, or an empty list.
     */
    public List<ExtractedEntry> getAdditionalEntries(ExtractedEntry entry)
    {
        return typeConverter.getAdditionalEntries(entry);
    }

    public void refresh()
    {
        typeConverter.forgetRemovedEntries();

        tableViewer.refresh();
        updateColumnVisibility();
    }

    /**
     * Hides all columns in which no entry shows a text or an image, e.g. the
     * converted amount if the import contains no transfer between currencies.
     * A hidden column is shown again with its stored or default width as soon
     * as an entry shows something in it. As long as there are no entries, only
     * the basic columns (status, date, type, amount) are shown.
     * <p>
     * The width is only changed when the visibility changes, so that a width
     * set manually by the user is kept.
     */
    public void updateColumnVisibility()
    {
        var table = tableViewer.getTable();
        if (table.isDisposed())
            return;

        var hasChanged = false;

        for (var index = 0; index < table.getColumnCount(); index++)
        {
            var column = table.getColumn(index);
            var isVisible = entries.isEmpty() ? COLUMNS_OF_EMPTY_TABLE.contains(columnKeys.get(column))
                            : hasContent(index);
            var isHidden = hiddenColumns.contains(column);

            if (isVisible && isHidden)
            {
                hiddenColumns.remove(column);
                column.setResizable(true);
                layout.setColumnData(column, new ColumnPixelData(defaultWidths.getOrDefault(column, 80), true));
                hasChanged = true;
            }
            else if (!isVisible && !isHidden)
            {
                hiddenColumns.add(column);
                // the layout does not apply the resizable flag to the column
                column.setResizable(false);
                layout.setColumnData(column, new ColumnPixelData(0, false));
                hasChanged = true;
            }
        }

        if (hasChanged)
            table.getParent().layout(true);
    }

    /**
     * Checks whether at least one entry shows a text or an image in the given
     * column. Uses the label provider of the column, so that the visibility
     * always matches what is displayed.
     */
    private boolean hasContent(int columnIndex)
    {
        if (!(tableViewer.getLabelProvider(columnIndex) instanceof FormattedLabelProvider labelProvider))
            return true;

        return entries.stream().anyMatch(entry -> {
            var text = labelProvider.getText(entry);
            return (text != null && !text.isEmpty()) || labelProvider.getImage(entry) != null;
        });
    }

    /**
     * Sets the width of the column: the width stored by the user if available,
     * otherwise the default width.
     */
    private void setColumnWidth(TableColumnLayout layout, TableViewerColumn column, String key, int defaultWidth)
    {
        var preferences = getPreferences();
        var storedWidth = preferences != null ? preferences.getInt(PREF_COLUMN_WIDTH + key) : 0;
        var width = storedWidth > 0 ? storedWidth : defaultWidth;

        columnKeys.put(column.getColumn(), key);
        defaultWidths.put(column.getColumn(), width);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(width, true));
    }

    /**
     * Stores the widths of all visible columns which the user has changed.
     * Hidden columns keep their stored width. The widths of a table which has
     * never been displayed are not stored, so that they do not overwrite the
     * widths changed in another table of the same wizard.
     * <p>
     * Called when the wizard page is left and when the table is disposed.
     */
    public void saveColumnWidths()
    {
        var preferences = getPreferences();
        if (preferences == null)
            return;

        for (var column : tableViewer.getTable().getColumns())
        {
            var key = columnKeys.get(column);
            var width = column.getWidth();

            if (key == null || hiddenColumns.contains(column) || width <= 0
                            || width == defaultWidths.getOrDefault(column, 0))
                continue;

            preferences.setValue(PREF_COLUMN_WIDTH + key, width);

            // the stored width is the new reference, so that this table does
            // not overwrite a width changed later in another table
            defaultWidths.put(column, width);
        }
    }

    /**
     * Applies the stored widths, e.g. those changed by the user on another
     * page of the same wizard. The pages of the wizard create their tables in
     * advance, therefore the widths are loaded again when a page is shown. A
     * hidden column gets the stored width when it is shown again.
     */
    public void loadColumnWidths()
    {
        var preferences = getPreferences();
        var table = tableViewer.getTable();
        if (preferences == null || table.isDisposed())
            return;

        var hasChanged = false;

        for (var column : table.getColumns())
        {
            var key = columnKeys.get(column);
            if (key == null)
                continue;

            var storedWidth = preferences.getInt(PREF_COLUMN_WIDTH + key);
            if (storedWidth <= 0 || storedWidth == defaultWidths.getOrDefault(column, 0))
                continue;

            defaultWidths.put(column, storedWidth);

            if (!hiddenColumns.contains(column))
            {
                layout.setColumnData(column, new ColumnPixelData(storedWidth, true));
                hasChanged = true;
            }
        }

        if (hasChanged)
            table.getParent().layout(true);
    }

    private static IPreferenceStore getPreferences()
    {
        var plugin = PortfolioPlugin.getDefault();
        return plugin != null ? plugin.getPreferenceStore() : null;
    }

    /**
     * Returns the type as the item will be imported with the options of the
     * wizard page, e.g. a purchase as inbound delivery. Uses the same
     * conversion as the InsertAction.
     */
    private String getTypeInformation(Item item)
    {
        var subject = item.getSubject();

        if (subject instanceof BuySellEntry entry && convertToDelivery.getAsBoolean())
        {
            var type = entry.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY
                            ? PortfolioTransaction.Type.DELIVERY_INBOUND
                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            return type.toString();
        }

        if (subject instanceof AccountTransaction transaction
                        && transaction.getType() == AccountTransaction.Type.DIVIDENDS
                        && removeDividends.getAsBoolean())
            return transaction.getType().toString() + " + " + AccountTransaction.Type.REMOVAL.toString(); //$NON-NLS-1$

        return item.getTypeInformation();
    }

    /**
     * Returns the account the item is booked to. Uses the same resolution as
     * {@link Item#apply}: the account set explicitly on the item, otherwise
     * the account selected for the currency on the wizard page.
     */
    private Account getEffectiveAccountPrimary(Item item)
    {
        // a delivery is not booked on an account
        if (item.getSubject() instanceof BuySellEntry && convertToDelivery.getAsBoolean())
            return null;

        if (item.getAccountPrimary() != null || context == null || item.isFailure() || item.isSkipped())
            return item.getAccountPrimary();

        var subject = item.getSubject();

        if (subject instanceof AccountTransaction transaction)
            return context.getAccount(transaction.getCurrencyCode());
        else if (subject instanceof BuySellEntry entry)
            return context.getAccount(entry.getAccountTransaction().getCurrencyCode());
        else if (subject instanceof AccountTransferEntry entry)
            return context.getAccount(entry.getSourceTransaction().getCurrencyCode());

        return null;
    }

    /**
     * Returns the offset account the item is booked to. Uses the same
     * resolution as {@link Item#apply}: the account set explicitly on the
     * item, otherwise the account selected for the target currency of a
     * transfer on the wizard page.
     */
    private Account getEffectiveAccountSecondary(Item item)
    {
        if (item.getAccountSecondary() != null || context == null || item.isFailure() || item.isSkipped())
            return item.getAccountSecondary();

        if (item.getSubject() instanceof AccountTransferEntry entry)
            return context.getSecondaryAccount(entry.getSourceTransaction().getCurrencyCode(),
                            entry.getTargetTransaction().getCurrencyCode());

        return null;
    }

    /**
     * Returns the portfolio the item is booked to. Uses the same resolution as
     * {@link Item#apply}: the portfolio set explicitly on the item, otherwise
     * the portfolio selected on the wizard page.
     */
    private Portfolio getEffectivePortfolioPrimary(Item item)
    {
        if (item.getPortfolioPrimary() != null || context == null || item.isFailure() || item.isSkipped())
            return item.getPortfolioPrimary();

        var subject = item.getSubject();

        if (subject instanceof BuySellEntry || subject instanceof PortfolioTransaction
                        || subject instanceof PortfolioTransferEntry)
            return context.getPortfolio();

        return null;
    }

    /**
     * Returns the offset portfolio the item is booked to. Uses the same
     * resolution as {@link Item#apply}: the portfolio set explicitly on the
     * item, otherwise the offset portfolio selected on the wizard page.
     */
    private Portfolio getEffectivePortfolioSecondary(Item item)
    {
        if (item.getPortfolioSecondary() != null || context == null || item.isFailure() || item.isSkipped())
            return item.getPortfolioSecondary();

        if (item.getSubject() instanceof PortfolioTransferEntry)
            return context.getSecondaryPortfolio();

        return null;
    }

    /**
     * Returns the amount booked on the offset account if a transfer converts
     * between two currencies. Returns null for all other items because the
     * amount column already shows everything there is.
     */
    private Money getConvertedAmount(Item item)
    {
        if (item.getSubject() instanceof AccountTransferEntry entry)
        {
            var source = entry.getSourceTransaction();
            var target = entry.getTargetTransaction();

            if (!Objects.equals(source.getCurrencyCode(), target.getCurrencyCode()))
                return target.getMonetaryAmount();
        }

        return null;
    }

    private Images getStatusImage(Code code)
    {
        return switch (code)
        {
            case WARNING -> Images.WARNING;
            case ERROR -> Images.CIRCLE_X_MARK_FILLED;
            case SKIP -> Images.CIRCLE_SLASH_FILLED;
            case OK -> Images.CIRCLE_CHECK;
            default -> throw new IllegalArgumentException();
        };
    }

    private void addColumns(TableColumnLayout layout)
    {
        var column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnStatus);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public Image getImage(ExtractedEntry entry)
            {
                return getStatusImage(entry.getMaxCode()).image();
            }

            @Override
            public String getToolTipText(Object entry)
            {
                String message = ((ExtractedEntry) entry).getStatus() //
                                .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                                .filter(s -> s.getMessage() != null) //
                                .map(s -> s.getMessage()) // NOSONAR
                                .collect(Collectors.joining("\n")); //$NON-NLS-1$
                return TextUtil.wordwrap(message);
            }

            @Override
            public String getText(ExtractedEntry entry)
            {
                return ""; //$NON-NLS-1$
            }
        });
        ColumnViewerSorter.create(entry -> ((ExtractedEntry) entry).getMaxCode()).attachTo(tableViewer, column);
        setColumnWidth(layout, column, "status", 22); //$NON-NLS-1$

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                var date = entry.getItem().getDate();
                return date != null ? Values.DateTime.format(date) : null;
            }
        });
        ColumnViewerSorter.create(entry -> ((ExtractedEntry) entry).getItem().getDate()).attachTo(tableViewer, column);
        setColumnWidth(layout, column, "date", 100); //$NON-NLS-1$

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return getTypeInformation(entry.getItem());
            }

            @Override
            public String getToolTipText(Object entry)
            {
                String message = ((ExtractedEntry) entry).getStatus() //
                                .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                                .filter(s -> s.getMessage() != null) //
                                .map(s -> s.getMessage()) // NOSONAR
                                .collect(Collectors.joining("\n")); //$NON-NLS-1$
                return TextUtil.wordwrap(message);
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
        setColumnWidth(layout, column, "type", 130); //$NON-NLS-1$
        addDropDownEditor(column, this::getTypeOptionsForEditor,
                        (entry, option) -> typeConverter.getTypeLabel(typeConverter.getOriginalItem(entry), option),
                        this::getEffectiveType, (entry, option) -> {
                            typeConverter.changeType(entry, option);
                            onTypesChanged.run();
                        });

        column = new TableViewerColumn(tableViewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                var amount = entry.getItem().getAmount();
                return amount != null ? Values.Money.format(amount) : null;
            }
        });
        setColumnWidth(layout, column, "amount", 80); //$NON-NLS-1$

        column = new TableViewerColumn(tableViewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnConvertedAmount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                var amount = getConvertedAmount(entry.getItem());
                return amount != null ? Values.Money.format(amount) : null;
            }
        });
        setColumnWidth(layout, column, "convertedAmount", 130); //$NON-NLS-1$

        column = new TableViewerColumn(tableViewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return Values.Share.formatNonZero(entry.getItem().getShares());
            }
        });
        setColumnWidth(layout, column, "shares", 80); //$NON-NLS-1$

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        var nameConfig = client.getSecurityNameConfig();
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Security security = entry.getItem().getSecurity();
                if (security == null)
                    return null;
                return entry.getSecurityOverride() != null ? entry.getSecurityOverride().getName(nameConfig)
                                : security.getName(nameConfig);
            }
        });
        setColumnWidth(layout, column, "security", 250); //$NON-NLS-1$

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnAccount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Account a = getEffectiveAccountPrimary(entry.getItem());
                return a != null ? a.getName() : null;
            }
        });
        setColumnWidth(layout, column, "account", 100); //$NON-NLS-1$
        addDropDownEditor(column, entry -> getAccountOptions(entry.getItem(), Role.ACCOUNT),
                        (entry, account) -> account.getName(), entry -> getEffectiveAccountPrimary(entry.getItem()),
                        (entry, account) -> applyToEntry(entry.getItem(), account, Item::setAccountPrimary));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnOffsetAccount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Account a = getEffectiveAccountSecondary(entry.getItem());
                return a != null ? a.getName() : null;
            }
        });
        setColumnWidth(layout, column, "offsetAccount", 100); //$NON-NLS-1$
        addDropDownEditor(column, entry -> getAccountOptions(entry.getItem(), Role.OFFSET_ACCOUNT),
                        (entry, account) -> account.getName(), entry -> getEffectiveAccountSecondary(entry.getItem()),
                        (entry, account) -> applyToEntry(entry.getItem(), account, Item::setAccountSecondary));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnPortfolio);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Portfolio p = getEffectivePortfolioPrimary(entry.getItem());
                return p != null ? p.getName() : null;
            }
        });
        setColumnWidth(layout, column, "portfolio", 100); //$NON-NLS-1$
        addDropDownEditor(column, entry -> getPortfolioOptions(entry.getItem(), Role.PORTFOLIO),
                        (entry, portfolio) -> portfolio.getName(),
                        entry -> getEffectivePortfolioPrimary(entry.getItem()),
                        (entry, portfolio) -> applyToEntry(entry.getItem(), portfolio, Item::setPortfolioPrimary));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnOffsetPortfolio);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Portfolio p = getEffectivePortfolioSecondary(entry.getItem());
                return p != null ? p.getName() : null;
            }
        });
        setColumnWidth(layout, column, "offsetPortfolio", 100); //$NON-NLS-1$
        addDropDownEditor(column, entry -> getPortfolioOptions(entry.getItem(), Role.OFFSET_PORTFOLIO),
                        (entry, portfolio) -> portfolio.getName(),
                        entry -> getEffectivePortfolioSecondary(entry.getItem()),
                        (entry, portfolio) -> applyToEntry(entry.getItem(), portfolio, Item::setPortfolioSecondary));
    }

    private void attachContextMenu()
    {
        var table = tableViewer.getTable();
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
        IStructuredSelection selection = tableViewer.getStructuredSelection();

        if (selection.isEmpty())
            return;

        boolean atLeastOneImported = false;
        boolean atLeastOneNotImported = false;

        for (Object element : selection.toList())
        {
            ExtractedEntry entry = (ExtractedEntry) element;
            atLeastOneImported = atLeastOneImported || entry.isImported();

            // show "Do Import" only for entries the user can actually import
            // (not dependency-blocked, not ERROR/SKIP) that aren't currently
            // marked for import
            boolean canImport = entry.getMaxCode() == Code.OK || entry.getMaxCode() == Code.WARNING;
            boolean notDependencyBlocked = entry.getSecurityDependency() == null
                            || entry.getSecurityDependency().isImported()
                            || entry.getSecurityOverride() != null;
            atLeastOneNotImported = atLeastOneNotImported
                            || (!entry.isImported() && canImport && notDependencyBlocked);
        }

        // provide a hint to the user why the entry is struck out
        if (selection.size() == 1)
        {
            ExtractedEntry entry = (ExtractedEntry) selection.getFirstElement();
            entry.getStatus() //
                            .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                            .forEach(s -> {
                                Images image = getStatusImage(s.getCode());
                                manager.add(new LabelOnly(s.getMessage(), image));
                            });
        }

        if (atLeastOneImported)
        {
            manager.add(new SimpleAction(Messages.LabelDoNotImport, a -> {
                for (Object element : tableViewer.getStructuredSelection().toList())
                    ((ExtractedEntry) element).setImported(false);

                tableViewer.refresh();
            }));
        }

        if (atLeastOneNotImported)
        {
            manager.add(new SimpleAction(Messages.LabelDoImport, a -> {
                for (Object element : tableViewer.getStructuredSelection().toList())
                {
                    var entry = (ExtractedEntry) element;
                    entry.setImported(true);

                    if (entry.getItem() instanceof Extractor.SecurityItem)
                    {
                        entries.stream().filter(e -> e.getSecurityDependency() == entry)
                                        .forEach(e -> e.setSecurityOverride(null));
                    }
                }

                tableViewer.refresh();
            }));
        }

        // if exactly one security is selected, offer to use an alternative

        if (selection.size() == 1 && selection.getFirstElement() instanceof ExtractedEntry entry
                        && entry.getItem() instanceof Extractor.SecurityItem)
        {
            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.LabelUseExistingSecurity, a -> selectExistingSecurity(entry)));
        }

        manager.add(new Separator());

        if (onTypesChanged != null)
            showChangeTypeMenu(manager);

        // offer only the accounts and portfolios which the selected items are
        // booked to, and only accounts in the matching currency

        var accounts = getAccountOptions();
        var portfolios = getPortfolioOptions();

        showApplyToAllItemsMenu(manager, Messages.ColumnAccount, Role.ACCOUNT, accounts,
                        (item, account) -> isSuitableAccount(item, Role.ACCOUNT, account),
                        this::getEffectiveAccountPrimary, Item::setAccountPrimary);
        showApplyToAllItemsMenu(manager, Messages.ColumnOffsetAccount, Role.OFFSET_ACCOUNT, accounts,
                        (item, account) -> isSuitableAccount(item, Role.OFFSET_ACCOUNT, account),
                        this::getEffectiveAccountSecondary, Item::setAccountSecondary);
        showApplyToAllItemsMenu(manager, Messages.ColumnPortfolio, Role.PORTFOLIO, portfolios,
                        (item, portfolio) -> true, this::getEffectivePortfolioPrimary, Item::setPortfolioPrimary);
        showApplyToAllItemsMenu(manager, Messages.ColumnOffsetPortfolio, Role.OFFSET_PORTFOLIO, portfolios,
                        (item, portfolio) -> true, this::getEffectivePortfolioSecondary,
                        Item::setPortfolioSecondary);

        if (onEdit != null && selection.size() == 1)
        {
            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.PDFImportWizardManualEntryEditTransaction,
                            a -> onEdit.accept((ExtractedEntry) selection.getFirstElement())));
        }

        if (onDelete != null)
        {
            if (onEdit == null || selection.size() != 1)
                manager.add(new Separator());
            manager.add(new SimpleAction(Messages.PDFImportWizardManualEntryDeleteTransaction, a -> {
                @SuppressWarnings("unchecked")
                List<ExtractedEntry> selected = (List<ExtractedEntry>) (List<?>) selection.toList();
                onDelete.accept(selected);
            }));
        }
    }

    /**
     * Adds a menu to set the account or portfolio of the given role for the
     * selected items. The menu is shown only if at least one selected item is
     * booked to the role, and it offers only the options suitable for at least
     * one of these items. An option is applied only to the suitable items.
     * An option is checked if all suitable items are currently booked to it
     * (the same account or portfolio as displayed in the table).
     */
    private <T extends Named> void showApplyToAllItemsMenu(IMenuManager parent, String label, Role role,
                    List<T> options, BiPredicate<Item, T> isSuitable, Function<Item, T> current,
                    BiConsumer<Extractor.Item, T> applier)
    {
        var items = new ArrayList<Item>();
        for (Object element : tableViewer.getStructuredSelection().toList())
        {
            var item = ((ExtractedEntry) element).getItem();
            if (getRequiredRoles(item).contains(role))
                items.add(item);
        }

        var suitableOptions = options.stream() //
                        .filter(option -> items.stream().anyMatch(item -> isSuitable.test(item, option))) //
                        .toList();

        if (suitableOptions.isEmpty())
            return;

        IMenuManager manager = new MenuManager(label);
        parent.add(manager);

        for (T subject : suitableOptions)
        {
            var isCurrent = items.stream() //
                            .filter(item -> isSuitable.test(item, subject)) //
                            .allMatch(item -> subject.equals(current.apply(item)));

            manager.add(new MenuContribution(subject.getName(), () -> {
                for (var item : items)
                {
                    if (isSuitable.test(item, subject))
                        applier.accept(item, subject);
                }

                if (onEntriesChanged != null)
                    onEntriesChanged.run();
                else
                    refresh();
            }, isCurrent));
        }
    }

    /**
     * Returns the accounts and portfolios the item is booked to. Uses the same
     * logic as {@link Item#apply} and the import options of the wizard page.
     */
    private Set<Role> getRequiredRoles(Item item)
    {
        if (item.isFailure() || item.isSkipped())
            return EnumSet.noneOf(Role.class);

        var subject = item.getSubject();

        if (subject instanceof AccountTransaction)
            return EnumSet.of(Role.ACCOUNT);
        else if (subject instanceof PortfolioTransaction)
            return EnumSet.of(Role.PORTFOLIO);
        else if (subject instanceof BuySellEntry)
            return convertToDelivery.getAsBoolean() ? EnumSet.of(Role.PORTFOLIO)
                            : EnumSet.of(Role.ACCOUNT, Role.PORTFOLIO);
        else if (subject instanceof AccountTransferEntry)
            return EnumSet.of(Role.ACCOUNT, Role.OFFSET_ACCOUNT);
        else if (subject instanceof PortfolioTransferEntry)
            return EnumSet.of(Role.PORTFOLIO, Role.OFFSET_PORTFOLIO);

        return EnumSet.noneOf(Role.class);
    }

    /**
     * Returns the currency of the account of the given role. Uses the same
     * currency as the lookup of the account in {@link Item#apply}.
     */
    private String getRequiredCurrency(Item item, Role role)
    {
        var subject = item.getSubject();

        if (role == Role.ACCOUNT)
        {
            if (subject instanceof AccountTransaction transaction)
                return transaction.getCurrencyCode();
            else if (subject instanceof BuySellEntry entry)
                return entry.getAccountTransaction().getCurrencyCode();
            else if (subject instanceof AccountTransferEntry entry)
                return entry.getSourceTransaction().getCurrencyCode();
        }
        else if (role == Role.OFFSET_ACCOUNT && subject instanceof AccountTransferEntry entry)
        {
            return entry.getTargetTransaction().getCurrencyCode();
        }

        return null;
    }

    /**
     * Opens the cell editors (dropdowns) by double click only, so that a
     * single click still just selects the row.
     */
    private void activateEditorsOnDoubleClick()
    {
        var activation = new ColumnViewerEditorActivationStrategy(tableViewer)
        {
            @Override
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event)
            {
                return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION;
            }
        };

        TableViewerEditor.create(tableViewer, activation, ColumnViewerEditor.DEFAULT);
    }

    /**
     * Adds a dropdown to the column which offers the same choices as the
     * context menu for the double clicked entry. Nothing happens if there is
     * no choice for the entry.
     */
    @SuppressWarnings("unchecked")
    private <T> void addDropDownEditor(TableViewerColumn column, Function<ExtractedEntry, List<T>> options,
                    BiFunction<ExtractedEntry, T, String> label, Function<ExtractedEntry, T> current,
                    BiConsumer<ExtractedEntry, T> apply)
    {
        column.setEditingSupport(new EditingSupport(tableViewer)
        {
            private final DropDownCellEditor editor = new DropDownCellEditor(tableViewer.getTable());
            private ExtractedEntry editedEntry;

            {
                editor.setLabelProvider(LabelProvider.createTextProvider(o -> label.apply(editedEntry, (T) o)));
            }

            @Override
            protected boolean canEdit(Object element)
            {
                return !options.apply((ExtractedEntry) element).isEmpty();
            }

            @Override
            protected CellEditor getCellEditor(Object element)
            {
                editedEntry = (ExtractedEntry) element;
                editor.setInput(options.apply(editedEntry));
                return editor;
            }

            @Override
            protected Object getValue(Object element)
            {
                return current.apply((ExtractedEntry) element);
            }

            @Override
            protected void setValue(Object element, Object value)
            {
                if (value != null && !value.equals(getValue(element)))
                    apply.accept((ExtractedEntry) element, (T) value);
            }
        });
    }

    /**
     * Returns the type as it is displayed and imported: with the option to
     * convert buy/sell transactions into deliveries, an unchanged purchase or
     * sale is imported as delivery. Used to check the current type; the
     * conversion of the entry itself stays unchanged.
     */
    private EntryTypeConverter.TypeOption getEffectiveType(ExtractedEntry entry)
    {
        var current = typeConverter.getCurrentType(entry);

        if (current == EntryTypeConverter.TypeOption.ORIGINAL && convertToDelivery.getAsBoolean()
                        && typeConverter.getOriginalItem(entry).getSubject() instanceof BuySellEntry)
            return EntryTypeConverter.TypeOption.DELIVERY;

        return current;
    }

    /**
     * The types offered in the dropdown: the same as in the context menu, and
     * only if the user may change the type.
     */
    private List<EntryTypeConverter.TypeOption> getTypeOptionsForEditor(ExtractedEntry entry)
    {
        return onTypesChanged != null ? typeConverter.getTypeOptions(entry) : List.of();
    }

    /** accounts offered: the active accounts, or all if there is none */
    private List<Account> getAccountOptions()
    {
        return client.getActiveAccounts().isEmpty() ? client.getAccounts() : client.getActiveAccounts();
    }

    /** portfolios offered: the active portfolios, or all if there is none */
    private List<Portfolio> getPortfolioOptions()
    {
        return client.getActivePortfolios().isEmpty() ? client.getPortfolios() : client.getActivePortfolios();
    }

    /** an account is suitable if it has the currency required for the role */
    private boolean isSuitableAccount(Item item, Role role, Account account)
    {
        return account.getCurrencyCode().equals(getRequiredCurrency(item, role));
    }

    private List<Account> getAccountOptions(Item item, Role role)
    {
        if (!getRequiredRoles(item).contains(role))
            return List.of();

        return getAccountOptions().stream().filter(account -> isSuitableAccount(item, role, account)).toList();
    }

    private List<Portfolio> getPortfolioOptions(Item item, Role role)
    {
        return getRequiredRoles(item).contains(role) ? getPortfolioOptions() : List.of();
    }

    /**
     * Applies the account or portfolio chosen in a dropdown to the item and
     * refreshes like the context menu does.
     */
    private <T> void applyToEntry(Item item, T subject, BiConsumer<Item, T> applier)
    {
        applier.accept(item, subject);

        if (onEntriesChanged != null)
            onEntriesChanged.run();
        else
            refresh();
    }

    /**
     * Adds a menu to change the type of the selected entries. For multiple
     * entries, an option is offered if it is available for at least one entry
     * and applied only to these entries. The current type is checked.
     */
    private void showChangeTypeMenu(IMenuManager parent)
    {
        var options = new LinkedHashMap<String, List<Pair<ExtractedEntry, EntryTypeConverter.TypeOption>>>();

        for (Object element : tableViewer.getStructuredSelection().toList())
        {
            var entry = (ExtractedEntry) element;
            var original = typeConverter.getOriginalItem(entry);

            for (var option : typeConverter.getTypeOptions(entry))
                options.computeIfAbsent(typeConverter.getTypeLabel(original, option), k -> new ArrayList<>())
                                .add(new Pair<>(entry, option));
        }

        if (options.isEmpty())
            return;

        IMenuManager manager = new MenuManager(Messages.ColumnTransactionType);
        parent.add(manager);

        for (var option : options.entrySet())
        {
            var targets = option.getValue();
            var isCurrent = targets.stream().allMatch(t -> getEffectiveType(t.getLeft()) == t.getRight());

            manager.add(new MenuContribution(option.getKey(), () -> {
                for (var target : targets)
                    typeConverter.changeType(target.getLeft(), target.getRight());

                onTypesChanged.run();
            }, isCurrent));
        }
    }

    private void selectExistingSecurity(ExtractedEntry entry)
    {
        var labelProvider = LabelProvider.createTextImageProvider(o -> ((Security) o).getName(),
                        o -> LogoManager.instance().getDefaultColumnImage(o, client.getSettings()));
        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);
        dialog.setTitle(Messages.LabelSecurities);
        dialog.setMultiSelection(false);

        dialog.setElements(client.getSecurities().stream().filter(s -> s.getCurrencyCode() != null)
                        .filter(s -> !s.isExchangeRate()).sorted(new Security.ByName()).toList());

        if (dialog.open() == Window.OK)
        {
            Object[] selected = dialog.getResult();
            if (selected.length > 0)
            {
                entry.setImported(false);
                entries.stream().filter(e -> e.getSecurityDependency() == entry)
                                .forEach(e -> e.setSecurityOverride((Security) selected[0]));
                tableViewer.refresh();
            }
        }
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

    /** the accounts and portfolios an item can be booked to */
    private enum Role
    {
        ACCOUNT, OFFSET_ACCOUNT, PORTFOLIO, OFFSET_PORTFOLIO
    }

    /**
     * Dropdown cell editor which opens its list right away and applies a
     * selection immediately (the default applies it only on Enter or when the
     * focus is lost).
     */
    private static final class DropDownCellEditor extends ComboBoxViewerCellEditor
    {
        DropDownCellEditor(Composite parent)
        {
            super(parent, SWT.READ_ONLY);
            setContentProvider(ArrayContentProvider.getInstance());

            // registered after the listener of the super class which stores
            // the selected value
            ((CCombo) getControl()).addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> focusLost()));
        }

        @Override
        public void activate(ColumnViewerEditorActivationEvent activationEvent)
        {
            super.activate(activationEvent);

            var combo = (CCombo) getControl();
            combo.getDisplay().asyncExec(() -> {
                if (!combo.isDisposed())
                    combo.setListVisible(true);
            });
        }
    }
}
