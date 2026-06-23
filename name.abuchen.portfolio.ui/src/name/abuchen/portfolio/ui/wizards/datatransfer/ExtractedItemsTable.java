package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.action.MenuContribution;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.util.TextUtil;

public class ExtractedItemsTable
{
    private final TableViewer tableViewer;
    private final Client client;
    private final List<ExtractedEntry> entries;

    private Runnable onEntriesChanged;
    private Consumer<List<ExtractedEntry>> onDelete;
    private Consumer<ExtractedEntry> onEdit;

    public ExtractedItemsTable(Composite parent, Client client, List<ExtractedEntry> entries)
    {
        this.client = client;
        this.entries = entries;

        var layout = new TableColumnLayout();
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

    public void refresh()
    {
        tableViewer.refresh();
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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(22, true));

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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(250, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnAccount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Account a = entry.getItem().getAccountPrimary();
                return a != null ? a.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnOffsetAccount);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Account a = entry.getItem().getAccountSecondary();
                return a != null ? a.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnPortfolio);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Portfolio p = entry.getItem().getPortfolioPrimary();
                return p != null ? p.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnOffsetPortfolio);
        column.setLabelProvider(new FormattedLabelProvider() // NOSONAR
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Portfolio p = entry.getItem().getPortfolioSecondary();
                return p != null ? p.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));
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

        showApplyToAllItemsMenu(manager, Messages.ColumnAccount, client::getAccounts, Item::setAccountPrimary);
        showApplyToAllItemsMenu(manager, Messages.ColumnOffsetAccount, client::getAccounts, Item::setAccountSecondary);
        showApplyToAllItemsMenu(manager, Messages.ColumnPortfolio, client::getPortfolios, Item::setPortfolioPrimary);
        showApplyToAllItemsMenu(manager, Messages.ColumnOffsetPortfolio, client::getPortfolios,
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

    private <T extends Named> void showApplyToAllItemsMenu(IMenuManager parent, String label, Supplier<List<T>> options,
                    BiConsumer<Extractor.Item, T> applier)
    {
        IMenuManager manager = new MenuManager(label);
        parent.add(manager);

        for (T subject : options.get())
        {
            manager.add(new MenuContribution(subject.getName(), () -> {
                for (Object element : tableViewer.getStructuredSelection().toList())
                    applier.accept(((ExtractedEntry) element).getItem(), subject);

                if (onEntriesChanged != null)
                    onEntriesChanged.run();
                else
                    tableViewer.refresh();
            }));
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
}
