package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.AbstractClientJob;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

public class ReviewExtractedItemsPage extends AbstractWizardPage
{
    private TableViewer tableViewer;
    private TableViewer errorTableViewer;

    private Label lblPrimaryPortfolio;
    private ComboViewer primaryPortfolio;
    private Label lblSecondaryPortfolio;
    private ComboViewer secondaryPortfolio;
    private Label lblPrimaryAccount;
    private ComboViewer primaryAccount;
    private Label lblSecondaryAccount;
    private ComboViewer secondaryAccount;

    private final Client client;
    private final Extractor extractor;
    private List<File> files;

    private List<Extractor.Item> allItems = new ArrayList<Extractor.Item>();

    public ReviewExtractedItemsPage(Client client, Extractor extractor, List<File> files)
    {
        super("reviewitems"); //$NON-NLS-1$

        this.client = client;
        this.extractor = extractor;
        this.files = files;

        setTitle(extractor.getLabel());
        setDescription(Messages.PDFImportWizardDescription);
    }

    public List<Extractor.Item> getItems()
    {
        return allItems;
    }

    public Portfolio getPrimaryPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) primaryPortfolio.getSelection()).getFirstElement();
    }

    public Portfolio getSecondaryPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) secondaryPortfolio.getSelection()).getFirstElement();
    }

    public Account getPrimaryAccount()
    {
        return (Account) ((IStructuredSelection) primaryAccount.getSelection()).getFirstElement();
    }

    public Account getSecondaryAccount()
    {
        return (Account) ((IStructuredSelection) secondaryAccount.getSelection()).getFirstElement();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Composite targetContainer = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).applyTo(targetContainer);

        lblPrimaryAccount = new Label(targetContainer, SWT.NONE);
        lblPrimaryAccount.setText(Messages.ColumnAccount);
        Combo cmbAccount = new Combo(targetContainer, SWT.READ_ONLY);
        primaryAccount = new ComboViewer(cmbAccount);
        primaryAccount.setContentProvider(ArrayContentProvider.getInstance());
        primaryAccount.setInput(client.getActiveAccounts());
        primaryAccount.addSelectionChangedListener(e -> markDuplicatesAndRefresh(allItems));
        cmbAccount.select(0);

        lblSecondaryAccount = new Label(targetContainer, SWT.NONE);
        lblSecondaryAccount.setText(Messages.LabelTransferTo);
        lblSecondaryAccount.setVisible(false);
        Combo cmbAccountTarget = new Combo(targetContainer, SWT.READ_ONLY);
        secondaryAccount = new ComboViewer(cmbAccountTarget);
        secondaryAccount.setContentProvider(ArrayContentProvider.getInstance());
        secondaryAccount.setInput(client.getActiveAccounts());
        secondaryAccount.getControl().setVisible(false);
        cmbAccountTarget.select(0);

        lblPrimaryPortfolio = new Label(targetContainer, SWT.NONE);
        lblPrimaryPortfolio.setText(Messages.ColumnPortfolio);
        Combo cmbPortfolio = new Combo(targetContainer, SWT.READ_ONLY);
        primaryPortfolio = new ComboViewer(cmbPortfolio);
        primaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        primaryPortfolio.setInput(client.getActivePortfolios());
        primaryPortfolio.addSelectionChangedListener(e -> markDuplicatesAndRefresh(allItems));
        cmbPortfolio.select(0);

        lblSecondaryPortfolio = new Label(targetContainer, SWT.NONE);
        lblSecondaryPortfolio.setText(Messages.LabelTransferTo);
        lblSecondaryPortfolio.setVisible(false);
        Combo cmbPortfolioTarget = new Combo(targetContainer, SWT.READ_ONLY);
        secondaryPortfolio = new ComboViewer(cmbPortfolioTarget);
        secondaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        secondaryPortfolio.setInput(client.getActivePortfolios());
        secondaryPortfolio.getControl().setVisible(false);
        cmbPortfolioTarget.select(0);

        Composite compositeTable = new Composite(container, SWT.NONE);
        Composite errorTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        targetContainer.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(targetContainer, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(70, 0);
        compositeTable.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(compositeTable, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        errorTable.setLayoutData(data);

        //
        // table & columns
        //

        TableColumnLayout layout = new TableColumnLayout();
        compositeTable.setLayout(layout);

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        tableViewer.setContentProvider(new SimpleListContentProvider());

        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumns(tableViewer, layout);
        attachContextMenu(table);

        layout = new TableColumnLayout();
        errorTable.setLayout(layout);
        errorTableViewer = new TableViewer(errorTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        errorTableViewer.setContentProvider(new SimpleListContentProvider());

        table = errorTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        addColumnsExceptionTable(errorTableViewer, layout);
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
                return ((Exception) element).getMessage();
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100, true));
    }

    private void addColumns(TableViewer viewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(Extractor.Item item)
            {
                Date date = item.getDate();
                return date != null ? Values.Date.format(date) : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(Extractor.Item item)
            {
                return item.getTypeInformation();
            }

            @Override
            public Image getImage(Extractor.Item item)
            {
                if (item.getSubject() instanceof AccountTransaction)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                else if (item.getSubject() instanceof PortfolioTransaction)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
                else if (item.getSubject() instanceof Security)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else if (item.getSubject() instanceof BuySellEntry)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
                else if (item.getSubject() instanceof AccountTransferEntry)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                else if (item.getSubject() instanceof PortfolioTransferEntry)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
                else
                    return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(Extractor.Item item)
            {
                return Values.Amount.formatNonZero(item.getAmount());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(Extractor.Item item)
            {
                return Values.Share.formatNonZero(item.getShares());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(Extractor.Item item)
            {
                Security security = item.getSecurity();
                return security != null ? security.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(250, true));
    }

    private void attachContextMenu(final Table table)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> showContextMenu(manager));

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

        boolean atLeastOneImported = false;
        boolean atLeastOneNotImported = false;

        for (Object element : selection.toList())
        {
            Extractor.Item item = (Extractor.Item) element;

            // an item will be imported if it is marked as to be
            // imported *and* not a duplicate
            atLeastOneImported = atLeastOneImported || (item.isImported() && !item.isDuplicate());

            // an item will not be imported if it marked as not to be
            // imported *or* if it is marked as duplicate
            atLeastOneNotImported = atLeastOneNotImported || (!item.isImported() || item.isDuplicate());
        }

        // provide a hint to the user why the item is struck out
        if (selection.size() == 1 && ((Extractor.Item) selection.getFirstElement()).isDuplicate())
        {
            manager.add(new LabelOnly(Messages.LabelPotentialDuplicate));
        }

        if (atLeastOneImported)
        {
            manager.add(new Action(Messages.LabelDoNotImport)
            {
                @Override
                public void run()
                {
                    for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                        ((Extractor.Item) element).setImported(false);

                    tableViewer.refresh();
                }
            });
        }

        if (atLeastOneNotImported)
        {
            manager.add(new Action(Messages.LabelDoImport)
            {
                @Override
                public void run()
                {
                    for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                    {
                        ((Extractor.Item) element).setImported(true);
                        ((Extractor.Item) element).setDuplicate(false);
                    }

                    tableViewer.refresh();
                }
            });
        }
    }

    @Override
    public void beforePage()
    {
        try
        {
            new AbstractClientJob(client, extractor.getLabel())
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    monitor.beginTask(Messages.PDFImportWizardMsgExtracting, files.size());

                    final List<Exception> errors = new ArrayList<Exception>();
                    final List<Extractor.Item> items = extractor.extract(files, errors);

                    // Logging them is not a bad idea if the whole method fails
                    PortfolioPlugin.log(errors);

                    Display.getDefault().asyncExec(() -> setResults(items, errors));

                    return Status.OK_STATUS;
                }
            }.schedule();
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }

    private void setResults(List<Extractor.Item> items, List<Exception> errors)
    {
        markDuplicates(items);

        allItems.addAll(items);
        tableViewer.setInput(allItems);
        errorTableViewer.setInput(errors);

        for (Extractor.Item item : items)
        {
            if (item instanceof Extractor.AccountTransferItem)
            {
                lblSecondaryAccount.setVisible(true);
                secondaryAccount.getControl().setVisible(true);
            }
            else if (item instanceof Extractor.PortfolioTransferItem)
            {
                lblSecondaryPortfolio.setVisible(true);
                secondaryPortfolio.getControl().setVisible(true);
            }
        }
    }

    private void markDuplicatesAndRefresh(List<Extractor.Item> items)
    {
        markDuplicates(items);
        tableViewer.refresh();
    }

    private void markDuplicates(List<Extractor.Item> items)
    {
        Account account = getPrimaryAccount();
        Portfolio portfolio = getPrimaryPortfolio();
        for (Extractor.Item item : items)
        {
            item.setDuplicate(false);
            item.markDuplicates(AccountTransaction.class, account.getTransactions());
            item.markDuplicates(PortfolioTransaction.class, portfolio.getTransactions());
        }
    }

    static class FormattedLabelProvider extends StyledCellLabelProvider
    {
        private static Styler strikeoutStyler = new Styler()
        {
            @Override
            public void applyStyles(TextStyle textStyle)
            {
                textStyle.strikeout = true;
            }
        };

        public String getText(Extractor.Item element)
        {
            return null;
        }

        public Image getImage(Extractor.Item element)
        {
            return null;
        }

        @Override
        public void update(ViewerCell cell)
        {
            Extractor.Item item = (Extractor.Item) cell.getElement();
            String text = getText(item);
            if (text == null)
                text = ""; //$NON-NLS-1$

            boolean strikeout = !item.isImported() || item.isDuplicate();
            StyledString styledString = new StyledString(text, strikeout ? strikeoutStyler : null);

            cell.setText(styledString.toString());
            cell.setStyleRanges(styledString.getStyleRanges());
            cell.setImage(getImage(item));

            super.update(cell);
        }
    }
}
