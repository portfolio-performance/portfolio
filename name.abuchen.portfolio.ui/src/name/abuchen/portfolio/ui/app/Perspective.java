package name.abuchen.portfolio.ui.app;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

public class Perspective implements IPerspectiveFactory
{

    public void createInitialLayout(IPageLayout layout)
    {
        String editorArea = layout.getEditorArea();
        IPlaceholderFolderLayout bottomMiddle = layout.createPlaceholderFolder(
                        "bottomMiddle", IPageLayout.BOTTOM, (float) 0.70, editorArea);//$NON-NLS-1$
        bottomMiddle.addPlaceholder("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
    }
}
