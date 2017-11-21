package name.abuchen.portfolio.p2;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

@Creatable
public class P2Service
{

    @Inject
    private IProvisioningAgent agent;

    public IInstallableUnit getProduct()
    {
        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
        IProfile p2Profile = profileRegistry.getProfile(IProfileRegistry.SELF);

        IQuery<IInstallableUnit> query = QueryUtil.createIUProductQuery();
        IQueryResult<IInstallableUnit> queryResult = p2Profile.query(query, null);
        if (!queryResult.isEmpty())
            return queryResult.iterator().next();
        return null;

    }

    public Set<IInstallableUnit> getInstalledPlugins()
    {
        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
        IProfile p2Profile = profileRegistry.getProfile(IProfileRegistry.SELF);

        IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();

        IQueryResult<IInstallableUnit> queryResult = p2Profile.query(query, null);
        return queryResult.toUnmodifiableSet().stream().filter(iu -> {
            String groupProperty = iu.getProperty("org.eclipse.equinox.p2.type.group"); //$NON-NLS-1$
            String pluginTypeProperty = iu.getProperty("portfolio.plugin"); //$NON-NLS-1$
            return groupProperty != null && groupProperty.equals("true") && pluginTypeProperty != null //$NON-NLS-1$
                            && pluginTypeProperty.equals("true");
        }).collect(Collectors.toSet());
    }

    public Set<IInstallableUnit> getLatestProductsFromUpdateSite(URI updateSite, IProgressMonitor monitor)
                    throws ProvisionException
    {
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository repository = manager.loadRepository(updateSite, monitor);
        return repository.query(QueryUtil.createLatestQuery(QueryUtil.createIUGroupQuery()), monitor)
                        .toUnmodifiableSet();
    }

    public List<IInstallableUnit> fetchPluginsFromUpdateSite(URI updateSite, IProgressMonitor monitor)
                    throws ProvisionException
    {
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository repository = manager.loadRepository(updateSite, monitor);
        return repository.query(QueryUtil.createLatestIUQuery(), monitor).toUnmodifiableSet().stream().filter(iu -> {
            String groupProperty = iu.getProperty("org.eclipse.equinox.p2.type.group"); //$NON-NLS-1$
            String pluginTypeProperty = iu.getProperty("portfolio.plugin"); //$NON-NLS-1$
            return groupProperty != null && groupProperty.equals("true") && pluginTypeProperty != null //$NON-NLS-1$
                            && pluginTypeProperty.equals("true");
        }).collect(Collectors.toList());
    }

    public Set<IInstallableUnit> fetchInstallableGroupsFromUpdateSite(URI updateSite, IProgressMonitor monitor)
                    throws ProvisionException
    {
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository repository = manager.loadRepository(updateSite, monitor);
        return repository.query(QueryUtil.createIUGroupQuery(), monitor).toUnmodifiableSet();
    }

    public Function<ProfileChangeContext, IStatus> install = context -> {
        final Collection<IInstallableUnit> toInstall = context.getInstallableUnits();
        final Collection<URI> updateSites = context.getUpdateSites();
        final IProgressMonitor monitor = context.getProgressMonitor();

        ProvisioningSession session = new ProvisioningSession(agent);
        InstallOperation operation = new InstallOperation(session, toInstall);
        configureOperation(operation, updateSites);
        return executeProfileChangeOperation(operation, monitor);
    };

    public Function<ProfileChangeContext, IStatus> uninstall = context -> {
        final Collection<IInstallableUnit> toUninstall = context.getInstallableUnits();
        final IProgressMonitor monitor = context.getProgressMonitor();
        UninstallOperation operation = newUninstallOperation(toUninstall);
        return executeProfileChangeOperation(operation, monitor);
    };

    public UninstallOperation newUninstallOperation(Collection<IInstallableUnit> toUninstall)
    {
        ProvisioningSession session = new ProvisioningSession(agent);
        UninstallOperation operation = new UninstallOperation(session, toUninstall);
        configureOperation(operation, Collections.emptyList());
        return operation;
    }

    public Function<ProfileChangeContext, IStatus> update = context -> {
        final Collection<IInstallableUnit> toUpdate = context.getInstallableUnits();
        final Collection<URI> updateSites = context.getUpdateSites();
        final IProgressMonitor monitor = context.getProgressMonitor();

        UpdateOperation operation = newUpdateOperation(toUpdate, updateSites);
        return executeProfileChangeOperation(operation, monitor);
    };

    public UpdateOperation newUpdateOperation(final Collection<URI> updateSites)
    {
        return newUpdateOperation(null, updateSites);
    }

    private UpdateOperation newUpdateOperation(Collection<IInstallableUnit> toUpdate, final Collection<URI> updateSites)
    {
        ProvisioningSession session = new ProvisioningSession(agent);
        UpdateOperation operation = new UpdateOperation(session, toUpdate);
        configureOperation(operation, updateSites);
        return operation;
    }

    public IInstallableUnit findInstalledVersion(IInstallableUnit installedVersion)
    {
        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);

        IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
        if (profile != null)
        {
            IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUQuery(installedVersion.getId()),
                            new NullProgressMonitor());
            Iterator<IInstallableUnit> iterator = query.iterator();
            if (iterator.hasNext())
                return iterator.next();
        }
        return null;
    }

    public IStatus executeProfileChangeOperation(final ProfileChangeOperation operation, final IProgressMonitor monitor)
    {
        final IStatus status = operation.resolveModal(monitor);

        if (!status.isOK())
            return status;

        final ProvisioningJob provisioningJob = operation.getProvisioningJob(monitor);
        if (provisioningJob == null)
            return Status.CANCEL_STATUS;

        provisioningJob.schedule();
        return status;
    }

    private void configureOperation(final ProfileChangeOperation operation, final Collection<URI> updateSites)
    {
        URI[] urisArray = updateSites.toArray(new URI[updateSites.size()]);
        operation.getProvisioningContext().setArtifactRepositories(urisArray);
        operation.getProvisioningContext().setMetadataRepositories(urisArray);
    }

    public static class ProfileChangeContext
    {
        private List<IInstallableUnit> installableUnits;
        private Collection<URI> updateSites;
        private IProgressMonitor progressMonitor;

        public ProfileChangeContext(List<IInstallableUnit> installableUnits, Collection<URI> updateSites,
                        IProgressMonitor progressMonitor)
        {
            this.installableUnits = installableUnits;
            this.updateSites = updateSites;
            this.progressMonitor = progressMonitor;
        }

        public List<IInstallableUnit> getInstallableUnits()
        {
            return installableUnits;
        }

        public IProgressMonitor getProgressMonitor()
        {
            return progressMonitor;
        }

        public Collection<URI> getUpdateSites()
        {
            return updateSites;
        }
    }
}
