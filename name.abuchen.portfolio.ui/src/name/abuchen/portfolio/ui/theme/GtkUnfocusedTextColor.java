package name.abuchen.portfolio.ui.theme;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Installs a GTK stylesheet that keeps a label's inherited text color while
 * its window is unfocused.
 * <p>
 * The rule applies to a label's backdrop state and leaves selected and
 * disabled text alone. It uses settings priority, so an explicit color
 * installed at application priority stays in place. The stylesheet is
 * installed only on GTK.
 */
@SuppressWarnings("nls")
final class GtkUnfocusedTextColor
{
    /**
     * GTK_STYLE_PROVIDER_PRIORITY_SETTINGS. The theme uses 200 and SWT's own
     * color rules use 600.
     */
    private static final int SETTINGS_PRIORITY = 400;

    private static final String STYLESHEET = "label:backdrop:not(:selected):not(:disabled) { color: inherit; }";

    private static boolean installed;

    private static long provider;

    /**
     * Prevents instantiation.
     */
    private GtkUnfocusedTextColor()
    {
    }

    /**
     * Installs the stylesheet once when the window system is GTK. Other
     * window systems return immediately. A failed attempt is logged and can
     * be retried.
     */
    static void install()
    {
        if (installed || !Platform.WS_GTK.equals(Platform.getWS()))
            return;

        try
        {
            installed = installProvider();
        }
        catch (ReflectiveOperationException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    /**
     * Creates a CSS provider and attaches the stylesheet to the default GTK 3
     * screen or GTK 4 display.
     *
     * @return {@code true} when the provider is attached
     * @throws ReflectiveOperationException
     *             if a GTK entry point cannot be called
     */
    private static boolean installProvider() throws ReflectiveOperationException
    {
        var gtk = Class.forName("org.eclipse.swt.internal.gtk.GTK");
        provider = (long) gtk.getMethod("gtk_css_provider_new").invoke(null);
        if (provider == 0)
            return false;

        var css = gtkCssBytes();
        if (gtk.getField("GTK4").getBoolean(null))
            return installForDisplay(provider, css);

        return installForScreen(provider, css);
    }

    /**
     * Adds the provider to the default GTK 3 screen and loads the stylesheet.
     *
     * @param cssProvider
     *            native CSS provider handle
     * @param css
     *            stylesheet encoded for GTK
     * @return {@code true} when a screen exists and the stylesheet is loaded
     * @throws ReflectiveOperationException
     *             if a GTK 3 entry point cannot be called
     */
    private static boolean installForScreen(long cssProvider, byte[] css) throws ReflectiveOperationException
    {
        var gdk = Class.forName("org.eclipse.swt.internal.gtk.GDK");
        long screen = (long) gdk.getMethod("gdk_screen_get_default").invoke(null);
        if (screen == 0)
            return false;

        var gtk3 = Class.forName("org.eclipse.swt.internal.gtk3.GTK3");
        invoke(gtk3, "gtk_style_context_add_provider_for_screen",
                        new Class<?>[] { long.class, long.class, int.class }, screen, cssProvider, SETTINGS_PRIORITY);
        invoke(gtk3, "gtk_css_provider_load_from_data",
                        new Class<?>[] { long.class, byte[].class, long.class, long[].class }, cssProvider, css, -1L,
                        null);
        return true;
    }

    /**
     * Adds the provider to the default GTK 4 display and loads the stylesheet.
     *
     * @param cssProvider
     *            native CSS provider handle
     * @param css
     *            stylesheet encoded for GTK
     * @return {@code true} when a display exists and the stylesheet is loaded
     * @throws ReflectiveOperationException
     *             if a GTK 4 entry point cannot be called
     */
    private static boolean installForDisplay(long cssProvider, byte[] css) throws ReflectiveOperationException
    {
        var gdk = Class.forName("org.eclipse.swt.internal.gtk.GDK");
        long display = (long) gdk.getMethod("gdk_display_get_default").invoke(null);
        if (display == 0)
            return false;

        var gtk4 = Class.forName("org.eclipse.swt.internal.gtk4.GTK4");
        invoke(gtk4, "gtk_style_context_add_provider_for_display",
                        new Class<?>[] { long.class, long.class, int.class }, display, cssProvider, SETTINGS_PRIORITY);
        invoke(gtk4, "gtk_css_provider_load_from_data", new Class<?>[] { long.class, byte[].class, long.class },
                        cssProvider, css, -1L);
        return true;
    }

    /**
     * Encodes {@link #STYLESHEET} as the null-terminated bytes GTK expects.
     *
     * @return stylesheet bytes
     * @throws ReflectiveOperationException
     *             if the SWT converter cannot be called
     */
    private static byte[] gtkCssBytes() throws ReflectiveOperationException
    {
        var converter = Class.forName("org.eclipse.swt.internal.Converter");
        return (byte[]) converter.getMethod("wcsToMbcs", String.class, boolean.class).invoke(null, STYLESHEET, true);
    }

    /**
     * Calls a static method.
     *
     * @param type
     *            class that declares the method
     * @param name
     *            method name
     * @param parameters
     *            parameter types
     * @param arguments
     *            arguments passed to the method
     * @throws ReflectiveOperationException
     *             if the method cannot be found or called
     */
    private static void invoke(Class<?> type, String name, Class<?>[] parameters, Object... arguments)
                    throws ReflectiveOperationException
    {
        Method method = type.getMethod(name, parameters);
        method.invoke(null, arguments);
    }
}
