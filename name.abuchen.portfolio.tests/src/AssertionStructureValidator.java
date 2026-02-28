package name.abuchen.portfolio.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AssertionStructureValidator
{
    // Require EXACTLY: assertThat(countSkippedItems(<resultsVar>), is(0L));
    // - allows whitespace/newlines
    // - <resultsVar> must look like an identifier (matches existing tests)
    private static final Pattern REQUIRED_ASSERTION = Pattern.compile(
                    "assertThat\\s*\\(\\s*"
                                    + "countSkippedItems\\s*\\(\\s*[A-Za-z_][A-Za-z0-9_]*\\s*\\)\\s*,\\s*"
                                    + "is\\s*\\(\\s*0\\s*L\\s*\\)\\s*\\)\\s*;",
                    Pattern.DOTALL);

    public static List<StructureError> validatePackage(Path packagePath) throws IOException
    {
        List<StructureError> errors = new ArrayList<>();

        if (!Files.exists(packagePath))
        {
            errors.add(new StructureError(packagePath.toString(), -1, "Path not found: " + packagePath));
            return errors;
        }

        try (Stream<Path> files = Files.walk(packagePath))
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

        // quick skip
        if (lines.stream().noneMatch(l -> l.contains("@Test")))
            return errors;

        int i = 0;
        while (i < lines.size())
        {
            if (!lines.get(i).contains("@Test"))
            {
                i++;
                continue;
            }

            boolean ignored = hasIgnoreInAnnotationBlock(lines, i);

            int methodStartLineIdx = findMethodBodyStart(lines, i + 1);
            if (methodStartLineIdx < 0)
            {
                errors.add(error(file, i + 1, "Found @Test but could not locate method body '{'."));
                i++;
                continue;
            }

            ExtractedBlock block = extractBlock(lines, methodStartLineIdx);
            if (block == null)
            {
                errors.add(error(file, i + 1, "Could not extract method body (brace mismatch)."));
                i++;
                continue;
            }

            if (!ignored)
            {
                String body = block.text;

                if (!REQUIRED_ASSERTION.matcher(body).find())
                {
                    errors.add(error(file, i + 1, "Missing assertion: assertThat(countSkippedItems(<resultsVar>), is(0L));"));
                }
            }

            i = Math.max(block.endLineIdx + 1, i + 1);
        }

        return errors;
    }

    private static boolean hasIgnoreInAnnotationBlock(List<String> lines, int testLineIdx)
    {
        for (int k = testLineIdx; k >= 0 && k >= testLineIdx - 8; k--)
        {
            String t = lines.get(k).trim();
            if (t.isEmpty())
                break;

            if (!t.startsWith("@") && (t.contains("public ") || t.contains("void ") || t.contains("(")))
                break;

            if (t.startsWith("@Ignore"))
                return true;
        }
        return false;
    }

    private static int findMethodBodyStart(List<String> lines, int fromIdx)
    {
        for (int idx = fromIdx; idx < lines.size(); idx++)
        {
            String t = lines.get(idx).trim();

            if (t.startsWith("@"))
                continue;

            if (lines.get(idx).indexOf('{') >= 0)
                return idx;
        }
        return -1;
    }

    private static ExtractedBlock extractBlock(List<String> lines, int startLineIdx)
    {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        boolean started = false;

        for (int idx = startLineIdx; idx < lines.size(); idx++)
        {
            String line = lines.get(idx);

            if (!started)
            {
                if (line.indexOf('{') < 0)
                    return null;
                started = true;
            }

            sb.append(line).append("\n");

            depth += countChar(line, '{');
            depth -= countChar(line, '}');

            if (started && depth == 0)
                return new ExtractedBlock(sb.toString(), idx);
        }
        return null;
    }

    private static int countChar(String s, char c)
    {
        int count = 0;
        for (int idx = 0; idx < s.length(); idx++)
            if (s.charAt(idx) == c)
                count++;
        return count;
    }

    private static StructureError error(Path file, int line, String msg)
    {
        return new StructureError(file.toString(), line, msg);
    }

    private static final class ExtractedBlock
    {
        final String text;
        final int endLineIdx;

        ExtractedBlock(String text, int endLineIdx)
        {
            this.text = text;
            this.endLineIdx = endLineIdx;
        }
    }
}
