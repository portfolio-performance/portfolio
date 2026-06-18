package name.abuchen.portfolio.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MessagesTool
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.err.println("Usage: <check|write> <repository-root> [<bundle-spec>...]"); //$NON-NLS-1$
            System.exit(2);
        }

        String command = args[0];
        Path repositoryRoot = Path.of(args[1]).normalize();
        List<ManagedBundleSpec> specs = args.length > 2 ? ManagedBundleSpecParser.parse(args, 2) : loadBundledSpecs();
        RepositoryFormatter formatter = new RepositoryFormatter(repositoryRoot, specs);

        CheckResult result;

        switch (command)
        {
            case "check": //$NON-NLS-1$
                result = formatter.check();
                break;
            case "write": //$NON-NLS-1$
                result = formatter.write();
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + command); //$NON-NLS-1$
        }

        for (Violation violation : result.violations())
        {
            System.err.println(violation);
        }

        if (result.hasViolations())
        {
            System.err.println(
                            "Run portfolio-build-tools/sort-messages.sh to fix the canonical sort order of managed properties files."); //$NON-NLS-1$
            System.exit(1);
        }

        if ("write".equals(command)) //$NON-NLS-1$
            System.out.println("Rewritten files: " + result.rewrittenFiles()); //$NON-NLS-1$
    }

    private static List<ManagedBundleSpec> loadBundledSpecs() throws IOException
    {
        try (InputStream stream = MessagesTool.class.getResourceAsStream("/managed-bundles.list")) //$NON-NLS-1$
        {
            if (stream == null)
                throw new IllegalStateException("Missing bundled managed-bundles.list resource"); //$NON-NLS-1$

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
            {
                List<String> lines = new ArrayList<>();
                String line;

                while ((line = reader.readLine()) != null)
                    lines.add(line);

                return ManagedBundleSpecParser.parse(lines);
            }
        }
    }
}
