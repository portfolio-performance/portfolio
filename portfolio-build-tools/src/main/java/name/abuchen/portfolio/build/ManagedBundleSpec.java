package name.abuchen.portfolio.build;

import java.nio.file.Path;

record ManagedBundleSpec(Path directory, String baseName, Path messagesJavaPath)
{
    Path basePropertiesPath(Path repositoryRoot)
    {
        return repositoryRoot.resolve(directory).resolve(baseName + ".properties").normalize();
    }
}
