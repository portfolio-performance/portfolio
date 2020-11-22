package name.abuchen.portfolio.ui.wizards.datatransfer;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ExportSelectionPage extends AbstractWizardPage
{
    private Client client;

    private TreeViewer treeViewer;

    protected ExportSelectionPage(Client client)
    {
        super("export"); //$NON-NLS-1$
        setTitle(Messages.ExportWizardTitle);
        setDescription(Messages.ExportWizardDescription);

        this.client = client;
    }

    public Object getExportItem()
    {
        return ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
    }

    public Class<?> getExportClass()
    {
        TreeItem parentItem = treeViewer.getTree().getSelection()[0].getParentItem();
        return parentItem == null ? null : (Class<?>) parentItem.getData();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        container.setLayout(new FillLayout());

        Composite treeComposite = new Composite(container, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        treeComposite.setLayout(layout);
        treeViewer = new TreeViewer(treeComposite, SWT.BORDER | SWT.SINGLE);
        Tree tree = treeViewer.getTree();
        tree.setHeaderVisible(false);
        tree.setLinesVisible(true);

        TreeColumn column = new TreeColumn(tree, SWT.None);
        column.setText(Messages.ColumnDate);
        layout.setColumnData(column, new ColumnWeightData(100));

        treeViewer.setContentProvider(new ExportItemsContentProvider());
        treeViewer.setLabelProvider(new ExportItemsLabelProvider(client));
        treeViewer.setInput(client);
        treeViewer.setExpandedElements(new Object[] { AccountTransaction.class, PortfolioTransaction.class,
                        Security.class });

        // wiring
        setPageComplete(false);

        treeViewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                Object element = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
                setPageComplete(element != null);
            }
        });

        treeViewer.getTree().addSelectionListener(new SelectionListener()
        {

            @Override
            public void widgetSelected(SelectionEvent event)
            {
                Object element = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
                setPageComplete(element != null);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event)
            {
                if (getWizard().performFinish())
                    ((WizardDialog) getContainer()).close();
            }
        });

    }

    private static class ExportItemsContentProvider implements ITreeContentProvider
    {
        private Client client;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            client = (Client) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return new Class[] { AccountTransaction.class, PortfolioTransaction.class, Security.class,
                            SecurityPrice.class };
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof Class)
            {
                if (parentElement == AccountTransaction.class)
                    return client.getAccounts().stream().sorted(new Account.ByName()).toArray();
                else if (parentElement == PortfolioTransaction.class)
                    return client.getPortfolios().stream().sorted(new Portfolio.ByName()).toArray();
                else if (parentElement == Security.class)
                    return new String[] { Messages.ExportWizardSecurityMasterData,
                                    Messages.ExportWizardMergedSecurityPrices,
                                    Messages.ExportWizardAllTransactionsAktienfreundeNet,
                                    Messages.ExportWizardVINISApp};
                else if (parentElement == SecurityPrice.class)
                    return client.getSecurities().stream().sorted(new Security.ByName()).toArray();
            }

            return null;
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return element instanceof Class;
        }

        @Override
        public void dispose()
        {
        }
    }

    static class ExportItemsLabelProvider extends LabelProvider
    {
        private Client client;
        
        public ExportItemsLabelProvider(Client client) 
        {
            this.client = client;
        }
        
        @Override
        public String getText(Object element)
        {
            if (element instanceof Account)
                return ((Account) element).getName();
            else if (element instanceof Portfolio)
                return ((Portfolio) element).getName();
            else if (element instanceof Security)
                return ((Security) element).getName();
            else if (element == AccountTransaction.class)
                return Messages.ExportWizardAccountTransactions;
            else if (element == PortfolioTransaction.class)
                return Messages.ExportWizardPortfolioTransactions;
            else if (element == Security.class)
                return Messages.ExportWizardSecurities;
            else if (element == SecurityPrice.class)
                return Messages.ExportWizardHistoricalQuotes;
            else if (element instanceof String)
                return (String) element;
            else
                return null;
        }

        @Override
        public Image getImage(Object element)
        {
            return LogoManager.instance().getDefaultColumnImage(element, client.getSettings());
        }
    }
}
