package name.abuchen.portfolio.bootstrap.swt;

import java.util.function.Consumer;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import name.abuchen.portfolio.bootstrap.Messages;

@SuppressWarnings("restriction")
public class CustomStackRenderer extends StackRenderer
{
    private static final String SELECTED_PART = "name.abuchen.portfolio.selectedPart"; //$NON-NLS-1$
    private static final int NEW_WINDOW_OFFSET = 30;

    @Override
    public Object createWidget(MUIElement element, Object parent)
    {
        var tabFolder = (CTabFolder) super.createWidget(element, parent);

        preventWrappingOfTopRight(tabFolder);

        return tabFolder;
    }

    /**
     * Keeps the tool bar in the top right corner out of a second row. The
     * renderer aligns the corner with SWT.WRAP and lays it out with a RowLayout
     * that wraps by default. Both let the tool bar jump into an additional row
     * while the tab folder is being laid out. On Linux that extra row remains
     * visible for a moment before it collapses again.
     */
    private void preventWrappingOfTopRight(CTabFolder tabFolder)
    {
        var topRight = tabFolder.getTopRight();
        if (topRight == null || topRight.isDisposed())
            return;

        if (topRight instanceof Composite composite && composite.getLayout() instanceof RowLayout layout && layout.wrap)
        {
            layout.wrap = false;
            composite.requestLayout();
        }

        tabFolder.setTopRight(topRight, SWT.RIGHT);
    }

    @Override
    protected void populateTabMenu(Menu menu, MPart part)
    {
        menu.setData(SELECTED_PART, part);

        if (isClosable(part))
            doCreateMenuItem(menu, Messages.LabelCloseWindow, e -> closeSelectedPart(menu));

        if (isCloneable(part))
        {
            doCreateMenuItem(menu, Messages.LabelCloneWindow, e -> cloneSelectedPart(menu));
            doCreateMenuItem(menu, Messages.LabelOpenInNewWindow, e -> openSelectedPartInNewWindow(menu));
        }
    }

    private MenuItem doCreateMenuItem(final Menu menu, String menuItemText, Consumer<SelectionEvent> c)
    {
        MenuItem menuItem = new MenuItem(menu, SWT.NONE);
        menuItem.setText(menuItemText);
        menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(c));
        return menuItem;

    }

    private void closeSelectedPart(final Menu menu)
    {
        MPart selectedPart = (MPart) menu.getData(SELECTED_PART);
        EPartService partService = getContextForParent(selectedPart).get(EPartService.class);
        if (partService.savePart(selectedPart, true))
            partService.hidePart(selectedPart);
    }

    private void cloneSelectedPart(final Menu menu)
    {
        MPart selectedPart = (MPart) menu.getData(SELECTED_PART);

        EPartService partService = getContextForParent(selectedPart).get(EPartService.class);

        MPart part = createClone(selectedPart, partService);

        selectedPart.getParent().getChildren().add(part);

        part.setVisible(true);
        part.getParent().setVisible(true);
        partService.showPart(part, PartState.ACTIVATE);
    }

    /**
     * Opens a clone of the selected part in a new window. Detaching via drag
     * and drop is disabled (NoDetach) because it too often happened by accident
     * and could leave the main window without any part stack. Cloning leaves
     * the selected part where it is.
     */
    private void openSelectedPartInNewWindow(final Menu menu)
    {
        MPart selectedPart = (MPart) menu.getData(SELECTED_PART);

        var context = getContextForParent(selectedPart);
        var partService = context.get(EPartService.class);
        var modelService = context.get(EModelService.class);

        // the part might have been removed from the model in the meantime
        var topLevelWindow = modelService.getTopLevelWindowFor(selectedPart);
        if (topLevelWindow == null)
            return;

        MPart part = createClone(selectedPart, partService);

        MPartStack stack = modelService.createModelElement(MPartStack.class);
        // prevent the stack of the new window from being dragged out again
        stack.getTags().add(IPresentationEngine.NO_DETACH);
        stack.getChildren().add(part);
        stack.setSelectedElement(part);

        MTrimmedWindow window = modelService.createModelElement(MTrimmedWindow.class);
        window.getChildren().add(stack);
        window.setSelectedElement(stack);

        // offset the new window so that it does not hide the original one
        var tabItem = findItemForPart(selectedPart);
        if (tabItem != null && !tabItem.getParent().isDisposed())
        {
            var tabFolder = tabItem.getParent();
            var size = tabFolder.getSize();
            var location = tabFolder.toDisplay(0, 0);
            window.setX(location.x + NEW_WINDOW_OFFSET);
            window.setY(location.y + NEW_WINDOW_OFFSET);
            window.setWidth(size.x);
            window.setHeight(size.y);
        }

        topLevelWindow.getWindows().add(window);

        if (window.getContext() != null)
            window.getContext().get(EPartService.class).activate(part);
    }

    private MPart createClone(MPart selectedPart, EPartService partService)
    {
        MPart part = partService.createPart(selectedPart.getElementId());
        part.setLabel(selectedPart.getLabel());

        selectedPart.getTransientData().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("name.abuchen.portfolio.")) //$NON-NLS-1$
                        .forEach(entry -> part.getTransientData().put(entry.getKey(), entry.getValue()));

        part.getTransientData().putAll(selectedPart.getTransientData());

        return part;
    }

    protected boolean isCloneable(MPart part)
    {
        return part.getTags().contains("Cloneable"); //$NON-NLS-1$
    }
}
