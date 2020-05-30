package name.abuchen.portfolio.ui.handlers;

import javax.inject.Inject;
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
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.wizards.client.NewClientWizard;

public class NewFileHandler
{
    @Inject
    private ClientInputFactory clientInputFactory;

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(IServiceConstants.ACTIVE_PART) MPart activePart, //
                    MApplication app, EPartService partService, EModelService modelService)
    {
        NewClientWizard wizard = new NewClientWizard();
        WizardDialog dialog = new WizardDialog(shell, wizard);
        if (dialog.open() == Window.OK)
        {
            ClientInput clientInput = clientInputFactory.create(Messages.LabelUnnamedXml, wizard.getClient());

            MPart part = partService.createPart(UIConstants.Part.PORTFOLIO);
            part.setLabel(Messages.LabelUnnamedXml);
            part.getTransientData().put(ClientInput.class.getName(), clientInput);

            if (activePart != null)
                activePart.getParent().getChildren().add(part);
            else
                ((MPartStack) modelService.find(UIConstants.PartStack.MAIN, app)).getChildren().add(part);

            part.setVisible(true);
            part.getParent().setVisible(true);
            partService.showPart(part, PartState.ACTIVATE);
        }
    }
}
