package name.abuchen.portfolio.bootstrap;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MBindingContext;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osgi.framework.FrameworkUtil;

/**
 * Merges the old part layout into the current application model.
 * <p/>
 * After every online update, a new application model must be created out of the
 * packaged e4xmi because the user should get the changes to menus, tool bars,
 * key bindings, etc. However, that also looses the layout of parts and requires
 * the user to re-open and re-arrange his files.
 * <p/>
 * To keep the layout, we
 * <ul>
 * <li>store a copy of the application model whenever the application is closed
 * (see {@link LifeCycleManager#saveCopyOfApplicationModel})</li>
 * <li>when starting the application, check if the state is cleared and then
 * copy the window elements (parts, part stack, part sash container) to the new
 * model</li>
 * <li>fix binding contexts and the error log parts so that the current
 * application model does not reference back to the copy</li>
 * </ul>
 */
@SuppressWarnings("restriction")
public class MergeOldLayoutIntoCurrentApplicationModelProcessor
{
    @Inject
    private EModelService modelService;

    @Inject
    private Logger logger;

    @Execute
    public void execute(MApplication theApp)
    {
        // if the users forces the reset of the UI model via menu, do not
        // attempt to merge the existing layout
        if (Boolean.parseBoolean(System.getProperty(ModelConstants.FORCE_CLEAR_PERSISTED_STATE)))
            return;

        // if the persisted state has not been cleared, we do not need to merge
        // the copy of the state back into the model
        if (!Boolean.parseBoolean(System.getProperty(IWorkbench.CLEAR_PERSISTED_STATE)))
            return;

        // read copy of the application model

        File file = Platform.getStateLocation(FrameworkUtil.getBundle(LifeCycleManager.class))
                        .append(ModelConstants.E4XMICOPY_FILENAME).toFile();
        if (!file.exists())
            return;

        logger.info(MessageFormat.format("Attempting to merge saved layout into current model from file {0}", //$NON-NLS-1$
                        file.getAbsolutePath()));

        URI uri = URI.createFileURI(file.getAbsolutePath());

        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = resourceSet.getResource(uri, true);

        if (resource.getContents().isEmpty())
            return;

        try
        {
            MApplication theAppCopy = (MApplication) resource.getContents().get(0);

            MWindow theWindow = theApp.getChildren().get(0);
            MWindow theWindowCopy = theAppCopy.getChildren().get(0);

            // retain window bounds

            theWindow.setX(theWindowCopy.getX());
            theWindow.setY(theWindowCopy.getY());
            theWindow.setHeight(theWindowCopy.getHeight());
            theWindow.setWidth(theWindowCopy.getWidth());

            // keep the error log part because of the tool bar references

            MUIElement theErrorLog = modelService.find(ModelConstants.ERRORLOG_PART, theApp);
            MUIElement theErrorLogCopy = modelService.find(ModelConstants.ERRORLOG_PART, theAppCopy);

            if (theErrorLog != null && theErrorLogCopy != null)
                EcoreUtil.replace((EObject) theErrorLogCopy, (EObject) theErrorLog);
            else if (theErrorLogCopy != null)
                EcoreUtil.remove((EObject) theErrorLogCopy);

            // copy window elements from the copy to the current window

            theWindow.getChildren().clear();
            theWindow.getChildren().addAll(theWindowCopy.getChildren());

            // fix binding context of portfolio file

            List<MBindingContext> bindingContexts = modelService.findElements(theApp, MBindingContext.class,
                            EModelService.ANYWHERE,
                            element -> ModelConstants.BINDING_CONTEXT_PORTFOLIO_FILE.equals(element.getElementId()));

            modelService.findElements(theApp, MPart.class, EModelService.IN_ACTIVE_PERSPECTIVE,
                            element -> ModelConstants.PORTFOLIO_PART.equals(element.getElementId())) //
                            .forEach(part -> {
                                part.getBindingContexts().clear();
                                part.getBindingContexts().addAll(bindingContexts);
                            });
        }
        finally
        {
            resource.unload();
            resourceSet.getResources().remove(resource);
        }
    }

}
