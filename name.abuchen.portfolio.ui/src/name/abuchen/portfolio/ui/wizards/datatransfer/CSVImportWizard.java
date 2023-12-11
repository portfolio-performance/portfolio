package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.csv.CSVConfig;
import name.abuchen.portfolio.datatransfer.csv.CSVConfigManager;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class CSVImportWizard extends Wizard
{
    private static class ExtractorProxy implements Extractor
    {
        private final CSVImporter importer;

        public ExtractorProxy(CSVImporter importer)
        {
            this.importer = importer;
        }

        @Override
        public String getLabel()
        {
            return this.importer.getExtractor().getLabel();
        }

        @Override
        public List<Item> extract(SecurityCache securityCache, Extractor.InputFile file, List<Exception> errors)
        {
            return this.importer.createItems(errors);
        }

        @Override
        public List<Item> extract(List<InputFile> file, List<Exception> errors)
        {
            return this.importer.createItems(errors);
        }
    }

    /* package */static final String REVIEW_PAGE_ID = "reviewitems"; //$NON-NLS-1$

    private Client client;
    private IPreferenceStore preferences;
    private CSVImporter importer;

    @Inject
    private IStylingEngine stylingEngine;

    @Inject
    private CSVConfigManager configManager;

    /**
     * If a target security is given, then only security prices are imported
     * directly into that security.
     */
    private Security target;

    /**
     * If a target account is given, then only security prices are imported
     * directly into that security.
     */
    private Account account;

    /**
     * If a target portfolio is given, then only security prices are imported
     * directly into that security.
     */
    private Portfolio portfolio;

    /**
     * If a CSVConfig is given, then this configuration is preset (used when
     * opening the wizard with a specific configuration from the menu)
     */
    private CSVConfig initialConfig;

    private CSVImportDefinitionPage definitionPage;
    private ReviewExtractedItemsPage reviewPage;
    private SelectSecurityPage selectSecurityPage;

    public CSVImportWizard(Client client, IPreferenceStore preferences, File inputFile)
    {
        this.client = client;
        this.preferences = preferences;
        this.importer = new CSVImporter(client, inputFile);

        setupEncoding();

        setWindowTitle(Messages.CSVImportWizardTitle);
    }

    private void setupEncoding()
    {
        // It is impossible to guess the encoding of the text file. But more
        // often than not, it is the same as last time. Therefore we remember
        // the last encoding used and preset it the next time.

        String prefKey = CSVImportWizard.class.getName() + "-encoding"; //$NON-NLS-1$
        String encoding = preferences.getString(prefKey);

        if (encoding != null)
        {
            try
            {
                importer.setEncoding(Charset.forName(encoding));
            }
            catch (IllegalCharsetNameException | UnsupportedCharsetException e)
            {
                // ignore faulty charsets from preferences
            }
        }

        importer.addPropertyChangeListener("encoding", //$NON-NLS-1$
                        e -> preferences.putValue(prefKey, importer.getEncoding().name()));
    }

    public void setTarget(Account target)
    {
        this.account = target;
    }

    public void setTarget(Portfolio target)
    {
        this.portfolio = target;
    }

    public void setTarget(Security target)
    {
        this.target = target;
    }

    public void setConfiguration(CSVConfig config)
    {
        this.initialConfig = config;
    }

    public void setExtractor(String code)
    {
        importer.getExtractorByCode(code).ifPresent(e -> importer.setExtractor(e));
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        definitionPage = new CSVImportDefinitionPage(client, importer, configManager, stylingEngine, target != null);
        if (initialConfig != null)
            definitionPage.setInitialConfiguration(initialConfig);
        addPage(definitionPage);

        selectSecurityPage = new SelectSecurityPage(client);
        addPage(selectSecurityPage);

        reviewPage = new ReviewExtractedItemsPage(client, new ExtractorProxy(importer), preferences,
                        Arrays.asList(new Extractor.InputFile(importer.getInputFile())), REVIEW_PAGE_ID);
        if (account != null)
            reviewPage.setAccount(account);
        if (portfolio != null)
            reviewPage.setPortfolio(portfolio);
        reviewPage.setDoExtractBeforeEveryPageDisplay(true);
        addPage(reviewPage);

        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean canFinish()
    {
        return super.canFinish() && (target != null || getContainer().getCurrentPage() != definitionPage);
    }

    @Override
    public boolean performFinish()
    {
        ((AbstractWizardPage) getContainer().getCurrentPage()).afterPage();

        if (importer.getExtractor() == importer.getSecurityPriceExtractor())
        {
            var isDirty = importSecurityPrices();
            if (isDirty)
                client.markDirty();
        }
        else
        {
            new ImportController(client).perform(List.of(reviewPage));
        }

        return true;
    }

    private boolean importSecurityPrices()
    {
        Security security = target != null ? target : selectSecurityPage.getSelectedSecurity();

        List<Item> imported = importer.createItems(new ArrayList<>());
        if (imported.isEmpty())
            return false; // No valid quotes could be parsed.

        List<SecurityPrice> prices = imported.get(0).getSecurity().getPrices();

        boolean isDirty = false;
        for (SecurityPrice p : prices)
        {
            if (security.addPrice(p))
                isDirty = true;
        }
        return isDirty;
    }
}
