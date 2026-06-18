package name.abuchen.portfolio.build;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class RepositoryFormatter
{
    private final Path repositoryRoot;
    private final List<ManagedBundleSpec> specs;
    private final PropertiesFamilyFormatter propertiesFormatter = new PropertiesFamilyFormatter();

    RepositoryFormatter(Path repositoryRoot, List<ManagedBundleSpec> specs)
    {
        this.repositoryRoot = repositoryRoot.normalize();
        this.specs = specs;
    }

    CheckResult check() throws IOException
    {
        return process(false);
    }

    CheckResult write() throws IOException
    {
        return process(true);
    }

    private CheckResult process(boolean write) throws IOException
    {
        List<Violation> violations = new ArrayList<>();
        int rewrittenFiles = 0;

        for (ManagedBundleSpec spec : specs)
        {
            PropertiesFamilyFormatter.PropertiesFamilyResult propertiesResult = propertiesFormatter
                            .process(repositoryRoot, spec, write);

            violations.addAll(propertiesResult.violations());
            rewrittenFiles += propertiesResult.rewrittenFiles();
        }

        return new CheckResult(violations, rewrittenFiles);
    }
}
