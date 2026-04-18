package name.abuchen.portfolio.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import name.abuchen.portfolio.build.rbe.Bundle;
import name.abuchen.portfolio.build.rbe.PropertiesGenerator;
import name.abuchen.portfolio.build.rbe.PropertiesParser;

final class PropertiesFamilyFormatter
{
    record PropertiesFamilyResult(List<Violation> violations, List<String> baseKeys, int rewrittenFiles)
    {}

    PropertiesFamilyResult process(Path repositoryRoot, ManagedBundleSpec spec, boolean write) throws IOException
    {
        List<Violation> violations = new ArrayList<>();
        List<String> baseKeys = List.of();
        int rewrittenFiles = 0;

        for (Path file : listPropertiesFiles(repositoryRoot, spec))
        {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            String normalized = normalizeLineEndings(original);
            Bundle bundle = PropertiesParser.parse(original);
            String canonical = PropertiesGenerator.generate(bundle);

            if (!write && original.indexOf('\r') >= 0)
                violations.add(violation(repositoryRoot, file, "wrong line endings; expected LF")); //$NON-NLS-1$

            List<String> encounteredKeys = bundle.getInsertionOrderKeys();
            List<String> sortedKeys = new ArrayList<>(bundle.getKeys());

            if (!write && !encounteredKeys.equals(sortedKeys))
                violations.add(violation(repositoryRoot, file, "property keys are not sorted")); //$NON-NLS-1$

            if (!write && !normalized.equals(canonical) && encounteredKeys.equals(sortedKeys))
                violations.add(violation(repositoryRoot, file, "properties file is not in canonical format")); //$NON-NLS-1$

            if (file.equals(spec.basePropertiesPath(repositoryRoot)))
                baseKeys = sortedKeys;

            if (write && !original.equals(canonical))
            {
                Files.writeString(file, canonical, StandardCharsets.UTF_8);
                rewrittenFiles++;
            }
        }

        if (baseKeys.isEmpty())
            throw new IllegalStateException("Base properties file has no keys: " + spec.basePropertiesPath(repositoryRoot)); //$NON-NLS-1$

        return new PropertiesFamilyResult(violations, baseKeys, rewrittenFiles);
    }

    private List<Path> listPropertiesFiles(Path repositoryRoot, ManagedBundleSpec spec) throws IOException
    {
        Path directory = repositoryRoot.resolve(spec.directory()).normalize();
        Path baseFile = spec.basePropertiesPath(repositoryRoot);

        if (!Files.exists(baseFile))
            throw new IllegalStateException("Managed properties file does not exist: " + baseFile); //$NON-NLS-1$

        List<Path> files = new ArrayList<>();
        files.add(baseFile);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, spec.baseName() + "_*.properties")) //$NON-NLS-1$
        {
            for (Path file : stream)
            {
                files.add(file.normalize());
            }
        }

        files.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return files;
    }

    static String normalizeLineEndings(String content)
    {
        return content.replace("\r\n", "\n").replace('\r', '\n'); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private Violation violation(Path repositoryRoot, Path file, String message)
    {
        return new Violation(repositoryRoot.relativize(file).toString().replace('\\', '/'), message);
    }
}
