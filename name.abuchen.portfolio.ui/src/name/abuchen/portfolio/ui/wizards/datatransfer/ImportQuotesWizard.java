package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

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
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        importPage = new ImportSourcePage(security);
        addPage(importPage);
        reviewPage = new ReviewImportedQuotesPage(importPage);
        addPage(reviewPage);

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        List<LatestSecurityPrice> quotes = reviewPage.getQuotes();

        for (LatestSecurityPrice p : quotes)
        {
            SecurityPrice quote = new SecurityPrice(p.getDate(), p.getValue());
            security.addPrice(quote);
        }

        return true;
    }

}
