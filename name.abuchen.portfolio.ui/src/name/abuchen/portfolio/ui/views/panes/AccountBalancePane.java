package name.abuchen.portfolio.ui.views.panes;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.views.AccountBalanceChart;

public class AccountBalancePane implements InformationPanePage
{
    @Inject
    private IStylingEngine stylingEngine;

    @Inject
    private ExchangeRateProviderFactory factory;

    private Account account;
    private AccountBalanceChart chart;

    @Inject
    @Optional
    public void onDiscreedModeChanged(@UIEventTopic(UIConstants.Event.Global.DISCREET_MODE) Object obj)
    {
        if (chart != null)
            chart.redraw();
    }

    @Override
    public String getLabel()
    {
        return Messages.TabAccountBalanceChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        chart = new AccountBalanceChart(parent);
        stylingEngine.style(chart);

        chart.getAxisSet().getYAxis(0).getTick().setFormat(new ThousandsNumberFormat());
        chart.getToolTip().setDefaultValueFormat(new AmountNumberFormat());

        return chart;
    }

    @Override
    public void setInput(Object input)
    {
        account = Adaptor.adapt(Account.class, input);
        chart.updateChart(account, factory);
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (account != null)
            setInput(account);
    }

}
