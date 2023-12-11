package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.e4.core.di.annotations.Creatable;

@Creatable
@Singleton
public class ProgressMonitorFactory extends ProgressProvider
{
    private List<ProgressProvider> providers = new ArrayList<ProgressProvider>();

    @Override
    public IProgressMonitor createMonitor(Job job)
    {
        for (ProgressProvider p : providers)
        {
            IProgressMonitor monitor = p.createMonitor(job);
            if (monitor != null)
                return monitor;
        }

        return new NullProgressMonitor();
    }

    public void addProgressProvider(ProgressProvider provider)
    {
        providers.add(provider);
    }

    public void removeProgressProvider(ProgressProvider provider)
    {
        providers.remove(provider);
    }
}
