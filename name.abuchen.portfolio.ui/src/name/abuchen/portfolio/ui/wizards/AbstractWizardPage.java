package name.abuchen.portfolio.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;

public abstract class AbstractWizardPage extends WizardPage
{

    protected AbstractWizardPage(String pageName)
    {
        super(pageName);
    }

    public abstract void beforePage();

    public abstract void afterPage();

}
