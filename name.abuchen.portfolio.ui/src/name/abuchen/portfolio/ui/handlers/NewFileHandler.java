package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.ui.ClientEditorInput;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.NewClientWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroManager;

public class NewFileHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        
        try
        {
            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
            IWorkbenchPage page = window.getActivePage();
            
            NewClientWizard wizard = new NewClientWizard();
            WizardDialog dialog = new WizardDialog(HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell(), wizard);
            int d = dialog.open();
//            int d = 1;
            if (d  == Window.OK) {
                System.out.println("df");
                page.openEditor(new ClientEditorInput(wizard.getClient()), "name.abuchen.portfolio.ui.editor"); //$NON-NLS-1$

                IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
                if (introManager.getIntro() != null)
                    introManager.closeIntro(introManager.getIntro());

            }
            
            return null;
        }
        catch (PartInitException e)
        {
            throw new ExecutionException(Messages.MsgErrorOpeningEditor, e);
        }
    }
}
