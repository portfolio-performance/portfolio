package name.abuchen.portfolio.ui.views.dashboard;

import name.abuchen.portfolio.ui.util.Colors;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

final class DashboardResources
{
    private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

    private Color headingColor;
    private Font kpiFont;
    private Font boldFont;
    private Font smallFont;

    public DashboardResources(Composite container)
    {
        kpiFont = resourceManager.createFont(FontDescriptor.createFrom(container.getFont()).setStyle(SWT.NORMAL)
                        .increaseHeight(10));

        boldFont = resourceManager.createFont(FontDescriptor.createFrom(
                        JFaceResources.getFont(JFaceResources.HEADER_FONT)).setStyle(SWT.BOLD));

        smallFont = resourceManager.createFont(FontDescriptor.createFrom(container.getFont()).increaseHeight(-2));

        headingColor = resourceManager.createColor(Colors.HEADINGS.swt());

        container.addDisposeListener(e -> resourceManager.dispose());
    }

    public LocalResourceManager getResourceManager()
    {
        return resourceManager;
    }

    public Font getSmallFont()
    {
        return smallFont;
    }

    public Color getHeadingColor()
    {
        return headingColor;
    }

    public Font getKpiFont()
    {
        return kpiFont;
    }

    public Font getBoldFont()
    {
        return boldFont;
    }
}
