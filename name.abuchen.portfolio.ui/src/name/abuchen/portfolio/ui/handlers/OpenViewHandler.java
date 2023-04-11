package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

import name.abuchen.portfolio.ui.editor.Navigation;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class OpenViewHandler
{
    @CanExecute
    boolean isVisible(MMenuItem menuItem)
    {
        Object data = menuItem.getTransientData().get(Navigation.Item.class.getName());
        return data instanceof Navigation.Item item && item.getViewClass() != null;
    }

    @Execute
    public void execute(MMenuItem menuItem)
    {
        PortfolioPart part = (PortfolioPart) menuItem.getTransientData().get(PortfolioPart.class.getName());
        Navigation.Item item = (Navigation.Item) menuItem.getTransientData().get(Navigation.Item.class.getName());

        part.activateView(item);
    }
}
