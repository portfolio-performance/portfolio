package name.abuchen.portfolio.ui.dialogs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;

import org.junit.Test;

import name.abuchen.portfolio.ui.PortfolioPlugin;

@SuppressWarnings("nls")
public class AboutDialogTest
{
    @Test
    public void testContributorsFileHasNoDuplicates() throws IOException
    {
        try (var inputStream = PortfolioPlugin.class.getResourceAsStream("dialogs/about.contributors.txt"))
        {
            if (inputStream == null)
                fail("about.contributors.txt not found");

            var contributors = new HashSet<String>();
            var lineNumber = 0;

            try (var scanner = new Scanner(inputStream, StandardCharsets.UTF_8))
            {
                while (scanner.hasNextLine())
                {
                    lineNumber++;
                    var line = scanner.nextLine().trim();

                    if (!line.isEmpty())
                    {
                        // Check that each line contains only one contributor
                        // (no commas, no spaces)
                        if (line.contains(","))
                            fail("Line " + lineNumber
                                            + " contains comma - each contributor must be on a separate line: " + line);

                        if (line.contains(" "))
                            fail("Line " + lineNumber
                                            + " contains spaces - each contributor must be a single username: " + line);

                        // Check for duplicates
                        if (!contributors.add(line))
                            fail("Found duplicate contributors: " + line);
                    }
                }
            }
        }
    }

    @Test
    public void testGenerateDeveloperListText()
    {
        var dialog = new AboutDialog(null);

        // Test with single contributor
        var input = "testuser";
        var expected = "[testuser](https://github.com/testuser)";
        assertThat(dialog.generateDeveloperListText(input), is(expected));

        // Test with multiple contributors
        input = "user1\nuser2\nuser3";
        expected = "[user1](https://github.com/user1), [user2](https://github.com/user2), [user3](https://github.com/user3)";
        assertThat(dialog.generateDeveloperListText(input), is(expected));

        // Test with empty string
        input = "";
        expected = "";
        assertThat(dialog.generateDeveloperListText(input), is(expected));
    }

    @Test
    public void testGenerateDeveloperListTextWithRealFile() throws IOException
    {
        try (var inputStream = PortfolioPlugin.class.getResourceAsStream("dialogs/about.contributors.txt"))
        {
            if (inputStream == null)
                fail("about.contributors.txt not found");

            var contributors = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            var dialog = new AboutDialog(null);

            var result = dialog.generateDeveloperListText(contributors);

            // Verify that the result contains markdown links
            assertThat(result.contains("["), is(true));
            assertThat(result.contains("](https://github.com/"), is(true));
            assertThat(result.contains(")"), is(true));

            // Verify that each contributor is included exactly once
            var lines = contributors.split("\n");
            for (String line : lines)
            {
                var contributor = line.trim();
                if (!contributor.isEmpty())
                {
                    var expected = "[" + contributor + "](https://github.com/" + contributor + ")";
                    assertThat("Missing contributor: " + contributor, result.contains(expected), is(true));
                }
            }
        }
    }
}
