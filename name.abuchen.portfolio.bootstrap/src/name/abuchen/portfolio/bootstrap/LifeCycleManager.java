package name.abuchen.portfolio.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

@SuppressWarnings("restriction")
public class LifeCycleManager
{
    @PostContextCreate
    public void beforeCreation()
    {
        // if the java version is < 8, show a message dialog because otherwise
        // the application would silently not start

        double version = Double.parseDouble(System.getProperty("java.specification.version")); //$NON-NLS-1$

        if (version < 1.8)
        {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.TitleJavaVersion,
                            Messages.MsgMinimumRequiredVersion);
            throw new UnsupportedOperationException("The minimum Java version required is Java 8"); //$NON-NLS-1$
        }
    }

    @PreSave
    public void removePortfolioPartsWithoutPersistedFile(MApplication app, EPartService partService,
                    EModelService modelService)
    {
        MPartStack stack = (MPartStack) modelService.find("name.abuchen.portfolio.ui.partstack.main", app); //$NON-NLS-1$

        List<MStackElement> toBeRemoved = new ArrayList<MStackElement>();

        for (MStackElement child : stack.getChildren())
        {
            if (!(child instanceof MPart))
                continue;

            if (!"name.abuchen.portfolio.ui.part.portfolio".equals(child.getElementId())) //$NON-NLS-1$
                continue;

            String filename = child.getPersistedState().get("file"); //$NON-NLS-1$
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
