package name.abuchen.portfolio.cli;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates files of the CLI bundle's source folder, wherever the tests run from. */
final class SourceTree
{
    private static final String BUNDLE = "name.abuchen.portfolio.cli";

    private SourceTree()
    {
    }

    /** Resolves a path relative to the <code>name.abuchen.portfolio.cli</code> folder. */
    static Path cliBundle(String relative)
    {
        for (var dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent())
        {
            var bundle = dir.resolve(BUNDLE);
            if (Files.isDirectory(bundle.resolve("src")))
                return bundle.resolve(relative);
        }
        throw new IllegalStateException("No " + BUNDLE + " folder above " + Path.of("").toAbsolutePath());
    }
}
