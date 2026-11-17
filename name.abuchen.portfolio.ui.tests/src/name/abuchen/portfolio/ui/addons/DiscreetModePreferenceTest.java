package name.abuchen.portfolio.ui.addons;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import name.abuchen.portfolio.money.DiscreetMode;

/**
 * The discreet mode is a preference of the installation: the addon applies it
 * to DiscreetMode even when no file is open.
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
