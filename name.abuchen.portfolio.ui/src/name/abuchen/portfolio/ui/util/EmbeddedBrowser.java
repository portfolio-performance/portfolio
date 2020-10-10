package name.abuchen.portfolio.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.base.Throwables;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TokenReplacingReader;

public class EmbeddedBrowser
{
    private String htmlpage;
    private Browser browser;
    private IThemeEngine themeEngine;

    @Inject
    public EmbeddedBrowser(IThemeEngine engine)
    {
        this.themeEngine = engine;
    }

    public void setHtmlpage(String htmlpage)
    {
        this.htmlpage = htmlpage;
    }

    public Control createControl(Composite parent, Consumer<Browser> functions)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(container);

        try
        {
            browser = new Browser(container, SWT.NONE);
            browser.setJavascriptEnabled(true);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(browser);

            if (functions != null)
                functions.accept(browser);

            browser.setText(loadHTML(htmlpage));
            browser.addTraverseListener(event -> event.doit = true);

            Menu menu = new Menu(browser);
            browser.setMenu(menu);
        }
        catch (SWTError e)
        {
            // if creation of embedded browser fails, provide some hints
            PortfolioPlugin.log(e);

            String stacktrace = Throwables.getStackTraceAsString(e);

            Text text = new Text(container, SWT.WRAP);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(text);
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
        Scanner scanner = null;

        try (InputStream h = FileLocator.openStream(PortfolioPlugin.getDefault().getBundle(), new Path(htmlpage),
                        false))
        {
            try // NOSONAR
            {
                boolean isDark = themeEngine.getActiveTheme().getId().contains("dark"); //$NON-NLS-1$

                scanner = new Scanner(new TokenReplacingReader(new InputStreamReader(h, StandardCharsets.UTF_8),
                                new PathResolver(isDark ? "dark" : "light"))); //$NON-NLS-1$ //$NON-NLS-2$
                return scanner.useDelimiter("\\Z").next(); //$NON-NLS-1$
            }
            finally
            {
                if (scanner != null)
                    scanner.close();
            }
        }
        catch (NoSuchElementException | IOException e)
        {
            // problem: the Scanner only throws a NoSuchElementException but
            // hides the IOException that might hint to permission issues

            Exception error = e;

            if (scanner != null && scanner.ioException() != null)
                error = scanner.ioException();

            PortfolioPlugin.log(error);
            return "<html><body><h1>" + error.getMessage() + "</h1></body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static final class PathResolver implements TokenReplacingReader.ITokenResolver
    {
        private Bundle bundle = PortfolioPlugin.getDefault().getBundle();
        private String cssTheme;

        public PathResolver(String cssTheme)
        {
            this.cssTheme = cssTheme;
        }

        @Override
        public String resolveToken(String tokenName) throws IOException
        {
            try
            {
                tokenName = tokenName.replace("THEME", cssTheme); //$NON-NLS-1$
                URL fileURL = FileLocator.toFileURL(bundle.getEntry(tokenName));
                return Platform.OS_WIN32.equals(Platform.getOS()) ? fileURL.getPath().substring(1) : fileURL.getPath();
            }
            catch (NullPointerException e)
            {
                throw new IOException(e);
            }
        }
    }

}
