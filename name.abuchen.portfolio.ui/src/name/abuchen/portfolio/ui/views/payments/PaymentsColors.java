package name.abuchen.portfolio.ui.views.payments;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.ui.util.Colors;

public class PaymentsColors
{
    private static final Color[] COLORS = new Color[] { //
                    Colors.getColor(140, 86, 75), //
                    Colors.getColor(227, 119, 194), //
                    Colors.getColor(127, 127, 127), //
                    Colors.getColor(188, 189, 34), //
                    Colors.getColor(23, 190, 207), //
                    Colors.getColor(114, 124, 201), //
                    Colors.getColor(250, 115, 92), //
                    Colors.getColor(253, 182, 103), //
                    Colors.getColor(143, 207, 112), //
                    Colors.getColor(87, 207, 253), //
                    Colors.getColor(31, 119, 180), //
                    Colors.getColor(255, 127, 14), //
                    Colors.getColor(44, 160, 44), //
                    Colors.getColor(214, 39, 40), //
                    Colors.getColor(148, 103, 189) }; //

    private PaymentsColors()
    {
    }

    public static Color getColor(int year)
    {
        return COLORS[year % COLORS.length];
    }
}
