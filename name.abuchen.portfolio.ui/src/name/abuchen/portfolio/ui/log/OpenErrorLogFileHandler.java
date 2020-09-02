package name.abuchen.portfolio.ui.log;

import java.io.File;
import java.text.MessageFormat;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

@SuppressWarnings("restriction")
public class OpenErrorLogFileHandler
{

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named(E4Workbench.INSTANCE_LOCATION) Location instanceLocation, //
                    MApplication app, EPartService partService, EModelService modelService)
    {
        File logfile = new File(instanceLocation.getURL().getFile(), ".metadata/.log"); //$NON-NLS-1$

        if (!logfile.exists())
        {
            MessageDialog.openError(shell, Messages.LabelError,
                            MessageFormat.format(Messages.MsgErrorOpeningFile, logfile.getAbsoluteFile()));
        }
        else
        {
            MPart part = partService.createPart(UIConstants.Part.TEXT_VIEWER);
            part.getPersistedState().put(UIConstants.PersistedState.FILENAME, logfile.getAbsolutePath());

            MPartStack stack = (MPartStack) modelService.find(UIConstants.PartStack.MAIN, app);
            stack.getChildren().add(part);

            partService.showPart(part, PartState.ACTIVATE);
        }
    }
}
