package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

public final class RetiredObjectLabelStyle
{
    private RetiredObjectLabelStyle()
    {
    }

    public static Color foreground(Object object)
    {
        return isRetired(object) ? Colors.DARK_GRAY : null;
    }

    public static boolean isRetired(Object object)
    {
        if (object instanceof Account account)
            return account.isRetired();
        else if (object instanceof Portfolio portfolio)
            return portfolio.isRetired();
        else if (object instanceof Security security)
            return security.isRetired();
        else if (object instanceof InvestmentVehicle vehicle)
            return vehicle.isRetired();
        else
            return false;
    }
}
