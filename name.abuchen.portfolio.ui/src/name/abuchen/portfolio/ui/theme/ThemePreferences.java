package name.abuchen.portfolio.ui.theme;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngine;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Helper class to read the configured themeId. There is no public API to read
 * this. And, more importantly, there is no way to remove the stored themeId to
 * go back to the automatic behavior.
 */
@SuppressWarnings("restriction")
public class ThemePreferences
{
    private static final String THEMEID_KEY = "themeid"; //$NON-NLS-1$
    private static final String PATH_TO_CUSTOM_CSS = "platform:/meta/name.abuchen.portfolio.ui/custom.css"; //$NON-NLS-1$
    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("font-size:\\s*(\\d+)px"); //$NON-NLS-1$

    private static OptionalInt sessionFontSize;

    private ThemePreferences()
    {
    }

    public static Optional<String> getConfiguredThemeId()
    {
        var preferences = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(ThemeEngine.class).getSymbolicName());
        return Optional.ofNullable(preferences != null ? preferences.get(THEMEID_KEY, null) : null);
    }

    /**
     * Returns the font size configured on the theme preference page, or -1 if the
     * default font size is used. Returns an empty result if the style sheet cannot
     * be read or does not parse, which callers must distinguish from the default
     * whenever they rely on knowing the size. The size is stored in the custom
     * style sheet written by the preference page; there is no public API to read
     * it.
     */
    /**
     * Returns the path to the custom style sheet that holds the configured font
     * size. The file is created on start up because the theme style sheets require
     * it to exist.
     */
    public static Path getPathToCustomCSS() throws IOException, URISyntaxException
    {
        var url = FileLocator.resolve(new URI(PATH_TO_CUSTOM_CSS).toURL()); // NOSONAR
        return new File(url.getFile()).toPath();
    }

    public static OptionalInt readConfiguredFontSize()
    {
        try
        {
            var customCSS = Files.readString(getPathToCustomCSS());

            // the file is created empty on start up and the preference page
            // writes it empty again for the default font size
            if (customCSS.isBlank())
                return OptionalInt.of(-1);

            var matcher = FONT_SIZE_PATTERN.matcher(customCSS);
            if (matcher.find())
                return OptionalInt.of(Integer.parseInt(matcher.group(1)));

            // content that was not written by the preference page: the size in
            // effect is unknown rather than the default
            return OptionalInt.empty();
        }
        catch (IOException | URISyntaxException | NumberFormatException e)
        {
            PortfolioPlugin.log(e);
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the font size configured when this session started, or an empty
     * result if it is unknown - including after the size has been changed during
     * this session, because such a change only takes full effect after a restart.
     */
    public static synchronized OptionalInt getSessionFontSize()
    {
        if (sessionFontSize == null)
            sessionFontSize = readConfiguredFontSize();
        return sessionFontSize;
    }

    /**
     * Marks the font size in effect for this session as unknown. Called when the
     * configured size is changed, because the running application keeps rendering
     * with the previous size until it is restarted.
     */
    public static synchronized void invalidateSessionFontSize()
    {
        sessionFontSize = OptionalInt.empty();
    }

    /**
     * Returns the font size configured on the theme preference page, or -1 if the
     * default font size is used or the style sheet cannot be read.
     */
    public static int getConfiguredFontSize()
    {
        return readConfiguredFontSize().orElse(-1);
    }

    public static void clearConfiguredThemeId()
    {
        var preferences = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(ThemeEngine.class).getSymbolicName());
        if (preferences == null)
            return;

        preferences.remove(THEMEID_KEY);
        try
        {
            preferences.flush();
        }
        catch (BackingStoreException e)
        {
            PortfolioPlugin.log(e);
        }
    }

}
