package name.abuchen.portfolio.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javafx.concurrent.Worker.State;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TokenReplacingReader;
import netscape.javascript.JSObject;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.osgi.framework.Bundle;

public class JavaFXBrowser extends FXCanvas
{
    public interface BrowserFunction
    {
        Object function(JSObject arguments);
    }

    public class JavaBridge
    {
        public void log(String text)
        {
            PortfolioPlugin.log(text);
        }
    }

    private WebView webView;

    private Map<String, BrowserFunction> functions = new HashMap<>();

    public JavaFXBrowser(Composite parent)
    {
        super(parent, SWT.NONE);
        webView = new WebView();

        this.setScene(new Scene(webView));

        getEngine().setOnAlert(new EventHandler<WebEvent<String>>()
        {
            @Override
            public void handle(WebEvent<String> event)
            {
                MessageBox messageBox = new MessageBox(getShell());
                messageBox.setMessage(event.getData());
                messageBox.open();
            }
        });

        getEngine().setConfirmHandler(new Callback<String, Boolean>()
        {
            @Override
            public Boolean call(String message)
            {
                MessageBox messageBox = new MessageBox(getShell());
                messageBox.setMessage(message);
                return messageBox.open() == SWT.OK;
            }
        });

        getEngine().getLoadWorker().stateProperty()
                        .addListener((val, oldState, newState) -> registerJavaFunctions(newState));
    }

    public void registerBrowserFunction(String name, BrowserFunction function)
    {
        functions.put(name, function);
    }

    public Object evaluate(String script)
    {
        return getEngine().executeScript("(function(){" + script + "}())"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void load(String resourcePath)
    {
        getEngine().loadContent(loadHTML(resourcePath));
    }

    private void registerJavaFunctions(State newState)
    {
        if (newState != State.SUCCEEDED)
            return;

        redirectConsoleLog();
        registerFunctions();

        // trigger script execution only after the java functions have
        // been installed otherwise they may not be available
        getEngine().executeScript("initPage()"); //$NON-NLS-1$
    }

    private void redirectConsoleLog()
    {
        JSObject window = (JSObject) getEngine().executeScript("window"); //$NON-NLS-1$
        JavaBridge bridge = new JavaBridge();
        window.setMember("java", bridge); //$NON-NLS-1$
        getEngine().executeScript("console.log = function(message) { java.log(message); };"); //$NON-NLS-1$
    }

    private void registerFunctions()
    {
        JSObject window = (JSObject) evaluate("return window"); //$NON-NLS-1$

        for (Map.Entry<String, BrowserFunction> entry : functions.entrySet())
        {
            String id = "__webViewProxy_" + entry.getKey(); //$NON-NLS-1$
            window.setMember(id, entry.getValue());
            evaluate("window['" + entry.getKey() + "'] = function(){return window['" + id + "'].function(arguments)}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    private WebEngine getEngine()
    {
        return webView.getEngine();
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
                return "file://" + FileLocator.toFileURL(bundle.getEntry(tokenName)).getPath(); //$NON-NLS-1$
            }
            catch (NullPointerException | IOException e)
            {
                PortfolioPlugin.log(e);
                return tokenName;
            }
        }
    }
}
