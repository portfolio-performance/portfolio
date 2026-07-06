package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.jobs.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.util.UnrecognizedPDFCache;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public final class ImportExtractedItemsWizard extends Wizard
{
    // manual items always carry their target account/portfolio, so the context is never consulted
    private static final ImportAction.Context EMPTY_CONTEXT = new ImportAction.Context()
    {
        @Override
        public Account getAccount(String currencyCode)
        {
            return null;
        }

        @Override
        public Portfolio getPortfolio()
        {
            return null;
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            return null;
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return null;
        }
    };

    private Client client;
    private IPreferenceStore preferences;
    private Map<Extractor, List<Item>> result;
    private Map<File, List<Exception>> errors;
    private Map<File, PDFInputFile> failedInputFiles;
    private PortfolioPart part;

    @Inject
    private UnrecognizedPDFCache debugCache;

    private List<ReviewExtractedItemsPage> pages = new ArrayList<>();
    private List<ManualTransactionEntryPage> manualPages = new ArrayList<>();

    /**
     * If a target account is given, then account is preselected to be imported
     */
    private Account account;
    private Portfolio portfolio;

    // securities seen in auto-extracted results but not yet part of the client;
    // offered as choices in the manual entry dialogs
    private List<Security> additionalSecurities = Collections.emptyList();

    public ImportExtractedItemsWizard(Client client, IPreferenceStore preferences, Map<Extractor, List<Item>> result,
                    Map<File, List<Exception>> errors, Map<File, PDFInputFile> failedInputFiles, PortfolioPart part)
    {
        this.client = client;
        this.preferences = preferences;
        this.result = result;
        this.errors = errors;
        this.failedInputFiles = failedInputFiles;
        this.part = part;

        setWindowTitle(Messages.PDFImportWizardTitle);
        setNeedsProgressMonitor(false);
        setDialogSettings(PortfolioPlugin.getDefault().getDialogSettings());

        // all securities seen in the auto-extracted results but not yet part of
        // the client; these are the only securities a manual transaction can
        // newly introduce, and they double as the candidate list for detecting
        // which new securities need price-feed configuration after import
        additionalSecurities = collectAdditionalSecurities();
    }

    private List<Security> collectAdditionalSecurities()
    {
        var seen = new HashSet<>(client.getSecurities());
        var additional = new ArrayList<Security>();

        for (var items : result.values())
        {
            for (var item : items)
            {
                var security = item.getSecurity();
                if (security != null && seen.add(security))
                    additional.add(security);
            }
        }

        return additional;
    }

    public void setTarget(Account target)
    {
        this.account = target;
    }

    public void setTarget(Portfolio target)
    {
        this.portfolio = target;
    }


    @Override
    public boolean canFinish()
    {
        // allow "Finish" only on the last page overall
        var lastPage = getLastPage();
        return lastPage != null && getContainer().getCurrentPage() == lastPage;
    }

    private IWizardPage getLastPage()
    {
        // mirrors addPages order: manual pages are always added after the review pages
        if (!manualPages.isEmpty())
            return manualPages.get(manualPages.size() - 1);
        if (!pages.isEmpty())
            return pages.get(pages.size() - 1);
        return null;
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        if (!errors.isEmpty())
        {
            addPage(new ErroneousImportFilesPage(errors, failedInputFiles.size()));
        }

        result.entrySet().stream() //
                        .sorted((r, l) -> r.getKey().getLabel().compareTo(l.getKey().getLabel())) //
                        .map(entry -> new Extractor()
                        {
                            @Override
                            public String getLabel()
                            {
                                return entry.getKey().getLabel();
                            }

                            @Override
                            public List<Item> extract(SecurityCache securityCache, InputFile file,
                                            List<Exception> errors)
                            {
                                return entry.getValue();
                            }

                            @Override
                            public List<Item> extract(List<InputFile> file, List<Exception> errors)
                            {
                                return entry.getValue();
                            }
                        }) //
                        .forEach(extractor -> {
                            ReviewExtractedItemsPage page = new ReviewExtractedItemsPage(client, extractor, preferences,
                                            Collections.emptyList());
                            if (account != null)
                                page.setAccount(account);
                            if (portfolio != null)
                                page.setPortfolio(portfolio);
                            pages.add(page);
                            addPage(page);
                        });

        // Add manual entry pages for failed PDF files, sorted for deterministic order
        var fileComparator = Comparator.comparingLong(File::lastModified).thenComparing(File::getPath);
        failedInputFiles.entrySet().stream()
                        .sorted((a, b) -> fileComparator.compare(a.getKey(), b.getKey()))
                        .forEach(entry -> {
                            var inputFile = entry.getValue();
                            if (inputFile.getText() != null)
                            {
                                debugCache.add(inputFile.getName(), inputFile.getText(),
                                                inputFile.getPDFBoxVersion());
                                var manualPage = new ManualTransactionEntryPage(part, client,
                                                additionalSecurities, inputFile, account, portfolio, debugCache);
                                manualPages.add(manualPage);
                                addPage(manualPage);
                            }
                        });

        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean performCancel()
    {
        if (manualPages.stream().anyMatch(ManualTransactionEntryPage::hasCreatedTransactions))
        {
            return MessageDialog.openQuestion(getShell(),
                            Messages.PDFImportWizardTitle,
                            Messages.PDFImportWizardCancelConfirmation);
        }
        return true;
    }

    @Override
    public boolean performFinish()
    {
        // 1. Import auto-extracted items (ImportController handles markDirty + consistency)
        if (!pages.isEmpty())
            new ImportController(client).perform(pages);

        // 2. Import manual items
        boolean manualItemsImported = false;

        if (!manualPages.isEmpty())
        {
            // raw InsertAction is fine: manual items were already validated by the transaction dialogs
            var action = new InsertAction(client);

            for (ManualTransactionEntryPage manualPage : manualPages)
            {
                for (Extractor.Item item : manualPage.getItems())
                {
                    try
                    {
                        var status = item.apply(action, EMPTY_CONTEXT);
                        if (status.getCode() == ImportAction.Status.Code.OK)
                            manualItemsImported = true;
                        else
                            PortfolioPlugin.log(MessageFormat.format(
                                            "Manual import of item ''{0}'' failed: {1}", //$NON-NLS-1$
                                            item.getTypeInformation(), status.getMessage()));
                    }
                    catch (RuntimeException e)
                    {
                        PortfolioPlugin.log(MessageFormat.format(
                                        "Manual import of item ''{0}'' failed", //$NON-NLS-1$
                                        item.getTypeInformation()), e);
                    }
                }
            }
        }

        if (manualItemsImported)
        {
            client.markDirty();
            new ConsistencyChecksJob(client, false).schedule();
        }

        // configure price feeds for the securities that ended up in the client
        // via either import path (auto selection or manual reference); a security
        // can only be new if it was among the auto-extracted ones
        var newSecurities = ImportController.newlyImportedSecurities(additionalSecurities, client);
        ImportController.configureNewSecurities(client, newSecurities);

        return true;
    }

}
