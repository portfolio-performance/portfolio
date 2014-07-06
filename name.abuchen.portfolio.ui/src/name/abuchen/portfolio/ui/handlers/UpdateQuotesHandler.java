package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.UpdateQuotesJob;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;

public class UpdateQuotesHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return Platform.OS_LINUX.equals(Platform.getOS())
                        || (null != part && part.getObject() instanceof PortfolioPart);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named("name.abuchen.portfolio.ui.param.target") @Optional String target)
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart))
            return;

        final PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        Client client = portfolioPart.getClient();

        boolean isHistoric = "historic".equals(target); //$NON-NLS-1$

        new UpdateQuotesJob(client, isHistoric, 0)
        {
            @Override
            protected void notifyFinished()
            {
                portfolioPart.notifyModelUpdated();
            }
        }.schedule();
    }
}
