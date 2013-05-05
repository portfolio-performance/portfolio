package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NewPlanDialog extends Dialog 
{
    
    public class Model{
        String name;
        Portfolio portfolio;
        float amount;
        Security security;
        Date start;
        int period;
    }
    
    private static final int OFFSET = 10;

    private Client client;
    Label nameLabel, amountLabel, portfolioLabel;
    Text nameText, amountText;
    Combo portfolioDropDown;
    
    public NewPlanDialog(Shell shell, Client client)
    {
        super(shell);
        this.client = client;
    }
    
    public Model getModel() {
        Model result = new Model();
        result.name = nameText.getText();
        result.amount = Float.parseFloat(amountText.getText());
        result.portfolio = (Portfolio) portfolioDropDown.getData(portfolioDropDown.getText());
        return result;
    }
    
    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = OFFSET;
        formLayout.marginHeight = OFFSET;
        editArea.setLayout(formLayout);
        
       
        nameLabel = new Label(editArea,SWT.NULL);
        nameLabel.setText("Name");
        nameText = new Text(editArea, SWT.BORDER | SWT.SINGLE);
        amountLabel = new Label(editArea,SWT.NULL);
        amountLabel.setText("Amount");
        amountText = new Text(editArea, SWT.BORDER | SWT.SINGLE);
        portfolioLabel = new Label(editArea, SWT.NULL);
        portfolioLabel.setText("Portfolio");
        portfolioDropDown = new Combo(editArea, SWT.DROP_DOWN );
        for (Portfolio portfolio: client.getPortfolios()) {
            portfolioDropDown.add(portfolio.getName());
            portfolioDropDown.setData(portfolio.getName(), portfolio);
        }
        
        FormData nameLabelData = new FormData();
        nameLabelData.left = new FormAttachment(0,0);
        nameLabelData.top = new FormAttachment(0,OFFSET);
        nameLabel.setLayoutData(nameLabelData);
        
        FormData nameTextData = new FormData();
        nameTextData.left = new FormAttachment(nameLabel, 0);
        nameTextData.top = new FormAttachment(0,OFFSET);
        nameTextData.right = new FormAttachment(100,0);
        nameText.setLayoutData(nameTextData);
        
        FormData amountLabelData = new FormData();
        amountLabelData.left = new FormAttachment(0,0);
        amountLabelData.top = new FormAttachment(nameText,OFFSET);
        amountLabel.setLayoutData(amountLabelData);
        
        FormData amountTextData = new FormData();
        amountTextData.left = new FormAttachment(amountLabel, 0);
        amountTextData.top = new FormAttachment(nameText, OFFSET);
        amountTextData.right = new FormAttachment(100,0);
        amountText.setLayoutData(amountTextData);
        
        FormData portfolioLabelData = new FormData();
        portfolioLabelData.left = new FormAttachment(0,0);
        portfolioLabelData.top = new FormAttachment(amountLabel,OFFSET);
        portfolioLabel.setLayoutData(portfolioLabelData);
        
        FormData portfolioDropDownData = new FormData();
        portfolioDropDownData.left = new FormAttachment(portfolioLabel, 0);
        portfolioDropDownData.right = new FormAttachment(100, 0);
        portfolioDropDownData.top = new FormAttachment(amountText, OFFSET);
        portfolioDropDown.setLayoutData(portfolioDropDownData);
        
        return composite;
    }
 
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Create new Plan");
     }

   

}
