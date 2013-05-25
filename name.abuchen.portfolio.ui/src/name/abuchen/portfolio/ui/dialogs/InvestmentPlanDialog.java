package name.abuchen.portfolio.ui.dialogs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.InvestmentPlanController;
import name.abuchen.portfolio.ui.dialogs.NewPlanDialog.Model;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class InvestmentPlanDialog extends Dialog implements SelectionListener
{

    private static final int OFFSET = 10;

    private Client client;
    Group group;
    InvestmentPlan plan;
    InvestmentPlanController controller;
    Text nameText, amountText;
    Label portfolioLabel, securityLabel, startLabel;
    Spinner spinner;
    TableViewer tViewer;
    Button createTransactionsButton, delButton;
    Combo comboDropDown;

    public InvestmentPlanDialog(Shell owner, Client client)
    {
        super(owner);
        this.client = client;
    }

    protected Button createButton(Composite parent, int id, String label, boolean defaultButton)
    {
        if (id == IDialogConstants.CANCEL_ID)
            return null;
        return super.createButton(parent, id, label, defaultButton);
    }

    @Override
    public void widgetSelected(SelectionEvent e)
    {
        Combo combo = (Combo) e.getSource();
        plan = (InvestmentPlan) combo.getData(combo.getText());
        controller = new InvestmentPlanController(plan);
        group.setText(plan.getName());
        nameText.setText(plan.getName());
        amountText.setText(new Float(plan.getAmount()).toString());
        portfolioLabel.setText("Portfolio: " + plan.getPortfolio().getName());
        portfolioLabel.pack();
        securityLabel.setText("Security: " + plan.getSecurity().getName());
        securityLabel.pack();
        startLabel.setText("Start: " + new SimpleDateFormat("dd.MM.yyyy").format(plan.getStart()));
        startLabel.pack();
        spinner.setSelection(plan.getPeriod());
        tViewer.setInput(plan.getTransactions());
        tViewer.refresh();
        delButton.setEnabled(true);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e)
    {}

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        editArea.setLayout(new FormLayout());
        FormData comboData = new FormData();
        comboData.left = new FormAttachment(0, 0);
        comboData.top = new FormAttachment(0, 0);
        comboDropDown = new Combo(editArea, SWT.DROP_DOWN);
        comboDropDown.setLayoutData(comboData);
        for (InvestmentPlan plan : client.getPlans())
        {
            comboDropDown.add(plan.getName());
            comboDropDown.setData(plan.getName(), plan);
        }
        comboDropDown.addSelectionListener(this);
        delButton = new Button(editArea, SWT.PUSH);
        delButton.setText("Delete Plan");
        delButton.setEnabled(false);
        FormData delButtonData = new FormData();
        delButtonData.left = new FormAttachment(comboDropDown);
        delButtonData.top = new FormAttachment(0,0);
        delButton.setLayoutData(delButtonData);
        delButton.addSelectionListener(new SelectionListener() 
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                client.removePlan(plan);
                comboDropDown.remove(plan.getName());
                comboDropDown.setData(plan.getName(), null);
                plan = null;
                controller = null;
                group.setText("");
                nameText.setText("");
                amountText.setText("");
                portfolioLabel.setText("Portfolio: ");
                portfolioLabel.pack();
                securityLabel.setText("Security: ");
                securityLabel.pack();
                startLabel.setText("Start: ");
                startLabel.pack();
                spinner.setSelection(0);
                tViewer.setInput(new ArrayList());
                tViewer.refresh();
                delButton.setEnabled(false);
             }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
            
        });
        FormData buttonData = new FormData();
        buttonData.left = new FormAttachment(delButton, OFFSET);
        buttonData.top = new FormAttachment(0, 0);
        Button newButton = new Button(editArea, SWT.PUSH);
        newButton.setLayoutData(buttonData);
        newButton.setText("New Plan");
        newButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                NewPlanDialog newDialog = new NewPlanDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell(), client);
                newDialog.open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });
        group = new Group(editArea, SWT.SHADOW_ETCHED_IN);
        group.setText("Investment Plan");
        group.setLayout(new FormLayout());
        FormData groupData = new FormData();
        groupData.left = new FormAttachment(0, 0);
        groupData.top = new FormAttachment(newButton, OFFSET);
        groupData.right = new FormAttachment(100, 0);
        groupData.width = 500;
        group.setLayoutData(groupData);
        Label periodLabel = new Label(group, SWT.NONE);
        periodLabel.setText("Plan period");
        portfolioLabel = new Label(group, SWT.NONE);
        portfolioLabel.setText("Portfolio: ");
        FormData portLabelData = new FormData();
        portLabelData.left = new FormAttachment(OFFSET / 2, 0);
        portLabelData.top = new FormAttachment(OFFSET / 2, 0);
        portfolioLabel.setLayoutData(portLabelData);
        securityLabel = new Label(group, SWT.NONE);
        securityLabel.setText("Security: ");
        FormData secLabelData = new FormData();
        secLabelData.left = new FormAttachment(OFFSET / 2, 0);
        secLabelData.top = new FormAttachment(portfolioLabel, OFFSET);
        securityLabel.setLayoutData(secLabelData);
        startLabel = new Label(group, SWT.NONE);
        startLabel.setText("Start: ");
        FormData startLblData = new FormData();
        startLblData.left = new FormAttachment(OFFSET / 2, 0);
        startLblData.top = new FormAttachment(securityLabel, OFFSET);
        startLabel.setLayoutData(startLblData);
        Label nameLbl = new Label(group, SWT.NULL);
        nameLbl.setText("Name");
        FormData nameLblData = new FormData();
        nameLblData.left = new FormAttachment(OFFSET / 2, 0);
        nameLblData.top = new FormAttachment(startLabel, OFFSET);
        nameLbl.setLayoutData(nameLblData);
        nameText = new Text(group, SWT.BORDER | SWT.SINGLE);
        FormData nameTextData = new FormData();
        nameTextData.left = new FormAttachment(periodLabel, OFFSET);
        nameTextData.top = new FormAttachment(nameLbl, 0, SWT.CENTER);
        nameTextData.right = new FormAttachment(90, OFFSET);
        nameText.setLayoutData(nameTextData);
        Label amountLbl = new Label(group, SWT.NULL);
        amountLbl.setText("Amount");
        FormData amountLblData = new FormData();
        amountLblData.left = new FormAttachment(OFFSET / 2, 0);
        amountLblData.top = new FormAttachment(nameText, OFFSET);
        amountLbl.setLayoutData(amountLblData);
        amountText = new Text(group, SWT.BORDER | SWT.SINGLE);
        FormData amountTextData = new FormData();
        amountTextData.left = new FormAttachment(periodLabel, OFFSET);
        amountTextData.top = new FormAttachment(amountLbl, 0, SWT.CENTER);
        amountTextData.right = new FormAttachment(90, OFFSET);
        amountText.setLayoutData(amountTextData);
        spinner = new Spinner(group, SWT.BORDER);
        spinner.setMinimum(1);
        spinner.setMaximum(30);
        spinner.setSelection(5);
        spinner.setIncrement(1);
        spinner.setPageIncrement(10);
        FormData periodLabelData = new FormData();
        periodLabelData.left = new FormAttachment(OFFSET / 2, 0);
        periodLabelData.top = new FormAttachment(amountText, OFFSET);
        periodLabel.setLayoutData(periodLabelData);
        FormData spinnerData = new FormData();
        spinnerData.left = new FormAttachment(periodLabel, OFFSET);
        spinnerData.top = new FormAttachment(periodLabel, 0, SWT.CENTER);
        spinner.setLayoutData(spinnerData);
        tViewer = new TableViewer(group);
        Table table = tViewer.getTable();
        FormData viewerFormData = new FormData();
        viewerFormData.left = new FormAttachment(OFFSET / 2, 0);
        viewerFormData.top = new FormAttachment(spinner, OFFSET);
        viewerFormData.height = 200;
        table.setLayoutData(viewerFormData);
        tViewer.setContentProvider(ArrayContentProvider.getInstance());
        table.setHeaderVisible(true);
        TableViewerColumn dateCol = new TableViewerColumn(tViewer, SWT.NONE);
        dateCol.getColumn().setText("Date");
        dateCol.getColumn().setWidth(100);
        dateCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return new SimpleDateFormat("dd.MM.yyyy").format(((PortfolioTransaction) element).getDate());
            }
        });
        TableViewerColumn sharesCol = new TableViewerColumn(tViewer, SWT.NONE);
        sharesCol.getColumn().setText("Shares");
        sharesCol.getColumn().setWidth(70);
        sharesCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return new Long(((PortfolioTransaction) element).getShares()).toString();
            }
        });
        TableViewerColumn feeCol = new TableViewerColumn(tViewer, SWT.NONE);
        feeCol.getColumn().setText("Fee");
        feeCol.getColumn().setWidth(70);
        feeCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getFees());
            }
        });
        TableViewerColumn priceCol = new TableViewerColumn(tViewer, SWT.NONE);
        priceCol.getColumn().setText("Price");
        priceCol.getColumn().setWidth(80);
        priceCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction transaction = (PortfolioTransaction) element;
                return Values.Amount.format(transaction.getActualPurchasePrice());
            }
        });
        TableViewerColumn amountCol = new TableViewerColumn(tViewer, SWT.NONE);
        amountCol.getColumn().setText("Amount");
        amountCol.getColumn().setWidth(80);
        amountCol.setLabelProvider(new ColumnLabelProvider() 
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getLumpSumPrice());
            }
            
        });
        createTransactionsButton = new Button(group, SWT.PUSH);
        createTransactionsButton.setText("Create Transactions");
        FormData cTBData = new FormData();
        cTBData.left = new FormAttachment(OFFSET / 2, 0);
        cTBData.top = new FormAttachment(table, OFFSET);
        createTransactionsButton.setLayoutData(cTBData);
        createTransactionsButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                controller.generateTransactions();
                tViewer.setInput(plan.getTransactions());
                tViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });
        return composite;
    }

    public InvestmentPlanController getInvestmentPlanController()
    {
        return controller;
    }

}
