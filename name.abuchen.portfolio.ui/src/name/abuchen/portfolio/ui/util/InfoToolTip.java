package name.abuchen.portfolio.ui.util;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.UIConstants;

public final class InfoToolTip extends ToolTip
{
    private Control control;
    private Supplier<String> message;

    private InfoToolTip(Control control, Supplier<String> message)
    {
        super(control, ToolTip.NO_RECREATE, false);
        this.control = control;
        this.message = message;
    }

    public static void attach(Control control, String message)
    {
        attach(control, () -> message);
    }

    public static void attach(Control control, Supplier<String> message)
    {
        InfoToolTip tooltip = new InfoToolTip(control, message);
        tooltip.setPopupDelay(0);
        tooltip.activate();
    }

    @Override
    protected Composite createToolTipContentArea(Event event, Composite parent)
    {
        parent.setData(UIConstants.CSS.CLASS_NAME, "tooltip"); //$NON-NLS-1$

        Composite result = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        result.setLayout(layout);

        // create tool tip with a reasonable width
        int maximumWidth = SWTHelper.stringWidth(result, "ABCDEFGHIJK") * 5; //$NON-NLS-1$
        int actualWidth = SWTHelper.stringWidth(result, message.get()) + layout.marginWidth * 2;
        int widthHint = Math.min(maximumWidth, actualWidth);

        Text text = new Text(result, SWT.WRAP);
        text.setText(message.get());
        GridData gridData = new GridData();
        gridData.widthHint = widthHint;
        text.setLayoutData(gridData);
        Dialog.applyDialogFont(result);
        return result;
    }

    @Override
    public Point getLocation(Point tipSize, Event event)
    {
        Point location = control.getLocation();
        return control.getParent().toDisplay(location.x, location.y + control.getSize().y + 5);
    }
}
