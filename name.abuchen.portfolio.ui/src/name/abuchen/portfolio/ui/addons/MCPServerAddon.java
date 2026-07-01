package name.abuchen.portfolio.ui.addons;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.mcp.MCPServer;
import name.abuchen.portfolio.mcp.MCPTools;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.mcp.UIClientSource;
import name.abuchen.portfolio.ui.mcp.UIModelExecutor;
import name.abuchen.portfolio.ui.preferences.MCPPreferencePage;

public class MCPServerAddon
{
    private MCPServer server;

    @Inject
    public void startServer(@Preference(value = UIConstants.Preferences.MCP_ENABLED) boolean enabled,
                    @Preference(value = UIConstants.Preferences.MCP_PORT) int port,
                    @Preference(value = UIConstants.Preferences.MCP_AUTOSAVE) boolean autoSave,
                    ClientInputFactory factory)
    {
        stopServer();

        if (!enabled)
            return;

        int effectivePort = port > 0 ? port : MCPPreferencePage.DEFAULT_MCP_PORT;

        try
        {
            var clientSource = new UIClientSource(factory);
            var tools = new MCPTools(clientSource, new UIModelExecutor(), autoSave);
            server = new MCPServer(tools, effectivePort);
            server.start();
        }
        catch (Exception e)
        {
            PortfolioPlugin.log(e);
        }
    }

    @PreDestroy
    public void stopServer()
    {
        if (server != null)
        {
            server.stop();
            server = null;
        }
    }
}
