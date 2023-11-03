package name.abuchen.portfolio.ui.wizards.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class SearchSecurityWizardPage extends WizardPage
{
    public static final String PAGE_ID = "searchpage"; //$NON-NLS-1$

    private Client client;

    private ResultItem item;

    public SearchSecurityWizardPage(Client client)
    {
        super(PAGE_ID);
        setTitle(Messages.SecurityMenuAddNewSecurity);
        setDescription(Messages.SecurityMenuAddNewSecurityDescription);
        setPageComplete(false);

        this.client = client;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

        final Text searchBox = new Text(container, SWT.BORDER | SWT.SINGLE);
        searchBox.setText(""); //$NON-NLS-1$
        searchBox.setFocus();
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(searchBox);

        final ComboViewer typeBox = new ComboViewer(container, SWT.READ_ONLY);
        typeBox.setContentProvider(ArrayContentProvider.getInstance());
        typeBox.setInput(SecuritySearchProvider.Type.values());
        typeBox.setSelection(new StructuredSelection(SecuritySearchProvider.Type.ALL));

        final Button searchButton = new Button(container, SWT.PUSH);
        searchButton.setText(Messages.LabelSearch);
        searchButton.setEnabled(false);

        final TableViewer resultTable = new TableViewer(container, SWT.FULL_SELECTION);
        CopyPasteSupport.enableFor(resultTable);
        GridDataFactory.fillDefaults().span(3, 1).grab(true, true).applyTo(resultTable.getControl());

        TableColumn column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnName);
        column.setWidth(250);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSymbol);
        column.setWidth(80);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnISIN);
        column.setWidth(100);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnWKN);
        column.setWidth(60);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSecurityType);
        column.setWidth(80);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSecurityExchange);
        column.setWidth(80);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnCurrency);
        column.setWidth(80);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSource);
        column.setWidth(120);

        resultTable.getTable().setHeaderVisible(true);
        resultTable.getTable().setLinesVisible(true);

        final Set<String> existingSymbols = new HashSet<>();
        for (Security s : client.getSecurities())
            existingSymbols.add(s.getTickerSymbol());

        resultTable.setLabelProvider(new ResultItemLabelProvider(existingSymbols));
        resultTable.setContentProvider(ArrayContentProvider.getInstance());

        // don't forward return to the default button
        searchBox.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN)
                e.doit = false;
        });

        Consumer<SelectionEvent> onSearchEvent = e -> doSearch(searchBox.getText(),
                        (SecuritySearchProvider.Type) typeBox.getStructuredSelection().getFirstElement(), resultTable);

        searchBox.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(onSearchEvent));
        searchBox.addModifyListener(e -> {
            if (e.widget instanceof Text txt)
            {
                searchButton.setEnabled(!txt.getText().isBlank());
            }
        });
        searchButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(onSearchEvent));

        resultTable.addSelectionChangedListener(event -> {
            setErrorMessage(null);
            item = (ResultItem) ((IStructuredSelection) event.getSelection()).getFirstElement();

            boolean symbolAlreadyExist = item != null && item.getSymbol() != null && !item.getSymbol().isEmpty()
                            && existingSymbols.contains(item.getSymbol());

            setPageComplete(item != null && !symbolAlreadyExist);

            if (symbolAlreadyExist)
                setErrorMessage(MessageFormat.format(Messages.SearchSecurityWizardPageSymbolAlreadyExistsInfo,
                                item.getSymbol()));
        });

        setControl(container);
    }

    public ResultItem getResult()
    {
        return item;
    }

    private void doSearch(String query, SecuritySearchProvider.Type type, TableViewer resultTable)
    {
        try
        {
            if (query.isBlank())
                return;

            // after searching, selection required to enable finish button
            setPageComplete(false);

            getContainer().run(true, false, progressMonitor -> {
                List<SecuritySearchProvider> providers = Factory.getSearchProvider();

                progressMonitor.beginTask(Messages.SecurityMenuSearch4Securities, providers.size());

                List<ResultItem> result = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                for (SecuritySearchProvider provider : providers)
                {
                    try
                    {
                        progressMonitor.setTaskName(provider.getName());
                        result.addAll(provider.search(query, type));
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);
                        errors.add(provider.getName() + ": " + e.getMessage()); //$NON-NLS-1$
                    }
                    progressMonitor.worked(1);
                }

                Display.getDefault().asyncExec(() -> {
                    resultTable.setInput(result);

                    if (!errors.isEmpty())
                        setErrorMessage(String.join(", ", errors)); //$NON-NLS-1$
                    else
                        setErrorMessage(null);
                });

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
            if (columnIndex != 0)
                return null;

            ResultItem item = (ResultItem) element;

            if (item.hasPrices())
                return Images.VIEW_LINECHART.image();
            else if (item.getOnlineId() != null)
                return Images.ONLINE.image();
            else
                return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            ResultItem item = (ResultItem) element;
            switch (columnIndex)
            {
                case 0:
                    return item.getName();
                case 1:
                    return item.getSymbol();
                case 2:
                    return item.getIsin();
                case 3:
                    return item.getWkn();
                case 4:
                    return item.getType();
                case 5:
                    return item.getExchange();
                case 6:
                    return item.getCurrencyCode() != null ? item.getCurrencyCode() : null;
                case 7:
                    return item.getSource();
                default:
                    throw new IllegalArgumentException(String.valueOf(columnIndex));
            }
        }

        @Override
        public Color getForeground(Object element, int columnIndex)
        {
            ResultItem item = (ResultItem) element;

            if (item.getSymbol() != null && !item.getSymbol().isEmpty() && symbols.contains(item.getSymbol()))
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
