package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.IBFlexStatementExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.CommerzbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaExtractor;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.SBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.UnicreditPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportPDFHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named("name.abuchen.portfolio.ui.param.pdf-type") String type) throws IOException
    {
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        try
        {
            // determine extractor class
            Extractor extractor = createExtractor(type, client);

            // open file dialog to pick pdf files

            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            fileDialog.setText(extractor.getLabel());
            fileDialog.setFilterNames(new String[] { MessageFormat.format("{0} ({1})", //$NON-NLS-1$
                            extractor.getLabel(), extractor.getFilterExtension()) });
            fileDialog.setFilterExtensions(new String[] { extractor.getFilterExtension() });
            fileDialog.open();

            String[] fileNames = fileDialog.getFileNames();

            if (fileNames.length == 0)
                return;

            List<File> files = new ArrayList<>();
            for (String file : fileNames)
                files.add(new File(fileDialog.getFilterPath(), file));

            // open wizard dialog
            IPreferenceStore preferences = ((PortfolioPart) part.getObject()).getPreferenceStore();
            Dialog wizwardDialog = new WizardDialog(shell,
                            new ImportExtractedItemsWizard(client, extractor, preferences, files));
            wizwardDialog.open();
        }
        catch (IllegalArgumentException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }

    private Extractor createExtractor(String type, Client client) throws IOException, IllegalArgumentException
    {
        switch (type)
        {
            case "baaderbank": //$NON-NLS-1$
                return new BaaderBankPDFExtractor(client);
            case "bankslm": //$NON-NLS-1$
                return new BankSLMPDFExctractor(client);
            case "comdirect": //$NON-NLS-1$
                return new ComdirectPDFExtractor(client);
            case "commerzbank": //$NON-NLS-1$
                return new CommerzbankPDFExctractor(client);
            case "consorsbank": //$NON-NLS-1$
                return new ConsorsbankPDFExctractor(client);
            case "dab": //$NON-NLS-1$
                return new DABPDFExctractor(client);
            case "db": //$NON-NLS-1$
                return new DeutscheBankPDFExctractor(client);
            case "degiro": //$NON-NLS-1$
                return new DegiroPDFExtractor(client);
            case "dkb": //$NON-NLS-1$
                return new DkbPDFExtractor(client);
            case "fintechgroupbank": //$NON-NLS-1$
                return new FinTechGroupBankPDFExtractor(client);
            case "ingdiba": //$NON-NLS-1$
                return new INGDiBaExtractor(client);
            case "onvista": //$NON-NLS-1$
                return new OnvistaPDFExtractor(client);
            case "sbroker": //$NON-NLS-1$
                return new SBrokerPDFExtractor(client);
            case "unicredit": //$NON-NLS-1$
                return new UnicreditPDFExtractor(client);
            case "ib": //$NON-NLS-1$
                return new IBFlexStatementExtractor(client);
            default:
                throw new UnsupportedOperationException("Unknown pdf type: " + type); //$NON-NLS-1$
        }
    }
}
