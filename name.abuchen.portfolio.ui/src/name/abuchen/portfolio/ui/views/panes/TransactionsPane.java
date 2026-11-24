package name.abuchen.portfolio.ui.views.panes;

import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionFilterDropDown;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionSearchField;
import name.abuchen.portfolio.ui.views.TransactionsViewer;

public class TransactionsPane implements InformationPanePage
{

    @Inject
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private TransactionsViewer transactions;
    private TransactionSearchField textFilter;
    private TransactionFilterDropDown transactionFilter;

    private Object source;

    @Inject
    public TransactionsPane(IPreferenceStore preferenceStore)
    {
        transactionFilter = new TransactionFilterDropDown(preferenceStore,
                        TransactionsPane.class.getSimpleName() + "-transaction-type-filter", //$NON-NLS-1$
                        criteria -> onRecalculationNeeded());

        textFilter = new TransactionSearchField(text -> onRecalculationNeeded());
    }

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabTransactions;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        transactions = new TransactionsViewer(TransactionsPane.class.getName(), parent, view);
        view.inject(transactions);

        transactions.addFilter(textFilter.getViewerFilter(element -> (TransactionPair<?>) element));

        transactions.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = (TransactionPair<?>) element;
                return transactionFilter.getFilterCriteria().matches(tx.getTransaction());
            }
        });

        return transactions.getControl();
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(textFilter);

        toolBar.add(new Separator());

        transactionFilter.dispose();
        toolBar.add(transactionFilter);

        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(transactions.getTableViewer()).export(getLabel(), getExportLabel())));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> transactions.getColumnSupport().menuAboutToShow(manager)));
    }

    @Override
    public void setInput(Object input)
    {
        var resolved = TransactionPaneInput.resolve(input, client);
        source = resolved.getSource();
        transactions.setInput(resolved.getTransactions());
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (source != null)
            setInput(source);
    }

    private String getExportLabel()
    {
        return TransactionPaneInput.exportLabelFor(source);
    }

    public void notifyModelUpdated()
    {
        onRecalculationNeeded();
    }

    public void markTransactions(List<TransactionPair<?>> list)
    {
        transactions.markTransactions(list);
    }
}
