package name.abuchen.portfolio.ui.update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Keeps the version number of the Windows list of installed programs in sync
 * with an in-app update.
 * <p/>
 * That list - and winget, which reads the same entries - does not ask the
 * application: it shows the number the Windows installer left in the uninstall
 * key of the registry (see installer.nsi). A self-update exchanges the bundles
 * of the installation but does not run the installer, so Windows keeps showing
 * the version that was installed once. See issue #3658.
 * <p/>
 * The entry is read with "reg export" and changed with "reg add", both of the
 * utility that comes with Windows. Java has no way to the registry other than
 * reflection into an internal class of the JDK, which is not an option (see the
 * discussion in the issue).
 */
@SuppressWarnings("nls")
final class WindowsUninstallRegistry
{
    /**
     * The keys an installation can be registered under. The installer writes to
     * the current user; until the end of 2025 it registered the installation for
     * the whole machine instead. A 32-bit installer reaches the machine-wide
     * entry of a 64-bit Windows only through its 32-bit view, therefore both are
     * looked at.
     */
    private static final String[] UNINSTALL_KEYS = { //
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Portfolio Performance",
                    "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Portfolio Performance",
                    "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Portfolio Performance" };

    private static final String DISPLAY_VERSION = "DisplayVersion";
    private static final String INSTALL_LOCATION = "InstallLocation";

    /**
     * The namespace of the units that belong to the application. An update
     * brings many units with it and not all of them follow the numbering of the
     * application: the bundled Java runtime carries a version of its own, such
     * as 21.0.5. Only a unit of the application has the number Windows has to
     * show for the installation.
     */
    private static final String APPLICATION_NAMESPACE = "name.abuchen.portfolio";

    /**
     * A string value of an exported key: the name and the data, both in quotes,
     * in which a backslash escapes the character that follows it. Values of
     * other types (dword:, hex:) are not needed and do not match.
     */
    private static final Pattern STRING_VALUE = Pattern
                    .compile("^\"(?<name>(?:[^\"\\\\]|\\\\.)*)\"=\"(?<data>(?:[^\"\\\\]|\\\\.)*)\"$");

    /** a version of the product, with or without the qualifier of the build */
    private static final Pattern RELEASE = Pattern.compile("(?<release>\\d+\\.\\d+\\.\\d+)([.\\-].*)?");

    /** a safety net: the utility answers immediately, also when a key is missing */
    private static final int TIMEOUT_SECONDS = 60;

    /** the reader of the output is through shortly after the utility is */
    private static final int READER_TIMEOUT_SECONDS = 5;

    private WindowsUninstallRegistry()
    {
    }

    /**
     * Sets the version Windows shows for this installation to the version of the
     * update that has just been installed.
     * <p/>
     * Runs on Windows only, and only for those uninstall keys that register
     * exactly this installation: the application can also be used from a plain
     * zip file, and more than one installation can be registered. The method
     * never throws - an update must not be reported as failed because a number
     * in a list of programs was not corrected.
     */
    static void updateInstalledVersion(String version, File installLocation)
    {
        if (!isWindows() || installLocation == null)
            return;

        String release = releaseOf(version);

        if (release == null)
        {
            PortfolioPlugin.log("Not updating the version of the Windows program list: no release in " + version);
            return;
        }

        List<String> notRead = new ArrayList<>();
        boolean foundAKey = false;

        for (String key : UNINSTALL_KEYS)
        {
            try
            {
                foundAKey |= updateKey(key, release, installLocation, notRead);
            }
            catch (IOException e)
            {
                // one key that cannot be read or written must not keep the
                // others from being corrected
                PortfolioPlugin.log("Could not run the Windows registry utility for " + key, e);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                PortfolioPlugin.log(e);
                return;
            }
        }

        if (!foundAKey)
        {
            // a plain zip file, or an installation of another user: there is no
            // entry that we could correct
            PortfolioPlugin.log("Not updating the version of the Windows program list: no entry for "
                            + installLocation.getAbsolutePath() + ". " + String.join(" ", notRead));
        }
    }

    /**
     * Corrects the version of one uninstall key if it registers this
     * installation.
     *
     * @return whether the key exists and could be read
     */
    private static boolean updateKey(String key, String release, File installLocation, List<String> notRead)
                    throws IOException, InterruptedException
    {
        Map<String, String> values = readKey(key, notRead);
        if (values == null)
            return false;

        String location = values.get(INSTALL_LOCATION);

        if (location == null)
        {
            PortfolioPlugin.log("Not updating the version of the Windows program list: " + key
                            + " does not say where it was installed");
            return true;
        }

        if (!samePath(location, installLocation.getAbsolutePath()))
        {
            PortfolioPlugin.log("Not updating the version of the Windows program list: " + key + " is for "
                            + location + ", not for " + installLocation.getAbsolutePath());
            return true;
        }

        // more than one key can name this very directory, that of the current
        // user and the machine-wide one, and winget reads them all: every one
        // of them gets the new number
        update(key, release);
        return true;
    }

    /**
     * Reads the string values of a key.
     * <p/>
     * "reg query" answers in the codepage of the Windows console, which Java
     * cannot know, so that a path with an umlaut would not arrive as it is.
     * "reg export" writes the key to a file in UTF-16 instead.
     *
     * @return the values, or null if the key does not exist or may not be read
     */
    private static Map<String, String> readKey(String key, List<String> notRead)
                    throws IOException, InterruptedException
    {
        Path file = Files.createTempFile("portfolio-uninstall-", ".reg");

        try
        {
            RegResult export = run("export", key, file.toString(), "/y");
            if (!export.succeeded())
            {
                // nothing registered here, or we may not read it
                notRead.add(key + ": " + export.output());
                return null;
            }

            return parseExport(decodeExport(Files.readAllBytes(file)));
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    /**
     * The release out of the version of an update, as the installer writes it:
     * it uses the version of the product. Returns null for anything that is not
     * a version, so that a number we cannot make sense of is not written to the
     * registry.
     */
    /* package */ static String releaseOf(String version)
    {
        if (version == null)
            return null;

        Matcher matcher = RELEASE.matcher(version.trim());
        return matcher.matches() ? matcher.group("release") : null;
    }

    /**
     * Whether a unit updated along with the application belongs to the
     * application itself, and therefore carries the version Windows has to show
     * for the installation.
     */
    /* package */ static boolean belongsToApplication(String unitId)
    {
        return unitId != null
                        && (unitId.equals(APPLICATION_NAMESPACE) || unitId.startsWith(APPLICATION_NAMESPACE + "."));
    }

    private static void update(String key, String release) throws IOException, InterruptedException
    {
        RegResult result = run("add", key, "/v", DISPLAY_VERSION, "/t", "REG_SZ", "/d", release, "/f");

        if (result.succeeded())
        {
            PortfolioPlugin.log("Version of the Windows program list set to " + release + " (" + key + ")");
            return;
        }

        // for example because the installation is registered machine-wide while
        // the application was not started with elevated permissions
        PortfolioPlugin.log("Could not set the version of the Windows program list (" + key + "): " + result.output());
    }

    /**
     * The text of a file written by "reg export": UTF-16 in the byte order of
     * Windows, with a byte order mark in front.
     */
    /* package */ static String decodeExport(byte[] bytes)
    {
        int offset = bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE ? 2 : 0;
        return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_16LE);
    }

    /**
     * Reads the string values of the exported key. The values of its subkeys,
     * which follow in sections of their own, are not part of it.
     */
    /* package */ static Map<String, String> parseExport(String content)
    {
        Map<String, String> values = new LinkedHashMap<>();
        boolean inKey = false;

        for (String line : content.split("\\r?\\n"))
        {
            if (line.startsWith("["))
            {
                // the first section is the key itself, every further one a
                // subkey
                if (inKey)
                    break;

                inKey = true;
                continue;
            }

            if (!inKey)
                continue;

            Matcher matcher = STRING_VALUE.matcher(line);
            if (matcher.matches())
                values.put(unescape(matcher.group("name")), unescape(matcher.group("data")));
        }

        return values;
    }

    private static String unescape(String text)
    {
        var result = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length())
                c = text.charAt(++i);
            result.append(c);
        }

        return result.toString();
    }

    /**
     * Whether a path of the registry and a path of the file system mean the same
     * directory. The installer wraps the value in quotes, Windows does not tell
     * upper from lower case, and a path can be given with either kind of
     * separator.
     */
    /* package */ static boolean samePath(String left, String right)
    {
        return normalizePath(left).equals(normalizePath(right));
    }

    /* package */ static String normalizePath(String path)
    {
        String p = path.trim();

        // the installer writes the path with quotes around it
        if (p.length() >= 2 && p.charAt(0) == '"' && p.charAt(p.length() - 1) == '"')
            p = p.substring(1, p.length() - 1).trim();

        p = p.replace('/', '\\');

        while (p.endsWith("\\"))
            p = p.substring(0, p.length() - 1);

        return p.toLowerCase(Locale.ROOT);
    }

    private static RegResult run(String... arguments) throws IOException, InterruptedException
    {
        var command = new ArrayList<String>();
        command.add(regUtility());
        command.addAll(Arrays.asList(arguments));

        var builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        var process = builder.start();

        // the output is read beside the wait and not before it: the utility
        // fills the buffer of the pipe and waits for room, so a single read of
        // the whole output would never let the timeout come to bear
        var output = new ByteArrayOutputStream();
        var reader = new Thread(() -> drain(process, output), "registry utility output");
        reader.setDaemon(true);
        reader.start();

        try
        {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            {
                process.destroyForcibly();
                throw new IOException("registry utility did not finish");
            }
        }
        catch (InterruptedException e)
        {
            // the update was cancelled: the utility must not outlive it
            process.destroyForcibly();
            throw e;
        }

        // the utility is through, so the reader reaches the end of the output
        // now - bounded, in case a process of its own keeps the pipe open
        reader.join(TimeUnit.SECONDS.toMillis(READER_TIMEOUT_SECONDS));

        if (reader.isAlive())
        {
            // what did not come by now will not come: end the stream so that the
            // reader stops, and work with the output that did come in
            process.getInputStream().close();
        }

        // only the messages of the utility, for the log: the values themselves
        // come from the exported file
        return new RegResult(process.exitValue(),
                        new String(output.toByteArray(), Charset.defaultCharset()).trim());
    }

    /**
     * Copies the output of the utility until it ends. A process that is killed
     * on the way leaves behind what was read until then.
     */
    private static void drain(Process process, ByteArrayOutputStream output)
    {
        try (var in = process.getInputStream())
        {
            in.transferTo(output);
        }
        catch (IOException e)
        {
            // nothing to do with it: the caller works with what came in
        }
    }

    /**
     * The utility lives next to the shell of Windows; do not depend on it being
     * found through the PATH of whoever started the application.
     */
    private static String regUtility()
    {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null)
        {
            File file = new File(systemRoot, "System32\\reg.exe");
            if (file.isFile())
                return file.getAbsolutePath();
        }

        return "reg.exe";
    }

    private static boolean isWindows()
    {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }

    private record RegResult(int exitCode, String output)
    {
        public boolean succeeded()
        {
            return exitCode == 0;
        }
    }
}
