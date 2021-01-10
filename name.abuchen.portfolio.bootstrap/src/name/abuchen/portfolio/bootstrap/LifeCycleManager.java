package name.abuchen.portfolio.bootstrap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.core.services.statusreporter.StatusReporter;
import org.eclipse.e4.ui.internal.workbench.swt.IEventLoopAdvisor;
import org.eclipse.e4.ui.model.application.MAddon;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IModelResourceHandler;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

@SuppressWarnings("restriction")
public class LifeCycleManager
{
    private static final String MODEL_VERSION = "model.version"; //$NON-NLS-1$

    @Inject
    @Preference(nodePath = "name.abuchen.portfolio.bootstrap")
    IEclipsePreferences preferences;

    @Inject
    Logger logger;

    @PostContextCreate
    public void doPostContextCreate(IEclipseContext context)
    {
        checkForModelChanges();
        checkForRequestToClearPersistedState();
        setupEventLoopAdvisor(context);
    }

    private void checkForModelChanges()
    {
        Version modelVersion = Version.parseVersion(preferences.get(MODEL_VERSION, null));
        Version programVersion = FrameworkUtil.getBundle(this.getClass()).getVersion();

        if (!modelVersion.equals(programVersion))
        {
            logger.info(MessageFormat.format(
                            "Detected model change from version {0} to version {1}; clearing persisted state", //$NON-NLS-1$
                            modelVersion, programVersion));
            System.setProperty(IWorkbench.CLEAR_PERSISTED_STATE, Boolean.TRUE.toString());
        }
    }

    private void checkForRequestToClearPersistedState()
    {
        boolean forceClearPersistedState = Boolean.parseBoolean(
                        preferences.get(ModelConstants.FORCE_CLEAR_PERSISTED_STATE, Boolean.FALSE.toString()));

        if (forceClearPersistedState)
        {
            logger.info(MessageFormat.format("Clearing persisted state due to ''{0}=true''", //$NON-NLS-1$
                            ModelConstants.FORCE_CLEAR_PERSISTED_STATE));
            System.setProperty(IWorkbench.CLEAR_PERSISTED_STATE, Boolean.TRUE.toString());

            // set as system property so that the ResourceWindowStateProcessor
            // does not attempt to merge parts of the old model into the new one
            System.setProperty(ModelConstants.FORCE_CLEAR_PERSISTED_STATE, Boolean.TRUE.toString());

            try
            {
                preferences.remove(ModelConstants.FORCE_CLEAR_PERSISTED_STATE);
                preferences.flush();
            }
            catch (BackingStoreException e)
            {
                logger.error(e);
            }
        }
    }

    public void setupEventLoopAdvisor(final IEclipseContext context)
    {
        // do not show an error popup if is the annoying NPE on El Capitan
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=434393

        context.set(IEventLoopAdvisor.class, new IEventLoopAdvisor()
        {
            @Override
            public void eventLoopIdle(final Display display)
            {
                display.sleep();
            }

            @Override
            public void eventLoopException(final Throwable exception)
            {
                boolean isAnnoyingNullPointerOnElCapitan = isAnnoyingNullPointerOnElCapitan(exception);

                StatusReporter statusReporter = (StatusReporter) context.get(StatusReporter.class.getName());
                if (!isAnnoyingNullPointerOnElCapitan && statusReporter != null)
                {
                    statusReporter.show(StatusReporter.ERROR, "Internal Error", exception); //$NON-NLS-1$
                }
                else if (logger != null)
                {
                    logger.error(exception);
                }
                else
                {
                    exception.printStackTrace(); // NOSONAR
                }
            }

            private boolean isAnnoyingNullPointerOnElCapitan(Throwable exception)
            {
                if (!(exception instanceof NullPointerException))
                    return false;

                StackTraceElement[] stackTrace = exception.getStackTrace();
                if (stackTrace == null || stackTrace.length == 0)
                    return true;

                if (!"org.eclipse.swt.widgets.Control".equals(stackTrace[0].getClassName())) //$NON-NLS-1$
                    return false;

                if (!"internal_new_GC".equals(stackTrace[0].getMethodName())) //$NON-NLS-1$ NOSONAR
                    return false;

                return true;
            }
        });
    }

    @ProcessRemovals
    public void removeDnDAddon(MApplication app)
    {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=394231#c3
        for (MAddon addon : new ArrayList<MAddon>(app.getAddons()))
        {
            String contributionURI = addon.getContributionURI();
            if (contributionURI.contains("ui.workbench.addons.minmax.MinMaxAddon") //$NON-NLS-1$
                            || contributionURI.contains("ui.workbench.addons.splitteraddon.SplitterAddon")) //$NON-NLS-1$
            {
                app.getAddons().remove(addon);
            }
        }
    }

    @PreSave
    public void doPreSave(MApplication app, EModelService modelService, IModelResourceHandler handler)
    {
        saveModelVersion();
        removePortfolioPartsWithoutPersistedFile(app, modelService);
        saveCopyOfApplicationModel(app, handler);
    }

    private void saveModelVersion()
    {
        String modelVersion = preferences.get(MODEL_VERSION, Version.emptyVersion.toString());
        String programVersion = FrameworkUtil.getBundle(this.getClass()).getVersion().toString();

        if (!modelVersion.equals(programVersion))
        {
            try
            {
                preferences.put(MODEL_VERSION, programVersion);
                preferences.flush();
            }
            catch (BackingStoreException e)
            {
                logger.error(e);
            }
        }
    }

    private void removePortfolioPartsWithoutPersistedFile(MApplication app, EModelService modelService)
    {
        List<MPart> parts = modelService.findElements(app, MPart.class, EModelService.IN_ACTIVE_PERSPECTIVE,
                        element -> {
                            if (!ModelConstants.PORTFOLIO_PART.equals(element.getElementId()))
                                return false;
                            return element.getPersistedState().get("file") == null; //$NON-NLS-1$
                        });

        Set<MElementContainer<?>> parentsWithRemovedChildren = new HashSet<>();

        for (MPart part : parts)
        {
            MElementContainer<MUIElement> parent = part.getParent();

            if (part.equals(parent.getSelectedElement()))
                parent.setSelectedElement(null);

            parent.getChildren().remove(part);
            parentsWithRemovedChildren.add(parent);
        }

        for (MElementContainer<?> container : parentsWithRemovedChildren)
        {
            if (modelService.isLastEditorStack(container))
                break;

            if (container.getChildren().isEmpty())
            {
                MElementContainer<MUIElement> parent = container.getParent();
                if (parent != null)
                {
                    container.setToBeRendered(false);

                    if (container.equals(parent.getSelectedElement()))
                        parent.setSelectedElement(null);
                    parent.getChildren().remove(container);
                }
                else if (container instanceof MWindow)
                {
                    // Must be a Detached Window
                    MUIElement eParent = (MUIElement) ((EObject) container).eContainer();
                    if (eParent instanceof MPerspective)
                    {
                        ((MPerspective) eParent).getWindows().remove(container);
                    }
                    else if (eParent instanceof MWindow)
                    {
                        ((MWindow) eParent).getWindows().remove(container);
                    }
                }
            }

        }
    }

    /**
     * Save a copy of the application model so that the
     * {@link MergeOldLayoutIntoCurrentApplicationModelProcessor} can merge the
     * layout back if the application has been updated.
     */
    private void saveCopyOfApplicationModel(MApplication app, IModelResourceHandler handler)
    {
        try
        {
            MApplication appCopy = (MApplication) EcoreUtil.copy((EObject) app);
            Resource resource = handler.createResourceWithApp(appCopy);

            File file = new File(Platform.getStateLocation(FrameworkUtil.getBundle(LifeCycleManager.class)).toFile(),
                            ModelConstants.E4XMICOPY_FILENAME);

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file)))
            {
                resource.save(out, null);
            }
            catch (IOException e)
            {
                // nothing to do: if no copy of the application model exist when
                // clearing the persisted state (for example after an upgrade),
                // then the user has to start with an empty layout
                logger.error(e);
            }

            resource.unload();
            resource.getResourceSet().getResources().remove(resource);
        }
        catch (IllegalArgumentException e)
        {
            // error while copying application model
            logger.error(e);
        }
    }
}
