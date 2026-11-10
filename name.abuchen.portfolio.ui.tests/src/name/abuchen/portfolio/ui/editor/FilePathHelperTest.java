package name.abuchen.portfolio.ui.editor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    public void testGetPathReturnsQualifierSpecificPath() throws IOException
    {
        File ownerDir = folder.newFolder("ownerDir");
        File globalDir = folder.newFolder("globalDir");

        var account = new Account();
        preferenceStore.setValue(KEY + "." + account.getUUID(), ownerDir.getAbsolutePath());
        preferenceStore.setValue(KEY, globalDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY, account.getUUID());

        assertThat(helper.getPath(), is(ownerDir.getAbsolutePath()));
    }

    @Test
    public void testGetPathFallsBackToGlobalPreferenceIfQualifierPathMissing() throws IOException
    {
        File globalDir = folder.newFolder("globalDir");

        var portfolio = new Portfolio();
        preferenceStore.setValue(KEY, globalDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY, portfolio.getUUID());

        assertThat(helper.getPath(), is(globalDir.getAbsolutePath()));
    }

    @Test
    public void testGetPathFallsBackToGlobalPreferenceIfQualifierDirectoryWasRemoved() throws IOException
    {
        File removedDir = folder.newFolder("removedDir");
        File globalDir = folder.newFolder("globalDir");

        var portfolio = new Portfolio();
        preferenceStore.setValue(KEY + "." + portfolio.getUUID(), removedDir.getAbsolutePath());
        preferenceStore.setValue(KEY, globalDir.getAbsolutePath());

        assertThat(removedDir.delete(), is(true));

        var helper = new FilePathHelper(part, KEY, portfolio.getUUID());

        assertThat(helper.getPath(), is(globalDir.getAbsolutePath()));
    }

    @Test
    public void testGetPathFallsBackToUserHomeIfNoDirectoryExistsAnymore() throws IOException
    {
        File removedDir = folder.newFolder("removedDir");

        var portfolio = new Portfolio();
        preferenceStore.setValue(KEY + "." + portfolio.getUUID(), removedDir.getAbsolutePath());
        preferenceStore.setValue(KEY, removedDir.getAbsolutePath());
        when(eclipsePreferences.get(KEY, null)).thenReturn(removedDir.getAbsolutePath());

        assertThat(removedDir.delete(), is(true));

        var helper = new FilePathHelper(part, KEY, portfolio.getUUID());

        assertThat(helper.getPath(), is(System.getProperty("user.home")));
    }

    /**
     * A client file can be moved to another operating system, where a stored
     * path is not merely missing but not a valid path at all.
     */
    @Test
    public void testGetPathFallsBackIfStoredPathIsMalformed() throws IOException
    {
        File globalDir = folder.newFolder("globalDir");

        var portfolio = new Portfolio();
        // a NUL character is not a permitted path character on any platform
        preferenceStore.setValue(KEY + "." + portfolio.getUUID(), "invalid\0path");
        preferenceStore.setValue(KEY, globalDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY, portfolio.getUUID());

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
    public void testSavePathWithQualifierDoesNotWritePreferencesWithoutQualifier() throws IOException
    {
        File newDir = folder.newFolder("newDir");
        var account = new Account();

        var helper = new FilePathHelper(part, KEY, account.getUUID());
        helper.savePath(newDir.getAbsolutePath());

        assertThat(preferenceStore.getString(KEY + "." + account.getUUID()), is(newDir.getAbsolutePath()));

        assertThat(preferenceStore.getString(KEY), is(""));
        verify(eclipsePreferences, never()).put(KEY, newDir.getAbsolutePath());
    }

    @Test
    public void testSavePathWithoutQualifier() throws IOException
    {
        File newDir = folder.newFolder("newDir");

        var helper = new FilePathHelper(part, KEY);
        helper.savePath(newDir.getAbsolutePath());

        assertThat(preferenceStore.getString(KEY), is(newDir.getAbsolutePath()));
        verify(eclipsePreferences).put(KEY, newDir.getAbsolutePath());
    }

    /**
     * The preference without qualifier is the default for the contexts that have
     * none, for example File - Import - PDF. An import for one depot must not
     * move it.
     */
    @Test
    public void testSavePathWithQualifierDoesNotChangeDefaultWithoutQualifier() throws IOException
    {
        File defaultDir = folder.newFolder("defaultDir");
        File newDir = folder.newFolder("newDir");

        new FilePathHelper(part, KEY).savePath(defaultDir.getAbsolutePath());
        new FilePathHelper(part, KEY, new Portfolio().getUUID()).savePath(newDir.getAbsolutePath());

        assertThat(new FilePathHelper(part, KEY).getPath(), is(defaultDir.getAbsolutePath()));
    }

    /**
     * A depot that has no entry of its own yet - a newly created one - starts in
     * the default directory.
     */
    @Test
    public void testDepotWithoutOwnEntryStartsAtDefault() throws IOException
    {
        File defaultDir = folder.newFolder("defaultDir");

        new FilePathHelper(part, KEY).savePath(defaultDir.getAbsolutePath());

        var helper = new FilePathHelper(part, KEY, new Portfolio().getUUID());
        assertThat(helper.getPath(), is(defaultDir.getAbsolutePath()));
    }
}
