package name.abuchen.portfolio.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FailureApiUsageValidator
{
    // Forbid old API:
    // v.getTransactionContext().put(FAILURE, Messages.MsgError...);
    // Allow new API:
    // v.markAsFailure(Messages.MsgError...);
    //
    // We match broadly to also catch formatting and line breaks.
    private static final Pattern FORBIDDEN = Pattern.compile(
                    "getTransactionContext\\s*\\(\\s*\\)\\s*\\.\\s*put\\s*\\(\\s*FAILURE\\s*,",
                    Pattern.DOTALL);

    public static List<StructureError> validatePackage(Path sourcePath) throws IOException
    {
        List<StructureError> errors = new ArrayList<>();

        if (!Files.exists(sourcePath))
        {
            errors.add(new StructureError(sourcePath.toString(), -1, "Path not found: " + sourcePath));
            return errors;
        }

        try (Stream<Path> files = Files.walk(sourcePath))
        {
            files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                try
                {
                    errors.addAll(validateFile(file));
                }
                catch (Exception e)
                {
                    errors.add(new StructureError(file.toString(), -1, "File could not be validated: " + e.getMessage()));
                }
            });
        }

        return errors;
    }

    private static List<StructureError> validateFile(Path file) throws IOException
    {
        List<StructureError> errors = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        // Join for regex across line breaks
        String joined = String.join("\n", lines);

        Matcher m = FORBIDDEN.matcher(joined);
        while (m.find())
        {
            int line = approximateLine(joined, m.start());
            errors.add(new StructureError(
                            file.toString(),
                            line,
                            "Deprecated API usage: v.getTransactionContext().put(FAILURE, ...) -> migrate to v.markAsFailure(...)"));
        }

        return errors;
    }

    private static int approximateLine(String text, int charIndex)
    {
        // 1-based line number
        int line = 1;
        for (int i = 0; i < charIndex && i < text.length(); i++)
            if (text.charAt(i) == '\n')
                line++;
        return line;
    }
}
