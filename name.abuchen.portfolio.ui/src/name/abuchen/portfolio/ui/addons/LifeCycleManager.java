package name.abuchen.portfolio.ui.addons;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.ui.UIConstants;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class LifeCycleManager
{
    @PreSave
    public void removePortfolioPartsWithoutPersistedFile(MApplication app, EPartService partService, EModelService modelService)
    {
        MPartStack stack = (MPartStack) modelService.find(UIConstants.PartStack.MAIN, app);

        List<MStackElement> toBeRemoved = new ArrayList<MStackElement>();

        for (MStackElement child : stack.getChildren())
        {
            if (!(child instanceof MPart))
                continue;

            if (!UIConstants.Part.PORTFOLIO.equals(child.getElementId()))
                continue;

            String filename = child.getPersistedState().get(UIConstants.Parameter.FILE);
            if (filename == null)
                toBeRemoved.add(child);
        }

        if (!toBeRemoved.isEmpty())
        {
            if (toBeRemoved.contains(stack.getSelectedElement()))
                stack.setSelectedElement(null);
            stack.getChildren().removeAll(toBeRemoved);
        }
    }
}
