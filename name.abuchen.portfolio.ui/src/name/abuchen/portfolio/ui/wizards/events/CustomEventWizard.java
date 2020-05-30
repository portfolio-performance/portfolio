package name.abuchen.portfolio.ui.wizards.events;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class CustomEventWizard extends Wizard
{
    private CustomEventModel model;

    public CustomEventWizard(Client client, Security security)
    {
        this.model = new CustomEventModel(client, security);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        addPage(new AddCustomEventPage(model));

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        model.applyChanges();
        return true;
    }

}
