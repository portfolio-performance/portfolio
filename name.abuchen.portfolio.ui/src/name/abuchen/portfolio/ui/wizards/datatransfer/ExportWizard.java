package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import name.abuchen.portfolio.datatransfer.csv.AktienfreundeNetExporter;
import name.abuchen.portfolio.datatransfer.csv.CSVExporter;
import name.abuchen.portfolio.datatransfer.csv.VINISExporter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.FilePathHelper;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;
import name.abuchen.portfolio.util.TextUtil;

public class ExportWizard extends Wizard
{
    private final PortfolioPart part;
    private final Client client;
    private final ExchangeRateProviderFactory factory;

    private ExportSelectionPage exportPage;

    public ExportWizard(PortfolioPart part, Client client, ExchangeRateProviderFactory factory)
    {
        this.part = part;
        this.client = client;
        this.factory = factory;
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        addPage(exportPage = new ExportSelectionPage(client));
        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        Object exportItem = exportPage.getExportItem();
        Class<?> exportClass = exportPage.getExportClass();

        File file = getFile(exportItem);

        if (file == null)
            return false;

        try
        {
            Optional<CurrencyConverter> converter;
            if (exportPage.convertCurrencies())
            {
                converter = Optional.of(new CurrencyConverterImpl(factory, client.getBaseCurrency()));
            }
            else
            {
                converter = Optional.empty();
            }

            // account transactions
            if (exportItem == AccountTransaction.class)
            {
                new CSVExporter().exportAccountTransactions(file, client.getAccounts());
            }
            else if (exportClass == AccountTransaction.class)
            {
                new CSVExporter().exportAccountTransactions(file, (Account) exportItem);
            }

            // portfolio transactions
            else if (exportItem == PortfolioTransaction.class)
            {
                new CSVExporter().exportPortfolioTransactions(file, client.getPortfolios());
            }
            else if (exportClass == PortfolioTransaction.class)
            {
                new CSVExporter().exportPortfolioTransactions(file, (Portfolio) exportItem);
            }

            // master data
            else if (exportItem == Security.class)
            {
                new CSVExporter().exportSecurityMasterData(
                                new File(file, Messages.ExportWizardSecurityMasterData + ".csv"), //$NON-NLS-1$
                                client.getSecurities());
            }
            else if (exportClass == Security.class)
            {
                if (Messages.ExportWizardSecurityMasterData.equals(exportItem))
                    new CSVExporter().exportSecurityMasterData(file, client.getSecurities());
                else if (Messages.ExportWizardMergedSecurityPrices.equals(exportItem))
                    new CSVExporter().exportMergedSecurityPrices(converter, file, client.getSecurities());
                else if (Messages.ExportWizardAllTransactionsAktienfreundeNet.equals(exportItem))
                    new AktienfreundeNetExporter().exportAllTransactions(file, client);
                else if (Messages.ExportWizardVINISApp.equals(exportItem))
                    new VINISExporter().exportAllValues(file, client, factory);
            }

            // historical quotes
            else if (exportItem == SecurityPrice.class)
            {
                new CSVExporter().exportSecurityPrices(converter, file, client.getSecurities());
            }
            else if (exportClass == SecurityPrice.class)
            {
                new CSVExporter().exportSecurityPrices(converter, file, (Security) exportItem);
            }
            else
            {
                throw new UnsupportedOperationException(
                                MessageFormat.format(Messages.ExportWizardUnsupportedExport, exportClass, exportItem));
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getShell(), Messages.ExportWizardErrorExporting, e.getMessage());
        }

        return true;
    }

    private File getFile(Object exportItem)
    {
        FilePathHelper helper = new FilePathHelper(part, UIConstants.Preferences.CSV_EXPORT_PATH);

        File file = null;
        if (exportItem instanceof Class)
        {
            DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
            directoryDialog.setMessage(Messages.ExportWizardSelectDirectory);
            directoryDialog.setFilterPath(helper.getPath());

            String dir = directoryDialog.open();
            if (dir != null)
            {
                file = new File(dir);
                helper.savePath(directoryDialog.getFilterPath());
            }
        }
        else
        {
            String name = null;
            if (exportItem instanceof Account account)
                name = account.getName();
            else if (exportItem instanceof Portfolio portfolio)
                name = portfolio.getName();
            else if (exportItem instanceof Security security)
                name = security.getExternalIdentifier();
            else if (exportItem instanceof String string)
                name = string;

            FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
            dialog.setFilterNames(new String[] { Messages.CSVImportLabelFileCSV });
            dialog.setFilterExtensions(new String[] { "*.csv" }); //$NON-NLS-1$
            dialog.setOverwrite(true);
            if (name != null)
                dialog.setFileName(TextUtil.sanitizeFilename(name + ".csv")); //$NON-NLS-1$
            dialog.setFilterPath(helper.getPath());

            String fileName = dialog.open();

            if (fileName != null)
            {
                file = new File(fileName);
                helper.savePath(dialog.getFilterPath());
            }
        }
        return file;
    }

}
