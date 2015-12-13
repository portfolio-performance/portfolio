package name.abuchen.portfolio.ui.views;

import java.util.Comparator;
import java.util.StringJoiner;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.JavaFXBrowser;
import name.abuchen.portfolio.util.ColorConversion;
import name.abuchen.portfolio.util.Dates;
import netscape.javascript.JSObject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class HoldingsPieChartView extends AbstractFinanceView
{
    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        JavaFXBrowser browser = new JavaFXBrowser(parent);
        browser.registerBrowserFunction("loadData", new LoadDataFunction()); //$NON-NLS-1$
        browser.load("/META-INF/html/pie.html"); //$NON-NLS-1$
        return browser;
    }

    private static final class JSColors
    {
        private static final int SIZE = 11;
        private static final float STEP = (360.0f / (float) SIZE);

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        private int nextSlice = 0;

        public String next()
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (nextSlice / SIZE)));
            return ColorConversion.toHex((HUE + (STEP * nextSlice++)) % 360f, SATURATION, brightness);
        }
    }

    public final class LoadDataFunction implements JavaFXBrowser.BrowserFunction
    {
        private static final String ENTRY = "{\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"%s  %s  (%s)\"," //$NON-NLS-1$
                        + "\"valueLabel\":\"%s\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        @Override
        public Object function(JSObject arguments)
        {
            try
            {
                ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());

                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                JSColors colors = new JSColors();

                snapshot.getAssetPositions() //
                                .filter(p -> p.getValuation() > 0) //
                                .sorted(Comparator.comparing(AssetPosition::getValuation).reversed())
                                .forEach(p -> {
                                    String name = StringEscapeUtils.escapeJson(p.getDescription());
                                    String percentage = Values.Percent2.format(p.getShare());
                                    joiner.add(String.format(ENTRY, name, //
                                                    p.getValuation(), //
                                                    colors.next(), //
                                                    name, Values.Amount.format(p.getValuation()), percentage, //
                                                    percentage));
                                });

                return joiner.toString();
            }
            catch (Throwable e)
            {
                PortfolioPlugin.log(e);
                return "[]"; //$NON-NLS-1$
            }
        }
    }
}
