package name.abuchen.portfolio.ui.editor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class FilePathHelperTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testScopedPathWinsOverLegacyFallback()
                    throws Exception
    {
        var legacyDirectory = tempFolder.newFolder("legacy").toPath();
        var scopedDirectory = tempFolder.newFolder("scoped").toPath();

        var preferences = new InMemoryPreferences();
        preferences.eclipseValues.put("PDF_IMPORT_PATH", legacyDirectory.toString());
        preferences.fileValues.put("PDF_IMPORT_PATH.account.UUID1", scopedDirectory.toString());

        var helper = new FilePathHelper(preferences, "PDF_IMPORT_PATH");

        assertThat(helper.getPath("account.UUID1"), is(scopedDirectory.toString()));
    }

    @Test
    public void testScopedPathFallsBackToLegacyValue()
                    throws Exception
    {
        var legacyDirectory = tempFolder.newFolder("legacy").toPath();

        var preferences = new InMemoryPreferences();
        preferences.fileValues.put("PDF_IMPORT_PATH", legacyDirectory.toString());

        var helper = new FilePathHelper(preferences, "PDF_IMPORT_PATH");

        assertThat(helper.getPath("portfolio.UUID2"), is(legacyDirectory.toString()));
    }

    @Test
    public void testSavePathUsesScopedKey()
                    throws Exception
    {
        var scopedDirectory = tempFolder.newFolder("scoped").toPath();

        var preferences = new InMemoryPreferences();
        var helper = new FilePathHelper(preferences, "PDF_IMPORT_PATH");

        helper.savePath("portfolio.UUID2", scopedDirectory.toString());

        assertThat(preferences.fileValues.get("PDF_IMPORT_PATH.portfolio.UUID2"), is(scopedDirectory.toString()));
        assertThat(preferences.eclipseValues.get("PDF_IMPORT_PATH.portfolio.UUID2"), is(scopedDirectory.toString()));
        assertThat(preferences.fileValues.get("PDF_IMPORT_PATH"), is(nullValue()));
        assertThat(preferences.eclipseValues.get("PDF_IMPORT_PATH"), is(nullValue()));
    }

    @Test
    public void testLegacyPathIsIgnoredWhenDirectoryNoLongerExists()
                    throws Exception
    {
        Path legacyDirectory = tempFolder.newFolder("legacy").toPath();
        Files.delete(legacyDirectory);

        var preferences = new InMemoryPreferences();
        preferences.eclipseValues.put("PDF_IMPORT_PATH", legacyDirectory.toString());

        var helper = new FilePathHelper(preferences, "PDF_IMPORT_PATH");

        assertThat(helper.getPath("account.UUID1"), is(System.getProperty("user.home")));
    }

    private static class InMemoryPreferences implements FilePathHelper.PreferencesAccess
    {
        private final Map<String, String> fileValues = new HashMap<>();
        private final Map<String, String> eclipseValues = new HashMap<>();

        @Override
        public String getPreferenceStoreValue(String key)
        {
            return fileValues.getOrDefault(key, "");
        }

        @Override
        public void setPreferenceStoreValue(String key, String value)
        {
            fileValues.put(key, value);
        }

        @Override
        public String getEclipsePreferenceValue(String key)
        {
            return eclipseValues.get(key);
        }

        @Override
        public void setEclipsePreferenceValue(String key, String value)
        {
            eclipseValues.put(key, value);
        }
    }
}
