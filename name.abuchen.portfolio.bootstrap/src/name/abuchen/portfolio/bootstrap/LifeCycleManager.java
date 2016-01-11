package name.abuchen.portfolio.bootstrap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
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
        checkForJava8();
        removeClearPersistedStateFlag();
        checkForModelChanges();
        setupEventLoopAdvisor(context);
    }

    private void checkForJava8()
    {
        // if the java version is < 8, show a message dialog because otherwise
        // the application would silently not start

        double version = Double.parseDouble(System.getProperty("java.specification.version")); //$NON-NLS-1$

        if (version < 1.8)
        {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.TitleJavaVersion,
                            Messages.MsgMinimumRequiredVersion);
            throw new UnsupportedOperationException("The minimum Java version required is Java 8"); //$NON-NLS-1$
        }
    }

    private void removeClearPersistedStateFlag()
    {
        // the 'old' update mechanism edited the ini file *after* the upgrade
        // and added the -clearPersistedState flag. The current mechanism does
        // not need it, hence it must be remove if present

        // not applicable on Mac OS X because only update is not supported
        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            return;

        try
        {
            IniFileManipulator iniFile = new IniFileManipulator();
            iniFile.load();
            iniFile.unsetClearPersistedState();
            if (iniFile.isDirty())
                iniFile.save();
        }
        catch (IOException ignore)
        {
            // ignore: in production, it will anyway be removed during the next
            // update; in development, it will annoy to always report this error
        }
    }

    private void checkForModelChanges()
    {
        Version modelVersion = Version.parseVersion(preferences.get(MODEL_VERSION, null));
        Version programVersion = FrameworkUtil.getBundle(this.getClass()).getVersion();

        if (!modelVersion.equals(programVersion))
        {
            logger.info(MessageFormat.format(
                            "Detected model change from version {0} to version {1}; clearing persisted state", //$NON-NLS-1$
                            modelVersion.toString(), programVersion.toString()));
            System.setProperty("clearPersistedState", "true"); //$NON-NLS-1$ //$NON-NLS-2$
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
                    exception.printStackTrace();
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

                if (!"internal_new_GC".equals(stackTrace[0].getMethodName())) //$NON-NLS-1$
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
    public void doPreSave(MApplication app, EPartService partService, EModelService modelService)
    {
        saveModelVersion();
        removePortfolioPartsWithoutPersistedFile(app, partService, modelService);
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

    private void removePortfolioPartsWithoutPersistedFile(MApplication app, EPartService partService,
                    EModelService modelService)
    {
        MPartStack stack = (MPartStack) modelService.find("name.abuchen.portfolio.ui.partstack.main", app); //$NON-NLS-1$

        List<MStackElement> toBeRemoved = new ArrayList<MStackElement>();

        for (MStackElement child : stack.getChildren())
        {
            if (!(child instanceof MPart))
                continue;

            if (!"name.abuchen.portfolio.ui.part.portfolio".equals(child.getElementId())) //$NON-NLS-1$
                continue;

            String filename = child.getPersistedState().get("file"); //$NON-NLS-1$
            if (filename == null)
                toBeRemoved.add(child);
        }

        if (!toBeRemoved.isEmpty())
        {
            if (toBeRemoved.contains(stack.getSelectedElement()))
                stack.setSelectedElement(null);
            stack.getChildren().removeAll(toBeRemoved);
        }
    }
}
