package name.abuchen.portfolio.ui.wizards;

import java.util.List;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

import org.eclipse.jface.wizard.Wizard;

public class ImportQuotesWizard extends Wizard
{
    private final Security security;

    private ImportSourcePage importPage;
    private ReviewImportedQuotesPage reviewPage;

    public ImportQuotesWizard(Security security)
    {
        this.security = security;
    }

    @Override
    public void addPages()
    {
        importPage = new ImportSourcePage(security);
        addPage(importPage);
        reviewPage = new ReviewImportedQuotesPage(importPage, security);
        addPage(reviewPage);

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        List<LatestSecurityPrice> quotes = reviewPage.getQuotes();

        for (LatestSecurityPrice p : quotes)
        {
            SecurityPrice quote = new SecurityPrice(p.getTime(), p.getValue());
            security.addPrice(quote);
        }

        return true;
    }

}
