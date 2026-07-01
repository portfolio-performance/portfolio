package name.abuchen.portfolio.ui.mcp;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.mcp.ClientSource;
import name.abuchen.portfolio.mcp.OpenFile;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;

public class UIClientSource implements ClientSource
{
    private final ClientInputFactory factory;

    public UIClientSource(ClientInputFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public List<OpenFile> listOpenFiles()
    {
        return factory.listOpenClients().stream().map(this::toOpenFile).toList();
    }

    private OpenFile toOpenFile(ClientInput input)
    {
        var file = input.getFile();
        String id = file != null ? file.getAbsolutePath() : input.getLabel();

        Runnable saveAction = () -> {
            if (file == null)
                return;

            try
            {
                ClientFactory.save(input.getClient(), file);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }
        };

        return new OpenFile(id, input.getLabel(), file, input.getClient(), input.isDirty(), saveAction);
    }
}
