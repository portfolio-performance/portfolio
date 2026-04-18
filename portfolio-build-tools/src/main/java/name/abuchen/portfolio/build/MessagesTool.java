package name.abuchen.portfolio.build;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MessagesTool
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 3)
        {
            System.err.println("Usage: <check|write> <repository-root> <bundle-spec>..."); //$NON-NLS-1$
            System.exit(2);
        }

        String command = args[0];
        Path repositoryRoot = Path.of(args[1]).normalize();
        List<ManagedBundleSpec> specs = ManagedBundleSpecParser.parse(args, 2);
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
            System.exit(1);

        if ("write".equals(command)) //$NON-NLS-1$
            System.out.println("Rewritten files: " + result.rewrittenFiles()); //$NON-NLS-1$
    }
}
