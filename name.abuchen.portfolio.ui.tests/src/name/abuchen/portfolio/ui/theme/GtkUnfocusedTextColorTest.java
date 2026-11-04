package name.abuchen.portfolio.ui.theme;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.osgi.framework.Version;

@SuppressWarnings("nls")
public class GtkUnfocusedTextColorTest
{
    /**
     * The SWT version against which the workaround was last checked.
     */
    private static final Version CHECKED_SWT_VERSION = new Version(3, 134, 0);

    @Test
    public void testWorkaroundCheckedAgainstCurrentSwtVersion()
    {
        var version = Platform.getBundle("org.eclipse.swt").getVersion();
        var current = new Version(version.getMajor(), version.getMinor(), version.getMicro());

        assertThat("SWT was updated. Check whether GtkUnfocusedTextColor is still needed "
                        + "(see its Javadoc), then remove it or update CHECKED_SWT_VERSION.",
                        current, is(CHECKED_SWT_VERSION));
    }
}
