package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.StringJoiner;

import javax.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.util.ColorConversion;

public class HoldingsPieChartView extends AbstractFinanceView
{
    @Inject
    private ExchangeRateProviderFactory factory;

    private EmbeddedBrowser browser;
    private ClientFilterDropDown clientFilter;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    public void notifyModelUpdated()
    {
        browser.refresh();
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        this.clientFilter = new ClientFilterDropDown(toolBar, getClient(), getPreferenceStore(),
                        HoldingsPieChartView.class.getSimpleName(), filter -> notifyModelUpdated());
    }

    @Override
    protected Control createBody(Composite parent)
    {
        browser = new EmbeddedBrowser("/META-INF/html/pie.html"); //$NON-NLS-1$
        return browser.createControl(parent, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
    }

    private static final class JSColors
    {
        private static final int SIZE = 11;
        private static final float STEP = 360.0f / (float) SIZE;

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

    private final class LoadDataFunction extends BrowserFunction
    {
        private static final String ENTRY = "{\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"<b>%s</b> (%s)<br>%s x %s = %s\"," //$NON-NLS-1$
                        + "\"valueLabel\":\"%s\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments)
        {
            try
            {
                CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
                Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
                ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());

                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                JSColors colors = new JSColors();

                snapshot.getAssetPositions() //
                                .filter(p -> p.getValuation().getAmount() > 0) //
                                .sorted((l, r) -> Long.compare(r.getValuation().getAmount(),
                                                l.getValuation().getAmount())) //
                                .forEach(p -> {
                                    String name = StringEscapeUtils.escapeJson(p.getDescription());
                                    String percentage = Values.Percent2.format(p.getShare());
                                    joiner.add(String.format(ENTRY, name, //
                                                    p.getValuation().getAmount(), //
                                                    colors.next(), //
                                                    name, percentage, Values.Share.format(p.getPosition().getShares()), //
                                                    Values.Money.format(p.getValuation().divide(
                                                                    (long) (p.getPosition().getShares() / Values.Share.divider()))), //
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
}
