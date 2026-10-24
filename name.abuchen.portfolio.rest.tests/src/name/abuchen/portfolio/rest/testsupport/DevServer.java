package name.abuchen.portfolio.rest.testsupport;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.online.impl.EurostatHICPLabels;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.RestApiServer;
import name.abuchen.portfolio.rest.spi.ApiAccessRequest;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.util.TokenReplacingReader;
import name.abuchen.portfolio.util.TokenReplacingReader.ITokenResolver;

/**
 * Serves the REST API from a plain classpath, without OSGi or SWT.
 * <p>
 * Uses a stub host, so UI threading, user edits and save semantics stay outside
 * this dev server.
 * <p>
 * Writes are in-memory only.
 */
@SuppressWarnings("nls")
public class DevServer
{
    /** sample portfolio from the UI bundle */
    private static final String SAMPLE_RESOURCE = "/name/abuchen/portfolio/ui/parts/kommer.xml";

    private static final String DEFAULT_TOKEN = "devtoken";
    private static final String DEFAULT_ALIAS = "kommer";

    /**
     * Headless host for script-driven API testing.
     */
    private record DevHost(List<OpenFile> files) implements HostApplication
    {
        @Override
        public List<OpenFile> listOpenFiles()
        {
            return files;
        }

        @Override
        public <T> T syncExec(Callable<T> callable) throws Exception
        {
            return callable.call();
        }

        @Override
        public boolean isUserEditing()
        {
            return false;
        }

        @Override
        public void requestApiAccessApproval(ApiAccessRequest request)
        {
            // PairingService still holds its lock here.
            var approval = new Thread(request::allowForSession, "dev-server-pairing");
            approval.setDaemon(true);
            approval.start();
        }
    }

    private record Options(File file, int port, String token, String alias)
    {
    }

    public static void main(String[] args) throws Exception
    {
        var options = parse(args);

        var file = options.file() != null ? options.file() : materializeSample();
        var path = file.getAbsolutePath();
        var client = ClientFactory.load(file, null, new NullProgressMonitor());

        // Use a stable alias instead of the generated uuid.
        var registry = new FileAccessRegistry((IEclipsePreferences) new MemoryPreferences().node("files"));
        registry.setEnabled(path, true);
        registry.setAlias(path, options.alias());

        var host = new DevHost(List.of(new FakeHost.FakeOpenFile(path, file.getName(), client)));
        var store = new ClientStore(Files.createTempDirectory("pp-devserver-clients"));

        // Keep pairing testable, but give scripts a fixed token.
        var server = new RestApiServer(options.port(),
                        token -> options.token().equals(token) || store.authenticate(token).isPresent(),
                        ApiRoutes.create(registry, host, new PairingService(store, host)));
        try
        {
            server.start();
        }
        catch (BindException e)
        {
            // The running application likely owns the default port.
            System.err.println("port " + options.port() + " is already in use - the desktop application is " // NOSONAR
                            + "probably serving the API there. Pass --port to pick another one.");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        banner(options, server.getPort(), path, client);

        new CountDownLatch(1).await();
    }

    private static Options parse(String[] args)
    {
        File file = null;
        var port = RestApiConstants.DEFAULT_PORT;
        var token = DEFAULT_TOKEN;
        var alias = DEFAULT_ALIAS;

        for (var ii = 0; ii < args.length; ii++)
        {
            var value = ii + 1 < args.length ? args[ii + 1] : null;
            switch (args[ii])
            {
                case "--file" -> file = new File(require(value, "--file"));
                case "--port" -> port = Integer.parseInt(require(value, "--port"));
                case "--token" -> token = require(value, "--token");
                case "--alias" -> alias = require(value, "--alias");
                default -> throw new IllegalArgumentException("unknown argument " + args[ii]
                                + "; expected --file, --port, --token or --alias");
            }
            ii++;
        }

        return new Options(file, port, token, alias);
    }

    private static String require(String value, String option)
    {
        if (value == null)
            throw new IllegalArgumentException(option + " requires a value");
        return value;
    }

    /**
     * Writes the bundled sample to a real, token-resolved file.
     */
    private static File materializeSample() throws IOException
    {
        var target = Path.of(System.getProperty("java.io.tmpdir"), "pp-devserver", "kommer.portfolio");
        Files.createDirectories(target.getParent());

        try (var in = DevServer.class.getResourceAsStream(SAMPLE_RESOURCE))
        {
            if (in == null)
                throw new IOException("sample " + SAMPLE_RESOURCE + " not on the classpath - build "
                                + "name.abuchen.portfolio.ui or pass --file");

            try (Reader reader = new TokenReplacingReader(new InputStreamReader(in, StandardCharsets.UTF_8),
                            sampleTokenResolver()); var out = Files.newBufferedWriter(target, StandardCharsets.UTF_8))
            {
                reader.transferTo(out);
            }
        }

        return target.toFile();
    }

    /**
     * Resolves sample label tokens without depending on the UI bundle.
     */
    private static ITokenResolver sampleTokenResolver()
    {
        var samples = ResourceBundle.getBundle("name.abuchen.portfolio.ui.parts.samplemessages");

        return tokenName -> {
            try
            {
                if (tokenName.startsWith("Messages."))
                    return Class.forName("name.abuchen.portfolio.ui.Messages")
                                    .getField(tokenName.substring("Messages.".length())).get(null).toString();
                else if (tokenName.startsWith("EurostatHICPLabels."))
                    return EurostatHICPLabels.getString(tokenName.substring("EurostatHICPLabels.".length()));
                else
                    return samples.getString(tokenName);
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                return tokenName;
            }
        };
    }

    /**
     * Prints the pid so background servers can be stopped.
     */
    private static void banner(Options options, int port, String path, Client client)
    {
        var base = "http://127.0.0.1:" + port;
        var pid = ProcessHandle.current().pid();
        var lines = new ArrayList<String>();
        lines.add("");
        lines.add("Portfolio Performance REST API dev server");
        lines.add("  file   " + path);
        lines.add("  data   " + client.getSecurities().size() + " instruments, " + client.getAccounts().size()
                        + " accounts, " + client.getPortfolios().size() + " portfolios, base "
                        + client.getBaseCurrency());
        lines.add("  base   " + base + "/v1/files/" + options.alias());
        lines.add("  token  " + options.token());
        lines.add("  pid    " + pid);
        lines.add("");
        lines.add("  curl -s -H 'Authorization: Bearer " + options.token() + "' \\");
        lines.add("       " + base + "/v1/files/" + options.alias() + "/holdings");
        lines.add("");
        lines.add("Writes are not saved to disk. Press Ctrl-C to stop, or: kill " + pid);
        lines.forEach(System.out::println); // NOSONAR
    }
}
