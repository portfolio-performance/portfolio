package name.abuchen.portfolio.ui.menu;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;

import name.abuchen.portfolio.datatransfer.FileExtractorService;
import name.abuchen.portfolio.ui.Messages;

public class ImportFileMenuContribution
{
    @Inject
    private FileExtractorService fileExtractorService;

    @AboutToShow
    public void aboutToShow(List<MMenuElement> items, MApplication application)
    {
        fileExtractorService.getAll().forEach((type, extractorMap) -> {
            Optional<MCommand> commandOptional = application.getCommands().stream()
                            .filter(c -> c.getElementId().equals("name.abuchen.portfolio.ui.command.import.file"))
                            .findAny();
            if (commandOptional.isPresent())
            {
                MCommand command = commandOptional.get();

                MHandledMenuItem handledMenuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
                handledMenuItem.setLabel(Messages.bind(Messages.FileImportMenuLabel, type.toUpperCase()));
                handledMenuItem.setCommand(command);
                MParameter menuParameter = MCommandsFactory.INSTANCE.createParameter();
                menuParameter.setContributorURI("platform:/plugin/name.abuchen.portfolio.bootstrap");
                menuParameter.setElementId("name.abuchen.portfolio.ui.menu.param.extractor-type-" + type);
                menuParameter.setName("name.abuchen.portfolio.ui.param.extractor-type");
                menuParameter.setValue(type);
                handledMenuItem.getParameters().add(menuParameter);
                items.add(handledMenuItem);
            }
        });
    }
}
