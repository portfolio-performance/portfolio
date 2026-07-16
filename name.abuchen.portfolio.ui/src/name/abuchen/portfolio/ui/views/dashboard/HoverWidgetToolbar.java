package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public class HoverWidgetToolbar
{
    private final Control leadingControl;
    private final Menu menu;

    private IHyperlinkListener viewListener;

    private Shell hoverShell;
    private long timestamp;

    public static final String VIEW_LISTENER_KEY = HoverWidgetToolbar.class.getName() + ".viewListener"; //$NON-NLS-1$

    public static void attachViewListener(Control control, IHyperlinkListener viewListener)
    {
        control.setData(VIEW_LISTENER_KEY, viewListener);
    }

    private HoverWidgetToolbar(Control leadingControl, Menu menu)
    {
        this.leadingControl = leadingControl;
        this.menu = menu;

        hookMouseListeners(leadingControl);

        if (leadingControl instanceof Composite)
            hookMouseListenersRecursively((Composite) leadingControl);

        this.leadingControl.addDisposeListener(e -> {
            if (hoverShell != null && !hoverShell.isDisposed())
                hoverShell.dispose();
        });
    }

    public static HoverWidgetToolbar build(Control leadingControl, Menu menu)
    {
        return new HoverWidgetToolbar(leadingControl, menu);
    }

    public HoverWidgetToolbar withViewListener(IHyperlinkListener viewListener)
    {
        this.viewListener = viewListener;
        return this;
    }

    private void hookMouseListenersRecursively(Composite composite)
    {
        for (Control child : composite.getChildren())
        {
            hookMouseListeners(child);

            if (child instanceof Composite)
                hookMouseListenersRecursively((Composite) child);
        }
    }

    private void hookMouseListeners(Control control)
    {
        control.addListener(SWT.MouseEnter, this::onMouseEnter);
        control.addListener(SWT.MouseExit, this::onMouseExit);
    }

    private void onMouseExit(Event event)
    {
        if (hoverShell != null && hoverShell.isVisible())
        {
            timestamp = event.time;

            event.display.timerExec(200, () -> {
                if (timestamp == event.time && hoverShell != null && !hoverShell.isDisposed() && hoverShell.isVisible())
                    hoverShell.setVisible(false);
            });
        }
    }

    private void onMouseEnter(Event event)
    {
        timestamp = 0;

        if (hoverShell == null || hoverShell.isDisposed())
            createHoverShell();

        if (!hoverShell.isVisible())
        {
            Rectangle controlBounds = leadingControl.getBounds();
            Rectangle hoverBounds = hoverShell.getBounds();

            int locationX = controlBounds.x + controlBounds.width - hoverBounds.width;
            hoverShell.setLocation(leadingControl.toDisplay(locationX, 0));

            hoverShell.setVisible(true);
        }
    }

    private void createHoverShell()
    {
        hoverShell = new Shell(leadingControl.getShell(), SWT.MODELESS | SWT.SHADOW_OUT);
        hoverShell.setData(UIConstants.CSS.CLASS_NAME, "hoverbutton"); //$NON-NLS-1$
        hoverShell.setBackground(Colors.theme().defaultBackground());

        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.spacing = 0;
        hoverShell.setLayout(layout);

        if (viewListener != null)
            createButton(Images.VIEW_SHARE, null, viewListener);

        createButton(Images.EDIT, Messages.LabelSettings,
                        IHyperlinkListener.linkActivatedAdapter(e -> showMenu()));

        hoverShell.pack();

        hoverShell.addListener(SWT.MouseEnter, this::onMouseEnter);
        hoverShell.addListener(SWT.MouseExit, this::onMouseExit);
    }

    private void createButton(Images image, String tooltip, IHyperlinkListener listener)
    {
        ImageHyperlink button = new ImageHyperlink(hoverShell, SWT.NONE);
        button.setImage(image.image());
        button.setData(UIConstants.CSS.CLASS_NAME, "hoverbutton"); //$NON-NLS-1$
        button.setBackground(Colors.theme().defaultBackground());

        if (tooltip != null)
            button.setToolTipText(tooltip);

        button.addHyperlinkListener(listener);
        button.addListener(SWT.MouseEnter, this::onMouseEnter);
        button.addListener(SWT.MouseExit, this::onMouseExit);
    }

    private void showMenu()
    {
        if (menu == null || menu.isDisposed())
            return;

        Point location = hoverShell.toDisplay(0, hoverShell.getSize().y);
        menu.setLocation(location);
        menu.setVisible(true);
    }
}