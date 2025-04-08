package name.abuchen.portfolio.bootstrap.swt;

import java.util.function.Consumer;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import name.abuchen.portfolio.bootstrap.Messages;

@SuppressWarnings("restriction")
public class CustomStackRenderer extends StackRenderer
{
    private static final String SELECTED_PART = "name.abuchen.portfolio.selectedPart"; //$NON-NLS-1$

    
    
    @Override
    public Object createWidget(MUIElement element, Object parent)
    {
        CTabFolder tabFolder = (CTabFolder)super.createWidget(element, parent);
        
        // remove the wrap alignment as the tool bar should never to into the second row
        tabFolder.setTopRight(tabFolder.getTopRight(), SWT.RIGHT);
        
        return tabFolder;
    }

    @Override
    protected void populateTabMenu(Menu menu, MPart part)
    {
        menu.setData(SELECTED_PART, part);

        if (isClosable(part))
            doCreateMenuItem(menu, Messages.LabelCloseWindow, e -> closeSelectedPart(menu));

        if (isCloneable(part))
            doCreateMenuItem(menu, Messages.LabelCloneWindow, e -> cloneSelectedPart(menu));
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

        MPart part = partService.createPart(selectedPart.getElementId());
        part.setLabel(selectedPart.getLabel());

        selectedPart.getTransientData().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("name.abuchen.portfolio.")) //$NON-NLS-1$
                        .forEach(entry -> part.getTransientData().put(entry.getKey(), entry.getValue()));

        part.getTransientData().putAll(selectedPart.getTransientData());

        selectedPart.getParent().getChildren().add(part);

        part.setVisible(true);
        part.getParent().setVisible(true);
        partService.showPart(part, PartState.ACTIVATE);
    }

    protected boolean isCloneable(MPart part)
    {
        return part.getTags().contains("Cloneable"); //$NON-NLS-1$
    }
}
