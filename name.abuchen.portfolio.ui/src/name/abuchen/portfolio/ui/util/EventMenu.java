package name.abuchen.portfolio.ui.util;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.Type;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.wizards.events.SecurityEventWizard;

public class EventMenu extends MenuManager
{
    private final Security security;
    private final AbstractFinanceView view;

    public EventMenu(AbstractFinanceView view, Security security)
    {
        super(Messages.SecurityMenuAddEvent);
        
        this.view = view;
        this.security = security;

        addDefaultPages();
    }

    private void addDefaultPages()
    {
        List<Type> supportedTypes = stream(SecurityEvent.Type.values())
                .filter(SecurityEvent.Type::isUserEditable)
                .collect(toUnmodifiableList());
        
        for (SecurityEvent.Type type : supportedTypes)
        {
            add(new Action(type.toString() + "...") //$NON-NLS-1$
            {
                @Override
                public void run()
                {
                    SecurityEventWizard wizard = new SecurityEventWizard(view.getClient(), security, type);
                    WizardDialog dialog = new WizardDialog(view.getActiveShell(), wizard);
                    if (dialog.open() == Window.OK)
                    {
                        view.markDirty();
                        view.notifyModelUpdated();
                    }
                }
            });
        }
    }

}
