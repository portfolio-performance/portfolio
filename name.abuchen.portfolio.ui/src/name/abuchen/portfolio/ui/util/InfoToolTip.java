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
        Composite result = new Composite(parent, SWT.NONE);

        result.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);
        result.setLayout(new GridLayout());

        // create tool tip with a reasonable width
        int width = SWTHelper.stringWidth(result, "ABCDEFGHIJK") * 5; //$NON-NLS-1$

        Text text = new Text(result, SWT.WRAP);
        text.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);
        text.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        text.setText(message.get());
        GridData gridData = new GridData();
        gridData.widthHint = width;
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
