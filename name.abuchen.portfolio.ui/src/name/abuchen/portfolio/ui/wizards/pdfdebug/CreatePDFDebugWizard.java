package name.abuchen.portfolio.ui.wizards.pdfdebug;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.UnrecognizedPDFCache;

public class CreatePDFDebugWizard extends Wizard
{
    private final UnrecognizedPDFCache cache;

    public CreatePDFDebugWizard(UnrecognizedPDFCache cache)
    {
        this.cache = cache;
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        var sourcePage = new SelectPDFDebugSourcePage(cache);
        addPage(sourcePage);
        addPage(new AnonymizePDFDebugPage(sourcePage));
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }
}
