package name.abuchen.portfolio.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Consumer;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TokenReplacingReader;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

public class EmbeddedBrowser
{
    private final String htmlpage;
    private Browser browser;

    public EmbeddedBrowser(String htmlpage)
    {
        this.htmlpage = htmlpage;
    }

    public Control createControl(Composite parent, Consumer<Browser> functions)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        try
        {
            browser = new Browser(container, SWT.NONE);
            browser.setJavascriptEnabled(true);

            if (functions != null)
                functions.accept(browser);

            LogFunction log = new LogFunction(browser);
            browser.addDisposeListener(e -> log.dispose());

            browser.setText(loadHTML(htmlpage));

            Menu menu = new Menu(browser);
            browser.setMenu(menu);
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

    public void refresh()
    {
        browser.setText(loadHTML(htmlpage));
    }

    private String loadHTML(String htmlpage)
    {
        try (InputStream h = FileLocator.openStream(PortfolioPlugin.getDefault().getBundle(), //
                        new Path(htmlpage), false))
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
                URL fileURL = FileLocator.toFileURL(bundle.getEntry(tokenName));
                return Platform.OS_WIN32.equals(Platform.getOS()) ? fileURL.getPath().substring(1) : fileURL.getPath();
            }
            catch (NullPointerException | IOException e)
            {
                PortfolioPlugin.log(e);
                return tokenName;
            }
        }
    }

    private static class LogFunction extends BrowserFunction
    {
        public LogFunction(Browser browser)
        {
            super(browser, "$log"); //$NON-NLS-1$
        }

        @Override
        public Object function(Object[] arguments)
        {
            if (arguments != null && arguments.length == 1)
                PortfolioPlugin.log(String.valueOf(arguments[0]));
            else
                PortfolioPlugin.log(Arrays.toString(arguments));
            return null;
        }
    }
}
