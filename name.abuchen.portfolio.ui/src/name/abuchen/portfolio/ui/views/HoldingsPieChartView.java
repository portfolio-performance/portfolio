package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.json.simple.JSONObject;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.ColorConversion;

public class HoldingsPieChartView extends AbstractFinanceView
{
    private static final String ID_WARNING_TOOL_ITEM = "warning"; //$NON-NLS-1$

    private CurrencyConverter converter;
    private EmbeddedBrowser browser;
    private ClientFilterDropDown clientFilter;
    private ClientSnapshot snapshot;

    @PostConstruct
    protected void contruct(ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

        clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        HoldingsPieChartView.class.getSimpleName(), filter -> notifyModelUpdated());

        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    public void notifyModelUpdated()
    {
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());

        browser.refresh();
        updateWarningInToolBar();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(clientFilter);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        browser = new EmbeddedBrowser("/META-INF/html/pie.html"); //$NON-NLS-1$
        updateWarningInToolBar();
        return browser.createControl(parent, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
    }

    private void updateWarningInToolBar()
    {
        boolean hasNegativePositions = snapshot.getAssetPositions().anyMatch(p -> p.getValuation().isNegative());

        ToolBarManager toolBar = getToolBarManager();

        if (hasNegativePositions)
        {
            if (toolBar.find(ID_WARNING_TOOL_ITEM) == null)
            {
                Action warning = new SimpleAction(Messages.HoldingsWarningAssetsWithNegativeValuation,
                                Images.ERROR_NOTICE.descriptor(), a -> {

                                    String details = String.join("\n", snapshot.getAssetPositions() //$NON-NLS-1$
                                                    .filter(p -> p.getValuation().isNegative())
                                                    .map(p -> p.getDescription() + " " //$NON-NLS-1$
                                                                    + Values.Money.format(p.getValuation()))
                                                    .collect(Collectors.toList()));

                                    MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                                    MessageFormat.format(
                                                                    Messages.HoldingsWarningAssetsWithNegativeValuationDetails,
                                                                    Values.Money.format(snapshot.getMonetaryAssets()),
                                                                    details));
                                });
                warning.setId(ID_WARNING_TOOL_ITEM);
                toolBar.insert(0, new ActionContributionItem(warning));
                toolBar.update(true);
            }
        }
        else
        {
            if (toolBar.remove(ID_WARNING_TOOL_ITEM) != null)
                toolBar.update(true);
        }
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
                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                JSColors colors = new JSColors();

                snapshot.getAssetPositions() //
                                .filter(p -> p.getValuation().getAmount() > 0) //
                                .sorted((l, r) -> Long.compare(r.getValuation().getAmount(),
                                                l.getValuation().getAmount())) //
                                .forEach(p -> {
                                    String name = JSONObject.escape(p.getDescription());
                                    String percentage = Values.Percent2.format(p.getShare());
                                    joiner.add(String.format(ENTRY, name, //
                                                    p.getValuation().getAmount(), //
                                                    colors.next(), //
                                                    name, percentage, Values.Share.format(p.getPosition().getShares()), //
                                                    Values.Money.format(p.getValuation()
                                                                    .multiply((long) Values.Share.divider())
                                                                    .divide((long) (p.getPosition().getShares()))), //
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
