package name.abuchen.portfolio.ui.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Scanner;
import java.util.StringJoiner;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.TokenReplacingReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

public class HoldingsPieChartView extends AbstractFinanceView
{
    private Browser browser;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(container);

        try
        {
            browser = new Browser(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(browser);

            new LoadDataFunction(browser, "loadData"); //$NON-NLS-1$
            browser.setText(loadHTML());
        }
        catch (SWTError e)
        {
            // if creation of embedded browser fails, provide some hints
            PortfolioPlugin.log(e);

            String stacktrace = ExceptionUtils.getStackTrace(e);

            Text text = new Text(container, SWT.WRAP);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(text);
            text.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            text.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
            text.setText(MessageFormat.format(Messages.MsgEmbeddedBrowserError, stacktrace));
        }

        return container;
    }

    private String loadHTML()
    {
        try (InputStream h = FileLocator.openStream(PortfolioPlugin.getDefault().getBundle(), //
                        new Path("/META-INF/html/holdings.html"), false)) //$NON-NLS-1$
        {
            return new Scanner(new TokenReplacingReader(new InputStreamReader(h, StandardCharsets.UTF_8),
                            new PathResolver())).useDelimiter("\\Z").next(); //$NON-NLS-1$
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            return "<html><body><h1>Error: " + e.getMessage() + "</h1></body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static final class PathResolver implements TokenReplacingReader.ITokenResolver
    {
        private Bundle bundle = PortfolioPlugin.getDefault().getBundle();

        @Override
        public String resolveToken(String tokenName)
        {
            try
            {
                return FileLocator.toFileURL(bundle.getEntry(tokenName)).getPath();
            }
            catch (NullPointerException | IOException e)
            {
                PortfolioPlugin.log(e);
                return tokenName;
            }
        }
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
