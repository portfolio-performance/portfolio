package name.abuchen.portfolio.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ManagedBundleSpecParser
{
    private static final String PREFIX = "--bundle="; //$NON-NLS-1$

    private ManagedBundleSpecParser()
    {}

    static List<ManagedBundleSpec> parse(String[] arguments, int startIndex)
    {
        List<ManagedBundleSpec> specs = new ArrayList<>();

        for (int index = startIndex; index < arguments.length; index++)
        {
            String argument = arguments[index];

            if (!argument.startsWith(PREFIX))
                throw new IllegalArgumentException("Unsupported argument: " + argument); //$NON-NLS-1$

            String[] parts = argument.substring(PREFIX.length()).split("\\|", 3); //$NON-NLS-1$

            if (parts.length != 3)
                throw new IllegalArgumentException("Invalid bundle specification: " + argument); //$NON-NLS-1$

            Path directory = Path.of(parts[0]).normalize();
            String baseName = parts[1];
            Path messagesJava = parts[2].isBlank() ? null : Path.of(parts[2]).normalize();

            specs.add(new ManagedBundleSpec(directory, baseName, messagesJava));
        }

        return specs;
    }
}
