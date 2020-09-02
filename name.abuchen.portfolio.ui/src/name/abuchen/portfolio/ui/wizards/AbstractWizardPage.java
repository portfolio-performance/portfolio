package name.abuchen.portfolio.ui.wizards;

import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;

public abstract class AbstractWizardPage extends WizardPage
{

    protected AbstractWizardPage(String pageName)
    {
        super(pageName);
    }

    public void beforePage()
    {
    }

    public void afterPage()
    {
    }

    public static final void attachPageListenerTo(final IWizardContainer c)
    {
        if (c instanceof IPageChangeProvider)
        {
            ((IPageChangeProvider) c).addPageChangedListener(new IPageChangedListener()
            {
                private AbstractWizardPage currentPage;

                @Override
                public void pageChanged(PageChangedEvent event)
                {
                    if (currentPage != null)
                        currentPage.afterPage();
                    currentPage = (AbstractWizardPage) event.getSelectedPage();
                    currentPage.beforePage();
                }
            });
        }

    }
}
