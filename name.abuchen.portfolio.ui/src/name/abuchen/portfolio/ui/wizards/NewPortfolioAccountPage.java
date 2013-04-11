package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewPortfolioAccountPage extends AbstractWizardPage
{
    Client client;
    Text portfolioName, accountName;
    Account currentAccount;
    Portfolio currentPortfolio;
    Composite pairs;

    public NewPortfolioAccountPage(Client client)
    {
        super("New ...");
        this.client = client;
        setTitle("Create Pairs of Portfolio and Reference Account");
    }
    
    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 5;
        Label lblPort = new Label(container, SWT.NULL);
        lblPort.setText("Portfolio");
        portfolioName = new Text(container, SWT.BORDER | SWT.SINGLE);
        portfolioName.setText("");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        portfolioName.setLayoutData(gd);
        Label lblAcc = new Label(container, SWT.NULL);
        lblAcc.setText("Account");
        accountName = new Text(container, SWT.BORDER | SWT.SINGLE);
        accountName.setText("");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        accountName.setLayoutData(gd);
        Button button =  new Button(container, SWT.PUSH);
        button.setText("+");
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
                  new Label(pairs, SWT.NULL).setText(portName);
                  new Label(pairs, SWT.NULL).setText("-->");
                  new Label(pairs, SWT.NULL).setText(acnName);
                  setPageComplete(true);
              }
          }
        }); 
        pairs = new Composite(container, SWT.NULL);
        layout = new GridLayout();
        layout.numColumns = 3;
        pairs.setLayout(layout);
        new Label(pairs, SWT.NULL).setText("Portfolios");
        new Label(pairs, SWT.NULL).setText("-->");
        new Label(pairs, SWT.NULL).setText("Accounts");
        setPageComplete(false);
    }

}
