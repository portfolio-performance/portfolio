package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.online.impl.DivvyDiaryUploader;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.util.Pair;

public class UploadToDivvyDiaryHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        if (divvyDiaryApiKey == null)
        {
            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.DivvyDiaryMissingAPIKey);
            return;
        }

        MenuHelper.getActiveClientInput(part).ifPresent(clientInput -> {
            DivvyDiaryUploader uploader = new DivvyDiaryUploader(divvyDiaryApiKey);

            retrieveAndPickPortfolio(shell, uploader)
                            .ifPresent(portfolioId -> uploadPortfolio(clientInput, uploader, portfolioId));
        });

    }

    @SuppressWarnings("unchecked")
    private Optional<Long> retrieveAndPickPortfolio(Shell shell, DivvyDiaryUploader uploader)
    {
        List<Pair<Long, String>> portfolios;

        try
        {
            portfolios = uploader.getPortfolios();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(ActiveShell.get(), Messages.LabelError, e.getMessage());
            return Optional.empty();
        }

        // should not happen because DivvyDiary always creates one portfolio
        if (portfolios.isEmpty())
            return Optional.empty();

        if (portfolios.size() == 1)
        {
            if (!MessageDialog.openConfirm(shell, Messages.LabelInfo, Messages.DivvyDiaryConfirmUpload))
                return Optional.empty();

            return Optional.of(portfolios.get(0).getLeft());
        }
        else
        {
            LabelProvider labelProvider = LabelProvider.createTextProvider(e -> ((Pair<Long, String>) e).getRight());
            ListSelectionDialog dialog = new ListSelectionDialog(shell, labelProvider);

            dialog.setTitle(Messages.LabelInfo);
            dialog.setMessage(Messages.DivvyDiaryConfirmUpload);
            dialog.setMultiSelection(false);

            dialog.setElements(portfolios);

            if (dialog.open() == Window.OK)
            {
                Object[] selected = dialog.getResult();
                if (selected.length > 0)
                    return Optional.of(((Pair<Long, String>) selected[0]).getLeft());
            }
        }

        return Optional.empty();
    }

    private void uploadPortfolio(ClientInput clientInput, DivvyDiaryUploader uploader, Long portfolioId)
    {
        new AbstractClientJob(clientInput.getClient(), Messages.DivvyDiaryMsgUploading)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try
                {
                    CurrencyConverter converter = new CurrencyConverterImpl(clientInput.getExchangeRateProviderFacory(),
                                    clientInput.getClient().getBaseCurrency());

                    uploader.upload(getClient(), converter, portfolioId);

                    Display.getDefault().asyncExec(() -> MessageDialog.openInformation(ActiveShell.get(),
                                    Messages.LabelInfo, Messages.DivvyDiaryUploadSuccessfulMsg));

                    return Status.OK_STATUS;
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);

                    Display.getDefault().asyncExec(() -> MessageDialog.openError(ActiveShell.get(), Messages.LabelError,
                                    e.getMessage()));

                    return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e);
                }
                catch (RuntimeException e)
                {
                    PortfolioPlugin.log(e);
                    throw e;
                }
            }
        }.schedule();
    }
}
