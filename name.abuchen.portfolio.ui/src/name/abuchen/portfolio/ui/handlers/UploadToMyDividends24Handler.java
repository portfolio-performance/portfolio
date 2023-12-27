package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.online.impl.MyDividends24Uploader;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.util.Pair;

public class UploadToMyDividends24Handler
{
    record Selection(String stringValue, Portfolio portfolio)
    {
    }

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.MYDIVIDENDS24_API_KEY) String myDividends24ApiKey)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.MYDIVIDENDS24_API_KEY) String myDividends24ApiKey)
    {
        if (myDividends24ApiKey == null)
        {
            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.MyDividends24MissingAPIKey);
            return;
        }

        MenuHelper.getActiveClientInput(part).ifPresent(clientInput -> {
            MyDividends24Uploader uploader = new MyDividends24Uploader(myDividends24ApiKey);

            retrieveAndPickPortfolio(shell, uploader, clientInput)
                            .ifPresent(portfolioId -> uploadPortfolio(clientInput, uploader, portfolioId));
        });

    }

    @SuppressWarnings("unchecked")
    private Optional<Selection> retrieveAndPickPortfolio(Shell shell, MyDividends24Uploader uploader,
                    ClientInput clientInput)
    {
        List<Pair<Integer, String>> portfolios;

        try
        {
            portfolios = uploader.getPortfolios();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(ActiveShell.get(), Messages.LabelError, e.getMessage());
            return Optional.empty();
        }

        // should not happen because MyDividends24 always creates one portfolio
        if (portfolios.isEmpty())
            return Optional.empty();

        /*
         * if (portfolios.size() == 1) { if (!MessageDialog.openConfirm(shell,
         * Messages.LabelInfo, Messages.MyDividends24ConfirmUpload)) return
         * Optional.empty(); return Optional.of(portfolios.get(0).getValue()); }
         */

        else
        {
            PortfolioSelectionDialog2 dialog = new PortfolioSelectionDialog2(shell);
            dialog.setMyDividendsPortfolio(portfolios);

            List<Object> ppPortfolios = new ArrayList<>();
            ppPortfolios.add(name.abuchen.portfolio.Messages.LabelJointPortfolio);
            ppPortfolios.addAll(clientInput.getClient().getPortfolios());
            dialog.setPortfolio(ppPortfolios);




            if (dialog.open() == Window.OK)
            {
                Pair<Integer, String> selected = dialog.getSelectedMyDividendsPortfolio();

                Portfolio portfolio = null;
                if (dialog.getSelectedPPPortfolio() instanceof Portfolio p)
                    portfolio = p;


                if (selected != null && portfolio != null)
                {
                    Selection selection = new Selection(selected.getValue(), portfolio);
                    return Optional.of(selection);
                }

            }
        }


        return Optional.empty();
    }

    private void uploadPortfolio(ClientInput clientInput, MyDividends24Uploader uploader, Selection selection)
    {
        new AbstractClientJob(clientInput.getClient(), Messages.MyDividends24MsgUploading)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try
                {
                    CurrencyConverter converter = new CurrencyConverterImpl(clientInput.getExchangeRateProviderFacory(),
                                    clientInput.getClient().getBaseCurrency());

                    uploader.upload(getClient(), converter, selection.stringValue, selection.portfolio);

                    Display.getDefault().asyncExec(() -> MessageDialog.openInformation(ActiveShell.get(),
                                    Messages.LabelInfo, Messages.MyDividends24UploadSuccessfulMsg));

                    return Status.OK_STATUS;
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);

                    Display.getDefault().asyncExec(() -> MessageDialog.openError(ActiveShell.get(), Messages.LabelError,
                                    e.getMessage()));

                    return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e);
                }
                catch (RuntimeException e)
                {
                    PortfolioPlugin.log(e);
                    throw e;
                }
            }
        }.schedule();
    }
}

class PortfolioSelectionDialog2 extends Dialog
{

    private List<Object> portfolios;
    private List<Pair<Integer, String>> myDividendsPortfolios;

    private Object selectedPortfolio;
    private Pair<Integer, String> selectedMyDividendsPortfolio;

    private TableViewer portfoliosTableViewer;
    private TableViewer myDividendsTableViewer;

    private boolean includeTransactions = false;

    protected PortfolioSelectionDialog2(Shell parentShell)
    {
        super(parentShell);
        // TODO Auto-generated constructor stub
    }

    public void setMyDividendsPortfolio(List<Pair<Integer, String>> myDividendsPortfolios)
    {
        this.myDividendsPortfolios = myDividendsPortfolios;
    }

    public void setPortfolio(List<Object> portfolios)
    {
        this.portfolios = portfolios;
    }

    public Object getSelectedPPPortfolio()
    {
        return selectedPortfolio;
    }

    public Pair<Integer, String> getSelectedMyDividendsPortfolio()
    {
        return selectedMyDividendsPortfolio;
    }

    @Override
    protected void setShellStyle(int newShellStyle)
    {
        super.setShellStyle(newShellStyle | SWT.RESIZE);
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        getShell().setText(Messages.LabelInfo);
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(400, 300).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(container);

        Label label = new Label(container, SWT.NONE | SWT.WRAP);
        label.setText(Messages.MyDividends24ConfirmUpload);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(label);

        label = new Label(container, SWT.NONE);
        label.setText(Messages.ColumnPortfolio);

        label = new Label(container, SWT.NONE);
        label.setText(Messages.PrefTitleMyDividends24);

        buildPortfolioTable(container);
        buildMyDividendsTable(container);

        /*
         * Button btnCheckbox = new Button(container, SWT.CHECK);
         * btnCheckbox.setText(Messages.DivvyDiaryIncludeTransactionHistory);
         * btnCheckbox.setSelection(includeTransactions);
         * btnCheckbox.addSelectionListener(
         * SelectionListener.widgetSelectedAdapter(e -> includeTransactions =
         * btnCheckbox.getSelection())); GridDataFactory.fillDefaults().span(2,
         * 1).grab(true, false).applyTo(btnCheckbox);
         */
        return composite;
    }

    private void buildPortfolioTable(Composite container)
    {
        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        portfoliosTableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        portfoliosTableViewer.setUseHashlookup(true);
        CopyPasteSupport.enableFor(portfoliosTableViewer);

        Table table = portfoliosTableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(portfoliosTableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        portfoliosTableViewer.setLabelProvider(LabelProvider.createTextProvider(String::valueOf));
        portfoliosTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        portfoliosTableViewer.setInput(portfolios);

        portfoliosTableViewer.setSelection(new StructuredSelection(portfolios.get(0)));
    }

    private void buildMyDividendsTable(Composite container)
    {
        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        myDividendsTableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        myDividendsTableViewer.setUseHashlookup(true);
        CopyPasteSupport.enableFor(myDividendsTableViewer);

        Table table = myDividendsTableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(myDividendsTableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        myDividendsTableViewer.setLabelProvider(
                        LabelProvider.createTextProvider(e -> ((Pair<Integer, String>) e).getValue()));
        myDividendsTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        myDividendsTableViewer.setInput(myDividendsPortfolios);

        myDividendsTableViewer.setSelection(new StructuredSelection(myDividendsPortfolios.get(0)));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void okPressed()
    {
        this.selectedPortfolio = portfoliosTableViewer.getStructuredSelection().getFirstElement();
        this.selectedMyDividendsPortfolio = (Pair<Integer, String>) myDividendsTableViewer.getStructuredSelection()
                        .getFirstElement();
        super.okPressed();
    }
}
