package name.abuchen.portfolio.ui.views.holdings;

import java.util.StringJoiner;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser.ItemSelectedFunction;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.util.ColorConversion;

public class HoldingsPieChartBrowser implements IPieChart
{
    private EmbeddedBrowser browser;
    private ClientSnapshot snapshot;
    private AbstractFinanceView view;


    public HoldingsPieChartBrowser(EmbeddedBrowser browser, ClientSnapshot snapshot, AbstractFinanceView view)
    {
        this.browser = browser;
        this.browser.setHtmlpage("/META-INF/html/pie.html"); //$NON-NLS-1$
        this.snapshot = snapshot;
        this.view = view;
    }

    @Override
    public Control createControl(Composite parent)
    {
        return browser.createControl(parent, LoadDataFunction::new,
            b -> new ItemSelectedFunction(b, uuid -> snapshot.getAssetPositions()
                .filter(p -> uuid.equals(p.getInvestmentVehicle().getUUID())).findAny()
                .ifPresent(p -> view.setInformationPaneInput(p.getInvestmentVehicle()))
            )
        );
    }

    @Override
    public void refresh(ClientSnapshot snapshot)
    {
        this.snapshot = snapshot;
        browser.refresh();
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private static final String ENTRY = "{\"uuid\":\"%s\"," //$NON-NLS-1$
                        + "\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"<b>%s</b> (%s)<br>%s x %s = %s\"," //$NON-NLS-1$
                        + "\"valueLabel\":\"%s\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        private LoadDataFunction(Browser browser) // NOSONAR
        {
            super(browser, "loadData"); //$NON-NLS-1$
        }

        @Override
        public Object function(Object[] arguments)
        {
            try
            {
                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                JSColors colors = new JSColors();

                snapshot.getAssetPositions() //
                                .filter(p -> p.getValuation().getAmount() > 0) //
                                .sorted((l, r) -> Long.compare(r.getValuation().getAmount(),
                                                l.getValuation().getAmount())) //
                                .forEach(p -> {
                                    String name = JSONObject.escape(p.getDescription());
                                    String percentage = Values.Percent2.format(p.getShare());
                                    joiner.add(String.format(ENTRY, p.getInvestmentVehicle().getUUID(), name, //
                                                    p.getValuation().getAmount(), //
                                                    colors.next(), //
                                                    name, percentage, Values.Share.format(p.getPosition().getShares()), //
                                                    Values.Money.format(p.getValuation()
                                                                    .multiply((long) Values.Share.divider())
                                                                    .divide(p.getPosition().getShares())), //
                                                    Values.Money.format(p.getValuation()), percentage));
                                });

                return joiner.toString();
            }
            catch (Throwable e) // NOSONAR
            {
                PortfolioPlugin.log(e);
                return "[]"; //$NON-NLS-1$
            }
        }
    }

    
    private static final class JSColors
    {
        private static final int SIZE = 11;
        private static final float STEP = 360.0f / SIZE;

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        private int nextSlice = 0;

        public String next()
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (nextSlice / (float) SIZE)));
            return ColorConversion.toHex((HUE + (STEP * nextSlice++)) % 360f, SATURATION, brightness);
        }
    }
}
