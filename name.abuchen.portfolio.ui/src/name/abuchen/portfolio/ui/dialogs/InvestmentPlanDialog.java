package name.abuchen.portfolio.ui.dialogs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.InvestmentPlanController;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.CellEditorFactory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

public class InvestmentPlanDialog extends AbstractDialog implements PropertyChangeListener
{

    public static class Model extends BindingHelper.Model
    {

        InvestmentPlan plan;
        int period;
        Portfolio portfolio;
        Security security;
        Date start;
        String name;
        long amount;
        long transactionCost;
        boolean generateAccountTransactions = false;

        public Model(Client client, InvestmentPlan plan)
        {
            super(client);
            this.plan = plan;
        }

        @Override
        public void applyChanges()
        {
            if (plan != null) {
                plan.setAmount(amount);
                plan.setDayOfMonth(period);
                plan.setStart(start);
                plan.setName(name);
                plan.setTransactionCost(transactionCost);
                plan.setSecurity(security);
                plan.setGenerateAccountTransactions(generateAccountTransactions);
            }
        }

        public void updateFromPlan()
        {
            setPeriod(plan.getDayOfMonth());
            setPortfolio(plan.getPortfolio());
            setSecurity(plan.getSecurity());
            setStart(plan.getStart());
            setName(plan.getName());
            setAmount(plan.getAmount());
            setTransactionCost(plan.getTransactionCost());
            setGenerateAccountTransactions(plan.isGenerateAccountTransactions());
        }

        public void resetPlan()
        {
            setPeriod(1);
            setPortfolio(null);
            setSecurity(null);
            setStart(new Date());
            setName("");
            setAmount(0);
            setTransactionCost(0);
            setGenerateAccountTransactions(false);
        }

        public InvestmentPlan getPlan()
        {
            return plan;
        }

        public void setPlan(InvestmentPlan plan)
        {
            firePropertyChange("plan", this.plan, this.plan = plan);
        }

        public int getPeriod()
        {
            return period;
        }

        public void setPeriod(int period)
        {
            firePropertyChange("period", this.period, this.period = period);
        }

        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        public void setPortfolio(Portfolio portfolio)
        {
            firePropertyChange("portfolio", this.portfolio, this.portfolio = portfolio);
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security);
        }

        public Date getStart()
        {
            return start;
        }

        public void setStart(Date start)
        {
            firePropertyChange("start", this.start, this.start = start);
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            firePropertyChange("name", this.name, this.name = name);
        }

        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
        {
            firePropertyChange("amount", this.amount, this.amount = amount);
        }
        
        public long getTransactionCost()
        {
            return transactionCost;
        }

        public void setTransactionCost(long cost)
        {
            firePropertyChange("transactionCost", this.transactionCost, this.transactionCost = cost);
        }

        public boolean isGenerateAccountTransactions()
        {
            return generateAccountTransactions;
        }

        public void setGenerateAccountTransactions(boolean generateAccountTransactions)
        {
            firePropertyChange("generateAccountTransactions", this.generateAccountTransactions, 
                            this.generateAccountTransactions = generateAccountTransactions);
        }
        
    }

  
    private Client client;
    InvestmentPlanController controller;
    TableViewer tViewer;
    Button createTransactionsButton, delButton;
    ComboViewer combo;
    ClientEditor editor;

    public InvestmentPlanDialog(Shell owner, Client client)
    {
        super(owner, "Manage Investment Plans", new Model(client, null));
        this.client = client;
        getModel().addPropertyChangeListener("plan", this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent ev)
    {
        if (ev.getPropertyName().equals("plan"))
        {
            if (ev.getNewValue() != null)
            {
                InvestmentPlan plan = ((Model)getModel()).getPlan();
                delButton.setEnabled(true);
                createTransactionsButton.setEnabled(true);
                ((Model) getModel()).updateFromPlan();
                tViewer.setInput(plan.getTransactions());
                controller = new InvestmentPlanController(plan);
            }
            else
            {
                delButton.setEnabled(false);
                createTransactionsButton.setEnabled(false);
                ((Model) getModel()).resetPlan();
            }
        }
    }
    
    public void setEditor(ClientEditor editor) {
        this.editor = editor;
    }

    protected Button createButton(Composite parent, int id, String label, boolean defaultButton)
    {
        if (id == IDialogConstants.CANCEL_ID)
            return null;
        return super.createButton(parent, id, label, defaultButton);
    }

    public void updatePlans()
    {
        combo.setInput(client.getPlans().toArray());
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        combo = bindings().bindComboViewer(editArea, "Plan", "plan", new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((InvestmentPlan) element).getName();
            }
        }, client.getPlans().toArray());
        bindings().bindSpinner(editArea, "Period", "period", 1, 30, 17, 1);
        Label lbl = new Label(editArea, SWT.NULL);
        lbl.setText("Portfolio:");
        GridDataFactory.fillDefaults().span(1, 1).grab(true, false).applyTo(lbl);
        bindings().bindLabel(editArea, "portfolio");
        bindings().bindComboViewer(editArea, "Security", "security", new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Security) element).getName();
            }
        }, client.getSecurities().toArray());
        bindings().bindDatePicker(editArea, "Start", "start");
        bindings().bindStringInput(editArea, "Name", "name");
        bindings().bindAmountInput(editArea, "Amount", "amount");
        bindings().bindBooleanInput(editArea, "Account Transactions?", "generateAccountTransactions");
        bindings().bindAmountInput(editArea, "Transaction Cost", "transactionCost");
        delButton = new Button(editArea, SWT.PUSH);
        delButton.setText("Delete Plan");
        delButton.setEnabled(false);
        GridDataFactory.fillDefaults().span(1, 1).grab(true, false).applyTo(delButton);
        delButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                client.removePlan(((Model) getModel()).getPlan());
                ((Model) getModel()).setPlan(null);
                updatePlans();
//                editor.markDirty();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });
        Button newButton = new Button(editArea, SWT.PUSH);
        newButton.setText("New Plan");
        GridDataFactory.fillDefaults().span(1, 1).grab(true, false).applyTo(newButton);
        newButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                NewPlanDialog newDialog = new NewPlanDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell(), client);
                newDialog.open();
                updatePlans();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });
        tViewer = new TableViewer(editArea);
        Table table = tViewer.getTable();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 200).span(2, 4).grab(true, false).applyTo(table);
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
        TableViewerColumn securityCol = new TableViewerColumn(tViewer, SWT.NONE);
        securityCol.getColumn().setText("Security");
        securityCol.getColumn().setWidth(70);
        securityCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((PortfolioTransaction) element).getSecurity().getTickerSymbol();
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
                return Values.Share.format(((PortfolioTransaction) element).getShares());
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
        createTransactionsButton = new Button(editArea, SWT.PUSH);
        createTransactionsButton.setText("Create Transactions");
        createTransactionsButton.setEnabled(false);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(createTransactionsButton);
        createTransactionsButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                controller.generateTransactions();
                tViewer.setInput(((Model) getModel()).getPlan().getTransactions());
                tViewer.refresh();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });
        new CellEditorFactory(tViewer, PortfolioTransaction.class) //
        .notify(new CellEditorFactory.ModificationListener()
        {
            public void onModified(Object element, String property)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                if (t.getCrossEntry() != null)
                    t.getCrossEntry().updateFrom(t);
                tViewer.refresh(element);

            }
        }) 
        .editable("date")
        .readonly("security")// //$NON-NLS-1$
        .shares("shares") // //$NON-NLS-1$
        .amount("fees") // //$NON-NLS-1$
        .readonly("price")
        .amount("amount") // //$NON-NLS-1$
        .apply();
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                final Portfolio portfolio = ((Model) getModel()).portfolio;
                if (portfolio == null) {
                    System.out.println("should not happen: InvestmentPlanDialog");
                    return;
                }
                final PortfolioTransaction transaction = 
                                (PortfolioTransaction) 
                                ((IStructuredSelection) tViewer.getSelection()).getFirstElement();
                if (transaction != null)
                {
                    manager.add(new Separator());
                    manager.add(new Action("Delete")
                    {
                        @Override
                        public void run()
                        {
                            if (transaction.getCrossEntry() != null) {
                                transaction.getCrossEntry().delete();
                            } else {
                                portfolio.getTransactions().remove(transaction);
                            }
                            ((Model) getModel()).getPlan().getTransactions().remove(transaction);
                            tViewer.setInput(((Model) getModel()).getPlan().getTransactions());
                            tViewer.refresh();
                        }
                    });
                }
            }
        });

        Menu contextMenu = menuMgr.createContextMenu(table);
        table.setMenu(contextMenu);
    }

    public InvestmentPlanController getInvestmentPlanController()
    {
        return controller;
    }

}
