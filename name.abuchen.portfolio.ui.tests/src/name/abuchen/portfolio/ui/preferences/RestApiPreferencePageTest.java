package name.abuchen.portfolio.ui.preferences;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RestApiPreferencePageTest
{
    @Test
    public void testPerformOkWithoutOpeningPage()
    {
        // The preference dialog saves every page, including pages whose
        // controls have never been created because the user did not visit them.
        var page = new RestApiPreferencePage(null);

        assertTrue(page.performOk());
    }
}
