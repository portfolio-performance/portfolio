package name.abuchen.portfolio.ui.wizards.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class NewPortfolioAccountPage extends AbstractWizardPage
{
    private Client client;

    public NewPortfolioAccountPage(Client client)
    {
        super(NewPortfolioAccountPage.class.getSimpleName());
        this.client = client;
        setTitle(Messages.NewFileWizardPortfolioTitle);
        setDescription(Messages.NewFileWizardPortfolioDescription);
    }

    class Pair
    {
        private final String portfolio;
        private final String account;

        public Pair(String p, String a)
        {
            portfolio = p;
            account = a;
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);

        Label lblPort = new Label(container, SWT.NULL);
        lblPort.setText(Messages.ColumnPortfolio);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblPort);

        final Text portfolioName = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(portfolioName);

        Label lblAcc = new Label(container, SWT.NULL);
        lblAcc.setText(Messages.ColumnReferenceAccount);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblAcc);

        final Text accountName = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(accountName);

        final List<Pair> data = new ArrayList<>();
        Button button = new Button(container, SWT.PUSH);
        button.setText(Messages.NewFileWizardButtonAdd);
        GridDataFactory.fillDefaults().applyTo(button);

        Composite tableContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().span(5, 1).grab(true, true).applyTo(tableContainer);
        TableColumnLayout layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        final TableViewer tViewer = new TableViewer(tableContainer);

        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String portName = portfolioName.getText();
                String acnName = accountName.getText();
                if (portName.length() > 0 && acnName.length() > 0)
                {
                    Account account = new Account();
                    account.setName(acnName);
                    account.setCurrencyCode(client.getBaseCurrency());
                    Portfolio portfolio = new Portfolio();
                    portfolio.setName(portName);
                    portfolio.setReferenceAccount(account);
                    client.addAccount(account);
                    client.addPortfolio(portfolio);
                    data.add(new Pair(portName, acnName));
                    tViewer.refresh();

                    // delete previous input
                    accountName.setText(""); //$NON-NLS-1$
                    portfolioName.setText(""); //$NON-NLS-1$

                    // focus first input field
                    portfolioName.setFocus();

                    setPageComplete(true);
                }
            }
        });

        Table table = tViewer.getTable();
        table.setEnabled(false);
        table.setHeaderVisible(true);
        table.setLinesVisible(false);

        tViewer.setContentProvider(ArrayContentProvider.getInstance());
        tViewer.setInput(data);
        TableViewerColumn pCol = new TableViewerColumn(tViewer, SWT.NONE);
        layout.setColumnData(pCol.getColumn(), new ColumnWeightData(50));
        pCol.getColumn().setText(Messages.ColumnPortfolio);
        pCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Pair) element).portfolio;
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.PORTFOLIO.image();
            }
        });
        TableViewerColumn aCol = new TableViewerColumn(tViewer, SWT.NONE);
        layout.setColumnData(aCol.getColumn(), new ColumnWeightData(50));
        aCol.getColumn().setText(Messages.ColumnReferenceAccount);
        aCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Pair) element).account;
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.ACCOUNT.image();
            }
        });
        container.pack();
        setPageComplete(false);
    }

}
