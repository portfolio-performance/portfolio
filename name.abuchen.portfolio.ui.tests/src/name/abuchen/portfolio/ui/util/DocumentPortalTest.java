package name.abuchen.portfolio.ui.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

@SuppressWarnings("nls")
public class DocumentPortalTest
{
    @Test
    public void testPathsOfTheDocumentPortal()
    {
        assertThat(DocumentPortal.isPortalPath("/run/user/1000/doc/1a2b3c4d"), is(true));
        assertThat(DocumentPortal.isPortalPath("/run/user/1000/doc/1a2b3c4d/statement.pdf"), is(true));
        assertThat(DocumentPortal.isPortalPath("/run/user/1000/doc"), is(true));
        assertThat(DocumentPortal.isPortalPath("/run/flatpak/doc/deadbeef"), is(true));
    }

    @Test
    public void testOtherPaths()
    {
        assertThat(DocumentPortal.isPortalPath(null), is(false));
        assertThat(DocumentPortal.isPortalPath(""), is(false));
        assertThat(DocumentPortal.isPortalPath("/home/user/doc"), is(false));
        assertThat(DocumentPortal.isPortalPath("/home/user/run/user/1000/doc/1a2b3c4d"), is(false));
        assertThat(DocumentPortal.isPortalPath("/run/user/1000/documents"), is(false));
        assertThat(DocumentPortal.isPortalPath("/run/user/abc/doc/1a2b3c4d"), is(false));
        assertThat(DocumentPortal.isPortalPath("C:\\Users\\user\\doc"), is(false));
    }
}
