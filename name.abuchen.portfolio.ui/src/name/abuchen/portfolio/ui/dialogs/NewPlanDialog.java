package name.abuchen.portfolio.ui.dialogs;

import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
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
    Combo portfolioDropDown, securityCombo;
    DateTime dateBox;
    Spinner spinner;
    Model model;
    
    public NewPlanDialog(Shell shell, Client client)
    {
        super(shell);
        model = new Model();
        this.client = client;
    }
    
    @Override
    protected void okPressed()
    {
        String name = nameText.getText();
        String amountString = amountText.getText();
        String portfolioString = portfolioDropDown.getText();
        String securityString = securityCombo.getText();
        if (name.equals("") || amountString.equals("") || portfolioString.equals("")
                        || securityString.equals("")) {
            MessageDialog.openError(super.getShell(), "Insufficient Input", "Please fill out all Fields");
            return;
        }
        try {
            model.amount = Float.parseFloat(amountString);
        } catch (NumberFormatException ex) {
            MessageDialog.openError(super.getShell(), "Error parsing amount", "The entered text cannot be parsed to a number");
            return;
        }
        model.portfolio = (Portfolio) portfolioDropDown.getData(portfolioString);
        model.security = (Security) securityCombo.getData(securityString);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, dateBox.getYear());
        cal.set(Calendar.MONTH, dateBox.getMonth());
        cal.set(Calendar.DAY_OF_MONTH, dateBox.getDay());
        model.start = cal.getTime();
        model.period = spinner.getSelection();
        model.name = name;
        super.okPressed();
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
        Label secLabel = new Label(editArea, SWT.NULL);
        secLabel.setText("Security");
        securityCombo = new Combo(editArea, SWT.DROP_DOWN);
        for (Security security: client.getSecurities()) {
            securityCombo.add(security.getName());
            securityCombo.setData(security.getName(), security);
        }
        Label dateLabel = new Label(editArea, SWT.NONE);
        dateLabel.setText("Plan Start");
        dateBox = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
        Label periodLabel = new Label(editArea, SWT.NONE);
        periodLabel.setText("Plan period");
        spinner = new Spinner (editArea, SWT.BORDER);
        spinner.setMinimum(1);
        spinner.setMaximum(30);
        spinner.setSelection(5);
        spinner.setIncrement(1);
        spinner.setPageIncrement(10);
        
        FormData nameLabelData = new FormData();
        nameLabelData.left = new FormAttachment(0,0);
        nameLabelData.top = new FormAttachment(0,OFFSET);
        nameLabel.setLayoutData(nameLabelData);
        
        FormData nameTextData = new FormData();
        nameTextData.left = new FormAttachment(periodLabel, OFFSET);
        nameTextData.top = new FormAttachment(nameLabel,0,SWT.CENTER);
        nameTextData.right = new FormAttachment(100,0);
        nameText.setLayoutData(nameTextData);
        
        FormData amountLabelData = new FormData();
        amountLabelData.left = new FormAttachment(0,0);
        amountLabelData.top = new FormAttachment(nameText,OFFSET);
        amountLabel.setLayoutData(amountLabelData);
        
        FormData amountTextData = new FormData();
        amountTextData.left = new FormAttachment(periodLabel, OFFSET);
        amountTextData.top = new FormAttachment(amountLabel, 0, SWT.CENTER);
        amountTextData.right = new FormAttachment(100,0);
        amountText.setLayoutData(amountTextData);
        
        FormData portfolioLabelData = new FormData();
        portfolioLabelData.left = new FormAttachment(0,0);
        portfolioLabelData.top = new FormAttachment(amountText,OFFSET);
        portfolioLabel.setLayoutData(portfolioLabelData);
        
        FormData portfolioDropDownData = new FormData();
        portfolioDropDownData.left = new FormAttachment(periodLabel, OFFSET);
        portfolioDropDownData.right = new FormAttachment(100, 0);
        portfolioDropDownData.top = new FormAttachment(portfolioLabel, 0, SWT.CENTER);
        portfolioDropDown.setLayoutData(portfolioDropDownData);
        
        FormData secLabelData = new FormData();
        secLabelData.left = new FormAttachment(0,0);
        secLabelData.top = new FormAttachment(portfolioDropDown,OFFSET);
        secLabel.setLayoutData(secLabelData);
        
        FormData securityComboData = new FormData();
        securityComboData.left = new FormAttachment(periodLabel,OFFSET);
        securityComboData.top = new FormAttachment(secLabel, 0, SWT.CENTER);
        securityComboData.right = new FormAttachment(100,0);
        securityCombo.setLayoutData(securityComboData);
        
        FormData dateLabelData = new FormData();
        dateLabelData.left = new FormAttachment(0,0);
        dateLabelData.top = new FormAttachment(securityCombo,OFFSET);
        dateLabel.setLayoutData(dateLabelData);
        
        FormData dateData = new FormData();
        dateData.left = new FormAttachment(periodLabel,OFFSET);
        dateData.top = new FormAttachment(dateLabel, 0, SWT.CENTER);
        dateData.right = new FormAttachment(100,0);
        dateBox.setLayoutData(dateData);
        
        FormData periodLabelData = new FormData();
        periodLabelData.left = new FormAttachment(0,0);
        periodLabelData.top = new FormAttachment(dateBox,OFFSET);
        periodLabel.setLayoutData(periodLabelData);
        
        FormData spinnerData = new FormData();
        spinnerData.left = new FormAttachment(periodLabel,OFFSET);
        spinnerData.top = new FormAttachment(periodLabel, 0, SWT.CENTER);
        spinner.setLayoutData(spinnerData);
        
        return composite;
    }
 
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Create new Plan");
     }

   

}
