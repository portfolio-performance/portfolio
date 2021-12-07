package name.abuchen.portfolio.ui.handlers;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.swt.SashLayout;

public class ShowHideSidebarHandler
{
    @CanExecute
    boolean canExecute(@Named(IServiceConstants.ACTIVE_PART) MPart part, MMenuItem menuItem,
                    @Named(UIConstants.Parameter.TAG) String tag)
    {
        Optional<SashLayout> sash = findChildWithSash(part, tag);
        if (!sash.isPresent())
            return false;

        if (UIConstants.Tag.INFORMATIONPANE.equals(tag))
            menuItem.setLabel(sash.get().isHidden() ? Messages.MenuShowInformationPane : Messages.MenuHideInformationPane);
        else
            menuItem.setLabel(sash.get().isHidden() ? Messages.MenuShowSidebar : Messages.MenuHideSidebar);

        return true;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, @Named(UIConstants.Parameter.TAG) String tag)
    {
        findChildWithSash(part, tag).ifPresent(SashLayout::flip);
    }

    private Optional<SashLayout> findChildWithSash(MPart part, String tag)
    {
        if (!MenuHelper.getActiveClientInput(part, false).isPresent())
            return Optional.empty();

        Object widget = part.getWidget();
        if (!(widget instanceof Composite))
            return Optional.empty();

        LinkedList<Composite> stack = new LinkedList<>();
        stack.add((Composite) widget);

        while (!stack.isEmpty())
        {
            Composite subject = stack.removeFirst();

            Layout layout = subject.getLayout();

            if (layout instanceof SashLayout && Objects.equals(((SashLayout) layout).getTag(), tag))
                return Optional.of((SashLayout) layout);

            for (Control control : subject.getChildren())
            {
                if (control instanceof Composite)
                    stack.add((Composite) control);
            }
        }

        return Optional.empty();
    }
}
