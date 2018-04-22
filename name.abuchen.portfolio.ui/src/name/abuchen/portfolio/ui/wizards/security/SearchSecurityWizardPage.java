package name.abuchen.portfolio.ui.wizards.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class SearchSecurityWizardPage extends WizardPage
{
    private Client client;

    private ResultItem item;

    public SearchSecurityWizardPage(Client client)
    {
        super("searchpage"); //$NON-NLS-1$
        setTitle(Messages.SecurityMenuAddNewSecurity);
        setDescription(Messages.SecurityMenuAddNewSecurityDescription);

        this.client = client;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        final Text searchBox = new Text(container, SWT.BORDER | SWT.SINGLE);
        searchBox.setText(""); //$NON-NLS-1$
        searchBox.setFocus();
        GridDataFactory.fillDefaults().grab(true, false).applyTo(searchBox);

        final TableViewer resultTable = new TableViewer(container, SWT.FULL_SELECTION);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(resultTable.getControl());

        TableColumn column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSymbol);
        column.setWidth(60);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnName);
        column.setWidth(250);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSecurityType);
        column.setWidth(60);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSecurityExchange);
        column.setWidth(80);

        resultTable.getTable().setHeaderVisible(true);
        resultTable.getTable().setLinesVisible(true);

        final Set<String> existingSymbols = new HashSet<>();
        for (Security s : client.getSecurities())
            existingSymbols.add(s.getTickerSymbol());

        resultTable.setLabelProvider(new ResultItemLabelProvider(existingSymbols));
        resultTable.setContentProvider(ArrayContentProvider.getInstance());

        searchBox.addTraverseListener(new TraverseListener()
        {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                // don't forward to the default button
                e.doit = false;
            }
        });

        searchBox.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetDefaultSelected(SelectionEvent event)
            {
                doSearch(searchBox.getText(), resultTable);
            }
        });

        resultTable.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                item = (ResultItem) ((IStructuredSelection) event.getSelection()).getFirstElement();
                setPageComplete(item != null && item.getSymbol() != null
                                && !existingSymbols.contains(item.getSymbol()));
            }
        });

        setControl(container);
    }

    public ResultItem getResult()
    {
        return item;
    }

    private void doSearch(String query, TableViewer resultTable)
    {
        try
        {
            getContainer().run(true, false, m -> {
                try
                {
                    SecuritySearchProvider provider = Factory.getSearchProvider().get(0);
                    List<ResultItem> result = provider.search(query);
                    Display.getDefault().asyncExec(() -> resultTable.setInput(result));
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);
                    Display.getDefault().asyncExec(() -> setErrorMessage(e.getMessage()));
                }
            });
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private static class ResultItemLabelProvider extends LabelProvider
                    implements ITableLabelProvider, ITableColorProvider
    {
        private final Set<String> symbols;

        public ResultItemLabelProvider(Set<String> symbols)
        {
            this.symbols = symbols;
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            ResultItem item = (ResultItem) element;
            switch (columnIndex)
            {
                case 0:
                    return item.getSymbol();
                case 1:
                    return item.getName();
                case 2:
                    return item.getType();
                case 3:
                    return item.getExchange();
                default:
                    throw new IllegalArgumentException(String.valueOf(columnIndex));
            }
        }

        @Override
        public Color getForeground(Object element, int columnIndex)
        {
            ResultItem item = (ResultItem) element;

            if (item.getSymbol() == null)
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
            else if (symbols.contains(item.getSymbol()))
                return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
            else
                return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex)
        {
            return null;
        }
    }

}
