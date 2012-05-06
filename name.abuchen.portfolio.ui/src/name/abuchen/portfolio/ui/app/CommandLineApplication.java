package name.abuchen.portfolio.ui.app;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.util.CSVExporter;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

@SuppressWarnings("nls")
public class CommandLineApplication implements IApplication
{

    public Object start(IApplicationContext context) throws Exception
    {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if ("export".equals(args[0])) //$NON-NLS-1$
        {
            return doExport(args);
        }
        else
        {
            return error("Usage: -application name.abuchen.portfolio.ui.commandline export -file <xml> -output <directory>");
        }
    }

    public void stop()
    {}

    private Integer doExport(String[] args)
    {
        File portfolio = null;
        File outputDirectory = null;

        int ii = 0;
        while (ii < args.length)
        {
            if (args[ii].equals("-file")) //$NON-NLS-1$
                portfolio = new File(args[++ii]);

            if (args[ii].equals("-output")) //$NON-NLS-1$
                outputDirectory = new File(args[++ii]);

            ii++;
        }

        if (portfolio == null || outputDirectory == null)
            return error("Missing parameters: -file <xml> -output <directory>");

        if (!portfolio.exists())
            return error("File not found: {0}", portfolio.getAbsolutePath());

        if (!outputDirectory.exists())
            return error("Output directory {0} does not exist", outputDirectory.getAbsolutePath());

        Client client = null;
        try
        {
            client = ClientFactory.load(portfolio);
        }
        catch (IOException e)
        {
            return error("Failed to load file: {0}", e.getMessage());
        }

        try
        {
            new CSVExporter().exportAccountTransactions(outputDirectory, client.getAccounts());
            new CSVExporter().exportPortfolioTransactions(outputDirectory, client.getPortfolios());
            new CSVExporter().exportSecurityPrices(outputDirectory, client.getSecurities());
        }
        catch (IOException e)
        {
            return error("Failed to create CSV files: {0}", e.getMessage());
        }

        return IApplication.EXIT_OK;
    }

    private Integer error(String message, Object... parameter)
    {
        System.err.println(MessageFormat.format(message, parameter));
        return -1;
    }

}
