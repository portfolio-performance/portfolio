package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class FocusSearchBarHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(UIConstants.Parameter.TYPE) String type)
    {
        if (!MenuHelper.isClientPartActive(part))
            return;

        // If there is no active view, then the client is either locked
        // (password not provided), loading, or failed to load. In these cases
        // we do not show a command palette at all.

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        final java.util.Optional<AbstractFinanceView> currentView = portfolioPart.getCurrentView();

        if (currentView.isPresent())
            findAndFocusSearchField(currentView.get());

    }

    private void findAndFocusSearchField(AbstractFinanceView view)
    {
        // first: try to find a search box in the upper tool bar

        if (findAndFocus(view.getToolBarManager()))
            return;

        // second: try to find a search box in the information pane (but only if
        // the pane is visible at the moment)

        if (!view.isPaneHidden())
        {
            var informationPane = view.getInformationPane();
            if (informationPane.isPresent())
            {
                findAndFocus(informationPane.get().getControlsToolBar());
            }
        }
    }

    private boolean findAndFocus(ToolBarManager toolBar)
    {
        if (toolBar != null)
        {
            for (ToolItem toolItem : toolBar.getControl().getItems())
            {
                // if the item already has focus, then do nothing. If there is
                // another item (for example in the information pane), then this
                // one will get the focus
                if (toolItem.getControl() instanceof Text && !toolItem.getControl().isFocusControl())
                {
                    toolItem.getControl().setFocus();
                    return true;
                }
            }
        }

        return false;
    }

}
