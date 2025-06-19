package name.abuchen.portfolio.ui.handlers.tools;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.handlers.MenuHelper;
import name.abuchen.portfolio.ui.wizards.security.FindQuoteProviderDialog;

public class MigratePortfolioReportHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, ClientInputFactory clientInputFactory)
    {
        var clientInput = MenuHelper.getActiveClientInput(part);
        if (clientInput.isEmpty())
            return;

        var client = clientInput.get().getClient();
        if (client == null)
            return;

        // collect securities that are configured to use Portfolio Report and
        // are (already) supported by the built-in provider (configured to use
        // EUR because they are traded on Xetra)

        var securities = client.getSecurities().stream() //
                        .filter(s -> PortfolioReportQuoteFeed.ID.equals(s.getFeed()))
                        .filter(s -> CurrencyUnit.EUR.equals(s.getCurrencyCode())) //
                        .toList();

        FindQuoteProviderDialog dialog = new FindQuoteProviderDialog(shell, client, securities);
        dialog.open();
    }
}
