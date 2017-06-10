package name.abuchen.portfolio.p2;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
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
        if (p2Profile != null)
        {
            IQuery<IInstallableUnit> query = QueryUtil.createIUProductQuery();
            IQueryResult<IInstallableUnit> queryResult = p2Profile.query(query, null);
            if (!queryResult.isEmpty()) { return queryResult.iterator().next(); }
        }
        return null;

    }

    public Set<IInstallableUnit> getInstalledUnits()
    {
        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
        IProfile p2Profile = profileRegistry.getProfile(IProfileRegistry.SELF);

        IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();

        IQueryResult<IInstallableUnit> queryResult = p2Profile.query(query, null);
        return queryResult.toSet();
    }

    public Stream<IInstallableUnit> getInstalledPlugins()
    {
        final IInstallableUnit product = getProduct();
        if (product != null)
        {
            final Collection<IRequirement> requirements = product.getRequirements();
            return getInstalledUnits().stream().filter(iu -> !iu.equals(product)
                            && !requirements.stream().filter(r -> r.isMatch(iu)).findFirst().isPresent());
        }
        return Stream.empty();
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

    public Set<IInstallableUnit> fetchInstallableUnitsFromUpdateSite(URI updateSite, IProgressMonitor monitor)
                    throws ProvisionException
    {
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository repository = manager.loadRepository(updateSite, monitor);
        return repository.query(QueryUtil.createLatestIUQuery(), monitor).toUnmodifiableSet();
    }

    public Set<IInstallableUnit> fetchInstallableGroupsFromUpdateSite(URI updateSite, IProgressMonitor monitor)
                    throws ProvisionException
    {
        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);
        IMetadataRepository repository = manager.loadRepository(updateSite, monitor);
        return repository.query(QueryUtil.createIUGroupQuery(), monitor).toUnmodifiableSet();
    }

    public IStatus install(Collection<IInstallableUnit> toInstall, final Collection<URI> updateSites,
                    final IJobChangeListener progressCallback)
    {
        ProvisioningSession session = new ProvisioningSession(agent);
        InstallOperation operation = new InstallOperation(session, toInstall);
        configureOperation(operation, updateSites);
        return executeProfileChangeOperation(operation, progressCallback);
    }

    public IStatus uninstall(Collection<IInstallableUnit> toUninstall, final IJobChangeListener progressCallback)
    {
        UninstallOperation operation = newUninstallOperation(toUninstall);
        return executeProfileChangeOperation(operation, progressCallback);
    }

    public UninstallOperation newUninstallOperation(Collection<IInstallableUnit> toUninstall)
    {
        ProvisioningSession session = new ProvisioningSession(agent);
        UninstallOperation operation = new UninstallOperation(session, toUninstall);
        configureOperation(operation, Collections.emptyList());
        return operation;
    }

    public IStatus update(Collection<IInstallableUnit> toUpdate, final Collection<URI> updateSites,
                    final IJobChangeListener progressCallback)
    {
        UpdateOperation operation = newUpdateOperation(toUpdate, updateSites);
        return executeProfileChangeOperation(operation, progressCallback);
    }

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
            if (iterator.hasNext()) { return iterator.next(); }
        }
        return null;
    }

    public IStatus executeProfileChangeOperation(final ProfileChangeOperation operation)
    {
        return executeProfileChangeOperation(operation, null);
    }

    public IStatus executeProfileChangeOperation(final ProfileChangeOperation operation,
                    final IJobChangeListener progressCallback)
    {
        NullProgressMonitor monitor = new NullProgressMonitor();

        final IStatus status = operation.resolveModal(monitor);

        if (!status.isOK()) { return status; }

        final ProvisioningJob provisioningJob = operation.getProvisioningJob(monitor);
        if (provisioningJob == null) { return Status.CANCEL_STATUS; }

        if (progressCallback != null)
        {
            provisioningJob.addJobChangeListener(progressCallback);
        }

        provisioningJob.schedule();
        return status;
    }

    private void configureOperation(final ProfileChangeOperation operation, final Collection<URI> updateSites)
    {
        URI[] urisArray = updateSites.toArray(new URI[updateSites.size()]);
        operation.getProvisioningContext().setArtifactRepositories(urisArray);
        operation.getProvisioningContext().setMetadataRepositories(urisArray);
    }
}
