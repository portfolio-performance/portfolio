package name.abuchen.portfolio.ui.views.panes;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

public class GroupedAccountBalancePane implements InformationPanePage
{
    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private PortfolioBalancePane portfolioBalancePane;
    private AccountBalancePane accountBalancePane;

    private Control portfolioBalanceControl;
    private Control accountBalanceControl;

    private Composite stack;
    private StackLayout layout;

    private ToolBarManager toolBarManager;

    @Override
    public String getLabel()
    {
        return Messages.ClientEditorLabelChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        stack = new Composite(parent, SWT.NONE);
        layout = new StackLayout();
        stack.setLayout(layout);

        portfolioBalancePane = view.make(PortfolioBalancePane.class);
        portfolioBalanceControl = portfolioBalancePane.createViewControl(stack);

        accountBalancePane = view.make(AccountBalancePane.class);
        accountBalanceControl = accountBalancePane.createViewControl(stack);

        layout.topControl = portfolioBalanceControl;
        stack.layout();

        return stack;
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        this.toolBarManager = toolBar;

        var pane = layout.topControl == portfolioBalanceControl ? portfolioBalancePane : accountBalancePane;
        pane.addButtons(toolBar);
    }

    @Override
    public void setInput(Object input)
    {
        var account = Adaptor.adapt(Account.class, input);

        if (account != null)
        {
            accountBalancePane.setInput(input);
            setTopControl(accountBalancePane, accountBalanceControl);
        }
        else
        {
            portfolioBalancePane.setInput(input);
            setTopControl(portfolioBalancePane, portfolioBalanceControl);
        }

        stack.layout();
    }

    private void setTopControl(InformationPanePage pane, Control control)
    {
        if (layout.topControl == control)
            return;

        layout.topControl = control;

        if (toolBarManager != null)
        {
            toolBarManager.removeAll();
            pane.addButtons(toolBarManager);
            toolBarManager.update(true);
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (layout.topControl == portfolioBalanceControl)
        {
            portfolioBalancePane.onRecalculationNeeded();
        }
        else
        {
            accountBalancePane.onRecalculationNeeded();
        }
    }
}
