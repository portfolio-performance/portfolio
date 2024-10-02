package name.abuchen.portfolio.ui.views.panes;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

public class ChartPane implements InformationPanePage
{
    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private SecurityPriceChartPane securityChartPane;
    private AccountBalancePane accountBalancePane;

    private Control securityChartControl;
    private Control accountBalanceControl;

    private Composite stack;
    private StackLayout layout;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        stack = new Composite(parent, SWT.NONE);
        layout = new StackLayout();
        stack.setLayout(layout);

        securityChartPane = view.make(SecurityPriceChartPane.class);
        securityChartControl = securityChartPane.createViewControl(stack);

        accountBalancePane = view.make(AccountBalancePane.class);
        accountBalanceControl = accountBalancePane.createViewControl(stack);

        layout.topControl = securityChartControl;
        stack.layout();

        return stack;
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        securityChartPane.addButtons(toolBar);
    }

    @Override
    public void setInput(Object input)
    {
        var security = Adaptor.adapt(Security.class, input);

        if (security != null)
        {
            securityChartPane.setInput(input);
            layout.topControl = securityChartControl;
        }
        else
        {
            accountBalancePane.setInput(input);
            layout.topControl = accountBalanceControl;
        }

        stack.layout();
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (layout.topControl == securityChartControl)
        {
            securityChartPane.onRecalculationNeeded();
        }
        else
        {
            accountBalancePane.onRecalculationNeeded();
        }
    }
}
