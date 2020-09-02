package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class SelectReportingCurrencyHandler
{
    @CanExecute
    boolean isVisible(MMenuItem menuItem)
    {
        CurrencyUnit currency = (CurrencyUnit) menuItem.getTransientData().get(CurrencyUnit.class.getName());
        PortfolioPart part = (PortfolioPart) menuItem.getTransientData().get(PortfolioPart.class.getName());

        if (currency == null || part == null)
            return false;

        menuItem.setSelected(currency.getCurrencyCode().equals(part.getClient().getBaseCurrency()));

        return true;
    }

    @Execute
    public void execute(MMenuItem menuItem)
    {
        PortfolioPart part = (PortfolioPart) menuItem.getTransientData().get(PortfolioPart.class.getName());
        CurrencyUnit currency = (CurrencyUnit) menuItem.getTransientData().get(CurrencyUnit.class.getName());
        part.getClient().setBaseCurrency(currency.getCurrencyCode());
    }
}
