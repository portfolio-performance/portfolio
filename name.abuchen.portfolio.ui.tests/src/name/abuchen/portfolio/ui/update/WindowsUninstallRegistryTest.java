package name.abuchen.portfolio.ui.update;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("nls")
public class WindowsUninstallRegistryTest
{
    private static final String KEY = "[HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Portfolio Performance]";

    private static String lines(String... lines)
    {
        return String.join("\r\n", lines);
    }

    @Test
    public void testTheReleaseOfAVersionBuiltByTheProduct()
    {
        assertThat(WindowsUninstallRegistry.releaseOf("0.90.1"), is("0.90.1"));
        assertThat(WindowsUninstallRegistry.releaseOf("0.90.1.v20261109-1745"), is("0.90.1"));

        // reads like a release, and is one - only not that of the application
        // (see testTheVersionOfTheApplicationAndNotThatOfOneOfItsUnits)
        assertThat(WindowsUninstallRegistry.releaseOf("21.0.5"), is("21.0.5"));

        // a version we cannot make sense of must not become the number of the
        // entry: Windows and winget would show it as it is
        assertThat(WindowsUninstallRegistry.releaseOf(null), is(nullValue()));
        assertThat(WindowsUninstallRegistry.releaseOf(""), is(nullValue()));
        assertThat(WindowsUninstallRegistry.releaseOf("unknown"), is(nullValue()));
    }

    @Test
    public void testTheVersionOfTheApplicationAndNotThatOfOneOfItsUnits()
    {
        // An update site carries the units of the application, and it also
        // carries what the application is bundled with. A version such as 21.0.5
        // reads like a release of the application, so only the unit tells them
        // apart. The names are the ones the update site of 0.90.0 lists.
        assertThat(WindowsUninstallRegistry.belongsToApplication("name.abuchen.portfolio"), is(true));
        assertThat(WindowsUninstallRegistry.belongsToApplication("name.abuchen.portfolio.product"), is(true));
        assertThat(WindowsUninstallRegistry.belongsToApplication("name.abuchen.portfolio.pdfbox1"), is(true));

        // the Windows installer is built from the product with the bundled Java
        // runtime, and that is the product an installation updates
        assertThat(WindowsUninstallRegistry.belongsToApplication("name.abuchen.portfolio.distro.product"), is(true));

        // the Java runtime that can be bundled, and the platform itself
        assertThat(WindowsUninstallRegistry.belongsToApplication("name.abuchen.zulu.jre.win32.win32.x86_64"),
                        is(false));
        assertThat(WindowsUninstallRegistry.belongsToApplication("org.eclipse.swt.win32.win32.x86_64"), is(false));

        // only the namespace itself, not a name that merely begins like it
        assertThat(WindowsUninstallRegistry.belongsToApplication("name.abuchen.portfolioextra"), is(false));

        assertThat(WindowsUninstallRegistry.belongsToApplication(null), is(false));
    }

    @Test
    public void testReadsTheValuesOfTheKey()
    {
        String content = lines( //
                        "Windows Registry Editor Version 5.00", //
                        "", //
                        KEY, //
                        "\"DisplayName\"=\"Portfolio Performance\"", //
                        "\"UninstallString\"=\"\\\"C:\\\\Users\\\\Anna\\\\AppData\\\\Local\\\\Programs\\\\PortfolioPerformance\\\\uninstall.exe\\\"\"", //
                        "\"DisplayVersion\"=\"0.72.0\"", //
                        "\"InstallLocation\"=\"\\\"C:\\\\Users\\\\Anna\\\\AppData\\\\Local\\\\Programs\\\\PortfolioPerformance\\\"\"", //
                        "\"Publisher\"=\"Andreas Buchen\"", //
                        "\"URLUpdateInfo\"=\"\"", //
                        "\"NoModify\"=dword:00000001", //
                        "\"EstimatedSize\"=dword:0002b63a", //
                        "", //
                        "");

        Map<String, String> values = WindowsUninstallRegistry.parseExport(content);

        // the name of the application contains spaces, and so does the path of
        // the uninstaller: both have to be read as one value, with the escapes
        // of the export removed
        assertThat(values.get("DisplayName"), is("Portfolio Performance"));
        assertThat(values.get("UninstallString"),
                        is("\"C:\\Users\\Anna\\AppData\\Local\\Programs\\PortfolioPerformance\\uninstall.exe\""));
        assertThat(values.get("InstallLocation"), is("\"C:\\Users\\Anna\\AppData\\Local\\Programs\\PortfolioPerformance\""));
        assertThat(values.get("DisplayVersion"), is("0.72.0"));
        assertThat(values.get("Publisher"), is("Andreas Buchen"));
        assertThat(values.get("URLUpdateInfo"), is(""));

        // values that are not strings are not needed
        assertThat(values.get("NoModify"), is(nullValue()));
        assertThat(values.get("EstimatedSize"), is(nullValue()));

        assertThat(values.get("Nope"), is(nullValue()));
    }

    @Test
    public void testTheValuesOfASubkeyAreNotThoseOfTheKey()
    {
        String content = lines( //
                        "Windows Registry Editor Version 5.00", //
                        "", //
                        KEY, //
                        "\"DisplayVersion\"=\"0.72.0\"", //
                        "", //
                        KEY.substring(0, KEY.length() - 1) + "\\Sub]", //
                        "\"DisplayVersion\"=\"1.0.0\"", //
                        "\"InstallLocation\"=\"C:\\\\Somewhere\"", //
                        "");

        Map<String, String> values = WindowsUninstallRegistry.parseExport(content);

        assertThat(values.get("DisplayVersion"), is("0.72.0"));
        assertThat(values.get("InstallLocation"), is(nullValue()));
    }

    @Test
    public void testNothingIsReadFromAnExportWithoutAKey()
    {
        assertThat(WindowsUninstallRegistry.parseExport(""), is(anEmptyMap()));
        assertThat(WindowsUninstallRegistry.parseExport(lines("Windows Registry Editor Version 5.00", "")),
                        is(anEmptyMap()));
    }

    @Test
    public void testAPathWithAnUmlautArrivesAsItIs()
    {
        // "reg export" writes UTF-16 with a byte order mark. The names are
        // written with escapes, so that the test does not depend on the
        // encoding of this file.
        String installDir = "C:\\Users\\J\u00f6rg\\AppData\\Local\\Programs\\PortfolioPerformance";

        String content = lines( //
                        "\uFEFFWindows Registry Editor Version 5.00", //
                        "", //
                        KEY, //
                        "\"InstallLocation\"=\"\\\"C:\\\\Users\\\\J\u00f6rg\\\\AppData\\\\Local\\\\Programs\\\\PortfolioPerformance\\\"\"", //
                        "");

        String decoded = WindowsUninstallRegistry.decodeExport(content.getBytes(StandardCharsets.UTF_16LE));

        assertThat(decoded.startsWith("Windows Registry Editor"), is(true));

        String location = WindowsUninstallRegistry.parseExport(decoded).get("InstallLocation");

        assertThat(location, is("\"" + installDir + "\""));
        assertThat(WindowsUninstallRegistry.samePath(location, installDir), is(true));

        // a different umlaut at the same place is a different directory
        assertThat(WindowsUninstallRegistry.samePath(location,
                        "C:\\Users\\J\u00fcrg\\AppData\\Local\\Programs\\PortfolioPerformance"), is(false));
    }

    @Test
    public void testPathIsComparedWithoutTheFormItIsGivenIn()
    {
        String installDir = "C:\\Users\\Anna\\AppData\\Local\\Programs\\PortfolioPerformance";

        // the installer writes the value of InstallLocation with quotes around it
        assertThat(WindowsUninstallRegistry.samePath("\"" + installDir + "\"", installDir), is(true));

        // Windows does not tell upper from lower case...
        assertThat(WindowsUninstallRegistry.samePath(installDir,
                        "c:\\users\\anna\\appdata\\local\\programs\\portfolioperformance"), is(true));

        // ... and a path can be given with either kind of separator, with or
        // without a closing one
        assertThat(WindowsUninstallRegistry.samePath(installDir + "\\", installDir.replace('\\', '/')), is(true));
    }

    @Test
    public void testADifferentDirectoryIsNotTheSameInstallation()
    {
        String installDir = "C:\\Users\\Anna\\AppData\\Local\\Programs\\PortfolioPerformance";

        // an older installation that has been registered, but not removed
        assertThat(WindowsUninstallRegistry.samePath(installDir, "C:\\Programs\\PortfolioPerformance"), is(false));
        assertThat(WindowsUninstallRegistry.samePath(installDir, installDir + "2"), is(false));
        assertThat(WindowsUninstallRegistry.samePath(installDir, installDir.substring(0, installDir.length() - 1)),
                        is(false));

        // the entry of another user
        assertThat(WindowsUninstallRegistry.samePath(installDir,
                        "C:\\Users\\Someone\\AppData\\Local\\Programs\\PortfolioPerformance"), is(false));

        assertThat(WindowsUninstallRegistry.samePath("\"\"", installDir), is(false));
    }

    @Test
    public void testQuotesAndSeparatorsOfAPathAreRemoved()
    {
        // the value of the installer, the same path with a closing separator,
        // and the same path with the other kind of separator
        assertThat(WindowsUninstallRegistry.normalizePath("\"C:\\Programs\\PP\""), is("c:\\programs\\pp"));
        assertThat(WindowsUninstallRegistry.normalizePath("C:\\Programs\\PP\\"), is("c:\\programs\\pp"));
        assertThat(WindowsUninstallRegistry.normalizePath(" C:/Programs/PP "), is("c:\\programs\\pp"));
    }
}
