package name.abuchen.portfolio.ui.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Version;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/* package */class NewVersion
{
    private static final String VERSION_MARKER = "-- "; //$NON-NLS-1$

    /* package */ static class Release
    {
        private Version version;
        private List<String> lines = new ArrayList<>();

        public Release(Version version)
        {
            this.version = version;
        }

        public Version getVersion()
        {
            return version;
        }

        public List<String> getLines()
        {
            return lines;
        }
    }

    private String version;
    private String minimumJavaVersionRequired;
    private List<Release> releases = new ArrayList<>();

    public NewVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersionHistory(String history)
    {
        if (history == null)
            return;

        String lines[] = history.split("\\r?\\n"); //$NON-NLS-1$

        Release release = new Release(null); // dummy
        for (String line : lines)
        {
            if (line.startsWith(VERSION_MARKER))
            {
                try
                {
                    Version version = new Version(line.substring(VERSION_MARKER.length()));
                    release = new Release(version);
                    this.releases.add(release);
                }
                catch (IllegalArgumentException e)
                {
                    PortfolioPlugin.log(e);

                    // in case parsing of the version fails, let's setup a dummy
                    // release to not loose the update information
                    release = new Release(new Version(99, 0, 0));
                    this.releases.add(release);
                }
            }
            else
            {
                release.lines.add(line);
            }
        }

        // sort reverse by version
        Collections.sort(releases, (r, l) -> l.version.compareTo(r.version));
    }

    public List<Release> getReleases()
    {
        return releases;
    }

    public void setMinimumJavaVersionRequired(String minimumJavaVersionRequired)
    {
        this.minimumJavaVersionRequired = minimumJavaVersionRequired;
    }

    public boolean requiresNewJavaVersion()
    {
        if (minimumJavaVersionRequired == null)
            return false;

        double current = Double.parseDouble(System.getProperty("java.specification.version")); //$NON-NLS-1$
        double required = Double.parseDouble(minimumJavaVersionRequired);

        return required > current;
    }
}
