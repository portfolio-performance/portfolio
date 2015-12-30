package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
    private String message;

    public static void attach(Control control, String message)
    {
        InfoToolTip tooltip = new InfoToolTip(control, message);
        tooltip.setPopupDelay(0);
        tooltip.activate();
    }

    private InfoToolTip(Control control, String message)
    {
        super(control, ToolTip.NO_RECREATE, false);
        this.control = control;
        this.message = message;
    }

    @Override
    protected Composite createToolTipContentArea(Event event, Composite parent)
    {
        Composite result = new Composite(parent, SWT.NONE);
        Color background = new Color(result.getDisplay(), Colors.INFO_TOOLTIP_BACKGROUND.swt());
        result.addDisposeListener(e -> background.dispose());

        result.setBackground(background);
        result.setLayout(new GridLayout());

        Text text = new Text(result, SWT.WRAP);
        text.setBackground(background);
        text.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        text.setText(message);
        GridData gridData = new GridData();
        gridData.widthHint = control.getSize().x * 2;
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
