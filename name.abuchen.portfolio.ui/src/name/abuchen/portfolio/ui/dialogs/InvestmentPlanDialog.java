package name.abuchen.portfolio.ui.dialogs;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class InvestmentPlanDialog extends Dialog
{

    private Client client;

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
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        editArea.setLayout(new FormLayout());
        FormData comboData = new FormData();
        comboData.left = new FormAttachment(0,0);
        comboData.top = new FormAttachment(0,0);
        final Combo comboDropDown = new Combo(editArea, SWT.DROP_DOWN );
        comboDropDown.setLayoutData(comboData);
        for (InvestmentPlan plan: client.getPlans()) {
            comboDropDown.add(plan.getSecurity().getName());
            comboDropDown.setData(plan.getSecurity().getName(), plan);
        }
        FormData buttonData = new FormData();
        buttonData.left = new FormAttachment(comboDropDown);
        buttonData.top = new FormAttachment(0,0);
        Button button = new Button(editArea, SWT.PUSH);
        button.setLayoutData(buttonData);
        button.setText("New Plan");
        button.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                NewPlanDialog newDialog = new NewPlanDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), client);
                if (newDialog.open() == Dialog.OK) {
                    System.out.println("weiter gehts");
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
        
        return composite;
    }

}
