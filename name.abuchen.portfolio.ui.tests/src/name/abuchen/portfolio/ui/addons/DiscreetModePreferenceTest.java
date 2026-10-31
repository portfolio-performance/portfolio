package name.abuchen.portfolio.ui.addons;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import name.abuchen.portfolio.money.DiscreetMode;

/**
 * Issue #5685: the discreet mode must survive closing the program. It is a
 * setting of the installation and not of a single file, hence the preference is
 * applied to the statically used DiscreetMode class without any file being
 * open: all open and all later opened files hide the amounts in the same way.
 */
@SuppressWarnings("nls")
public class DiscreetModePreferenceTest
{
    @After
    public void restoreDiscreetMode()
    {
        DiscreetMode.setActive(false);
    }

    @Test
    public void testActivatedModeIsUsedWithoutOpenFile()
    {
        Preference2EnvAddon addon = new Preference2EnvAddon();

        addon.setDiscreetMode(true);

        assertTrue(DiscreetMode.isActive());
    }

    @Test
    public void testDeactivatedModeIsUsedWithoutOpenFile()
    {
        Preference2EnvAddon addon = new Preference2EnvAddon();

        DiscreetMode.setActive(true);
        addon.setDiscreetMode(false);

        assertFalse(DiscreetMode.isActive());
    }

    @Test
    public void testUnchangedPreferenceKeepsTheMode()
    {
        Preference2EnvAddon addon = new Preference2EnvAddon();

        addon.setDiscreetMode(true);
        addon.setDiscreetMode(true);

        assertTrue(DiscreetMode.isActive());
    }
}
