package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionFilterCriteria;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionFilterDropDown;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionSearchField;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.TextUtil;

public class AllTransactionsView extends AbstractFinanceView
{

    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private TransactionFilterDropDown transactionFilterMenu;
        private ClientFilterMenu clientFilterMenu;

        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityFilter, Images.FILTER_OFF, SWT.NONE);

            setMenuListener(this);

            transactionFilterMenu = new TransactionFilterDropDown(preferenceStore,
                            AllTransactionsView.class.getSimpleName() + "-transaction-type-filter", //$NON-NLS-1$
                            criteria -> {
                                typeFilter = criteria;
                                updateIcon();
                                notifyModelUpdated();
                            });

            clientFilterMenu = new ClientFilterMenu(getClient(), getPreferenceStore());

            clientFilterMenu.addListener(f -> {
                setInformationPaneInput(null);
                setupFilterInView(f);
                notifyModelUpdated();
                updateIcon();
            });

            clientFilterMenu.trackSelectedFilterConfigurationKey(AllTransactionsView.class.getSimpleName());

            // set initial filter
            setupFilterInView(clientFilterMenu.getSelectedFilter());

            updateIcon();
        }

        private void setupFilterInView(ClientFilter filter)
        {
            if (filter instanceof PortfolioClientFilter pcf)
                AllTransactionsView.this.clientFilter = pcf;
            else
                AllTransactionsView.this.clientFilter = null;
        }

        private void updateIcon()
        {
            boolean hasActiveFilter = clientFilterMenu.hasActiveFilter() || transactionFilterMenu.hasActiveCriteria();
            setImage(hasActiveFilter ? Images.FILTER_ON : Images.FILTER_OFF);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            transactionFilterMenu.menuAboutToShow(manager);

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.MenuChooseClientFilter));
            clientFilterMenu.menuAboutToShow(manager);
        }
    }

    private TransactionsViewer table;

    private TransactionSearchField textFilter;
    private PortfolioClientFilter clientFilter;
    private TransactionFilterCriteria typeFilter = TransactionFilterCriteria.NONE;

    @Inject
    public AllTransactionsView(IPreferenceStore preferenceStore)
    {
        textFilter = new TransactionSearchField(text -> table.refresh(false));
    }

    @Override
    public void notifyModelUpdated()
    {
        var allTransactions = getClient().getAllTransactions();
        table.setInput(allTransactions);

        updateTitle(Messages.LabelAllTransactions + " (" //$NON-NLS-1$
                        + MessageFormat.format(Messages.LabelTransactionCount, allTransactions.size()) + ")"); //$NON-NLS-1$
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAllTransactions;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(textFilter);

        toolBar.add(new Separator());

        toolBar.add(new FilterDropDown(getPreferenceStore()));

        toolBar.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE, manager -> {
            manager.add(new SimpleAction(Messages.LabelAllTransactions + " (CSV)", //$NON-NLS-1$
                            a -> new TableViewerCSVExporter(table.getTableViewer())
                                            .export(Messages.LabelAllTransactions + ".csv"))); //$NON-NLS-1$

            manager.add(new SimpleAction(Messages.LabelAllTransactions + " (JSON)", //$NON-NLS-1$
                            exportToJSON(Messages.LabelAllTransactions + ".json", //$NON-NLS-1$
                                            (List<TransactionPair<?>>) table.getTableViewer().getInput())));

            IStructuredSelection selection = table.getTableViewer().getStructuredSelection();
            if (!selection.isEmpty())
            {
                manager.add(new SimpleAction(Messages.LabelSelectedTransactions + " (CSV)", //$NON-NLS-1$
                                a -> new TableViewerCSVExporter(table.getTableViewer(), true)
                                                .export(Messages.LabelSelectedTransactions + ".csv"))); //$NON-NLS-1$

                manager.add(new SimpleAction(Messages.LabelSelectedTransactions + " (JSON)", //$NON-NLS-1$
                                exportToJSON(Messages.LabelSelectedTransactions + ".json", selection.toList()))); //$NON-NLS-1$
            }

        }));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> table.getColumnSupport().menuAboutToShow(manager)));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        table = new TransactionsViewer(AllTransactionsView.class.getName(), parent, this);
        inject(table);

        table.addFilter(textFilter.getViewerFilter(element -> (TransactionPair<?>) element));

        table.addFilter(new ViewerFilter()
        {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = (TransactionPair<?>) element;
                return typeFilter.matches(tx.getTransaction());
            }
        });

        // We filter the table like this and not by using the
        // clientFilter#filter(Client) method, because that modifies some
        // transactions (e.g. removes the cross entry for purchases/sells and
        // adds fictitious removals for dividend payments when only the
        // portfolio without the account passes the filter)
        table.addFilter(new ViewerFilter()
        {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (clientFilter == null)
                    return true;
                TransactionPair<?> tx = (TransactionPair<?>) element;
                // check owner and cross owner
                return clientFilter.hasElement(tx.getOwner()) || (tx.getTransaction().getCrossEntry() != null
                                && clientFilter.hasElement(tx.getTransaction().getCrossEntry() //
                                                .getCrossOwner(tx.getTransaction())));
            }
        });

        table.getTableViewer().addSelectionChangedListener(
                        e -> setInformationPaneInput(e.getStructuredSelection().getFirstElement()));

        notifyModelUpdated();

        return table.getControl();
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
    }

    private SimpleAction.Runnable exportToJSON(String filename, List<TransactionPair<?>> transactions)
    {
        return action -> {
            FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
            dialog.setFilterNames(new String[] { Messages.CSVConfigCSVImportLabelFileJSON });
            dialog.setFilterExtensions(new String[] { "*.json" }); //$NON-NLS-1$

            dialog.setFileName(TextUtil.sanitizeFilename(filename));
            dialog.setOverwrite(true);
            String name = dialog.open();
            if (name == null)
                return;

            File file = new File(name);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
            {
                writer.append(JClient.from(transactions).toJson());
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
            }
        };
    }
}
