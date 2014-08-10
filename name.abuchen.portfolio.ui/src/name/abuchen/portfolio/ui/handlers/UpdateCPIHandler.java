package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.UpdateCPIJob;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;

public class UpdateCPIHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return null != part && part.getObject() instanceof PortfolioPart;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        final PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        Client client = portfolioPart.getClient();

        new UpdateCPIJob(client)
        {
            @Override
            protected void notifyFinished()
            {
                portfolioPart.notifyModelUpdated();
            }
        }.schedule();
    }
}
