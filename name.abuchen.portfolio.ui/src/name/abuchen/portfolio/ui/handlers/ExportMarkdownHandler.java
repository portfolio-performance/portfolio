package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.datatransfer.markdown.PortfolioMarkdownExporter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.ExportMarkdownDialog;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.preferences.Experiments;
import name.abuchen.portfolio.util.TextUtil;

/**
 * Exports the portfolio as a Markdown report that can be used as context for AI
 * assistants (LLMs).
 */
public class ExportMarkdownHandler
{
    private static final String EXTENSION = ".md"; //$NON-NLS-1$

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return isEnabled() && MenuHelper.isClientPartActive(part);
    }

    /**
     * The export is an experimental feature for now.
     */
    public static boolean isEnabled()
    {
        return new Experiments().isEnabled(Experiments.Feature.NOV26_MARKDOWN_EXPORT);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        MenuHelper.getActiveClientInput(part).ifPresent(input -> export(shell, input));
    }

    private void export(Shell shell, ClientInput input)
    {
        var optionsDialog = new ExportMarkdownDialog(shell);
        if (optionsDialog.open() != Window.OK)
            return;

        var name = getName(input);

        var dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setFilterNames(new String[] { Messages.ExportMarkdownFileFilter });
        dialog.setFilterExtensions(new String[] { "*" + EXTENSION }); //$NON-NLS-1$
        dialog.setFileName(TextUtil.sanitizeFilename(name) + EXTENSION);
        dialog.setOverwrite(true);

        var fileName = dialog.open();
        if (fileName == null)
            return;

        try
        {
            var client = input.getClient();
            var converter = new CurrencyConverterImpl(input.getExchangeRateProviderFacory(), client.getBaseCurrency());

            var exporter = new PortfolioMarkdownExporter(client, converter, name)
                            .withTransactionScope(optionsDialog.getTransactionScope()) //
                            .withGenerator(getGenerator());

            // the performance calculations for all periods and years can take
            // a while: do not block the UI thread
            new ProgressMonitorDialog(shell).run(true, false, monitor -> {
                monitor.beginTask(Messages.ExportMarkdownDialogTitle, IProgressMonitor.UNKNOWN);
                try (var out = new FileOutputStream(new File(fileName)))
                {
                    exporter.export(out);
                }
                catch (IOException e)
                {
                    throw new InvocationTargetException(e);
                }
                finally
                {
                    monitor.done();
                }
            });
        }
        catch (InvocationTargetException e)
        {
            var cause = e.getCause() != null ? e.getCause() : e;
            PortfolioPlugin.log(cause);
            MessageDialog.openError(shell, Messages.ExportWizardErrorExporting, String.valueOf(cause.getMessage()));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (RuntimeException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.ExportWizardErrorExporting, String.valueOf(e.getMessage()));
        }
    }

    private String getName(ClientInput input)
    {
        var file = input.getFile();
        var name = file != null ? file.getName() : input.getLabel();

        var index = name.lastIndexOf('.');
        return index > 0 ? name.substring(0, index) : name;
    }

    private String getGenerator()
    {
        var bundle = FrameworkUtil.getBundle(getClass());
        if (bundle == null)
            return "Portfolio Performance"; //$NON-NLS-1$

        var version = bundle.getVersion();
        return MessageFormat.format("Portfolio Performance {0}.{1}.{2}", //$NON-NLS-1$
                        version.getMajor(), version.getMinor(), version.getMicro());
    }
}
