package name.abuchen.portfolio.ui.addons;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.ui.UIConstants;

/**
 * Keeps the main part stack usable when parts are moved between windows.
 * <p/>
 * The main part stack must never be detached and never collapse, otherwise the
 * main window is left without a stack into which parts can be dropped. But a
 * stack that does not collapse remains as an empty area if it is split, for
 * example when a part is dragged back from another window onto the empty main
 * stack. Therefore, whenever the main stack has no visible parts, the parts of
 * another stack of the main window are moved into the main stack.
 */
public class MainPartStackAddon
{
    @Inject
    private EModelService modelService;

    @Inject
    private MApplication application;

    private boolean isUpdateScheduled = false;

    @PostConstruct
    public void setup()
    {
        // the layout of existing users is merged from a copy of the previous
        // application model (see MergeOldLayoutIntoCurrentApplicationModel-
        // Processor). Tags declared below the window in Application.e4xmi do
        // not reach them, therefore add them at runtime.

        var mainStack = findMainStack();
        if (mainStack != null)
        {
            addTag(mainStack, IPresentationEngine.NO_DETACH);
            addTag(mainStack, IPresentationEngine.NO_AUTO_COLLAPSE);
        }

        modelService.findElements(application, UIConstants.Part.PORTFOLIO, MPart.class)
                        .forEach(part -> addTag(part, IPresentationEngine.NO_DETACH));

        // repair layouts that have been persisted with an empty main stack
        mergeIntoEmptyMainStack();
    }

    @Inject
    @Optional
    public void onChildrenChanged(@UIEventTopic(UIEvents.ElementContainer.TOPIC_CHILDREN) Event event)
    {
        var element = event.getProperty(UIEvents.EventTags.ELEMENT);
        if (element instanceof MPartStack || element instanceof MPartSashContainer || element instanceof MWindow)
            scheduleUpdate();
    }

    @Inject
    @Optional
    public void onToBeRenderedChanged(@UIEventTopic(UIEvents.UIElement.TOPIC_TOBERENDERED) Event event)
    {
        var element = event.getProperty(UIEvents.EventTags.ELEMENT);
        if (element instanceof MStackElement || element instanceof MPartStack)
            scheduleUpdate();
    }

    private void scheduleUpdate()
    {
        if (isUpdateScheduled)
            return;

        var display = Display.getCurrent();
        if (display == null)
            return;

        // wait until the model changes of the current operation (drop, close,
        // ...) are complete
        isUpdateScheduled = true;
        display.asyncExec(() -> {
            isUpdateScheduled = false;
            mergeIntoEmptyMainStack();
        });
    }

    private void mergeIntoEmptyMainStack()
    {
        var mainStack = findMainStack();
        if (mainStack == null || hasVisibleParts(mainStack))
            return;

        var window = getWindow(mainStack);
        if (window == null)
            return;

        // only consider stacks of the main window, not of the windows opened
        // via "Open in new window"
        var otherStack = modelService.findElements(window, null, MPartStack.class).stream()
                        .filter(stack -> stack != mainStack && getWindow(stack) == window)
                        .filter(this::hasVisibleParts) //
                        .findFirst().orElse(null);
        if (otherStack == null)
            return;

        var selected = otherStack.getSelectedElement();

        for (MStackElement element : List.copyOf(otherStack.getChildren()))
        {
            otherStack.getChildren().remove(element);
            mainStack.getChildren().add(element);
        }

        MElementContainer<MUIElement> parent = otherStack.getParent();
        otherStack.setToBeRendered(false);
        if (parent != null)
        {
            if (otherStack.equals(parent.getSelectedElement()))
                parent.setSelectedElement(null);
            parent.getChildren().remove(otherStack);
        }

        if (selected != null)
        {
            mainStack.setSelectedElement(selected);

            if (selected instanceof MPart part && window.getContext() != null)
                window.getContext().get(EPartService.class).activate(part);
        }
    }

    /**
     * Returns true if the stack shows at least one part. The error log part is
     * always a child of the main stack, but invisible until opened. Therefore
     * EModelService#countRenderableChildren, which ignores the visibility,
     * cannot be used.
     */
    private boolean hasVisibleParts(MPartStack stack)
    {
        return stack.getChildren().stream().anyMatch(element -> element.isToBeRendered() && element.isVisible());
    }

    private MPartStack findMainStack()
    {
        return modelService.find(UIConstants.PartStack.MAIN, application) instanceof MPartStack stack ? stack : null;
    }

    private MWindow getWindow(MUIElement element)
    {
        MUIElement parent = element.getParent();
        while (parent != null && !(parent instanceof MWindow))
            parent = parent.getParent();
        return (MWindow) parent;
    }

    private void addTag(MUIElement element, String tag)
    {
        if (!element.getTags().contains(tag))
            element.getTags().add(tag);
    }
}
