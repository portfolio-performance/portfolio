package name.abuchen.portfolio.ui.util.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.util.Pair;

/**
 * Assign colors along the color wheel and remember the color per assigned
 * instrument. The algorithm is picking the largest gap and then picking the
 * largest gap that has the biggest distance to the hue assigned last. The idea
 * is to create colors as distinct as possible while still being able to create
 * an infinite number of colors.
 */
public class ChartColorWheel
{
    private static final float HUE = 262.3f;
    private static final float SATURATION = 0.464f;
    private static final float BRIGHTNESS = 0.886f;

    private final Map<InvestmentVehicle, Pair<Float, RGB>> assignedColors = new HashMap<>();

    /**
     * The last hue assigned in order to pick the next color with a as big as
     * possible distance.
     */
    private float lastHue = HUE;

    public RGB getRGB(InvestmentVehicle vehicle)
    {
        var rgb = assignedColors.get(vehicle);
        if (rgb != null)
            return rgb.getValue();

        var size = assignedColors.size();

        if (size == 0)
        {
            rgb = new Pair<Float, RGB>(HUE, new RGB(HUE, SATURATION, BRIGHTNESS));
            assignedColors.put(vehicle, rgb);
            return rgb.getValue();
        }

        var hues = new ArrayList<>(assignedColors.values());
        Collections.sort(hues, (l, r) -> l.getKey().compareTo(r.getKey()));

        float bestGap = -1f;
        float bestDist = -1f;
        float bestMid = -1f;

        for (int ii = 0; ii < size; ii++)
        {
            float h1 = hues.get(ii).getKey();
            float h2 = (ii + 1 < size) ? hues.get(ii + 1).getKey() : hues.get(0).getKey() + 360;
            float gap = h2 - h1;
            float mid = (h1 + gap / 2) % 360;

            if (gap > bestGap)
            {
                bestGap = gap;
                bestMid = mid;
                bestDist = circularDistance(mid, lastHue);
            }
            else if (gap == bestGap)
            {
                float dist = circularDistance(mid, lastHue);
                if (dist > bestDist)
                {
                    bestMid = mid;
                    bestDist = dist;
                }
            }
        }

        rgb = new Pair<Float, RGB>(bestMid, new RGB(bestMid, SATURATION, BRIGHTNESS));
        lastHue = bestMid;
        assignedColors.put(vehicle, rgb);
        return rgb.getValue();
    }

    private static float circularDistance(float a, float b)
    {
        float diff = Math.abs(a - b) % 360;
        return diff > 180 ? 360 - diff : diff;
    }
}
