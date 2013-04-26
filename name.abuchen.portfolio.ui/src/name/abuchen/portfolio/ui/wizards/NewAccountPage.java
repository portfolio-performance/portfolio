package name.abuchen.portfolio.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class NewAccountPage extends AbstractWizardPage
{
    Client client;
    Text accountName;
    Account currentAccount;
   
    public NewAccountPage(Client client)
    {
        super("New ...");
        this.client = client;
        setTitle("Create Accounts wihtout a reference to a Portfolio");
    }
    
    
    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout());
        Composite inputRow = new Composite(container, SWT.NULL);
        RowLayout inputRowLayout = new RowLayout();
        inputRowLayout.type = SWT.HORIZONTAL;
        inputRowLayout.marginHeight = 10;
        inputRowLayout.spacing = 5;
        inputRowLayout.center = true;
        inputRow.setLayout(inputRowLayout);
        Label lblAcc = new Label(inputRow, SWT.NULL);
        lblAcc.setText("Account");
        accountName = new Text(inputRow, SWT.BORDER | SWT.SINGLE);
        accountName.setText("");
        final List<Account> data = new ArrayList<Account>();
        Button button =  new Button(inputRow, SWT.PUSH);
        button.setText("+");
        final TableViewer tViewer = new TableViewer(container);
        inputRow.pack();
        button.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
              String acnName = accountName.getText();
              if (acnName.length()>0) {
                  currentAccount = new Account();
                  currentAccount.setName(acnName);
                  client.addAccount(currentAccount);
                  data.add(currentAccount);
                  tViewer.refresh();
              }
          }
        }); 
        Table table = tViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gridData = new GridData();
        gridData.heightHint = 300;
        table.setLayoutData(gridData);
        tViewer.setContentProvider(ArrayContentProvider.getInstance());
        tViewer.setInput(data);
        TableViewerColumn aCol = new TableViewerColumn(tViewer, SWT.NONE);
        aCol.getColumn().setText("Account");
        aCol.getColumn().setWidth(200);
        aCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element)
            {
                return ((Account) element).getName();
            }
        });
        container.pack();
        setPageComplete(true);
    }

}

