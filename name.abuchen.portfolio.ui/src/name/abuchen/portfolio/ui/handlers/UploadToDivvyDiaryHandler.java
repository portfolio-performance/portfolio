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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.online.impl.DivvyDiaryUploader;
import name.abuchen.portfolio.online.impl.DivvyDiaryUploader.DDPortfolio;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class UploadToDivvyDiaryHandler
{
    record Selection(long divvyDiaryPortfolioId, Portfolio portfolio, boolean includeTransations)
    {
    }

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        if (divvyDiaryApiKey == null)
        {
            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.DivvyDiaryMissingAPIKey);
            return;
        }

        MenuHelper.getActiveClientInput(part).ifPresent(clientInput -> {
            DivvyDiaryUploader uploader = new DivvyDiaryUploader(divvyDiaryApiKey);

            retrieveAndPickPortfolio(shell, clientInput, uploader)
                            .ifPresent(selection -> uploadPortfolio(clientInput, uploader, selection));
        });

    }

    private Optional<Selection> retrieveAndPickPortfolio(Shell shell, ClientInput clientInput,
                    DivvyDiaryUploader uploader)
    {
        List<DDPortfolio> portfolios;

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

        // should not happen because DivvyDiary always creates one portfolio
        if (portfolios.isEmpty())
            return Optional.empty();

        PortfolioSelectionDialog dialog = new PortfolioSelectionDialog(shell);
        dialog.setDivvyDiaryPortfolio(portfolios);

        List<Object> ppPortfolios = new ArrayList<>();
        ppPortfolios.add(name.abuchen.portfolio.Messages.LabelJointPortfolio);
        ppPortfolios.addAll(clientInput.getClient().getPortfolios());
        dialog.setPortfolio(ppPortfolios);

        dialog.setIncludeTransactions(
                        clientInput.getPreferenceStore().getBoolean(UploadToDivvyDiaryHandler.class.getSimpleName()));

        if (dialog.open() == Window.OK)
        {
            DDPortfolio selected = dialog.getSelectedDDPortfolio();

            Portfolio portfolio = null;
            if (dialog.getSelectedPPPortfolio() instanceof Portfolio p)
                portfolio = p;

            clientInput.getPreferenceStore().setValue(UploadToDivvyDiaryHandler.class.getSimpleName(),
                            dialog.includeTransactions());

            return Optional.of(new Selection(selected.id(), portfolio, dialog.includeTransactions()));
        }

        return Optional.empty();
    }

    private void uploadPortfolio(ClientInput clientInput, DivvyDiaryUploader uploader, Selection selection)
    {
        new AbstractClientJob(clientInput.getClient(), Messages.DivvyDiaryMsgUploading)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try
                {
                    CurrencyConverter converter = new CurrencyConverterImpl(clientInput.getExchangeRateProviderFacory(),
                                    clientInput.getClient().getBaseCurrency());

                    Client client = getClient();
                    if (selection.portfolio != null)
                        client = new PortfolioClientFilter(selection.portfolio).filter(client);

                    uploader.upload(client, converter, selection.divvyDiaryPortfolioId, selection.includeTransations);

                    Display.getDefault().asyncExec(() -> MessageDialog.openInformation(ActiveShell.get(),
                                    Messages.LabelInfo, Messages.DivvyDiaryUploadSuccessfulMsg));

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

class PortfolioSelectionDialog extends Dialog
{
    private List<Object> portfolios;
    private List<DDPortfolio> ddPortfolios;

    private Object selectedPortfolio;
    private DDPortfolio selectedDDPortfolio;

    private TableViewer portfoliosTableViewer;
    private TableViewer ddTableViewer;

    private boolean includeTransactions = false;

    public PortfolioSelectionDialog(Shell parentShell)
    {
        super(parentShell);
    }

    public void setDivvyDiaryPortfolio(List<DDPortfolio> ddPortfolios)
    {
        this.ddPortfolios = ddPortfolios;
    }

    public void setPortfolio(List<Object> portfolios)
    {
        this.portfolios = portfolios;
    }

    public Object getSelectedPPPortfolio()
    {
        return selectedPortfolio;
    }

    public DDPortfolio getSelectedDDPortfolio()
    {
        return selectedDDPortfolio;
    }

    public boolean includeTransactions()
    {
        return includeTransactions;
    }

    public void setIncludeTransactions(boolean includeTransactions)
    {
        this.includeTransactions = includeTransactions;
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
        label.setText(Messages.DivvyDiaryConfirmUpload);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(label);

        label = new Label(container, SWT.NONE);
        label.setText(Messages.ColumnPortfolio);

        label = new Label(container, SWT.NONE);
        label.setText(Messages.PrefTitleDivvyDiary);

        buildPortfolioTable(container);
        buildDivvyDiaryTable(container);

        Button btnCheckbox = new Button(container, SWT.CHECK);
        btnCheckbox.setText(Messages.DivvyDiaryIncludeTransactionHistory);
        btnCheckbox.setSelection(includeTransactions);
        btnCheckbox.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> includeTransactions = btnCheckbox.getSelection()));
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(btnCheckbox);

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

    private void buildDivvyDiaryTable(Composite container)
    {
        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        ddTableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        ddTableViewer.setUseHashlookup(true);
        CopyPasteSupport.enableFor(ddTableViewer);

        Table table = ddTableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(ddTableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        ddTableViewer.setLabelProvider(LabelProvider.createTextProvider(e -> ((DDPortfolio) e).name()));
        ddTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        ddTableViewer.setInput(ddPortfolios);

        ddTableViewer.setSelection(new StructuredSelection(ddPortfolios.get(0)));
    }

    @Override
    protected void okPressed()
    {
        this.selectedPortfolio = portfoliosTableViewer.getStructuredSelection().getFirstElement();
        this.selectedDDPortfolio = (DDPortfolio) ddTableViewer.getStructuredSelection().getFirstElement();
        super.okPressed();
    }
}
