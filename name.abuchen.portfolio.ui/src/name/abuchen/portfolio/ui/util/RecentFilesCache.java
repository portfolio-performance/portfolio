package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.di.extensions.Preference;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

@Creatable
@Singleton
public class RecentFilesCache
{
    @Inject
    @Preference
    IEclipsePreferences preferences;

    private static final int MAXIMUM = 10;

    private final Set<String> files = Collections
                    .newSetFromMap(new LinkedHashMap<String, Boolean>(32, 0.7f, true)
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest)
                        {
                            return size() > MAXIMUM;
                        }
                    });

    @PostConstruct
    public void load()
    {
        String pref = preferences.get(UIConstants.Preferences.RECENT_FILES, ""); //$NON-NLS-1$
        if (!pref.isEmpty())
            files.addAll(Arrays.asList(pref.split(File.pathSeparator)));
    }

    @PreDestroy
    public void save()
    {
        try
        {
            preferences.put(UIConstants.Preferences.RECENT_FILES, String.join(File.pathSeparator, files));
            preferences.flush();
        }
        catch (BackingStoreException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    @Inject
    @Optional
    public void onFileOpened(@EventTopic(UIConstants.Event.File.OPENED) String file)
    {
        files.add(file);
    }

    @Inject
    @Optional
    public void onFileSaved(@EventTopic(UIConstants.Event.File.SAVED) String file)
    {
        files.add(file);
    }

    @Inject
    @Optional
    public void onFileRemoved(@EventTopic(UIConstants.Event.File.REMOVED) String file)
    {
        files.remove(file);
    }

    public List<String> getRecentFiles()
    {
        List<String> answer = new ArrayList<>(files);
        Collections.reverse(answer);
        return answer;
    }

    public void clearRecentFiles()
    {
        files.clear();
    }
}
