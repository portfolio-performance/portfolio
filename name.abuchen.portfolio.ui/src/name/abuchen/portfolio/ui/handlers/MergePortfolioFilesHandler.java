package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ClientMergeService;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.OpenPasswordDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;

public class MergePortfolioFilesHandler
{
    @Inject
    @Preference
    private IEclipsePreferences preferences;

    @Inject
    private ClientInputFactory clientInputFactory;

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(IServiceConstants.ACTIVE_PART) MPart activePart, MApplication app,
                    EPartService partService, EModelService modelService)
    {
        String path = preferences.get(UIConstants.Preferences.DEFAULT_OPEN_PATH, null);

        if (path != null && !Files.isDirectory(Paths.get(path)))
            path = null;

        if (path == null)
            path = System.getProperty("user.home"); //$NON-NLS-1$

        FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        dialog.setFilterPath(path);
        dialog.setFilterExtensions(new String[] { "*.*" }); //$NON-NLS-1$
        dialog.setFilterNames(new String[] { Messages.LabelPortfolioPerformanceFile, Messages.LabelAllFiles });

        if (dialog.open() == null)
            return;

        preferences.put(UIConstants.Preferences.DEFAULT_OPEN_PATH, dialog.getFilterPath());

        String filterPath = dialog.getFilterPath();
        String[] fileNames = dialog.getFileNames();

        if (fileNames == null || fileNames.length < 2)
            return;

        if (!confirmMerge(shell))
            return;

        List<File> files = new ArrayList<>();
        for (String fileName : fileNames)
            files.add(new File(filterPath, fileName));

        Map<File, char[]> passwords = collectPasswords(shell, files);

        ClientMergeService mergeService = new ClientMergeService();
        Client merged;

        try
        {
            merged = mergeService.merge(files, passwords, new NullProgressMonitor());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(Messages.LabelClientMergeFailed, e);
        }
        finally
        {
            clearPasswords(passwords);
        }

        ClientInput clientInput = clientInputFactory.create(buildLabel(files), merged);

        MPart part = partService.createPart(UIConstants.Part.PORTFOLIO);
        part.setLabel(clientInput.getLabel());
        part.getTransientData().put(ClientInput.class.getName(), clientInput);

        if (activePart != null)
            activePart.getParent().getChildren().add(part);
        else
            ((MPartStack) modelService.find(UIConstants.PartStack.MAIN, app)).getChildren().add(part);

        part.setVisible(true);
        part.getParent().setVisible(true);
        partService.showPart(part, PartState.ACTIVATE);
    }

    private Map<File, char[]> collectPasswords(Shell shell, List<File> files)
    {
        Map<File, char[]> passwords = new HashMap<>();

        for (File file : files)
        {
            if (!ClientFactory.isEncrypted(file))
            {
                passwords.put(file, null);
                continue;
            }

            OpenPasswordDialog passwordDialog = new OpenPasswordDialog(shell, file.getName());

            if (passwordDialog.open() != Window.OK)
                throw new IllegalStateException(MessageFormat.format(Messages.LabelClientMergePasswordCancelled,
                                file.getAbsolutePath()));

            String password = passwordDialog.getPassword();
            passwords.put(file, password != null ? password.toCharArray() : null);
        }

        return passwords;
    }

    private void clearPasswords(Map<File, char[]> passwords)
    {
        for (char[] password : passwords.values())
        {
            if (password == null)
                continue;

            for (int i = 0; i < password.length; i++)
                password[i] = 0;
        }
    }

    private String buildLabel(List<File> files)
    {
        return MessageFormat.format(Messages.LabelClientMergeFilename, files.size());
    }

    private boolean confirmMerge(Shell shell)
    {
        return MessageDialog.openConfirm(shell, Messages.LabelClientMergeConfirmTitel,
                        Messages.LabelClientMergeConfirmMessage);
    }
}