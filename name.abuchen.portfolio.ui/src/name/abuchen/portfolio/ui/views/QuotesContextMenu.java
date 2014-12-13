package name.abuchen.portfolio.ui.views;

import java.io.File;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UpdateQuotesJob;
import name.abuchen.portfolio.ui.dialogs.SecurityPriceDialog;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportQuotesWizard;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportWizard;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

public class QuotesContextMenu
{
    private AbstractFinanceView owner;

    public QuotesContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager parent, final Security security)
    {
        IMenuManager manager = new MenuManager(Messages.SecurityMenuQuotes);
        parent.add(manager);

        Action action = new Action(Messages.SecurityMenuUpdateQuotes)
        {
            @Override
            public void run()
            {
                new UpdateQuotesJob(owner.getClient(), security).schedule();
            }
        };
        // enable only if online updates are configured
        action.setEnabled(!QuoteFeed.MANUAL.equals(security.getFeed())
                        || (security.getLatestFeed() != null && !QuoteFeed.MANUAL.equals(security.getLatestFeed())));
        manager.add(action);

        manager.add(new Action(Messages.SecurityMenuConfigureOnlineUpdate)
        {
            @Override
            public void run()
            {
                EditSecurityDialog dialog = new EditSecurityDialog(Display.getDefault().getActiveShell(), owner
                                .getClient(), security);
                dialog.setShowQuoteConfigurationInitially(true);

                if (dialog.open() != Dialog.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new Separator());

        manager.add(new Action(Messages.SecurityMenuImportCSV)
        {
            @Override
            public void run()
            {
                FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
                fileDialog.setFilterNames(new String[] { Messages.CSVImportLabelFileCSV, Messages.CSVImportLabelFileAll });
                fileDialog.setFilterExtensions(new String[] { "*.csv", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
                String fileName = fileDialog.open();

                if (fileName == null)
                    return;

                ImportWizard wizard = new ImportWizard(owner.getClient(), new File(fileName));
                wizard.setTarget(security);
                Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);

                if (dialog.open() != Dialog.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new Action(Messages.SecurityMenuImportHTML)
        {
            @Override
            public void run()
            {
                Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(),
                                new ImportQuotesWizard(security));

                if (dialog.open() != Dialog.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });

        manager.add(new Action(Messages.SecurityMenuCreateManually)
        {
            @Override
            public void run()
            {
                Dialog dialog = new SecurityPriceDialog(Display.getDefault().getActiveShell(), owner.getClient(),
                                security);

                if (dialog.open() != Dialog.OK)
                    return;

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });
    }
}
