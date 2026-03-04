package name.abuchen.portfolio.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import name.abuchen.portfolio.datatransfer.csv.exporter.CSVExporter;
import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.json.JSecurity;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.TransactionPair;

public class CLIApplication implements IApplication
{
    @Override
    public Object start(IApplicationContext context) throws Exception
    {
        var rawArgs = context.getArguments().get("application.args");
        var args = rawArgs instanceof String[] a ? a : new String[0];

        String filePath = null;
        String password = null;
        String export = null;
        String format = null;
        String output = null;

        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--file" -> filePath = args[++i];
                case "--password" -> password = args[++i];
                case "--export" -> export = args[++i];
                case "--format" -> format = args[++i];
                case "--output" -> output = args[++i];
                default -> {
                    /* ignore unknown args */ }
            }
        }

        if (filePath == null || export == null || format == null)
        {
            System.err.println(
                            "Usage: --file <path> [--password <pass>] --export transactions|securities|accounts --format json|csv [--output <path>]");
            return IApplication.EXIT_OK;
        }

        var portfolioFile = new File(filePath);
        var client = ClientFactory.load(portfolioFile, password != null ? password.toCharArray() : null, noopMonitor());

        var outputFile = output != null ? new File(output) : null;

        switch (format)
        {
            case "json" -> writeJson(client, export, outputFile);
            case "csv" -> writeCsv(client, export, outputFile);
            default -> System.err.println("Unknown format: " + format);
        }

        return IApplication.EXIT_OK;
    }

    private void writeJson(Client client, String export, File outputFile) throws IOException
    {
        String json = switch (export)
        {
            case "transactions" -> JClient.from(client.getAllTransactions()).toJson();
            case "securities" -> JClient.GSON.toJson(client.getSecurities().stream().map(JSecurity::from).toList());
            case "accounts" -> {
                List<TransactionPair<?>> accountPairs = client.getAllTransactions().stream()
                                .filter(TransactionPair::isAccountTransaction).toList();
                yield JClient.from(accountPairs).toJson();
            }
            default -> throw new IOException("Unknown export type: " + export);
        };

        if (outputFile != null)
        {
            try (var writer = new FileWriter(outputFile, StandardCharsets.UTF_8))
            {
                writer.write(json);
            }
        }
        else
        {
            System.out.println(json);
        }
    }

    private void writeCsv(Client client, String export, File outputFile) throws IOException
    {
        boolean isTemp = outputFile == null;
        File target = isTemp ? File.createTempFile("pp-cli", ".csv") : outputFile;

        var exporter = new CSVExporter();
        var allPairs = client.getAllTransactions();

        switch (export)
        {
            case "transactions" -> {
                var txs = allPairs.stream().map(TransactionPair::getTransaction).toList();
                exporter.exportTransactions(target, txs);
            }
            case "securities" -> exporter.exportSecurityMasterData(target, client.getSecurities());
            case "accounts" -> {
                var accountTxs = allPairs.stream().filter(TransactionPair::isAccountTransaction)
                                .map(TransactionPair::getTransaction).toList();
                exporter.exportTransactions(target, accountTxs);
            }
            default -> throw new IOException("Unknown export type: " + export);
        }

        if (isTemp)
        {
            try (var in = new FileInputStream(target))
            {
                in.transferTo(System.out);
            }
            finally
            {
                target.delete();
            }
        }
    }

    private IProgressMonitor noopMonitor()
    {
        return new IProgressMonitor()
        {
            public void beginTask(String name, int totalWork)
            {
                // no op
            }

            public void done()
            {
                // no op
                
            }

            public void internalWorked(double work)
            {
                // no op

            }

            public boolean isCanceled()
            {
                return false;
            }

            public void setCanceled(boolean value)
            {
                // no op

            }

            public void setTaskName(String name)
            {
                // no op

            }

            public void subTask(String name)
            {
                // no op
            }

            public void worked(int work)
            {
                // no op
            }
        };
    }

    @Override
    public void stop()
    {
        // no op
    }
}
