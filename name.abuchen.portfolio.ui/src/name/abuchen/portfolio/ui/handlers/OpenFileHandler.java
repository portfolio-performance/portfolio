package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class OpenFileHandler
{
    @Inject
    @Preference
    private IEclipsePreferences preferences;

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(IServiceConstants.ACTIVE_PART) MPart activePart, //
                    MApplication app, EPartService partService, EModelService modelService)
    {
        String path = preferences.get(UIConstants.Preferences.DEFAULT_OPEN_PATH, null);

        if (path != null && !Files.isDirectory(Paths.get(path)))
            path = null;

        if (path == null)
            path = System.getProperty("user.home"); //$NON-NLS-1$

        FileDialog dialog = new FileDialog(shell, SWT.OPEN);
        dialog.setFilterPath(path);
        dialog.setFilterExtensions(new String[] { "*.xml;*.XML;*.zip;*.ZIP;*.portfolio;*.PORTFOLIO", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setFilterNames(new String[] { Messages.LabelPortfolioPerformanceFile, Messages.LabelAllFiles });
        String fileSelected = dialog.open();

        if (fileSelected != null)
        {
            preferences.put(UIConstants.Preferences.DEFAULT_OPEN_PATH, dialog.getFilterPath());

            MPart part = partService.createPart(UIConstants.Part.PORTFOLIO);
            part.setLabel(new File(fileSelected).getName());
            part.setTooltip(fileSelected);
            part.getPersistedState().put(UIConstants.PersistedState.FILENAME, fileSelected);

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
