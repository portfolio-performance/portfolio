package name.abuchen.portfolio.ui.wizards.splits;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class PreviewTransactionsPage extends AbstractWizardPage
{
    private class TransactionLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            if (columnIndex == 1)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();

                if (t instanceof AccountTransaction)
                    return Images.ACCOUNT.image();
                else if (t instanceof PortfolioTransaction)
                    return Images.PORTFOLIO.image();
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            TransactionPair<?> pair = (TransactionPair<?>) element;
            Transaction t = pair.getTransaction();

            switch (columnIndex)
            {
                case 0:
                    return Values.DateTime.format(t.getDateTime());
                case 1:
                    if (t instanceof AccountTransaction)
                        return ((AccountTransaction) t).getType().toString();
                    else if (t instanceof PortfolioTransaction)
                        return ((PortfolioTransaction) t).getType().toString();
                    return null;
                case 2:
                    return Values.Share.format(t.getShares());
                case 3:
                    if (model.isChangeTransactions() && t.getDateTime().toLocalDate().isBefore(model.getExDate()))
                    {
                        long shares = t.getShares() * model.getNewShares() / model.getOldShares();
                        return Values.Share.format(shares);
                    }
                    return null;
                case 4:
                    return pair.getOwner().toString();
                default:
                    return null;
            }
        }
    }

    private StockSplitModel model;

    private TableViewer tableViewer;

    public PreviewTransactionsPage(StockSplitModel model)
    {
        super("preview-transactions"); //$NON-NLS-1$

        setTitle(Messages.SplitWizardReviewTransactionsTitle);
        setDescription(Messages.SplitWizardReviewTransactionsDescription);

        this.model = model;
    }

    @Override
    public void beforePage()
    {
        Security security = model.getSecurity();
        List<TransactionPair<?>> transactions = security.getTransactions(model.getClient());
        Collections.sort(transactions, new TransactionPair.ByDate());
        tableViewer.setInput(transactions);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        Button checkbox = new Button(container, SWT.CHECK);
        checkbox.setText(Messages.SplitWizardLabelUpdateTransactions);

        Composite tableContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tableContainer);

        TableColumnLayout layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        tableViewer = new TableViewer(tableContainer, SWT.BORDER);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnDate);
        layout.setColumnData(column, new ColumnPixelData(100, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnTransactionType);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.RIGHT);
        column.setText(Messages.ColumnCurrentShares);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.RIGHT);
        column.setText(Messages.ColumnUpdatedShares);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnSource);
        layout.setColumnData(column, new ColumnPixelData(200, true));

        tableViewer.setLabelProvider(new TransactionLabelProvider());
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        // bindings

        DataBindingContext context = new DataBindingContext();

        IObservableValue<?> targetObservable = WidgetProperties.buttonSelection().observe(checkbox);
        IObservableValue<?> modelObservable = BeanProperties.value("changeTransactions").observe(model); //$NON-NLS-1$
        context.bindValue(targetObservable, modelObservable);

        checkbox.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                tableViewer.refresh();
            }
        });
    }
}
