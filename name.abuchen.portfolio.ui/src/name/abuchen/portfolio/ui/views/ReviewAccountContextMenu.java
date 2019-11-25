package name.abuchen.portfolio.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;


import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.wizards.datatransfer.ExtractedEntry;


public class ReviewAccountContextMenu
{
    public interface IApplyAccount {
        public void execute(Extractor.Item item, Account account);
    }
    
    public void menuAboutToShow(IMenuManager parent, String label, TableViewer tableViewer, Client client, IApplyAccount applier)
    {
        IMenuManager manager = new MenuManager(label);
        parent.add(manager);

        for (Account acc : client.getAccounts()) {
            manager.add(new Action(acc.getName())
            {
                @Override
                public void run()
                {
                    for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                        applier.execute(((ExtractedEntry) element).getItem(), acc);

                    tableViewer.refresh();
                }
            });
        }
    }
}
