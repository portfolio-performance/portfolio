package name.abuchen.portfolio.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

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

public class NewPortfolioAccountPage extends AbstractWizardPage
{
    Client client;
    Text portfolioName, accountName;
    Account currentAccount;
    Portfolio currentPortfolio;
   
    public NewPortfolioAccountPage(Client client)
    {
        super("New ...");
        this.client = client;
        setTitle("Create Pairs of Portfolio and Reference Account");
    }
    
    class IdentityColumLabelProvider extends ColumnLabelProvider {
        @Override
        public String getText(Object element)
        {
            return element.toString();
        }
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
        Label lblPort = new Label(inputRow, SWT.NULL);
        lblPort.setText("Portfolio");
        portfolioName = new Text(inputRow, SWT.BORDER | SWT.SINGLE);
        portfolioName.setText("");
        Label lblAcc = new Label(inputRow, SWT.NULL);
        lblAcc.setText("Account");
        accountName = new Text(inputRow, SWT.BORDER | SWT.SINGLE);
        accountName.setText("");
        final List<String> data = new ArrayList<String>();
        Button button =  new Button(inputRow, SWT.PUSH);
        button.setText("+");
        final TableViewer tViewer = new TableViewer(container);
        inputRow.pack();
        button.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
              String portName = portfolioName.getText();
              String acnName = accountName.getText();
              if (portName.length() > 0 && acnName.length()>0) {
                  currentAccount = new Account();
                  currentAccount.setName(acnName);
                  currentPortfolio = new Portfolio();
                  currentPortfolio.setName(portName);
                  currentPortfolio.setReferenceAccount(currentAccount);
                  client.addAccount(currentAccount);
                  client.addPortfolio(currentPortfolio);
                  data.add(portName);
                  data.add(acnName);
                  tViewer.refresh();
                  setPageComplete(true);
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
        TableViewerColumn pCol = new TableViewerColumn(tViewer, SWT.NONE);
        pCol.getColumn().setText("Portfolio");
        pCol.getColumn().setWidth(200);
        pCol.setLabelProvider(new IdentityColumLabelProvider());
        TableViewerColumn aCol = new TableViewerColumn(tViewer, SWT.NONE);
        aCol.getColumn().setText("Referenzkonto");
        aCol.getColumn().setWidth(200);
        aCol.setLabelProvider(new IdentityColumLabelProvider());
        container.pack();
        setPageComplete(false);
    }

}

