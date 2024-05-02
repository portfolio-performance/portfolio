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
import name.abuchen.portfolio.online.impl.MyDividends24Uploader;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class UploadToMyDividends24Handler
{
    record Selection(String md24portfolio, Portfolio portfolio)
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
        if (myDividends24ApiKey == null || myDividends24ApiKey.isBlank())
        {
            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.MyDividends24MissingAPIKey);
            return;
        }

        MenuHelper.getActiveClientInput(part).ifPresent(clientInput -> {
            MyDividends24Uploader uploader = new MyDividends24Uploader(myDividends24ApiKey);

            retrieveAndPickPortfolio(shell, uploader, clientInput)
                            .ifPresent(selection -> uploadPortfolio(clientInput, uploader, selection));
        });

    }

    private Optional<Selection> retrieveAndPickPortfolio(Shell shell, MyDividends24Uploader uploader,
                    ClientInput clientInput)
    {
        List<String> md24portfolios;

        try
        {
            md24portfolios = uploader.getPortfolios();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(ActiveShell.get(), Messages.LabelError, e.getMessage());
            return Optional.empty();
        }

        // should not happen because MyDividends24 always creates one portfolio
        if (md24portfolios.isEmpty())
            return Optional.empty();

        MyDividends24PortfolioSelector dialog = new MyDividends24PortfolioSelector(shell);
        dialog.setMyDividendsPortfolio(md24portfolios);

        List<Object> ppPortfolios = new ArrayList<>();
        ppPortfolios.add(name.abuchen.portfolio.Messages.LabelJointPortfolio);
        ppPortfolios.addAll(clientInput.getClient().getPortfolios());
        dialog.setPortfolio(ppPortfolios);

        if (dialog.open() == Window.OK)
        {
            Portfolio portfolio = null;
            if (dialog.getSelectedPortfolio() instanceof Portfolio p)
                portfolio = p;

            String md24Portfolio = dialog.getSelectedMyDividends24Portfolio();
            if (md24Portfolio != null)
                return Optional.of(new Selection(md24Portfolio, portfolio));

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
                    uploader.upload(getClient(), selection.md24portfolio, selection.portfolio);

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

class MyDividends24PortfolioSelector extends Dialog
{

    private List<Object> portfolios;
    private List<String> myDividends24Portfolios;

    private Object selectedPortfolio;
    private String myDividends24SelectedPortfolio;

    private TableViewer portfoliosTableViewer;
    private TableViewer myDividends24TableViewer;

    protected MyDividends24PortfolioSelector(Shell parentShell)
    {
        super(parentShell);
    }

    public void setMyDividendsPortfolio(List<String> myDividends24Portfolio)
    {
        this.myDividends24Portfolios = myDividends24Portfolio;
    }

    public void setPortfolio(List<Object> portfolios)
    {
        this.portfolios = portfolios;
    }

    public Object getSelectedPortfolio()
    {
        return selectedPortfolio;
    }

    public String getSelectedMyDividends24Portfolio()
    {
        return myDividends24SelectedPortfolio;
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

        myDividends24TableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        myDividends24TableViewer.setUseHashlookup(true);
        CopyPasteSupport.enableFor(myDividends24TableViewer);

        Table table = myDividends24TableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(myDividends24TableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        myDividends24TableViewer.setLabelProvider(LabelProvider.createTextProvider(e -> ((String) e)));
        myDividends24TableViewer.setContentProvider(ArrayContentProvider.getInstance());
        myDividends24TableViewer.setInput(myDividends24Portfolios);

        myDividends24TableViewer.setSelection(new StructuredSelection(myDividends24Portfolios.get(0)));
    }

    @Override
    protected void okPressed()
    {
        this.selectedPortfolio = portfoliosTableViewer.getStructuredSelection().getFirstElement();
        this.myDividends24SelectedPortfolio = (String) myDividends24TableViewer.getStructuredSelection()
                        .getFirstElement();
        super.okPressed();
    }
}
