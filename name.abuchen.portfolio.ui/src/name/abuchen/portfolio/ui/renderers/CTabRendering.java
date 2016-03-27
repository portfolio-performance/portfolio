package name.abuchen.portfolio.ui.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class CTabRendering extends CTabFolderRenderer
{
    public CTabRendering(CTabFolder parent)
    {
        super(parent);
    }

    @Override
    protected Point computeSize(int part, int state, GC gc, int wHint, int hHint)
    {
        if (part != PART_HEADER)
            return super.computeSize(part, state, gc, wHint, hHint);

        int height = gc.textExtent("Default", SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC).y + 12; //$NON-NLS-1$

        Rectangle trim = computeTrim(part, state, 0, 0, 0, height);
        return new Point(trim.width, trim.height);
    }
}
