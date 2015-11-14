package name.abuchen.portfolio.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class IniFileManipulatorTest
{
    private static final List<String> LINES = Arrays.asList("-startup", // 0
                    "plugins/org.eclipse.equinox.launcher_1.3.0.v20140415-2008.jar", // 1
                    "--launcher.library", // 2
                    "plugins/org.eclipse.equinox.launcher.win32.win32.x86_1.1.200.v20140603-1326", // 3
                    "-nl", // 4
                    "de_DE", // 5
                    "-vmargs", // 6
                    "-Xms40m", // 7
                    "-Xmx512m"); // 8

    private IniFileManipulator manipulator = new IniFileManipulator();

    @Test
    public void givenThatLanguageFlagExists_thenUpdateLanguage()
    {
        List<String> input = new ArrayList<String>(LINES);

        manipulator.setLines(input);
        manipulator.setLanguage("us");

        List<String> result = manipulator.getLines();
        assertThat(result.size(), is(input.size()));
        assertThat(result.get(5), is("us"));
    }

    @Test
    public void givenThatLanguageFlagDoesNotExists_thenAddBeforeVmArgs()
    {
        List<String> input = new ArrayList<String>(LINES);
        input.remove(5); // de_DE
        input.remove(4); // -nl

        manipulator.setLines(input);
        manipulator.setLanguage("us");

        List<String> result = manipulator.getLines();
        assertThat(result.size(), is(input.size() + 2));
        assertThat(result.get(4), is("-nl"));
        assertThat(result.get(5), is("us"));
        assertThat(manipulator.isDirty(), is(true));
    }

    @Test
    public void givenThatVmArgsFlagDoesNotExists_thenAppendLanguageFlag()
    {
        List<String> input = new ArrayList<String>(LINES);
        input.remove(6); // -vmargs
        input.remove(5); // de_DE
        input.remove(4); // -nl

        manipulator.setLines(input);
        manipulator.setLanguage("us");

        List<String> result = manipulator.getLines();
        assertThat(result.size(), is(input.size() + 2));
        assertThat(result.get(result.size() - 2), is("-nl"));
        assertThat(result.get(result.size() - 1), is("us"));
        assertThat(manipulator.isDirty(), is(true));
    }
}
