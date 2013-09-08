package name.abuchen.portfolio.ui;

import java.util.LinkedList;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ClientEditor
{
    public static class PortfolioSite
    {
        public Shell getShell()
        {
            return Display.getCurrent().getActiveShell();
        }
    }

    private PortfolioPart portfolioPart;

    public ClientEditor(PortfolioPart portfolioPart)
    {
        this.portfolioPart = portfolioPart;
    }

    public Client getClient()
    {
        return portfolioPart.getClient();
    }

    public void markDirty()
    {
        portfolioPart.markDirty();
    }

    public void notifyModelUpdated()
    {
        portfolioPart.notifyModelUpdated();
    }

    public LinkedList<ReportingPeriod> loadReportingPeriods()
    {
        return portfolioPart.loadReportingPeriods();
    }

    public void storeReportingPeriods(LinkedList<ReportingPeriod> periods)
    {
        portfolioPart.storeReportingPeriods(periods);
    }

    public IPreferenceStore getPreferenceStore()
    {
        return portfolioPart.getPreferenceStore();
    }

    void activateView(String target, Object parameter)
    {
        portfolioPart.activateView(target, parameter);
    }

    public PortfolioSite getSite()
    {
        return new PortfolioSite();
    }

}
