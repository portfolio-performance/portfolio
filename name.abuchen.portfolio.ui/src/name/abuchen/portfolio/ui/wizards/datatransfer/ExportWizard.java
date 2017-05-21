package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import name.abuchen.portfolio.datatransfer.csv.AktienfreundeNetExporter;
import name.abuchen.portfolio.datatransfer.csv.CSVExporter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ExportWizard extends Wizard
{
    private final Client client;

    private ExportSelectionPage exportPage;

    public ExportWizard(Client client)
    {
        this.client = client;
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
                new CSVExporter().exportSecurityMasterData(new File(file, Messages.ExportWizardSecurityMasterData
                                + ".csv"), client.getSecurities()); //$NON-NLS-1$
            }
            else if (exportClass == Security.class)
            {
                if (Messages.ExportWizardSecurityMasterData.equals(exportItem))
                    new CSVExporter().exportSecurityMasterData(file, client.getSecurities());
                else if (Messages.ExportWizardMergedSecurityPrices.equals(exportItem))
                    new CSVExporter().exportMergedSecurityPrices(file, client.getSecurities());
                else if (Messages.ExportWizardAllTransactionsAktienfreundeNet.equals(exportItem))
                    new AktienfreundeNetExporter().exportAllTransactions(file, client);
            }

            // historical quotes
            else if (exportItem == SecurityPrice.class)
            {
                new CSVExporter().exportSecurityPrices(file, client.getSecurities());
            }
            else if (exportClass == SecurityPrice.class)
            {
                new CSVExporter().exportSecurityPrices(file, (Security) exportItem);
            }
            else
            {
                throw new UnsupportedOperationException(MessageFormat.format(Messages.ExportWizardUnsupportedExport,
                                exportClass, exportItem));
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
        File file = null;
        if (exportItem instanceof Class)
        {
            DirectoryDialog directoryDialog = new DirectoryDialog(getShell());

            directoryDialog.setMessage(Messages.ExportWizardSelectDirectory);

            String dir = directoryDialog.open();
            if (dir != null)
                file = new File(dir);
        }
        else
        {
            String name = null;
            if (exportItem instanceof Account)
                name = ((Account) exportItem).getName();
            else if (exportItem instanceof Portfolio)
                name = ((Portfolio) exportItem).getName();
            else if (exportItem instanceof Security)
                name = ((Security) exportItem).getIsin();
            else if (exportItem instanceof String)
                name = (String) exportItem;

            FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
            dialog.setOverwrite(true);
            if (name != null)
                dialog.setFileName(name + ".csv"); //$NON-NLS-1$
            String fileName = dialog.open();

            if (fileName != null)
                file = new File(fileName);
        }
        return file;
    }

}
