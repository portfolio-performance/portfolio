package name.abuchen.portfolio.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ManagedBundleSpecParser
{
    private static final String PREFIX = "--bundle="; //$NON-NLS-1$

    private ManagedBundleSpecParser()
    {}

    static List<ManagedBundleSpec> parse(String[] arguments, int startIndex)
    {
        List<String> lines = new ArrayList<>();

        for (int index = startIndex; index < arguments.length; index++)
            lines.add(arguments[index]);

        return parse(lines);
    }

    static List<ManagedBundleSpec> parse(Collection<String> specifications)
    {
        List<ManagedBundleSpec> specs = new ArrayList<>();

        for (String specification : specifications)
        {
            if (specification == null)
                continue;

            String trimmed = specification.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) //$NON-NLS-1$
                continue;

            String normalized = trimmed.startsWith(PREFIX) ? trimmed.substring(PREFIX.length()) : trimmed;
            String[] parts = normalized.split("\\|", -1); //$NON-NLS-1$

            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid bundle specification: " + specification); //$NON-NLS-1$

            Path directory = Path.of(parts[0]).normalize();
            String baseName = parts[1];
            specs.add(new ManagedBundleSpec(directory, baseName));
        }

        return specs;
    }
}
