package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Keeps the <code>pp-cli</code> agent skill honest: every command, option,
 * operation, error or warning code and environment variable that the skill
 * names in code must exist in <code>describe</code>.
 */
public class SkillDriftTest
{
    private static final String SKILL = "skills/pp-cli";
    private static final List<String> FILES = List.of("SKILL.md", "write-protocol.md");

    private static final Pattern INLINE_CODE = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern FENCED_CODE = Pattern.compile("(?ms)^```[^\\n]*\\n(.*?)^```");
    private static final Pattern COMMAND = Pattern.compile("\\bpp-cli\\s+([a-z][a-z-]*)");
    private static final Pattern OPTION = Pattern.compile("(?<![\\w-])--([a-z][a-z-]*)");
    private static final Pattern OPERATION = Pattern.compile("[a-z][a-z-]*\\.[a-z][a-z-]*");
    private static final Pattern CODE = Pattern.compile("[a-z]+(?:-[a-z]+)+");
    private static final Pattern VARIABLE = Pattern.compile("\\bPP_[A-Z_]+\\b");
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)#:]+)\\)");

    private final JsonObject describe = describe();

    @Test
    public void frontmatterNamesTheSkillAfterItsFolderAndDescribesIt() throws IOException
    {
        var frontmatter = frontmatter(read("SKILL.md"));

        assertThat(frontmatter.get("name"), is(Path.of(SKILL).getFileName().toString()));
        assertThat(frontmatter.getOrDefault("description", "").isBlank(), is(false));
    }

    @Test
    public void everyCommandOptionOperationCodeAndVariableTheSkillNamesIsDescribed() throws IOException
    {
        var problems = new ArrayList<String>();
        for (var file : FILES)
            for (var span : codeSpans(read(file)))
                check(file, span, problems);

        assertThat(problems, is(empty()));
    }

    @Test
    public void everyLinkInTheSkillResolves() throws IOException
    {
        for (var file : FILES)
        {
            var matcher = LINK.matcher(read(file));
            while (matcher.find())
                assertThat(file + " links " + matcher.group(1),
                                Files.isRegularFile(SourceTree.cliBundle(SKILL).resolve(matcher.group(1))), is(true));
        }
    }

    @Test
    public void noCommandTakesAPasswordOption()
    {
        // the skill keeps passwords off the command line, which only holds while no option accepts one
        for (var command : describe.getAsJsonArray("commands"))
            for (var option : command.getAsJsonObject().getAsJsonArray("options"))
                assertThat(option.getAsJsonObject().get("name").getAsString().toLowerCase(),
                                not(containsString("password")));
    }

    private void check(String file, String span, List<String> problems)
    {
        var commands = names(describe.getAsJsonArray("commands"), "name");
        var where = file + " `" + span + "`: ";

        String command = null;
        var commandMatcher = COMMAND.matcher(span);
        while (commandMatcher.find())
        {
            if (!commands.contains(commandMatcher.group(1)))
                problems.add(where + "unknown command " + commandMatcher.group(1));
            else if (command == null)
                command = commandMatcher.group(1);
        }

        var options = command != null ? options(command) : allOptions();
        var optionMatcher = OPTION.matcher(span);
        while (optionMatcher.find())
        {
            if (!options.contains(optionMatcher.group(1)))
                problems.add(where + "unknown option --" + optionMatcher.group(1)
                                + (command != null ? " for " + command : ""));
        }

        var token = span.strip();
        if (OPERATION.matcher(token).matches()
                        && !names(describe.getAsJsonArray("operations"), "name").contains(token))
            problems.add(where + "unknown operation");

        if (CODE.matcher(token).matches() && !token.equals("pp-cli") && !codes(commands).contains(token))
            problems.add(where + "unknown error or warning code");

        var variables = names(describe.getAsJsonArray("environment"), "name");
        var variableMatcher = VARIABLE.matcher(span);
        while (variableMatcher.find())
        {
            if (!variables.contains(variableMatcher.group()))
                problems.add(where + "unknown environment variable " + variableMatcher.group());
        }
    }

    private Set<String> codes(Set<String> commands)
    {
        var codes = new HashSet<>(names(describe.getAsJsonArray("errors"), "code"));
        for (var command : describe.getAsJsonArray("commands"))
            codes.addAll(names(command.getAsJsonObject().getAsJsonArray("warnings"), "code"));
        codes.addAll(commands);
        return codes;
    }

    private Set<String> options(String command)
    {
        for (var element : describe.getAsJsonArray("commands"))
        {
            if (element.getAsJsonObject().get("name").getAsString().equals(command))
                return names(element.getAsJsonObject().getAsJsonArray("options"), "name");
        }
        throw new AssertionError(command);
    }

    private Set<String> allOptions()
    {
        var all = new HashSet<String>();
        for (var element : describe.getAsJsonArray("commands"))
            all.addAll(names(element.getAsJsonObject().getAsJsonArray("options"), "name"));
        return all;
    }

    private static Set<String> names(JsonArray array, String member)
    {
        var names = new HashSet<String>();
        array.forEach(e -> names.add(e.getAsJsonObject().get(member).getAsString()));
        return names;
    }

    private static List<String> codeSpans(String markdown)
    {
        var spans = new ArrayList<String>();

        var fenced = FENCED_CODE.matcher(markdown);
        while (fenced.find())
            spans.addAll(fenced.group(1).lines().toList());

        var inline = INLINE_CODE.matcher(FENCED_CODE.matcher(markdown).replaceAll(""));
        while (inline.find())
            spans.add(inline.group(1));

        return spans;
    }

    private static Map<String, String> frontmatter(String markdown)
    {
        var lines = markdown.lines().toList();
        assertThat("SKILL.md starts with frontmatter", lines.get(0), is("---"));

        var values = new HashMap<String, String>();
        for (var line : lines.subList(1, lines.size()))
        {
            if (line.equals("---"))
                return values;
            var colon = line.indexOf(':');
            if (colon > 0)
                values.put(line.substring(0, colon).strip(), line.substring(colon + 1).strip());
        }
        throw new AssertionError("SKILL.md frontmatter is not closed");
    }

    private static String read(String file) throws IOException
    {
        return Files.readString(SourceTree.cliBundle(SKILL).resolve(file));
    }

    private static JsonObject describe()
    {
        var run = CliRun.run("describe");
        assertThat(run.exitCode, is(0));
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }
}
