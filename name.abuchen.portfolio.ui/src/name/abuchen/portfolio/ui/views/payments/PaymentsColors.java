package name.abuchen.portfolio.ui.views.payments;

import org.eclipse.swt.graphics.Color;

public class PaymentsColors
{
    private PaymentsColors()
    {
    }

    public static Color getColor(int year)
    {
        return PaymentsPalette.instance().getCyclic(year);
    }
}