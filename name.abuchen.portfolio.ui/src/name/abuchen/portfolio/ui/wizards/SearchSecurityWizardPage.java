package name.abuchen.portfolio.ui.wizards;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class SearchSecurityWizardPage extends AbstractWizardPage
{
    public static final String PAGE_NAME = "searchpage"; //$NON-NLS-1$

    private EditSecurityModel model;
    private ResultItem item;

    public SearchSecurityWizardPage(EditSecurityModel model)
    {
        super(PAGE_NAME);
        setTitle(Messages.SecurityMenuAddNewSecurity);
        setDescription(Messages.SecurityMenuAddNewSecurityDescription);

        this.model = model;
    }

    @Override
    public void beforePage()
    {}

    @Override
    public void afterPage()
    {
        if (item != null)
        {
            // keep name (overwriting is confusing to the user)
            String name = model.getName();
            if (name == null || name.trim().length() == 0)
                model.setName(item.getName());

            model.setTickerSymbol(item.getSymbol());
            model.setIsin(item.getIsin());
        }
    }

    @Override
    public IWizardPage getNextPage()
    {
        return getWizard().getPage(SecurityMasterDataPage.PAGE_NAME);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

        Label label = new Label(container, SWT.NULL);
        label.setText(Messages.ColumnSymbol);

        final Text searchBox = new Text(container, SWT.BORDER | SWT.SINGLE);
        searchBox.setText(""); //$NON-NLS-1$
        searchBox.setFocus();
        GridDataFactory.fillDefaults().grab(true, false).applyTo(searchBox);

        final TableViewer resultTable = new TableViewer(container, SWT.FULL_SELECTION);
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(resultTable.getControl());

        TableColumn column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSymbol);
        column.setWidth(60);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnName);
        column.setWidth(140);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnISIN);
        column.setWidth(100);

        column = new TableColumn(resultTable.getTable(), SWT.RIGHT);
        column.setText(Messages.ColumnLastTrade);
        column.setWidth(60);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSecurityType);
        column.setWidth(60);

        column = new TableColumn(resultTable.getTable(), SWT.NONE);
        column.setText(Messages.ColumnSecurityExchange);
        column.setWidth(60);

        resultTable.getTable().setHeaderVisible(true);
        resultTable.getTable().setLinesVisible(true);

        final Set<String> existingSymbols = new HashSet<String>();
        for (Security s : model.getClient().getSecurities())
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
                BusyIndicator.showWhile(getContainer().getShell().getDisplay(), new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            SecuritySearchProvider provider = Factory.getSearchProvider().get(0);
                            resultTable.setInput(provider.search(searchBox.getText()));
                        }
                        catch (IOException e)
                        {
                            setErrorMessage(e.getMessage());
                            PortfolioPlugin.log(new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
                        }
                    }
                });
            }
        });

        resultTable.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                item = (ResultItem) ((IStructuredSelection) event.getSelection()).getFirstElement();
                setPageComplete(item != null && item.getSymbol() != null && !existingSymbols.contains(item.getSymbol()));
            }
        });

        setControl(container);
    }

    private static class ResultItemLabelProvider extends LabelProvider implements ITableLabelProvider,
                    ITableColorProvider
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
                    return item.getIsin();
                case 3:
                    if (item.getLastTrade() != 0)
                        return Values.Quote.format(item.getLastTrade());
                    else
                        return null;
                case 4:
                    return item.getType();
                case 5:
                    return item.getExchange();
            }
            return null;
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
