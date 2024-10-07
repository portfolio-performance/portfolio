package name.abuchen.portfolio.ui.views.panes;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
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

public class SecurityPriceChartAndAccountBalancePane implements InformationPanePage
{
    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private SecurityPriceChartPane chartPane;
    private AccountBalancePane balancePane;

    private Control chartControl;
    private Control balanceControl;

    private Composite stack;
    private StackLayout layout;

    private boolean isSecurity = true;

    @Inject
    @Optional
    public void onDiscreedModeChanged(@UIEventTopic(UIConstants.Event.Global.DISCREET_MODE) Object obj)
    {
        if (balancePane != null)
            balancePane.onDiscreedModeChanged(obj);
    }

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

        chartPane = view.make(SecurityPriceChartPane.class);
        chartControl = chartPane.createViewControl(stack);

        balancePane = view.make(AccountBalancePane.class);
        balanceControl = balancePane.createViewControl(stack);

        layout.topControl = chartControl;
        stack.layout();

        return stack;
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        chartPane.addButtons(toolBar);
    }

    @Override
    public void setInput(Object input)
    {
        var security = Adaptor.adapt(Security.class, input);

        isSecurity = security != null;

        if (isSecurity)
        {
            chartPane.setInput(input);

            layout.topControl = chartControl;
        }
        else
        {
            balancePane.setInput(input);

            layout.topControl = balanceControl;
        }

        stack.layout();
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (isSecurity)
        {
            chartPane.onRecalculationNeeded();
        }
        else
        {
            balancePane.onRecalculationNeeded();
        }
    }
}
