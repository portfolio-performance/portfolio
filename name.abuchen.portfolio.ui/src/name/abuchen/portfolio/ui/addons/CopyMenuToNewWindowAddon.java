package name.abuchen.portfolio.ui.addons;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osgi.service.event.Event;

public class CopyMenuToNewWindowAddon
{
    @Inject
    @Optional
    public void onEvent(@UIEventTopic(UIEvents.Window.TOPIC_ALL) Event event)
    {
        if (UIEvents.isADD(event))
        {
            MTrimmedWindow origin = (MTrimmedWindow) event.getProperty(EventTags.ELEMENT);
            MTrimmedWindow window = (MTrimmedWindow) event.getProperty(EventTags.NEW_VALUE);

            MMenu mainMenu = (MMenu) EcoreUtil.copy((EObject) origin.getMainMenu());
            mainMenu.setVisible(true);

            window.setMainMenu(mainMenu);
        }
    }
}
