package name.abuchen.portfolio.ui.handlers;

import java.io.File;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.UIConstants;

public class OpenRecentFileHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(IServiceConstants.ACTIVE_PART) MPart activePart, //
                    MApplication app, EPartService partService, EModelService modelService,
                    @Named("name.abuchen.portfolio.ui.param.file") String file)
    {
        MPart part = partService.createPart(UIConstants.Part.PORTFOLIO);
        part.setLabel(new File(file).getName());
        part.setTooltip(file);
        part.getPersistedState().put(UIConstants.PersistedState.FILENAME, file);

        if (activePart != null)
            activePart.getParent().getChildren().add(part);
        else
            ((MPartStack) modelService.find(UIConstants.PartStack.MAIN, app)).getChildren().add(part);

        part.setVisible(true);
        part.getParent().setVisible(true);
        partService.showPart(part, PartState.ACTIVATE);
    }
}
