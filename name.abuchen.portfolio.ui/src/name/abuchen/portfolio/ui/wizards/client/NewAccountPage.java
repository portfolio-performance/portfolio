package name.abuchen.portfolio.ui.wizards.client;

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
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class NewAccountPage extends AbstractWizardPage
{
    private Client client;
    private TableViewer tViewer;

    public NewAccountPage(Client client)
    {
        super(NewAccountPage.class.getSimpleName());
        this.client = client;
        setTitle(Messages.NewFileWizardAccountTitle);
        setDescription(Messages.NewFileWizardAccountDescription);
    }

    @Override
    public void beforePage()
    {
        tViewer.refresh();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

        Label lblAcc = new Label(container, SWT.NULL);
        lblAcc.setText(Messages.ColumnAccount);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblAcc);

        final Text accountName = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(accountName);

        Button button = new Button(container, SWT.PUSH);
        button.setText(Messages.NewFileWizardButtonAdd);
        GridDataFactory.fillDefaults().applyTo(button);

        Composite tableContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().span(3, 1).grab(true, true).applyTo(tableContainer);
        TableColumnLayout layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        tViewer = new TableViewer(tableContainer);

        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String acnName = accountName.getText();
                if (acnName.length() > 0)
                {
                    Account currentAccount = new Account();
                    currentAccount.setName(acnName);
                    currentAccount.setCurrencyCode(client.getBaseCurrency());
                    client.addAccount(currentAccount);
                    tViewer.refresh();

                    accountName.setText(""); //$NON-NLS-1$
                    accountName.setFocus();
                }
            }
        });

        Table table = tViewer.getTable();
        table.setHeaderVisible(true);
        table.setEnabled(false);

        tViewer.setContentProvider(ArrayContentProvider.getInstance());
        tViewer.setInput(client.getAccounts());
        TableViewerColumn aCol = new TableViewerColumn(tViewer, SWT.NONE);
        layout.setColumnData(aCol.getColumn(), new ColumnWeightData(50));
        aCol.getColumn().setText(Messages.ColumnAccount);
        aCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Account) element).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.ACCOUNT.image();
            }
        });
        container.pack();
        setPageComplete(true);
    }

}
