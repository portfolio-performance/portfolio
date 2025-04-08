package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.window.Window;

import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.EditClientPropertiesDialog;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;

public class RiskFreeRateOfReturnConfig implements WidgetConfig
{
    private WidgetDelegate<?> delegate;
    private ClientProperties clientProperties;

    public RiskFreeRateOfReturnConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;
        this.clientProperties = new ClientProperties(delegate.getClient());
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        manager.add(new SimpleAction(Messages.LabelConfigureSharpeRatioRisklessIRR, a -> {

            var dialog = new EditClientPropertiesDialog(ActiveShell.get(), clientProperties);
            if (dialog.open() == Window.OK)
            {
                // force recalculation of all values (there can be more share
                // ratio widgets)
                delegate.getClient().markDirty();
            }
        }));

    }

    @Override
    public String getLabel()
    {
        return Messages.SharpeRatioRisklessIRR + ": " //$NON-NLS-1$
                        + Values.Percent2.format(clientProperties.getRiskFreeRateOfReturn());
    }

}
