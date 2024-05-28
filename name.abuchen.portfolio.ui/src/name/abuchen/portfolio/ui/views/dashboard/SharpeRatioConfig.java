package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleAction;

/**
 * Configuration of the Sharpe Ratio widget
 */
public class SharpeRatioConfig implements WidgetConfig
{
    private WidgetDelegate<?> delegate;
    private int riskFreeIRR;

    public SharpeRatioConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;
        String riskFreeIRRStr = this.delegate.getWidget().getConfiguration().get(Dashboard.Config.RISKFREE_IRR.name());
        try
        {
            this.riskFreeIRR = Integer.parseInt(riskFreeIRRStr);
        }
        catch (NumberFormatException ex)
        {
            this.riskFreeIRR = 0;
            PortfolioPlugin.info(ex.getLocalizedMessage());
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        SimpleAction sa = new SimpleAction(this.getLabel(), a -> {
            InputDialog in = new InputDialog(Display.getCurrent().getActiveShell(), this.getLabel(),
                            Messages.SharpeRatioRiskfreeIRR, String.valueOf(this.riskFreeIRR), r -> {
                                try
                                {
                                    Integer.parseInt(r);
                                    return null;
                                }
                                catch (NumberFormatException ex)
                                {
                                    return ex.getLocalizedMessage();
                                }
                            });
            if (in.open() == Window.OK)
            {
                String riskFreeIRRStr = in.getValue();
                try
                {
                    this.riskFreeIRR = Integer.parseInt(riskFreeIRRStr);
                }
                catch (NumberFormatException ex)
                {
                    this.riskFreeIRR = 0;
                    PortfolioPlugin.info(ex.getLocalizedMessage());
                }
                this.delegate.getWidget().getConfiguration().put(Dashboard.Config.RISKFREE_IRR.name(), String.valueOf(this.riskFreeIRR));
                this.delegate.update();
                this.delegate.getClient().touch();
            }
        });
        manager.add(sa);
    }

    @Override
    public String getLabel()
    {
        return Messages.SharpeRatioRiskfreeIRRLabel;
    }

    public int getRiskFreeIRR()
    {
        return riskFreeIRR;
    }
}
