package name.abuchen.portfolio.ui.views.payments;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import name.abuchen.portfolio.ui.util.swt.ActiveShell;

public class PaymentsColors
{
    private static final int[][] COLORS = new int[][] { //
                    new int[] { 140, 86, 75 }, //
                    new int[] { 227, 119, 194 }, //
                    new int[] { 127, 127, 127 }, //
                    new int[] { 188, 189, 34 }, //
                    new int[] { 23, 190, 207 }, //
                    new int[] { 114, 124, 201 }, //
                    new int[] { 250, 115, 92 }, //
                    new int[] { 253, 182, 103 }, //
                    new int[] { 143, 207, 112 }, //
                    new int[] { 87, 207, 253 }, //
                    new int[] { 31, 119, 180 }, //
                    new int[] { 255, 127, 14 }, //
                    new int[] { 44, 160, 44 }, //
                    new int[] { 214, 39, 40 }, //
                    new int[] { 148, 103, 189 } }; //

    private PaymentsColors()
    {
    }

    public static Color getColor(int year)
    {
        RGB rgb = new RGB(COLORS[year % COLORS.length][0], //
                        COLORS[year % COLORS.length][1], //
                        COLORS[year % COLORS.length][2]);
        return ColorDescriptor.createFrom(rgb).createColor(ActiveShell.get().getDisplay());
    }
}
