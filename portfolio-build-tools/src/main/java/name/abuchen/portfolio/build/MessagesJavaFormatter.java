package name.abuchen.portfolio.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MessagesJavaFormatter
{
    record MessagesJavaResult(List<Violation> violations, int rewrittenFiles)
    {}

    private static final Pattern FIELD_PATTERN = Pattern.compile("^(\\s*public static String )([A-Za-z0-9_]+)(;.*)$"); //$NON-NLS-1$

    MessagesJavaResult process(Path repositoryRoot, ManagedBundleSpec spec, List<String> expectedKeys, boolean write)
                    throws IOException
    {
        Path file = repositoryRoot.resolve(spec.messagesJavaPath()).normalize();

        if (!Files.exists(file))
            throw new IllegalStateException("Managed Messages.java file does not exist: " + file); //$NON-NLS-1$

        String original = Files.readString(file, StandardCharsets.UTF_8);
        String normalized = PropertiesFamilyFormatter.normalizeLineEndings(original);
        List<String> lines = new ArrayList<>(List.of(normalized.split("\n", -1))); //$NON-NLS-1$
        List<FieldLine> fieldLines = extractFieldLines(lines);
        List<Violation> violations = new ArrayList<>();

        if (fieldLines.isEmpty())
            throw new IllegalStateException("No message declarations found in " + file); //$NON-NLS-1$

        List<String> actualKeys = fieldLines.stream().map(FieldLine::key).toList();
        Set<String> actualSet = new HashSet<>(actualKeys);
        Set<String> expectedSet = new HashSet<>(expectedKeys);

        if (!actualSet.equals(expectedSet) || actualKeys.size() != expectedKeys.size())
        {
            violations.add(violation(repositoryRoot, file, "Messages.java keys do not match base properties keys")); //$NON-NLS-1$
            return new MessagesJavaResult(violations, 0);
        }

        return new MessagesJavaResult(violations, 0);
    }

    private List<FieldLine> extractFieldLines(List<String> lines)
    {
        List<FieldLine> fields = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++)
        {
            Matcher matcher = FIELD_PATTERN.matcher(lines.get(index));
            if (matcher.matches())
            {
                fields.add(new FieldLine(index, matcher.group(2), lines.get(index)));
            }
        }

        return fields;
    }

    private Violation violation(Path repositoryRoot, Path file, String message)
    {
        return new Violation(repositoryRoot.relativize(file).toString().replace('\\', '/'), message);
    }

    private record FieldLine(int lineIndex, String key, String line)
    {}
}
