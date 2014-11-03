package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
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
    private ComboViewer portfolio;
    private ComboViewer account;

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

    public Portfolio getPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) portfolio.getSelection()).getFirstElement();
    }

    public Account getAccount()
    {
        return (Account) ((IStructuredSelection) account.getSelection()).getFirstElement();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Label lblAccount = new Label(container, SWT.NONE);
        lblAccount.setText(Messages.ColumnAccount);
        Combo cmbAccount = new Combo(container, SWT.READ_ONLY);
        account = new ComboViewer(cmbAccount);
        account.setContentProvider(ArrayContentProvider.getInstance());
        account.setInput(client.getAccounts());
        cmbAccount.select(0);

        Label lblPortfolio = new Label(container, SWT.NONE);
        lblPortfolio.setText(Messages.ColumnPortfolio);
        Combo cmbPortfolio = new Combo(container, SWT.READ_ONLY);
        portfolio = new ComboViewer(cmbPortfolio);
        portfolio.setContentProvider(ArrayContentProvider.getInstance());
        portfolio.setInput(client.getPortfolios());
        cmbPortfolio.select(0);

        Composite compositeTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        FormData data = new FormData();
        data.top = new FormAttachment(cmbAccount, 0, SWT.CENTER);
        lblAccount.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(lblPortfolio, 5);
        data.right = new FormAttachment(50, -5);
        cmbAccount.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbPortfolio, 0, SWT.CENTER);
        lblPortfolio.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbAccount, 5);
        data.left = new FormAttachment(cmbAccount, 0, SWT.LEFT);
        data.right = new FormAttachment(50, -5);
        cmbPortfolio.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbPortfolio, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        data.width = 100;
        data.height = 100;
        compositeTable.setLayoutData(data);

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
    }

    private void addColumns(TableViewer viewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Date date = ((Extractor.Item) element).getDate();
                return date != null ? Values.Date.format(date) : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Extractor.Item) element).getTypeInformation();
            }

            @Override
            public Image getImage(Object element)
            {
                Extractor.Item item = (Extractor.Item) element;

                if (item.getSubject() instanceof AccountTransaction)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                else if (item.getSubject() instanceof PortfolioTransaction)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
                else if (item.getSubject() instanceof Security)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else if (item.getSubject() instanceof BuySellEntry)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
                else
                    return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.formatNonZero(((Extractor.Item) element).getAmount());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof Extractor.BuySellEntryItem)
                    return Values.Share.format(((BuySellEntry) ((Extractor.Item) element).getSubject())
                                    .getPortfolioTransaction().getShares());
                else
                    return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Security security = ((Extractor.Item) element).getSecurity();
                return security != null ? security.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(250, true));
    }

    private void attachContextMenu(final Table table)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                manager.add(new Action(Messages.LabelDelete)
                {
                    @Override
                    public void run()
                    {
                        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();

                        allItems.removeAll(selection.toList());
                        tableViewer.remove(selection.toArray());
                    }
                });
            }
        });

        final Menu contextMenu = menuMgr.createContextMenu(table.getShell());
        table.setMenu(contextMenu);

        table.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (contextMenu != null && !contextMenu.isDisposed())
                    contextMenu.dispose();
            }
        });
    }

    @Override
    public void beforePage()
    {
        try
        {
            getContainer().run(true, false, new IRunnableWithProgress()
            {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    monitor.beginTask(Messages.PDFImportWizardMsgExtracting, files.size());

                    final List<Extractor.Item> items = extractor.extract(files);

                    Display.getDefault().asyncExec(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            allItems.addAll(items);
                            tableViewer.setInput(allItems);
                        }
                    });
                }
            });
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }
}
