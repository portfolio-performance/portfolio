package name.abuchen.portfolio.ui.editor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;

@SuppressWarnings("nls")
public class FilePathHelperTest
{
    private static final String KEY = "pdf.import.path";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private PortfolioPart part;
    private IPreferenceStore preferenceStore;
    private IEclipsePreferences eclipsePreferences;

    @Before
    public void setUp()
    {
        part = mock(PortfolioPart.class);
        preferenceStore = new PreferenceStore();
        eclipsePreferences = mock(IEclipsePreferences.class);

        when(part.getPreferenceStore()).thenReturn(preferenceStore);
        when(part.getEclipsePreferences()).thenReturn(eclipsePreferences);
    }

    @Test
    public void testGetPathReturnsOwnerSpecificPath() throws IOException
    {
        File ownerDir = folder.newFolder("ownerDir");
        File globalDir = folder.newFolder("globalDir");

        var account = new Account();
        preferenceStore.setValue(KEY + "." + account.getUUID(), ownerDir.getAbsolutePath());
        preferenceStore.setValue(KEY, globalDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY, account);

        assertThat(helper.getPath(), is(ownerDir.getAbsolutePath()));
    }

    @Test
    public void testGetPathFallsBackToGlobalPreferenceIfOwnerPathMissing() throws IOException
    {
        File globalDir = folder.newFolder("globalDir");

        var portfolio = new Portfolio();
        preferenceStore.setValue(KEY, globalDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY, portfolio);

        assertThat(helper.getPath(), is(globalDir.getAbsolutePath()));
    }

    @Test
    public void testGetPathFallsBackToEclipsePreferences() throws IOException
    {
        File eclipseDir = folder.newFolder("eclipseDir");
        when(eclipsePreferences.get(KEY, null)).thenReturn(eclipseDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY);

        assertThat(helper.getPath(), is(eclipseDir.getAbsolutePath()));
    }

    @Test
    public void testGetPathFallsBackToUserHomeIfNoPreferenceValid()
    {
        var helper = new FilePathHelper(part, KEY);

        assertThat(helper.getPath(), is(System.getProperty("user.home")));
    }

    @Test
    public void testSavePathWithTransactionOwner() throws IOException
    {
        File newDir = folder.newFolder("newDir");
        var account = new Account();

        var helper = new FilePathHelper(part, KEY, account);
        helper.savePath(newDir.getAbsolutePath());

        assertThat(preferenceStore.getString(KEY), is(newDir.getAbsolutePath()));
        assertThat(preferenceStore.getString(KEY + "." + account.getUUID()), is(newDir.getAbsolutePath()));
        verify(eclipsePreferences).put(KEY, newDir.getAbsolutePath());
    }

    @Test
    public void testSavePathWithoutTransactionOwner() throws IOException
    {
        File newDir = folder.newFolder("newDir");

        var helper = new FilePathHelper(part, KEY);
        helper.savePath(newDir.getAbsolutePath());

        assertThat(preferenceStore.getString(KEY), is(newDir.getAbsolutePath()));
        verify(eclipsePreferences).put(KEY, newDir.getAbsolutePath());
    }
}
