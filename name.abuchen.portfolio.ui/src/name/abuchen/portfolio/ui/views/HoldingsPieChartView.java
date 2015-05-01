package name.abuchen.portfolio.ui.views;

import java.util.StringJoiner;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.util.Dates;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
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
        return new EmbeddedBrowser("/META-INF/html/holdings.html") ///$NON-NLS-1$
                        .createControl(parent, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private static final String ENTRY = "{\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"%s  %s  (%s)\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        public Object function(Object[] arguments)
        {
            try
            {
                ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());

                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                long totalAssets = snapshot.getAssets();

                for (AccountSnapshot a : snapshot.getAccounts())
                {
                    long value = a.getFunds();
                    if (value == 0)
                        continue;

                    String name = StringEscapeUtils.escapeJson(a.getAccount().getName());
                    joiner.add(String.format(ENTRY,
                                    name, //
                                    value, //
                                    Colors.CASH.asHex(), //
                                    name, Values.Amount.format(value),
                                    Values.Percent2.format(value / (double) totalAssets)));
                }

                for (SecurityPosition position : snapshot.getJointPortfolio().getPositions())
                {
                    long value = position.calculateValue();
                    if (value == 0)
                        continue;

                    String name = StringEscapeUtils.escapeJson(position.getSecurity().getName());
                    joiner.add(String.format(ENTRY,
                                    name, //
                                    value, //
                                    Colors.EQUITY.asHex(), //
                                    name, Values.Amount.format(value),
                                    Values.Percent2.format(value / (double) totalAssets)));
                }

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
